"""Reusable client-only Windows capture: 180s JFR/frame/GPU, then heap, then offline MAT.
No Minecraft/server launch, stop, settings write, or forced GC inside the timed trace.
Personal tool paths live in INSTANCE/ClientProfiler/settings.local.json, outside Git.
"""
from pathlib import Path
import argparse,base64,csv,ctypes,datetime as dt,gzip,hashlib,json,os,re,shutil,subprocess,sys,threading,time,uuid,xml.etree.ElementTree as ET,zipfile
from ctypes import wintypes as wt

SOURCE=Path(__file__).resolve().parent
FLAGS=getattr(subprocess,'CREATE_NO_WINDOW',0)
KERNEL=ctypes.WinDLL('kernel32',use_last_error=True) if os.name=='nt' else None
if KERNEL:
 KERNEL.OpenProcess.argtypes=[wt.DWORD,wt.BOOL,wt.DWORD];KERNEL.OpenProcess.restype=wt.HANDLE
 KERNEL.CloseHandle.argtypes=[wt.HANDLE];KERNEL.GetExitCodeProcess.argtypes=[wt.HANDLE,ctypes.POINTER(wt.DWORD)]

def stamp():return dt.datetime.now(dt.timezone.utc).isoformat()
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def jcpath(path):
 # jcmd has its own command parser in addition to Windows argv parsing.
 value=Path(path).resolve().as_posix();assert '"' not in value;return '"'+value+'"'
def write(path,value):
 path=Path(path);path.parent.mkdir(parents=True,exist_ok=True);temp=path.with_name(path.name+'.pending');temp.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n','utf8')
 for attempt in range(20):
  try:os.replace(temp,path);return
  except PermissionError:
   if attempt==19:raise
   time.sleep(.025)
def command(args,cwd=None,timeout=120):return subprocess.run(list(map(str,args)),cwd=cwd,capture_output=True,text=True,encoding='utf8',errors='replace',creationflags=FLAGS,timeout=timeout)
def alive(handle):
 code=wt.DWORD();return bool(handle and KERNEL.GetExitCodeProcess(handle,ctypes.byref(code)) and code.value==259)
def processes():
 script="[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new($false); Get-CimInstance Win32_Process -Filter \"Name='javaw.exe' OR Name='java.exe'\" | Select-Object ProcessId,ExecutablePath,CommandLine | ConvertTo-Json -Compress"
 reply=command(['powershell.exe','-NoProfile','-EncodedCommand',base64.b64encode(script.encode('utf-16le')).decode()]);assert reply.returncode==0,reply.stderr
 raw=reply.stdout.strip().lstrip('\ufeff');data=json.loads(raw) if raw else [];return data if isinstance(data,list) else [data]
def target(rows,instance,pid=None,fixture_nonce=None):
 root=str(instance.resolve()).replace('\\','/').casefold();found=[]
 for row in rows:
  text=(row.get('CommandLine') or '').replace('\\','/');lower=text.casefold()
  # Prism's actual JVM command may identify the instance only through the
  # exact java.library.path property, while the game directory is its cwd.
  native=re.search(r'(?:^|\s)(?:"-Djava\.library\.path=([^"]+)"|-Djava\.library\.path=(?:"([^"]+)"|(\S+)))',text,re.IGNORECASE)
  native_path=next((v for v in native.groups() if v is not None),'').rstrip('/').casefold() if native else ''
  game_path=any(re.search(re.escape(root+suffix)+r'(?=$|[/"\s;])',lower) for suffix in ['/minecraft','/.minecraft'])
  valid='org.prismlauncher.EntryPoint' in text and (native_path==root+'/natives' or game_path)
  if fixture_nonce is not None:valid='ClientProfileFixture' in text and fixture_nonce in text and pid==row['ProcessId']
  if valid and (pid is None or pid==row['ProcessId']):found.append(row)
 if len(found)!=1:raise RuntimeError('Could not identify exactly one client for this instance; start it in Prism and wait at the server list. Matching PIDs: '+str([r['ProcessId'] for r in found]))
 return found[0]
def current(instance):
 path=instance/'ClientProfiler/latest.json';return Path(json.loads(path.read_text('utf8'))['run']) if path.exists() else None
def settings(instance):
 p=instance/'ClientProfiler/settings.local.json';return json.loads(p.read_text('utf8')) if p.exists() else {}
def activation(instance,build):
 path=instance/'ClientProfiler/policy.json';policy=json.loads(path.read_text('utf8')).get('clientProfiling',{}) if path.exists() else {}
 expected=list(policy.get('requiredOptimizationActivationMessages',[]))
 for rule in policy.get('activationByBuild',[]):
  if (build or 0)>=rule['minimumBuild']:expected.extend(rule['messages'])
 for rule in policy.get('retiredActivationByBuild',[]):
  if (build or 0)>=rule['minimumBuild']:expected=[m for m in expected if m not in rule['messages']]
 deferred=set(policy.get('deferredActivationMessages',[]));log=instance/'minecraft/logs/latest.log';text=log.read_text('utf8',errors='replace') if log.exists() else ''
 return {'verified':[m for m in expected if m in text],'missing':[m for m in expected if m not in text and m not in deferred],'deferred':[m for m in expected if m not in text and m in deferred]}
def lease(path,payload):
 try:
  with path.open('x',encoding='utf8') as out:json.dump(payload,out)
  return True
 except FileExistsError:
  old=json.loads(path.read_text('utf8'));handle=KERNEL.OpenProcess(0x1000,False,int(old['ownerPid']))
  try:running=alive(handle)
  finally:
   if handle:KERNEL.CloseHandle(handle)
  if running:return False
  if json.loads(path.read_text('utf8'))==old:path.unlink()
  return lease(path,payload)
def release_lease(path,payload):
 if path.exists() and json.loads(path.read_text('utf8'))==payload:path.unlink()
def qpc():
 counter=ctypes.c_longlong();freq=ctypes.c_longlong();KERNEL.QueryPerformanceFrequency(ctypes.byref(freq));KERNEL.QueryPerformanceCounter(ctypes.byref(counter))
 return {'utc':stamp(),'qpc':counter.value,'frequency':freq.value}

class Counters(ctypes.Structure):
 _fields_=[('cb',wt.DWORD),('PageFaultCount',wt.DWORD)]+[(n,ctypes.c_size_t) for n in ['PeakWorkingSetSize','WorkingSetSize','QuotaPeakPagedPoolUsage','QuotaPagedPoolUsage','QuotaPeakNonPagedPoolUsage','QuotaNonPagedPoolUsage','PagefileUsage','PeakPagefileUsage','PrivateUsage']]

def sample(handle,out,done):
 psapi=ctypes.WinDLL('psapi',use_last_error=True);psapi.GetProcessMemoryInfo.argtypes=[wt.HANDLE,ctypes.POINTER(Counters),wt.DWORD]
 with out.open('w',encoding='utf8') as stream:
  while not done.is_set() and alive(handle):
   values=Counters();values.cb=ctypes.sizeof(values)
   if not psapi.GetProcessMemoryInfo(handle,ctypes.byref(values),values.cb):break
   stream.write(json.dumps({'utc':stamp(),'workingSetBytes':values.WorkingSetSize,'privateCommitBytes':values.PrivateUsage,'pageFaults':values.PageFaultCount})+'\n');stream.flush();done.wait(5)

def profile_settings(java,output):
 tree=ET.parse(java/'lib/jfr/profile.jfc');root=tree.getroot()
 edits={'jdk.ExecutionSample':{'enabled':'true','period':'10 ms'},'jdk.NativeMethodSample':{'enabled':'true','period':'10 ms'},'jdk.ObjectAllocationSample':{'enabled':'true','throttle':'100/s','stackTrace':'true'},'jdk.DirectBufferStatistics':{'enabled':'true','period':'5 s'},'jdk.ThreadAllocationStatistics':{'enabled':'true','period':'5 s'}}
 for event in ['jdk.ThreadPark','jdk.JavaMonitorEnter','jdk.FileRead','jdk.FileWrite']:edits[event]={'enabled':'true','threshold':'10 ms','stackTrace':'true'}
 for event,values in edits.items():
  element=next((e for e in root.findall('event') if e.get('name')==event),None)
  if element is None:element=ET.SubElement(root,'event',{'name':event})
  for key,value in values.items():
   entry=next((s for s in element.findall('setting') if s.get('name')==key),None)
   if entry is None:entry=ET.SubElement(element,'setting',{'name':key})
   entry.attrib.pop('control',None);entry.text=value
 tree.write(output,encoding='utf8',xml_declaration=True)

def normalize_frames(run,state):
 raw=run/'frames-qpc.csv'
 if not raw.exists() or not raw.stat().st_size:return {'available':False}
 warnings=[];count=0;previous=None;anchor=state['clockAnchor'];origin=dt.datetime.fromisoformat(anchor['utc']);largest=0
 with raw.open(encoding='utf-8-sig',newline='') as source,(run/'frames.csv').open('w',encoding='utf8',newline='') as output:
  reader=csv.DictReader(source);fields=[*reader.fieldnames,'CPUStartUTC','ObservedPreviousStartGapMs'];writer=csv.DictWriter(output,fieldnames=fields);writer.writeheader()
  for row in reader:
   try:clock=origin+dt.timedelta(seconds=(float(row['CPUStartQPC'])-anchor['qpc'])/anchor['frequency'])
   except (KeyError,ValueError,TypeError):continue
   gap=(clock-previous[0]).total_seconds()*1000 if previous else None
   if gap is not None:
    largest=max(largest,gap)
    if previous[1] is not None and gap>max(100,previous[1]*1.5+15):warnings.append({'utc':previous[0].isoformat(),'observedStartGapMs':gap,'reportedFrameTimeMs':previous[1],'note':'Loading/display path or capture inconsistency; do not claim FPS improvement from reported metric alone.'})
   row['CPUStartUTC']=clock.isoformat();row['ObservedPreviousStartGapMs']='' if gap is None else gap;writer.writerow(row);count+=1
   try:metric=float(row.get('FrameTime',row.get('MsBetweenAppStart','NA')))
   except (ValueError,TypeError):metric=None
   previous=(clock,metric)
 result={'available':True,'frames':count,'maxObservedStartGapMs':largest,'inconsistencies':warnings};write(run/'frame-consistency.json',result);return result

def analyze(run,state):
 java=Path(state['analysisJava']);classes=run/'analysis/classes';classes.mkdir(parents=True,exist_ok=True)
 reply=command([java/'bin/javac.exe','-d',classes,SOURCE/'ProfileSummary.java']);(run/'analysis/compile.log').write_text(reply.stdout+reply.stderr,'utf8');assert reply.returncode==0,reply.stderr
 reply=command([java/'bin/java.exe','-Xmx512m','-cp',classes,'ProfileSummary',run/'capture.jfr',run/'analysis',state.get('maxHeapBytes',0)]);(run/'analysis/jfr-analysis.log').write_text(reply.stdout+reply.stderr,'utf8');assert reply.returncode==0,reply.stderr

def mat(run,state,report=None):
 home=Path(state['matHome']);java=Path(state['analysisJava']);launcher=next((home/'plugins').glob('org.eclipse.equinox.launcher_*.jar'))
 config=run/'mat-configuration'
 if not config.exists():shutil.copytree(home/'configuration',config)
 # MAT's random seeking into a large gzip produced a DEFLATE error on a valid
 # capture. Decompress with CRC verification first, then parse the raw HPROF.
 # Keep the original compressed dump, use a separate short ASCII work path.
 work=run/'mat-uncompressed';work.mkdir(exist_ok=True);heap=work/'heap.hprof';partial=work/'heap.hprof.partial'
 if not heap.exists():
  try:
   with gzip.open(run/'heap.hprof.gz','rb') as source,partial.open('wb') as output:shutil.copyfileobj(source,output,8*1024*1024)
   os.replace(partial,heap)
  except BaseException:
   partial.unlink(missing_ok=True);raise
 script=Path(report) if report else SOURCE/'heap-report.xml'
 if state.get('fixture') and report is None:
  script=run/'fixture-retention.xml';script.write_text('<section xmlns="http://www.eclipse.org/mat/report.xsd" name="Fixture"><query name="Dominators"><command>dominator_tree</command></query><query name="Histogram"><command>histogram</command></query></section>','utf8')
 safe_script=work/'report.xml';shutil.copy2(script,safe_script)
 args=[java/'bin/java.exe','-Xms128m','-Xmx'+state.get('matHeap','3g'),'-XX:ActiveProcessorCount=2','--add-exports=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-jar',launcher,'-configuration',config,'-data',run/'mat-workspace','-consoleLog','-nosplash','-application','org.eclipse.mat.api.parse',heap,'org.eclipse.mat.api:suspects','org.eclipse.mat.api:overview',safe_script]
 reply=command(args,cwd=home,timeout=7200);(run/'mat.log').write_text(reply.stdout+reply.stderr,'utf8')
 reports=[str(p) for p in work.glob('heap*.zip')]
 assert reply.returncode==0 and len(reports)>=3 and all(zipfile.is_zipfile(p) for p in reports),'MAT failed or did not produce all three report archives; inspect mat.log'
 return reports

def sample_gpu(run,done):
 # A terminated nvidia-smi --filename loop can lose its buffered tail. Each
 # short query now exits normally; commit only complete CSV rows, then flush.
 samples=errors=0;first=last=None;max_query_ms=0.0
 with (run/'gpu.csv').open('w',encoding='utf8',newline='') as out, (run/'gpu.log').open('w',encoding='utf8') as log:
  writer=csv.writer(out);header=None
  while not done.is_set():
   started=time.monotonic()
   try:
    reply=command(['nvidia-smi','--query-gpu=timestamp,utilization.gpu,memory.used','--format=csv'],timeout=3)
    rows=list(csv.reader(reply.stdout.splitlines()))
    assert reply.returncode==0 and len(rows)>=2 and len(rows[0])==3 and rows[0][0].strip()=='timestamp','Incomplete GPU sample'
    assert all(len(row)==3 and row[0].strip() for row in rows[1:]),'Malformed GPU row'
    if header is None:header=rows[0];writer.writerow(header)
    assert rows[0]==header,'GPU header changed'
    for row in rows[1:]:writer.writerow(row);samples+=1;first=first or row[0];last=row[0]
    out.flush()
   except Exception as error:errors+=1;log.write(stamp()+' '+repr(error)+'\n');log.flush()
   max_query_ms=max(max_query_ms,(time.monotonic()-started)*1000)
   done.wait(max(0,1-(time.monotonic()-started)))
 write(run/'gpu-summary.json',{'rows':samples,'errors':errors,'first':first,'last':last,'maxQueryMs':max_query_ms,'normalExitQueries':True})

def worker(run):
 state=json.loads((run/'state.json').read_text('utf8'));instance=Path(state['instance']);cfg=settings(instance);pid=state['pid'];handle=KERNEL.OpenProcess(0x0400|0x0010,False,pid)
 java=Path(state['runtimeJava']);jcmd=java/'bin/jcmd.exe';name='client_profile_'+state['runId'];active=instance/'ClientProfiler/capture.lock';token={'ownerPid':os.getpid(),'runId':state['runId']};recording=False;frames=gpu=None;logs=[];done=threading.Event();sampler=None
 def phase(value,**extra):state.update(phase=value,updatedUtc=stamp(),**extra);write(run/'state.json',state)
 def diagnostic(args,filename,timeout=120):
  reply=command(args,timeout=timeout);(run/filename).write_text(reply.stdout+reply.stderr,'utf8');return reply
 try:
  assert lease(active,token),'Another client capture is active'
  row=target(processes(),instance,pid,state.get('fixtureNonce'));assert hashlib.sha256((row.get('CommandLine') or '').encode()).hexdigest()==state['commandDigest'],'PID/command changed'
  assert alive(handle),'Client exited before recording'
  reply=diagnostic([jcmd,pid,'JFR.check'],'recordings-before.log');assert reply.returncode==0 and 'No available recordings' in reply.stdout,'An existing JFR is active; do not stop someone else\'s trace'
  reply=diagnostic([jcmd,pid,'VM.flags','-all'],'vm-flags.log');match=re.search(r'\bMaxHeapSize\s*(?::?=)\s*(\d+)',reply.stdout);state['maxHeapBytes']=int(match[1]) if match else 0
  assert state['maxHeapBytes']>0,'Cannot verify the client heap limit; do not substitute process/system RAM'
  diagnostic([jcmd,pid,'GC.heap_info'],'heap-before.log');profile_settings(java,run/'profile.jfc')
  reply=diagnostic([jcmd,pid,'JFR.start','name='+name,'settings='+jcpath(run/'profile.jfc'),'duration='+str(state['durationSeconds'])+'s','filename='+jcpath(run/'capture.jfr'),'maxsize=256m','dumponexit=true'],'jfr-start.log');assert reply.returncode==0 and 'Started recording' in reply.stdout,reply.stdout+reply.stderr;recording=True
  reply=diagnostic([jcmd,pid,'JFR.check','name='+name],'jfr-confirmed.log');assert reply.returncode==0 and name in reply.stdout
  state['clockAnchor']=qpc()
  if not state.get('fixture'):
   log=(run/'presentmon.log').open('w',encoding='utf8');logs.append(log);session='CodexClient_'+state['runId'];state['presentSession']=session
   frames=subprocess.Popen([state['presentMon'],'--process_id',str(pid),'--session_name',session,'--v2_metrics','--qpc_time','--no_console_stats','--no_track_input','--timed',str(state['durationSeconds']),'--terminate_after_timed','--output_file',str(run/'frames-qpc.csv')],stdout=log,stderr=subprocess.STDOUT,creationflags=FLAGS)
   if shutil.which('nvidia-smi'):
    gpu=threading.Thread(target=sample_gpu,args=(run,done),daemon=True);gpu.start()
  sampler=threading.Thread(target=sample,args=(handle,run/'process-memory.jsonl',done),daemon=True);sampler.start();start=time.monotonic()
  phase('recording',recordingStartedUtc=stamp(),heapDumpPending=True,canCloseClient=False)
  while time.monotonic()-start<state['durationSeconds'] and not (run/'stop-request').exists() and alive(handle):time.sleep(.25)
  reply=diagnostic([jcmd,pid,'JFR.stop','name='+name,'filename='+jcpath(run/'capture.jfr')],'jfr-stop.log');recording=False;done.set();sampler.join(timeout=6)
  if frames and frames.poll() is None:
   diagnostic([state['presentMon'],'--session_name',state['presentSession'],'--terminate_existing_session'],'presentmon-stop.log')
   try:frames.wait(timeout=10)
   except subprocess.TimeoutExpired:frames.terminate();frames.wait(timeout=5)
  if gpu:gpu.join(timeout=5)
  reply=diagnostic([java/'bin/jfr.exe','summary',run/'capture.jfr'],'jfr-summary.txt');assert reply.returncode==0 and 'Duration:' in reply.stdout,'JFR save not confirmed'
  phase('trace-saved',validJfr=True,traceSavedUtc=stamp());state['frameSummary']=normalize_frames(run,state)
  assert alive(handle),'Client exited before required post-recording heap dump'
  row=target(processes(),instance,pid,state.get('fixtureNonce'));assert hashlib.sha256((row.get('CommandLine') or '').encode()).hexdigest()==state['commandDigest'],'Do not heap-dump a reused PID'
  phase('dumping-heap',heapDumpStartedUtc=stamp(),canCloseClient=False)
  reply=diagnostic([jcmd,pid,'GC.heap_dump','-gz=1',jcpath(run/'heap.hprof.gz')],'heap-dump.log',timeout=900)
  assert reply.returncode==0 and (run/'heap.hprof.gz').stat().st_size>1024,'Heap dump save failed'
  diagnostic([jcmd,pid,'GC.heap_info'],'heap-after-dump.log')
  source=instance/'minecraft/logs/latest.log'
  if source.exists() and not state.get('fixture'):shutil.copy2(source,run/'latest.log')
  if not state.get('fixture'):state['activationAfter']=activation(instance,state.get('installedBuild'))
  phase('heap-saved-client-can-close',heapDumpPending=False,heapDumpSavedUtc=stamp(),heapDumpBytes=(run/'heap.hprof.gz').stat().st_size,heapDumpMayRequestFullGc=True,canCloseClient=True)
  release_lease(active,token);analyze(run,state)
  phase('waiting-for-client-exit-for-MAT',canCloseClient=True)
  deadline=time.monotonic()+6*3600
  while alive(handle) and time.monotonic()<deadline:time.sleep(2)
  if alive(handle):phase('MAT-pending-client-still-running');return
  lock=Path(state['runRoot'])/'mat.lock';matToken={'ownerPid':os.getpid(),'runId':state['runId']}
  while not lease(lock,matToken):time.sleep(2)
  try:phase('MAT-running');reports=mat(run,state);phase('complete',matReports=reports)
  finally:release_lease(lock,matToken)
 except BaseException as error:phase('failed',error=repr(error),canCloseClient=state.get('validJfr',False) and not state.get('heapDumpPending',True))
 finally:
  done.set()
  if recording and alive(handle):diagnostic([jcmd,pid,'JFR.stop','name='+name,'filename='+jcpath(run/'capture.jfr')],'emergency-jfr-stop.log')
  if frames and frames.poll() is None:frames.terminate()
  if gpu:gpu.join(timeout=5)
  for log in logs:log.close()
  release_lease(active,token)
  if handle:KERNEL.CloseHandle(handle)

def start(instance,args):
 cfg=settings(instance);row=target(processes(),instance,args.pid,args.fixture_nonce);java=Path(cfg.get('runtime_java') or Path(row['ExecutablePath']).parent.parent)
 assert (java/'bin/jcmd.exe').is_file(),'Use a JDK matching the client runtime in settings.local.json'
 requested=Path(cfg.get('run_root') or Path(os.environ['LOCALAPPDATA'])/'Temp/prominence-profile')
 # HotSpot diagnostic commands on Windows can misencode non-ASCII paths even
 # when Python/Java Path support them. Keep raw/index paths short and ASCII.
 root=requested if str(requested).isascii() and len(str(requested))<=100 else Path(cfg.get('ascii_run_root','C:/Temp/ProminenceProfiles'))
 assert str(root).isascii() and len(str(root))<=100,'Configure a short ASCII run_root for HotSpot/MAT'
 root.mkdir(parents=True,exist_ok=True)
 ident=dt.datetime.now().strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:6];run=root/ident;run.mkdir()
 present=cfg.get('presentmon') or str(Path(os.environ.get('ProgramFiles(x86)','C:/Program Files (x86)'))/'CapFrameX/PresentMon/CX-PresentMon-2.6.1-x64.exe')
 if not args.fixture_nonce:assert Path(present).is_file(),'Configure presentmon before recording'
 matHome=Path(cfg['mat_home']);assert (matHome/'plugins').is_dir(),'Configure Eclipse MAT before recording'
 analysis=Path(cfg.get('analysis_java') or java);assert (analysis/'bin/javac.exe').is_file()
 state={'runId':ident,'phase':'starting','createdUtc':stamp(),'instance':str(instance),'pid':row['ProcessId'],'commandDigest':hashlib.sha256((row.get('CommandLine') or '').encode()).hexdigest(),'runtimeJava':str(java),'analysisJava':str(analysis),'matHome':str(matHome),'matHeap':cfg.get('mat_heap','3g'),'runRoot':str(root),'durationSeconds':args.seconds,'label':getattr(args,'label','client-3min'),'presentMon':present,'fixture':bool(args.fixture_nonce),'fixtureNonce':args.fixture_nonce,'gameLaunched':False,'serverModified':False,'heapDumpPending':True,'canCloseClient':False,'systemRamIsPrimaryEvidence':False}
 jar=instance/'minecraft/mods/prominence-korean-compat-1.0.0.jar'
 if jar.exists():
  state['installedJarSha256']=sha(jar)
  with zipfile.ZipFile(jar) as z:state['installedBuild']=json.loads(z.read('fabric.mod.json')).get('custom',{}).get('prominenceAutoUpdate',{}).get('build')
 if not state['fixture']:
  state['activationBefore']=activation(instance,state.get('installedBuild'));state['diagnosticRun']=getattr(args,'diagnostic',False)
  assert not state['activationBefore']['missing'] or state['diagnosticRun'],'Missing current startup activation: '+str(state['activationBefore']['missing'])
 write(run/'state.json',state)
 log=(run/'worker.log').open('w',encoding='utf8');subprocess.Popen([sys.executable,str(Path(__file__).resolve()),'worker','--run',str(run)],stdout=log,stderr=subprocess.STDOUT,creationflags=FLAGS)
 for _ in range(120):
  state=json.loads((run/'state.json').read_text('utf8'))
  if state['phase']!='starting':break
  time.sleep(.25)
 assert state['phase']=='recording',state
 write(instance/'ClientProfiler/latest.json',{'run':str(run),'runId':ident})
 print(json.dumps({'phase':state['phase'],'run':str(run),'seconds':args.seconds,'notice':'Recording confirmed. Enter the server now. After recording, heap dumping can pause the game/request Full GC; keep the client open until canCloseClient=true. MAT runs after client exit.'},ensure_ascii=False,indent=2));return run

def wait_capture(run,initial_duration,interval=.5):
 """Display capture and MAT progress; success means reports were produced."""
 last=None;announced=False;deadline=time.monotonic()+initial_duration+1000;last_reminder=time.monotonic()
 labels={'starting':'측정 준비 중', 'recording':'3분 측정 중', 'trace-saved':'측정 기록 저장 완료',
         'dumping-heap':'힙 덤프 저장 중 — 게임을 종료하지 마세요',
         'waiting-for-client-exit-for-MAT':'클라이언트 종료 대기 중 — 종료하면 MAT가 시작됩니다',
         'MAT-running':'MAT 분석 중 — 완료될 때까지 기다려 주세요'}
 while time.monotonic()<deadline:
  state=json.loads((run/'state.json').read_text('utf8'));phase=state['phase']
  if phase=='failed':
   raise RuntimeError('측정 또는 MAT 분석 실패: '+str(state.get('error','원인은 작업 로그를 확인하세요')))
  if state.get('canCloseClient') and not announced:
   if phase in ('MAT-running','complete'):
    print('힙 덤프 저장 완료. MAT 완료까지 이 창이 진행 상황을 보여줍니다.',flush=True)
   else:
    print('힙 덤프 저장 완료. 이제 클라이언트를 종료해 주세요. MAT 완료까지 이 창이 진행 상황을 보여줍니다.',flush=True)
   announced=True;deadline=time.monotonic()+8*3600+300
  if phase!=last:
   if phase in labels:print(labels[phase],flush=True)
   last=phase;last_reminder=time.monotonic()
  if phase=='complete':
   reports=state.get('matReports') or []
   if not reports or not all(Path(p).is_file() for p in reports):raise RuntimeError('MAT 완료 상태지만 보고서가 없거나 일부를 찾을 수 없습니다.')
   print('MAT 분석 완료. 이제 Codex에게 알려주세요.',flush=True)
   print('분석 자료 폴더: '+str(run),flush=True);return
  if phase=='MAT-pending-client-still-running':raise RuntimeError('MAT 대기 시간이 끝났습니다. 클라이언트 종료 후 MAT 분석을 다시 실행해야 합니다.')
  if phase=='MAT-running' and time.monotonic()-last_reminder>=60:
   print('MAT가 계속 분석 중입니다. 창을 유지해 주세요.',flush=True);last_reminder=time.monotonic()
  time.sleep(interval)
 raise RuntimeError('완료 대기 시간이 초과됐습니다. 작업 상태와 로그를 확인하세요.')

def main():
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('action',choices=['start','status','stop','worker','mat','analyze','wait']);parser.add_argument('--instance',type=Path);parser.add_argument('--run',type=Path);parser.add_argument('--seconds',type=int,default=180);parser.add_argument('--label',default='client-3min');parser.add_argument('--diagnostic',action='store_true');parser.add_argument('--pid',type=int);parser.add_argument('--fixture-nonce');parser.add_argument('--report',type=Path);args=parser.parse_args()
 if os.name!='nt':raise SystemExit('Windows client profiler')
 if args.action=='worker':worker(args.run.resolve());return
 if args.action=='start':
  assert args.instance and 1<=args.seconds<=1800
  start(args.instance.resolve(),args);return
 run=args.run or (current(args.instance.resolve()) if args.instance else None);assert run,'Pass --instance or --run'
 state=json.loads((run/'state.json').read_text('utf8'))
 if args.action=='status':print(json.dumps(state,ensure_ascii=False,indent=2))
 elif args.action=='wait':
  wait_capture(run,state['durationSeconds'])
 elif args.action=='stop':(run/'stop-request').touch();print('Stop requested. Timed trace is saved before the mandatory heap dump; wait for canCloseClient=true.')
 elif args.action=='analyze':
  assert state.get('validJfr') and state.get('canCloseClient'),'Finish the trace and heap before offline analysis'
  analyze(run,state);print((run/'analysis/heap-summary.json').read_text('utf8'))
 elif args.action=='mat':
  handle=KERNEL.OpenProcess(0x1000,False,state['pid'])
  try:assert not alive(handle),'Close the client before offline MAT'
  finally:
   if handle:KERNEL.CloseHandle(handle)
  assert (run/'heap.hprof.gz').exists();lock=Path(state['runRoot'])/'mat.lock';token={'ownerPid':os.getpid(),'runId':state['runId']}
  assert lease(lock,token),'Another MAT analysis is active; keep analyses serial'
  try:
   state.pop('error',None);state.update(phase='MAT-running',canCloseClient=True);write(run/'state.json',state)
   reports=mat(run,state,args.report);state.update(phase='complete',matReports=reports,canCloseClient=True);write(run/'state.json',state);print(json.dumps(reports))
  except BaseException as error:
   state.update(phase='failed',error=repr(error),canCloseClient=True);write(run/'state.json',state);raise
  finally:release_lease(lock,token)

if __name__=='__main__':main()

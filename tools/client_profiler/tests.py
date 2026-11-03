import argparse,contextlib,datetime as dt,importlib.util,io,json,pathlib,subprocess,sys,tempfile,time,unittest,uuid
from unittest.mock import patch
P=pathlib.Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('profiler',P/'profile.py');profiler=importlib.util.module_from_spec(spec);spec.loader.exec_module(profiler)

class Tests(unittest.TestCase):
 def test_activation_reads_midnight_rollover_but_rejects_previous_launch(self):
  import gzip,datetime as dt
  with tempfile.TemporaryDirectory() as temp:
   root=pathlib.Path(temp);logs=root/'minecraft/logs';logs.mkdir(parents=True)
   policy={'clientProfiling':{'requiredOptimizationActivationMessages':['guard A','guard B','later','retired'],
           'deferredActivationMessages':['later'],'retiredActivationByBuild':[{'minimumBuild':2,'messages':['retired']}]}}
   profiler.write(root/'ClientProfiler/policy.json',policy)
   today=dt.datetime.now().astimezone().date();yesterday=today-dt.timedelta(days=1)
   started=dt.datetime.combine(yesterday,dt.time(20)).astimezone().isoformat()
   with gzip.open(logs/f'{yesterday}-1.log.gz','wt',encoding='utf8') as out:out.write('[19:00:00] [main/INFO]: guard B\n')
   with gzip.open(logs/f'{yesterday}-2.log.gz','wt',encoding='utf8') as out:out.write('[20:00:01] [main/INFO]: guard A\n')
   (logs/'latest.log').write_text('[00:01:00] [Render thread/INFO]: Playing\n','utf8')
   result=profiler.activation(root,2,started)
   self.assertEqual(result['verified'],['guard A']);self.assertEqual(result['missing'],['guard B']);self.assertEqual(result['deferred'],['later'])
   self.assertEqual(result['readErrors'],[]);self.assertEqual(len(result['sources']),3)
   # A missing guard stays missing; no policy bypass or unscoped archive fallback.
   self.assertIn('guard A',profiler.activation(root,2)['missing'])
   with (logs/'latest.log').open('a',encoding='utf8') as out:out.write('[00:02:00] [main/INFO]: guard B\n')
   self.assertEqual(profiler.activation(root,2,started)['missing'],[])
 def test_activation_corrupt_archive_is_reported_not_verified(self):
  import datetime as dt
  with tempfile.TemporaryDirectory() as temp:
   root=pathlib.Path(temp);logs=root/'minecraft/logs';logs.mkdir(parents=True)
   profiler.write(root/'ClientProfiler/policy.json',{'clientProfiling':{'requiredOptimizationActivationMessages':['guard A']}})
   day=dt.datetime.now().astimezone().date();started=dt.datetime.combine(day,dt.time()).astimezone().isoformat()
   (logs/f'{day}-1.log.gz').write_bytes(b'not a gzip')
   result=profiler.activation(root,1,started)
   self.assertEqual(result['missing'],['guard A']);self.assertEqual(len(result['readErrors']),1)
 def test_duration_menu_persists_and_preserves_settings(self):
  with tempfile.TemporaryDirectory() as temp:
   instance=pathlib.Path(temp);path=instance/'ClientProfiler/settings.local.json'
   original={'runtime_java':'keep-this-path','mat_heap':'3g','anotherSetting':{'keep':True}}
   profiler.write(path,original);self.assertEqual(profiler.configured_seconds(instance),180)
   args=argparse.Namespace(seconds=180)
   with patch('builtins.input',side_effect=['5','0','31','bad','1.5','5','']),contextlib.redirect_stdout(io.StringIO()):
    self.assertEqual(profiler.menu(instance,args),'1')
   self.assertEqual(args.seconds,300);self.assertEqual(profiler.configured_seconds(instance),300)
   self.assertEqual(json.loads(path.read_text('utf8')),{**original,'capture_seconds':300})
   before=path.read_bytes();args=argparse.Namespace(seconds=300)
   with patch('builtins.input',side_effect=['5','','0']),contextlib.redirect_stdout(io.StringIO()):self.assertEqual(profiler.menu(instance,args),'0')
   self.assertEqual(path.read_bytes(),before)
 def test_duration_cli_override_does_not_change_saved_setting(self):
  with tempfile.TemporaryDirectory() as temp:
   instance=pathlib.Path(temp);path=instance/'ClientProfiler/settings.local.json';profiler.write(path,{'capture_seconds':300})
   with patch.object(sys,'argv',['profile.py','menu','--instance',str(instance),'--seconds','120']),patch('builtins.input',return_value='0'),contextlib.redirect_stdout(io.StringIO()) as output:profiler.main()
   self.assertIn('2분',output.getvalue());self.assertEqual(profiler.configured_seconds(instance),300)
 def test_log_interval_excludes_before_and_after_and_handles_rotation(self):
  with tempfile.TemporaryDirectory() as temp:
   root=pathlib.Path(temp);instance=root/'instance';log=instance/'minecraft/logs/latest.log';log.parent.mkdir(parents=True);run=root/'run';run.mkdir()
   log.write_text('[time] [Render thread/ERROR]: old error\n','utf8');mark=profiler.log_mark(log)
   with log.open('a',encoding='utf8') as stream:stream.write('[now] [Render thread/WARN]: repeated\n'*3+'[now] [Render thread/ERROR]: new error\n')
   summary=profiler.capture_log(instance,run,'interval.txt',mark)
   self.assertEqual(summary['errors'],1);self.assertEqual(summary['warnings'],3);self.assertNotIn('old error',(run/'interval.txt').read_text())
   self.assertEqual(summary['topMessages'][0],('repeated',3));self.assertFalse(summary['rotated'])
   log.write_text('rotated\n','utf8');summary=profiler.capture_log(instance,run,'rotated.txt',mark);self.assertTrue(summary['rotated'])
 def test_disk_budget_before_capture(self):
  with patch.object(profiler.shutil,'disk_usage',return_value=argparse.Namespace(free=1)):
   with self.assertRaisesRegex(RuntimeError,'저장 공간 부족'):profiler.disk_preflight(pathlib.Path('.'),1024**3)
  with patch.object(profiler.shutil,'disk_usage',return_value=argparse.Namespace(free=10*1024**3)):
   self.assertEqual(profiler.disk_preflight(pathlib.Path('.'),1024**3)['estimatedRequiredBytes'],3*1024**3+512*1024**2)
 def test_analyzer_compile_cache_reused(self):
  with tempfile.TemporaryDirectory() as temp:
   run=pathlib.Path(temp)/'run';run.mkdir();state={'instance':temp,'analysisJava':'C:/fake-java','maxHeapBytes':64}
   calls=[]
   def execute(args,**kw):
    calls.append(args)
    if str(args[0]).endswith('javac.exe'):(pathlib.Path(args[2])/'ProfileSummary.class').touch()
    return subprocess.CompletedProcess(args,0,'','')
   with patch.object(profiler,'command',side_effect=execute):profiler.analyze(run,state);profiler.analyze(run,state)
   self.assertEqual(sum(str(c[0]).endswith('javac.exe') for c in calls),1);self.assertEqual(len(calls),3)
 def test_resume_only_uses_saved_artifacts(self):
  with tempfile.TemporaryDirectory() as temp:
   run=pathlib.Path(temp);(run/'heap.hprof.gz').write_bytes(b'saved')
   profiler.write(run/'state.json',{'instance':temp,'phase':'failed','error':'MAT interrupted','validJfr':True,'canCloseClient':True})
   with patch.object(profiler,'active_capture',return_value=None),patch.object(profiler,'publish_current'),patch.object(profiler.subprocess,'Popen') as child:
    child.return_value.pid=123;self.assertEqual(profiler.resume_analysis(run),run)
    self.assertIn('resume-worker',child.call_args[0][0]);self.assertNotIn('worker',child.call_args[0][0])
   state=json.loads((run/'state.json').read_text('utf8'));self.assertEqual(state['phase'],'resuming-analysis');self.assertNotIn('error',state)
   state['canCloseClient']=False;profiler.write(run/'state.json',state)
   with patch.object(profiler,'active_capture',return_value=None),self.assertRaises(AssertionError):profiler.resume_analysis(run)
 def test_reconnect_active_and_mat_without_starting_capture(self):
  with tempfile.TemporaryDirectory() as temp:
   instance=pathlib.Path(temp)/'instance';local=instance/'ClientProfiler';local.mkdir(parents=True)
   root=pathlib.Path(temp)/'runs';run=root/'capture';run.mkdir(parents=True)
   profiler.write(local/'settings.local.json',{'run_root':str(root)})
   profiler.write(run/'state.json',{'instance':str(instance),'phase':'recording','workerPid':123})
   profiler.write(local/'capture.lock',{'ownerPid':123,'runId':run.name})
   with patch.object(profiler,'process_running',return_value=True),patch.object(profiler,'processes',side_effect=AssertionError('must not start a second capture')):
    with contextlib.redirect_stdout(io.StringIO()):self.assertEqual(profiler.start(instance,argparse.Namespace()),run)
    self.assertEqual(profiler.current(instance),run)
    (local/'capture.lock').unlink()
    profiler.write(run/'state.json',{'instance':str(instance),'phase':'waiting-for-client-exit-for-MAT','workerPid':123})
    self.assertEqual(profiler.active_capture(instance),run)
   with patch.object(profiler,'process_running',return_value=False):self.assertIsNone(profiler.active_capture(instance))
   profiler.write(run/'state.json',{'instance':str(instance),'phase':'complete','workerPid':123})
   with patch.object(profiler,'process_running',return_value=True):self.assertIsNone(profiler.active_capture(instance))

 def test_gpu_complete_rows_and_flush(self):
  import threading,csv
  with tempfile.TemporaryDirectory() as temp:
   run=pathlib.Path(temp);done=threading.Event();calls=[]
   def query(args,timeout):
    calls.append(args);n=len(calls)
    if n==3:done.set()
    text='timestamp, utilization.gpu [%], memory.used [MiB]\n2026/10/27 17:00:0'+str(n)+'.000, 20 %, 500 MiB\n'
    if n==2:text='timestamp, utilization.gpu [%], memory.used [MiB]\npartial'
    return subprocess.CompletedProcess(args,0,text,'')
   with patch.object(profiler,'command',side_effect=query),patch.object(done,'wait',return_value=False):profiler.sample_gpu(run,done)
   rows=list(csv.reader((run/'gpu.csv').read_text('utf8').splitlines()));self.assertEqual(len(rows),3)
   result=json.loads((run/'gpu-summary.json').read_text('utf8'));self.assertEqual(result['rows'],2);self.assertEqual(result['errors'],1)
   self.assertTrue(result['last'].endswith('03.000'));self.assertTrue(all('--loop=1' not in args for args in calls))
 def test_gpu_timeout_does_not_break_capture(self):
  import threading
  with tempfile.TemporaryDirectory() as temp:
   run=pathlib.Path(temp);done=threading.Event()
   def timeout(*args,**kwargs):done.set();raise subprocess.TimeoutExpired('nvidia-smi',3)
   with patch.object(profiler,'command',side_effect=timeout):profiler.sample_gpu(run,done)
   result=json.loads((run/'gpu-summary.json').read_text('utf8'));self.assertEqual(result['rows'],0);self.assertEqual(result['errors'],1)
 def test_client_selection(self):
  root=pathlib.Path('C:/test/PrismInstance')
  rows=[{'ProcessId':1,'CommandLine':'java org.prismlauncher.EntryPoint C:/test/PrismInstance/minecraft'},{'ProcessId':2,'CommandLine':'java server.jar C:/test/PrismInstance'}]
  self.assertEqual(profiler.target(rows,root)['ProcessId'],1)
  with self.assertRaises(RuntimeError):profiler.target(rows,root,2)
  with self.assertRaises(RuntimeError):profiler.target(rows+rows,root)
  with self.assertRaises(RuntimeError):profiler.target(rows,pathlib.Path('C:/other'))
  with self.assertRaises(RuntimeError):profiler.target([{'ProcessId':3,'CommandLine':'java org.prismlauncher.EntryPoint C:/test/PrismInstance-copy/minecraft'}],root)
  actual=[{'ProcessId':4,'CommandLine':'javaw "-Djava.library.path=C:/test/PrismInstance/natives" -cp launch.jar org.prismlauncher.EntryPoint'}]
  self.assertEqual(profiler.target(actual,root)['ProcessId'],4)
  alternate=[{'ProcessId':5,'CommandLine':'javaw -Djava.library.path="C:/test/PrismInstance/natives" org.prismlauncher.EntryPoint'}]
  self.assertEqual(profiler.target(alternate,root)['ProcessId'],5)
  for cmd in ('javaw "-Djava.library.path=C:/test/PrismInstance-copy/natives" org.prismlauncher.EntryPoint',
              'javaw "-Djava.library.path=C:/test/PrismInstance/natives-extra" org.prismlauncher.EntryPoint',
              'javaw "-Djava.library.path=C:/test/PrismInstance/natives" server.jar'):
   with self.assertRaises(RuntimeError):profiler.target([{'ProcessId':6,'CommandLine':cmd}],root)
 def test_frame_clock_and_missing_interval(self):
  with tempfile.TemporaryDirectory() as temp:
   run=pathlib.Path(temp);(run/'frames-qpc.csv').write_text('CPUStartQPC,FrameTime\n10000,20\n10200,20\n30200,20\n','utf8')
   state={'clockAnchor':{'utc':'2026-10-27T00:00:00+00:00','qpc':10000,'frequency':10000}}
   result=profiler.normalize_frames(run,state);self.assertEqual(result['frames'],3);self.assertEqual(len(result['inconsistencies']),1);self.assertEqual(result['maxObservedStartGapMs'],2000)
   self.assertIn('2026-10-27T00:00:00+00:00',(run/'frames.csv').read_text('utf8'))
 def test_atomic_state(self):
  with tempfile.TemporaryDirectory() as temp:
   path=pathlib.Path(temp)/'state.json';profiler.write(path,{'phase':'recording'});profiler.write(path,{'phase':'heap-saved-client-can-close'})
   self.assertEqual(json.loads(path.read_text('utf8'))['phase'],'heap-saved-client-can-close');self.assertFalse(path.with_suffix('.json.pending').exists())
 def test_wait_until_mat_complete_and_failures(self):
  with tempfile.TemporaryDirectory() as temp:
   run=pathlib.Path(temp);report=run/'heap_report.zip';report.write_bytes(b'report')
   steps=[{'phase':'recording'}, {'phase':'heap-saved-client-can-close','canCloseClient':True},
          {'phase':'waiting-for-client-exit-for-MAT','canCloseClient':True},
          {'phase':'MAT-running','canCloseClient':True},
          {'phase':'complete','canCloseClient':True,'matReports':[str(report)]}]
   output=io.StringIO()
   with patch.object(pathlib.Path,'read_text',side_effect=[json.dumps(s) for s in steps]),patch.object(profiler.time,'sleep'),contextlib.redirect_stdout(output):
    profiler.wait_capture(run,180)
   content=output.getvalue();self.assertLess(content.index('힙 덤프 저장 완료'),content.index('MAT 분석 중'));self.assertLess(content.index('MAT 분석 중'),content.index('MAT 분석 완료'))
   for failure in ({'phase':'failed','error':'MAT failure','canCloseClient':True}, {'phase':'complete','canCloseClient':True,'matReports':[str(run/'missing.zip')]}, {'phase':'MAT-pending-client-still-running','canCloseClient':True}):
    with patch.object(pathlib.Path,'read_text',return_value=json.dumps(failure)),contextlib.redirect_stdout(io.StringIO()),self.assertRaises(RuntimeError):profiler.wait_capture(run,180)

def fixture(config,output):
 """Real JFR -> post-JFR compressed heap -> offline MAT, on a tiny disposable JVM only."""
 output.mkdir(parents=True,exist_ok=True);instance=output/'fixture-instance';local=instance/'ClientProfiler';local.mkdir(parents=True)
 cfg=json.loads(config.read_text('utf8'));cfg['run_root']=str(output/'runs');profiler.write(local/'settings.local.json',cfg)
 java=pathlib.Path(cfg['runtime_java']);nonce=uuid.uuid4().hex;stop=output/'fixture-finish'
 src=output/'ClientProfileFixture.java';src.write_text('''import java.nio.file.*; public final class ClientProfileFixture { static final Object retained=new byte[4*1024*1024]; public static void main(String[] args)throws Exception {while(!Files.exists(Path.of(args[1]))){for(int i=0;i<300;i++){byte[] work=new byte[4096];work[0]=1;}Thread.sleep(10);}}}''','utf8')
 compile=profiler.command([java/'bin/javac.exe','-d',output,src]);assert compile.returncode==0,compile.stderr
 log=(output/'fixture.log').open('w',encoding='utf8');process=subprocess.Popen([str(java/'bin/java.exe'),'-Xmx64m','-cp',str(output),'ClientProfileFixture',nonce,str(stop)],stdout=log,stderr=subprocess.STDOUT,creationflags=profiler.FLAGS)
 run=None
 try:
  time.sleep(.5);args=argparse.Namespace(pid=process.pid,fixture_nonce=nonce,seconds=3);run=profiler.start(instance,args)
  deadline=time.monotonic()+60
  while time.monotonic()<deadline:
   state=json.loads((run/'state.json').read_text('utf8'))
   if state.get('canCloseClient'):break
   assert state['phase']!='failed',state;time.sleep(.25)
  else:raise AssertionError('Post-recording heap did not finish')
  assert state['maxHeapBytes']==64*1024*1024,state
  assert state['heapDumpBytes']>1024 and state['traceSavedUtc']<state['heapDumpStartedUtc']<state['heapDumpSavedUtc'],state
  metrics=[json.loads(l) for l in (run/'process-memory.jsonl').read_text('utf8').splitlines()]
  assert any(m['cpuOneCorePercent'] is not None for m in metrics),metrics
  stop.touch();process.wait(timeout=10)
  deadline=time.monotonic()+150
  while time.monotonic()<deadline:
   state=json.loads((run/'state.json').read_text('utf8'))
   assert state['phase']!='failed',state
   if state['phase']=='complete':break
   time.sleep(.5)
  else:raise AssertionError('MAT fixture did not complete')
  summary=json.loads((run/'analysis/heap-summary.json').read_text('utf8'));assert summary['maxHeapBytes']==64*1024*1024 and summary['heapDumpExcluded'] and not summary['systemRamUsedAsHeapEvidence']
  result={'status':'passed','run':str(run),'heapSavedAfterJfr':True,'actualHeapLimitBytes':state['maxHeapBytes'],'matReports':state['matReports'],'processCpuMetrics':True,'matLogStreamed':(run/'mat.log').stat().st_size>0,'minecraftStarted':False,'serverStarted':False}
  profiler.write(output/'fixture-report.json',result);print(json.dumps(result,indent=2))
 finally:
  stop.touch()
  if process.poll() is None:
   try:process.wait(timeout=10)
   except subprocess.TimeoutExpired:process.terminate() # Only the process this test created.
  log.close()

if __name__=='__main__':
 if '--fixture-config' in sys.argv:
  parser=argparse.ArgumentParser();parser.add_argument('--fixture-config',type=pathlib.Path,required=True);parser.add_argument('--output',type=pathlib.Path,required=True);args=parser.parse_args();fixture(args.fixture_config,args.output.resolve())
 else:unittest.main()

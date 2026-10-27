import argparse,datetime as dt,importlib.util,json,pathlib,subprocess,sys,tempfile,time,unittest,uuid
P=pathlib.Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('profiler',P/'profile.py');profiler=importlib.util.module_from_spec(spec);spec.loader.exec_module(profiler)

class Tests(unittest.TestCase):
 def test_client_selection(self):
  root=pathlib.Path('C:/test/PrismInstance')
  rows=[{'ProcessId':1,'CommandLine':'java org.prismlauncher.EntryPoint C:/test/PrismInstance/minecraft'},{'ProcessId':2,'CommandLine':'java server.jar C:/test/PrismInstance'}]
  self.assertEqual(profiler.target(rows,root)['ProcessId'],1)
  with self.assertRaises(RuntimeError):profiler.target(rows,root,2)
  with self.assertRaises(RuntimeError):profiler.target(rows+rows,root)
  with self.assertRaises(RuntimeError):profiler.target(rows,pathlib.Path('C:/other'))
  with self.assertRaises(RuntimeError):profiler.target([{'ProcessId':3,'CommandLine':'java org.prismlauncher.EntryPoint C:/test/PrismInstance-copy/minecraft'}],root)
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

def fixture(config,output):
 """Real JFR -> post-JFR compressed heap -> offline MAT, on a tiny disposable JVM only."""
 output.mkdir(parents=True,exist_ok=True);instance=output/'fixture-instance';local=instance/'KoreanPatch/profiler';local.mkdir(parents=True)
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
  stop.touch();process.wait(timeout=10)
  deadline=time.monotonic()+150
  while time.monotonic()<deadline:
   state=json.loads((run/'state.json').read_text('utf8'))
   assert state['phase']!='failed',state
   if state['phase']=='complete':break
   time.sleep(.5)
  else:raise AssertionError('MAT fixture did not complete')
  summary=json.loads((run/'analysis/heap-summary.json').read_text('utf8'));assert summary['maxHeapBytes']==64*1024*1024 and summary['heapDumpExcluded'] and not summary['systemRamUsedAsHeapEvidence']
  result={'status':'passed','run':str(run),'heapSavedAfterJfr':True,'actualHeapLimitBytes':state['maxHeapBytes'],'matReports':state['matReports'],'minecraftStarted':False,'serverStarted':False}
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

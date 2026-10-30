"""Execute the native Iris comparison and production scoped compiler with small Minecraft API doubles."""
from pathlib import Path
import argparse,json,os,subprocess
from build import safe_arg
p=argparse.ArgumentParser()
for n in ['output','classpath','java-home']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();r=Path(__file__).resolve().parents[1];w=a.output.resolve();src=w/'stubs';classes=w/'classes';classes.mkdir(parents=True,exist_ok=True)
cp=os.pathsep.join(json.loads(a.classpath.read_text('utf8')))
def run(opts,name,compiler=False):
 f=w/(name+'.args');f.write_text('\n'.join('"'+str(x).replace('\\','/')+'"' for x in opts),'utf8')
 q=subprocess.run([str(a.java_home/('bin/javac.exe' if compiler else 'bin/java.exe')),'@'+safe_arg(f)],capture_output=True,text=True,encoding='utf8',errors='replace',cwd=w);(w/(name+'.log')).write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stdout+q.stderr
 print(q.stdout[-1600:])
run(['--release','17','-cp',cp,'-d',safe_arg(classes),safe_arg(r/'tests/native/joinverification/ExtractMaterialPredicate.java')],'extract-compile',True)
run(['-cp',os.pathsep.join([safe_arg(classes),cp]),'joinverification.ExtractMaterialPredicate',safe_arg(classes)],'extract')
stubs={
'net/minecraft/class_2769':'''public abstract class class_2769<T extends Comparable<T>> {public static long names;public abstract java.util.Collection<T> method_11898();public abstract String method_11901(T value);public abstract Class<T> method_11902();public abstract java.util.Optional<T> method_11900(String text);}''',
'net/minecraft/class_2758':'''public class class_2758 extends class_2769<Integer>{public java.util.Collection<Integer> method_11898(){return java.util.stream.IntStream.range(0,16).boxed().toList();}public String method_11901(Integer v){names++;return v.toString();}public Class<Integer> method_11902(){return Integer.class;}public java.util.Optional<Integer> method_11900(String text){try{int n=Integer.parseInt(text);return n>=0&&n<16?java.util.Optional.of(n):java.util.Optional.empty();}catch(Exception e){return java.util.Optional.empty();}}}''',
'net/minecraft/class_2746':'''public class class_2746 extends class_2769<Boolean>{public java.util.Collection<Boolean> method_11898(){return java.util.List.of(false,true);}public String method_11901(Boolean v){names++;return v.toString();}public Class<Boolean> method_11902(){return Boolean.class;}public java.util.Optional<Boolean> method_11900(String text){return text.equals("true")?java.util.Optional.of(true):text.equals("false")?java.util.Optional.of(false):java.util.Optional.empty();}}''',
'net/minecraft/Axis':'''public enum Axis {X,Y,Z,A,B,C}''',
'net/minecraft/class_2754':'''public class class_2754 extends class_2769<Axis>{public java.util.Collection<Axis> method_11898(){return java.util.List.of(Axis.values());}public String method_11901(Axis v){names++;return switch(v){case X->"x";case Y->"y";case Z->"z";case A->"a";case B->"b";case C->"c";};}public Class<Axis> method_11902(){return Axis.class;}public java.util.Optional<Axis> method_11900(String text){return java.util.Arrays.stream(Axis.values()).filter(v->v.name().toLowerCase(java.util.Locale.ROOT).equals(text)).findFirst();}}''',
'net/minecraft/class_2753':'''public class class_2753 extends class_2754 {}''',
'net/minecraft/class_2680':'''public class class_2680 {public final java.util.Map<class_2769<?>,Comparable<?>> values;public class_2680(java.util.Map<class_2769<?>,Comparable<?>> v){values=v;}public <T extends Comparable<T>> T method_11654(class_2769<T> p){if(!values.containsKey(p))throw new IllegalArgumentException("missing");return (T)values.get(p);}}'''
}
for n,body in stubs.items():
 f=src/(n+'.java');f.parent.mkdir(parents=True,exist_ok=True);f.write_text('package net.minecraft;'+body,'utf8')
run(['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join([safe_arg(classes),cp]),'-d',safe_arg(classes),*[safe_arg(f) for f in src.rglob('*.java')],safe_arg(r/'src/main/java/local/prominence/join/MaterialPredicateScope.java'),safe_arg(r/'tests/native/joinverification/MaterialPredicateBehavior.java')],'compile',True)
run(['-ea','-cp',os.pathsep.join([safe_arg(classes),cp]),'local.prominence.join.MaterialPredicateBehavior'],'behavior')
(w/'report.json').write_text(json.dumps({'status':'passed','nativeIrisCheckStateExecuted':True,'productionCompilerExecuted':True,'minecraftApiDoubles':True,'gameStarted':False},indent=2),'utf8')

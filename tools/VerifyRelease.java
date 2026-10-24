import local.prominence.autoupdate.*;
import java.nio.file.*;
public final class VerifyRelease {
    public static void main(String[] args)throws Exception {
        if(args[0].equals("fetch")){
            var release=SignedRelease.verify(new SafeHttp().manifest(),SignedRelease.publicKey());
            Path file=new SafeHttp().download(release,Path.of(args[1]),n->{});
            System.out.println("Verified published release: build="+release.build()+", sha256="+UpdateFiles.sha(file));
        }else{
            var release=SignedRelease.verify(Files.readAllBytes(Path.of(args[0])),SignedRelease.publicKey());
            UpdateFiles.verify(Path.of(args[1]),release);
            System.out.println("Verified signed metadata and JAR: build="+release.build()+", sha256="+release.sha256());
        }
    }
}

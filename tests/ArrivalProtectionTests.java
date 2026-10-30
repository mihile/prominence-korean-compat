package local.prominence.arrival;

import java.nio.ByteBuffer;
import java.util.*;
import local.prominence.arrival.client.ArrivalClientGate;

public final class ArrivalProtectionTests {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void invalid(byte[] bytes){try{ArrivalMessages.decode(bytes);throw new AssertionError("Malformed packet accepted");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args){
        var window=new ArrivalWindow(17,0);check(window.active(0,1),"protect during actual teleport");
        check(!window.ready(17,0,1),"cannot acknowledge before arrival");window.arrived(true,10);
        check(window.active(500,50_000_000_000L),"loading is independent of server tick count");
        check(!window.ready(99,500,50_000_000_001L),"old/forged token does not start grace");
        check(window.ready(17,501,50_000_000_002L),"valid readiness starts grace");
        check(window.active(560,80_000_000_000L),"grace lasts 60 server ticks even if server lags");
        check(!window.ready(17,560,80_000_000_001L),"duplicate acknowledgement does not extend grace");
        check(!window.active(561,80_000_000_002L),"grace expires at tick60");
        var timeout=new ArrivalWindow(1,0);timeout.arrived(true,0);
        check(timeout.active(1000,ArrivalWindow.MAX_LOADING_NANOS),"missing peer has bounded timeout plus grace");
        check(timeout.phase()==ArrivalWindow.Phase.GRACE,"timeout exits loading");
        check(!timeout.ready(1,1010,ArrivalWindow.MAX_LOADING_NANOS+1),"late ready cannot extend timeout grace");
        check(!timeout.active(1060,ArrivalWindow.MAX_LOADING_NANOS+2),"timeout grace expires");
        var legacy=new ArrivalWindow(1,0);legacy.arrived(false,100);check(legacy.active(299,1),"old client fixed fallback");check(!legacy.active(300,2),"old client fallback ends");
        var failed=new ArrivalWindow(1,0);check(!failed.active(1,ArrivalWindow.MAX_LOADING_NANOS),"failed or stalled transit is not indefinite");
        int start=Integer.MAX_VALUE-20;var wrap=new ArrivalWindow(1,0);wrap.arrived(true,start);wrap.ready(1,start,1);check(wrap.active(start+59,2),"tick overflow keeps correct grace");check(!wrap.active(start+60,3),"tick overflow expires correctly");
        var gate=new ArrivalClientGate();gate.frame(true,0);gate.frame(true,16_000_000L);check(!gate.rendered(),"need three rendered frames");gate.frame(true,32_000_000L);check(gate.rendered(),"normal destination frames ready");
        gate.frame(false,40_000_000L);check(!gate.rendered(),"wrong dimension/chunk/loading overlay resets readiness");
        gate.frame(true,50_000_000L);gate.frame(true,500_000_000L);gate.frame(true,516_000_000L);check(!gate.rendered(),"stutter resets frame streak");gate.frame(true,532_000_000L);check(gate.rendered(),"readiness recovers after stable frames");
        for(int i=0;i<1000;i++){
            var message=new ArrivalMessages.Begin(i,"namespace:world/"+i,-i,i);
            check(ArrivalMessages.decode(ArrivalMessages.encode(message)).equals(message),"dimension/token/chunk round trip");
            byte[] raw=ArrivalMessages.encode(message);invalid(Arrays.copyOf(raw,raw.length-1));invalid(Arrays.copyOf(raw,raw.length+1));
        }
        byte[] valid=ArrivalMessages.encode(new ArrivalMessages.Begin(0,"minecraft:the_end",0,0));
        byte[] bad=valid.clone();ByteBuffer.wrap(bad).putInt(2);invalid(bad);
        bad=valid.clone();ByteBuffer.wrap(bad).putInt(12,257);invalid(bad);
        bad=valid.clone();bad[16]=(byte)0xff;invalid(bad);invalid(new byte[281]);invalid(new byte[0]);
        System.out.println("PASS "+checks+" arrival loading/60-tick grace/timeout/replay/legacy/overflow/frame readiness and packet bounds checks; no game or server.");
    }
}

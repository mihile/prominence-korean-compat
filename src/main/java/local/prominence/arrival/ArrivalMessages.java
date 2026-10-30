package local.prominence.arrival;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Small bounded protocol, independent of game objects and player data. */
public final class ArrivalMessages {
    public static final int VERSION=1, MAX_BYTES=280;
    public record Begin(long nonce,String dimension,int chunkX,int chunkZ){}
    private ArrivalMessages(){}
    public static byte[] encode(Begin begin){
        byte[] dimension=begin.dimension().getBytes(StandardCharsets.UTF_8);
        if(dimension.length<3||dimension.length>256||!validDimension(begin.dimension()))throw new IllegalArgumentException("Arrival dimension");
        return ByteBuffer.allocate(24+dimension.length).putInt(VERSION).putLong(begin.nonce()).putInt(dimension.length).put(dimension).putInt(begin.chunkX()).putInt(begin.chunkZ()).array();
    }
    public static Begin decode(byte[] bytes){
        if(bytes.length<27||bytes.length>MAX_BYTES)throw new IllegalArgumentException("Arrival size");
        var in=ByteBuffer.wrap(bytes);if(in.getInt()!=VERSION)throw new IllegalArgumentException("Arrival version");
        long nonce=in.getLong();int length=in.getInt();
        if(length<3||length>256||in.remaining()!=length+8)throw new IllegalArgumentException("Arrival length");
        byte[] raw=new byte[length];in.get(raw);String dimension=new String(raw,StandardCharsets.UTF_8);
        if(!Arrays.equals(raw,dimension.getBytes(StandardCharsets.UTF_8))||!validDimension(dimension))throw new IllegalArgumentException("Arrival dimension");
        return new Begin(nonce,dimension,in.getInt(),in.getInt());
    }
    private static boolean validDimension(String value){return value.matches("[a-z0-9_.-]+:[a-z0-9/._-]+");}
}

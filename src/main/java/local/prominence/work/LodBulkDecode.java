package local.prominence.work;

import com.dripps.voxyserver.network.LODBulkPayload;
import com.dripps.voxyserver.network.LODSectionPayload;
import net.minecraft.class_2540;
import java.util.ArrayList;

/** Exact 1.20.1 wire decoder; only short-array ownership differs during an ingest scope. */
public final class LodBulkDecode {
    public static LODBulkPayload read(class_2540 buf){
        var dimension=buf.method_10810();
        int count=buf.method_10816();
        if(count<1 || count>LODBulkPayload.MAX_SECTIONS)throw new IllegalArgumentException("invalid bulk section count: "+count);
        var sections=new ArrayList<LODSectionPayload>(count);
        for(int s=0;s<count;s++){
            long key=buf.readLong(),hash=buf.readLong();
            int len=buf.method_10816();
            if(len<1 || len>LODBulkPayload.MAX_LUT_ENTRIES)throw new IllegalArgumentException("invalid lookup table size: "+len);
            int[] blocks=new int[len],biomes=new int[len];byte[] light=new byte[len];
            for(int i=0;i<len;i++){blocks[i]=buf.method_10816();biomes[i]=buf.method_10816();light[i]=buf.readByte();}
            int n=buf.method_10816(),bits=buf.readByte() & 255;
            if(n<1 || n>LODBulkPayload.MAX_INDEX_ENTRIES || bits<1 || bits>16)throw new IllegalArgumentException("invalid packed lod index data");
            int per=64/bits;long mask=(1L<<bits)-1;short[] indices=RemoteIngestScratch.indices(n);int idx=0;
            while(idx<n){long packed=buf.readLong();for(int i=0;i<per && idx<n;i++,idx++)indices[idx]=(short)((packed>>(i*bits))&mask);}
            sections.add(new LODSectionPayload(dimension,key,blocks,biomes,light,indices,hash));
        }
        return new LODBulkPayload(dimension,sections);
    }
}

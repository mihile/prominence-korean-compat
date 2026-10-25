package local.prominence.memory;
import java.util.List;
public final class FootstepTiming {
    private FootstepTiming(){}
    public static long relativeNext(long now,long absoluteNext) {
        return absoluteNext==Long.MAX_VALUE||absoluteNext<=now?0:absoluteNext-now;
    }
    public static long wake(long current,long due) {
        return current<=0||current==Long.MAX_VALUE?due:Math.min(current,due);
    }
    public static long earliest(List<?> pending) {
        long result=Long.MAX_VALUE;
        for(Object sound:pending)result=Math.min(result,((FootstepPendingAccess)sound).prominence$dueTime());
        return pending.isEmpty()?0:result;
    }
}

package local.prominence.cinder;
public final class CinderMath {
    private CinderMath(){}
    public static int scale(int base,int factor){
        if(base<0||factor<1||factor>1000)throw new IllegalArgumentException("Invalid cinder reward");
        return (int)Math.min(Integer.MAX_VALUE,(long)base*factor);
    }
}

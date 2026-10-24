package local.prominence.autoupdate;

/** Screen geometry shared with offline layout checks; no game dependencies. */
public record UpdateLayout(int x,int y,int width,int height,int notesTop,int notesBottom,int buttonsY,int buttonWidth) {
    public static UpdateLayout of(int screenWidth,int screenHeight){
        int w=Math.min(440,screenWidth-24),h=Math.min(300,screenHeight-24);
        int x=(screenWidth-w)/2,y=(screenHeight-h)/2;
        return new UpdateLayout(x,y,w,h,y+64,y+h-66,y+h-32,(w-36)/3);
    }
}

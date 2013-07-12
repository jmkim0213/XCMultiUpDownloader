package net.xenix.image.lib;


public class ImageProcessing {

    static {
    	try {
    		
    		System.loadLibrary("xeniximage");
    		
    	} catch (Throwable e) {
    		
    		e.printStackTrace();
    	}
    }
    

	public static native void resizeImage(
			String sourcePath,
			String savePath,
            int resizewidth,
            int resizeheight);

	public static native void rotateImage(
			String sourcePath,
			String savePath,
            int angle);

}

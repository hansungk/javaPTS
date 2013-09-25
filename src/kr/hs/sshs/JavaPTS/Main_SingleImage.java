package kr.hs.sshs.JavaPTS;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class Main_SingleImage {
	// / Path to store resources
	static final String PATH = "D:\\video\\";

	// / OpenCV Canvases
	static CanvasFrame canvas1; // Canvas for showing result image
	static CanvasFrame canvas2; // Canvas for showing original(BW) image
	
	// / FFmpeg variables
	static FrameGrabber grabber;
	static FrameRecorder recorder;

	// / IplImage variables
	IplImage imgOrig;		// Original image (RGB)
	IplImage imgBW; 		// Original blackwhite image
	IplImage imgResult; 	// Result image
	
	
	public static void main(String[] args) throws InterruptedException {
		Main_SingleImage m = new Main_SingleImage();
		m.imgOrig = cvLoadImage(PATH+"137.jpg");
		
		canvas1 = new CanvasFrame("original", CV_WINDOW_AUTOSIZE);
		canvas2 = new CanvasFrame("result", CV_WINDOW_AUTOSIZE);
		
		canvas1.showImage(m.imgOrig);
		canvas1.waitKey(0);
		
		m.imgOrig.release();
		canvas1.dispose();
	}	
}

package kr.hs.sshs.JavaPTS;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class Main_SingleImage {
	// / Path to store resources
	private static String PATH = "/d/";

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
}

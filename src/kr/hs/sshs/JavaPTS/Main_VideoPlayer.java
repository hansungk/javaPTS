package kr.hs.sshs.JavaPTS;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import static com.googlecode.javacv.cpp.opencv_highgui.CV_WINDOW_AUTOSIZE;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RGB2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;

import java.awt.event.KeyEvent;
import java.util.Scanner;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FFmpegFrameGrabber;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class Main_VideoPlayer {
	/// Path to store resources
		private static String PATH = "video/";

		/// OpenCV Canvases
		static CanvasFrame canvas1;
		static CanvasFrame canvas2; // Canvas for showing original(BW) image
		//static CanvasFrame canvas7;
		//static CanvasFrame canvas8;
		
		/// FFmpeg variables
		static FrameGrabber grabber;
		static FrameRecorder recorder;

		/// IplImage variables
		IplImage imgTmpl;	// template image (RGB)
		IplImage imgBW;		// template blackwhite image
		
		/// Width and height of original frame
		static CvSize _size;
		static int width;
		static int height;
		static int cropsize=60;

		/// Current frame number
		static int framecount = 1;

		/// Flag to determine whether only display BW image
	static char flag_BW = 'x';

	public static void main(String[] args) throws InterruptedException,
			Exception, com.googlecode.javacv.FrameRecorder.Exception {
		System.out.println("< Key Usage >\n" + "'ESC'	: Escape\n"
				+ "'f'	: Fast forward (x2)\n" + "'TAB'	: Fast forward (x20)\n"
				+ "'j'	: Jump to frame\n" + "'p'	: Print current\n"
				+ "'1'	: Print as \"ssA.jpg\"\n"
				+ "'2'	: Print as \"ssB.jpg\"\n"
				+ "'r'	: Record(append) current\n" + "'d'	: Process\n"
				+ "others	: Bypass processing");

		Main_VideoPlayer m = new Main_VideoPlayer();

		// Initialize canvases
		canvas1 = new CanvasFrame("original", CV_WINDOW_AUTOSIZE);
		canvas2 = new CanvasFrame("blackwhite", CV_WINDOW_AUTOSIZE);

		// Initialize FrameRecorder/FrameGrabber
		recorder = new FFmpegFrameRecorder(PATH + "trash.mp4", 640, 480);
		recorder.setFrameRate(30);
		recorder.start();
		grabber = new FFmpegFrameGrabber(PATH + "corner.mp4");
		grabber.start();

		// Get frame size
		_size = cvGetSize(grab());
		width = _size.width();
		height = _size.height();
		
		while (true) {
			m.imgTmpl = cvCreateImage(_size, IPL_DEPTH_8U, 3);
			m.imgBW = cvCreateImage(_size, IPL_DEPTH_8U, 1);

			cvCopy(grab(), m.imgTmpl);
			cvSmooth(m.imgTmpl, m.imgTmpl, CV_GAUSSIAN, 3);

			cvCvtColor(m.imgTmpl, m.imgBW, CV_RGB2GRAY);

			canvas2.showImage(m.imgBW);		

			System.out.println("############## FRAME " + framecount + " ##############");

			flag_BW = 'x';
			// Read user key input and do the following
			KeyEvent key = canvas1.waitKey(0);
			if (key != null) {
				if ( key.getKeyChar() == 27 ) {
					m.cvReleaseAll();	
					break;
				} else if	(key.getKeyCode() == KeyEvent.VK_TAB) { // FFW 20 frames
					m.cvReleaseAll();	
					// pass 19 frame
					for (int i=0; i<19; i++)
						grab();
					continue;
				} else if (key.getKeyCode() == KeyEvent.VK_P ) { // Take screenshot
					cvSaveImage(PATH + "screenshot.jpg", m.imgBW);
					System.out.println("Saved screenshot!");
				} else if (key.getKeyCode() == KeyEvent.VK_1 ) { // Take screenshot
					cvSaveImage(PATH + "ssA.jpg", m.imgBW);
					System.out.println("Saved as ssA.jpg");
				} else if (key.getKeyCode() == KeyEvent.VK_2 ) { // Take screenshot
					cvSaveImage(PATH + "ssB.jpg", m.imgBW);
					System.out.println("Saved as ssB.jpg");
				} else if (key.getKeyCode() == KeyEvent.VK_R) { // Record frames in an .avi file
					//recorder.record(m.imgResult);
				} else if (key.getKeyCode() == KeyEvent.VK_F) { // FFW 2 frames
					m.cvReleaseAll();	
					grab();
				} else if (key.getKeyCode() == KeyEvent.VK_C) {
					flag_BW = 'c';
				} else if (key.getKeyCode() == KeyEvent.VK_D) {
					flag_BW = 'd';
				} else if (key.getKeyCode() == KeyEvent.VK_J) {
					m.cvReleaseAll();	
					moveToFrame();
				}
			}
			// Don't forget to do this!!!
			m.cvReleaseAll();	
		}

		// Release resources, dispose grabber/canvas and exit
		m.cvReleaseAll();
		recorder.stop();
		recorder.release();
		grabber.stop();
		grabber.release();	
		canvas2.dispose();
		//canvas7.dispose();
		//canvas8.dispose();

		System.out.println("(TERMINATED)");
	}
	
	public static IplImage grab() throws com.googlecode.javacv.FrameGrabber.Exception {
		framecount++;
		return grabber.grab();
	}
	
	/**
	* Release all redundant resources.
	*/
	public void cvReleaseAll() {
		cvReleaseImage(imgBW);
		cvReleaseImage(imgTmpl);
	}
	
	public static void moveToFrame() throws com.googlecode.javacv.FrameGrabber.Exception {
		System.out.print("Move to frame(empty:cancel) : ");
		Scanner s = new Scanner(System.in);
		String ui = s.nextLine();		
		if (!ui.isEmpty()) {
			int temp = framecount;
			for (int i=1; i<Integer.parseInt(ui)-temp; i++)
				grab();
		}
		s.close();
	}
}
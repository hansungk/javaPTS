package kr.hs.sshs.JavaPTS;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_video.cvCalcOpticalFlowPyrLK;

import java.awt.event.KeyEvent;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class Main_SingleImage {
	// / Path to store resources
	static final String PATH = "video/";

	// / OpenCV Canvases
	static CanvasFrame canvas1; // Canvas for showing result image
	static CanvasFrame canvas2; // Canvas for showing original(BW) image
	static CanvasFrame canvas3; // Canvas for showing result image
	
	// / FFmpeg variables
	static FrameGrabber grabber;
	static FrameRecorder recorder;

	/// IplImage variables
	IplImage imgPrev;		// Previous image (BW)
	IplImage imgCurr;		// Current image (BW)
	IplImage imgResult; 	// Result image
	// cvGoodFeaturesToTrack 
	IplImage imgEig;
	IplImage imgTemp;
	
	
	public static void main(String[] args) throws InterruptedException {
		Main_SingleImage m = new Main_SingleImage();
		CvSize _size = new CvSize(200, 300);
		CvSize _winSize = new CvSize(10,10);
		
		m.imgPrev = cvLoadImage(PATH+"ssA.jpg", CV_LOAD_IMAGE_GRAYSCALE);
		m.imgCurr = cvLoadImage(PATH+"ssB.jpg", CV_LOAD_IMAGE_GRAYSCALE);
		m.imgResult = cvLoadImage(PATH+"ssB.jpg", CV_LOAD_IMAGE_UNCHANGED);
		
		canvas1 = new CanvasFrame("original", CV_WINDOW_AUTOSIZE);
		canvas2 = new CanvasFrame("result", CV_WINDOW_AUTOSIZE);
		canvas3 = new CanvasFrame("result", CV_WINDOW_AUTOSIZE);
		canvas1.showImage(m.imgPrev);
		canvas2.showImage(m.imgCurr);

		while (true) {
			// cvGoodFeaturesToTrack
			m.imgEig = cvCreateImage(_size, IPL_DEPTH_32F, 1);
			m.imgTemp = cvCreateImage(_size, IPL_DEPTH_32F, 1);
			final int _maxCornerCount = 5000;
			CvPoint2D32f cornersA = new CvPoint2D32f(_maxCornerCount);
			int[] cornerCount = {_maxCornerCount};
			cvGoodFeaturesToTrack(
					m.imgPrev,
					m.imgEig,
					m.imgTemp,
					cornersA,
					cornerCount,
					0.10,
					0.1,
					null, 3, 0, 0.04
					);
			System.out.println("# of corners: " + cornerCount[0]);
			//System.out.println(m.doubleArrayToString(cornersA.get()));
			
			// Find subpizel corners
			cvFindCornerSubPix(
					m.imgPrev,
					cornersA,
					cornerCount[0],
					_winSize,
					cvSize(-1,-1),
					cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, .03)
					);
			
			// Optical Flow
			CvSize _pyrSize = new CvSize(_size.width()+8, _size.height()/3+1);
			IplImage imgPyrA = cvCreateImage(_pyrSize, IPL_DEPTH_32F, 1);
			IplImage imgPyrB = cvCreateImage(_pyrSize, IPL_DEPTH_32F, 1);
			CvPoint2D32f cornersB = new CvPoint2D32f(_maxCornerCount);
			
			byte[] status = new byte[cornerCount[0]];
			float[] featureErrors = new float[cornerCount[0]];
			
			cvCalcOpticalFlowPyrLK(
					m.imgPrev,
					m.imgCurr,
					imgPyrA,
					imgPyrB,
					cornersA,
					cornersB,
					cornerCount[0],
					_winSize,
					5,
					status,
					featureErrors,
					cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, .3),
					0
					);
			
			// Show what we are looking at
			float errorCriteria = 500.0f;
			for (int i=0; i<cornerCount[0]; i++) {
				CvPoint p0 = new CvPoint((int)Math.round(cornersA.get()[2*i]), (int)Math.round(cornersA.get()[2*i+1]));
				CvPoint p1 = new CvPoint((int)Math.round(cornersB.get()[2*i]), (int)Math.round(cornersB.get()[2*i+1]));
				
				System.out.print("Status of " + (i+1) + " [" + p0.x() + "," + p0.y() + "]	: " + status[i]);
				
				if (status[i]==0) {
					System.out.println("	<<< Error -- Zero status");
					continue;
				} else if (featureErrors[i] > errorCriteria) {
					System.out.println("	<<< Error -- Too long error (" + featureErrors[i] + ", criteria:" + errorCriteria + ")");
					continue;
				} else {System.out.println();}
							
				cvLine(m.imgResult, p0, p1, CV_RGB(0, 255, 0), 2, 0, 0);
			}
			canvas3.showImage(m.imgResult);

			KeyEvent key = canvas1.waitKey(0);
			if (key != null) {
				if (key.getKeyChar() == 27) {
					break;
				}
			}
		}

		cvReleaseImage(m.imgPrev);
		cvReleaseImage(m.imgCurr);
		cvReleaseImage(m.imgEig);
		cvReleaseImage(m.imgTemp);
		cvReleaseImage(m.imgResult);
		//cvReleaseImage(m.imgResult);
		canvas1.dispose();
		canvas2.dispose();
		canvas3.dispose();
		System.out.println("[TERMINATED]");
	}	
	
	public String doubleArrayToString(double[] ds) {
		String result = "";
		for(int i=0; i<ds.length; i++) {
			result += ds[i] + " ";
		}
		return result;
	}
}

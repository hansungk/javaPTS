package kr.hs.sshs.JavaPTS;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_video.cvCalcOpticalFlowPyrLK;

import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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
	
	public static void main(String[] args) throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {
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
			System.out.println("CornerB: " + cornersB.get().length);
			System.out.println(m.doubleArrayToString(cornersB.get()));
			// Show what we are looking at
			float errorCriteria = 500.0f;
			List<double[]> successFlowsL = new ArrayList<double[]>();
			for (int i=0; i<cornerCount[0]; i++) {
				double p0x = cornersA.get()[2*i];
				double p0y = cornersA.get()[2*i+1];
				double p1x = cornersB.get()[2*i];
				double p1y = cornersB.get()[2*i+1];
				CvPoint p0 = new CvPoint((int)p0x, (int)p0y);
				CvPoint p1 = new CvPoint((int)p1x, (int)p1y);
				
				System.out.print("Status of " + (i+1) + " [" + p0.x() + "," + p0.y() + "]	: " + status[i]);
				
				if (status[i]==0) {
					System.out.println("	<<< Error -- Zero status");
					continue;
				} else if (featureErrors[i] > errorCriteria) {
					System.out.println("	<<< Error -- Too long error (" + featureErrors[i] + ", criteria:" + errorCriteria + ")");
					continue;
				} else { // Passed the test!
					System.out.println();
					successFlowsL.add(new double[] {p1x-p0x, p1y-p0y});
				}							
				cvLine(m.imgResult, p0, p1, CV_RGB(0, 255, 0), 2, 0, 0);
			}
			double[][] successFlows = new double[successFlowsL.size()][2];
			successFlowsL.toArray(successFlows);
			findBgMovement(successFlows);
			
			canvas3.showImage(m.imgResult);
			cvSaveImage(PATH+"opticalflow.jpg", m.imgResult);

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
	
	public static void findBgMovement(double[][] flows) throws FileNotFoundException, UnsupportedEncodingException {
		int flowsCount = flows.length;
		System.out.println("Successful: " + flowsCount);
		
		/// Theta
		
		PrintWriter writer = new PrintWriter(PATH+"theta.txt", "cp949");
		ArrayList<Double> thetas = new ArrayList<Double>();
		for(int i=0; i<flowsCount; i++) {
			double theta = Math.atan2(flows[i][1], flows[i][0]);
			thetas.add(theta);
			writer.println(theta / Math.PI * 180.0);	// Remember that Y-axis is flipped!!!
		}
		writer.close();
		// Find min and max
		double mintheta = 2*Math.PI;
		double maxtheta = 0;
		for (double d : thetas) {
			if (d < mintheta) mintheta = d;
			if (d > maxtheta) maxtheta = d;
		}
		System.out.println("Maxtheta: " + maxtheta);
		System.out.println("Mintheta: " + mintheta);
		//double thetaInterval = (Math.PI / 180 ) * 5.0;		// 'Hard' interval (based on absolute theta)
		double thetaInterval = (maxtheta - mintheta) / 2000;		// 'Soft' interval (based on interval counts)
		int roomsCount = (int)Math.floor((maxtheta-mintheta) / thetaInterval) + 1;
		List<ArrayList<Double>> rooms = new ArrayList<ArrayList<Double>>();
		for(int i=0; i<roomsCount; i++) rooms.add(new ArrayList<Double>());	// initialize
		for (double d : thetas) {
			int whatroom = (int)Math.floor((d - mintheta) / thetaInterval);	// from 0
			rooms.get(whatroom).add(d);
		}
		int probableRoom1=0, probableRoomCount1=0;
		int probableRoom2=0, probableRoomCount2=0;
		for (int i=0; i<roomsCount; i++) {
			int thisRoomCount = rooms.get(i).size();
			if (thisRoomCount > probableRoomCount1) {
				probableRoomCount1 = thisRoomCount;
				probableRoom1 = i;
			}
			if (probableRoomCount2 < thisRoomCount && thisRoomCount < probableRoomCount1) {
				probableRoomCount2 = thisRoomCount;
				probableRoom2 = i;
			}
		}
		System.out.println("1st probable theta count: " + probableRoomCount1);
		System.out.println("2nd probable theta count: " + probableRoomCount2);
		//for(double d : rooms.get(probableRoom1)) System.out.print(d/Math.PI*180 + " ");
		//for(double d : drooms.get(probableDRoom1)) System.out.print(d);
		double sum1 = 0, sum2 = 0;
		for (double d : rooms.get(probableRoom1)) {sum1 += d;}
		for (double d : rooms.get(probableRoom2)) {sum2 += d;}
		double probableTheta1 = sum1 / probableRoomCount1;
		double probableTheta2 = sum2 / probableRoomCount2;
		System.out.println("1st probable theta AVG: " + probableTheta1 / Math.PI * 180);
		System.out.println("2nd probable theta AVG: " + probableTheta2 / Math.PI * 180);
		System.out.println();
		
		/// Distance
		
		writer = new PrintWriter(PATH+"distance.txt","cp949");
		ArrayList<Double> distances = new ArrayList<Double>();
		for(int i=0; i<flowsCount; i++) {
			double euclidlength = Math.sqrt(flows[i][0]*flows[i][0] + flows[i][1]*flows[i][1]);
			distances.add(euclidlength);
			writer.println(euclidlength);
		}
		writer.close();
		// Find min and max
		double mindistance = 2 * Math.PI;
		double maxdistance = 0;
		for (double d : distances) {
			if (d < mindistance)
				mindistance = d;
			if (d > maxdistance)
				maxdistance = d;
		}
		System.out.println("Maxdistance: " + maxdistance);
		System.out.println("Mindistance: " + mindistance);
		//double thetaInterval = (Math.PI / 180 ) * 5.0;		// 'Hard' interval (based on absolute theta)
		double distanceInterval = (maxdistance - mindistance) / 2000.0;		// 'Soft' interval (based on interval counts)
		int droomsCount = (int)Math.floor((maxdistance-mindistance) / distanceInterval) + 1;
		List<ArrayList<Double>> drooms = new ArrayList<ArrayList<Double>>();
		for(int i=0; i<droomsCount; i++) drooms.add(new ArrayList<Double>());	// initialize
		for (double d : distances) {
			int whatroom = (int)Math.floor((d - mindistance) / distanceInterval);	// from 0
			drooms.get(whatroom).add(d);
		}
		int probableDRoom1=0, probableDRoomCount1=0;
		int probableDRoom2=0, probableDRoomCount2=0;
		for (int i=0; i<droomsCount; i++) {
			int thisDRoomCount = drooms.get(i).size();
			if (thisDRoomCount > probableDRoomCount1) {
				probableDRoomCount1 = thisDRoomCount;
				probableDRoom1 = i;
			}
			if (probableDRoomCount2 < thisDRoomCount && thisDRoomCount < probableDRoomCount1) {
				probableDRoomCount2 = thisDRoomCount;
				probableDRoom2 = i;
			}
		}
		System.out.println("1st probable distance count: " + probableDRoomCount1);
		System.out.println("2nd probable distance count: " + probableDRoomCount2);
		//for(double d : drooms.get(probableDRoom1)) System.out.print(d);
		sum1 = 0; sum2 = 0;
		
		for (double d : drooms.get(probableDRoom1)) {sum1 += d;}
		for (double d : drooms.get(probableDRoom2)) {sum2 += d;}
		double probableDistance1 = sum1 / probableDRoomCount1;
		double probableDistance2 = sum2 / probableDRoomCount2;
		System.out.println("1st probable distance AVG: " + probableDistance1);
		System.out.println("2nd probable distance AVG: " + probableDistance2);
		System.out.println();
		writer = new PrintWriter(PATH+"result.txt", "cp949");
		writer.println("Distance: " + probableDistance1);
		writer.println("Theta   : " + probableTheta1);
		writer.close();
	}
	
	public String doubleArrayToString(double[] ds) {
		String result = "";
		for(int i=0; i<ds.length; i++) {
			result += (ds[i] + " ");
		}
		return result;
	}
}

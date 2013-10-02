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

public class Main_OpticalFlow {
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
		Main_OpticalFlow m = new Main_OpticalFlow();
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
			final int _maxCornerCount = 100;
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
			
			/// Calculation
			findBgMovement(successFlows);
			///
			
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
		double mindistance = 2 * Math.PI;
		double maxdistance = 0;
		for (double[] d : flows) {
			double theta = Math.atan2(d[1], d[0]);
			double distance = Math.sqrt(d[0]*d[0] + d[1]*d[1]);
			if (theta < mintheta) mintheta = theta;
			if (theta > maxtheta) maxtheta = theta;
			if (distance < mindistance) mindistance = distance;
			if (distance > maxdistance) maxdistance = distance;
		}
		//double thetaInterval = (Math.PI / 180 ) * 5.0;		// 'Hard' interval (based on absolute theta)
		double thetaInterval = (maxtheta - mintheta) / 100;		// 'Soft' interval (based on interval counts)
		//double thetaInterval = (Math.PI / 180 ) * 5.0;		// 'Hard' interval (based on absolute theta)
		double distanceInterval = (maxdistance - mindistance) / 100.0;		// 'Soft' interval (based on interval counts)
		
		// 'Rooms' where vectors get sorted in
		List<ArrayList<double[]>> thetaRooms = new ArrayList<ArrayList<double[]>>();	
		List<ArrayList<double[]>> distanceRooms = new ArrayList<ArrayList<double[]>>();	
		int thetaRoomsCount = (int)Math.floor((maxdistance-mindistance) / distanceInterval) + 1;
		int distanceRoomsCount = (int)Math.floor((maxtheta-mintheta) / thetaInterval) + 1;
		for(int i=0; i<thetaRoomsCount; i++) distanceRooms.add(new ArrayList<double[]>());	// Initialize
		for(int i=0; i<distanceRoomsCount; i++) thetaRooms.add(new ArrayList<double[]>());	// Initialize
		// Sort and add vectors
		for (double[] d : flows) {
			double theta = Math.atan2(d[1], d[0]);
			double distance = Math.sqrt(d[0]*d[0] + d[1]*d[1]);
			int indexTheta = (int)Math.floor((theta - mintheta) / thetaInterval);	// from 0
			int indexDistance = (int)Math.floor((distance - mindistance) / distanceInterval);	// from 0
			thetaRooms.get(indexTheta).add(d);
			distanceRooms.get(indexDistance).add(d);
		}
		
		// Now let's find the most 'populated' rooms
		ArrayList<double[]> biggestTRoom1 = null, biggestTRoom2 = null;
		ArrayList<double[]> biggestDRoom1 = null, biggestDRoom2 = null;
		int biggestTRoomSize1=0, biggestTRoomSize2=0;
		int biggestDRoomSize1=0, biggestDRoomSize2=0;
		double tSum1=0, tSum2=0, dSum1=0, dSum2=0;
		for(ArrayList<double[]> al : thetaRooms) {
			int roomSize = al.size();
			if (biggestTRoomSize1 < roomSize) {
				biggestTRoom1 = al;
				biggestTRoomSize1 = roomSize;
			}
			if (biggestTRoomSize2 < roomSize && roomSize < biggestTRoomSize1) {
				biggestTRoom2 = al;
				biggestTRoomSize2 = roomSize;
			}
		}
		for(ArrayList<double[]> al : distanceRooms) {
			int roomSize = al.size();
			if (biggestDRoomSize1 < roomSize) {
				biggestDRoom1 = al;
				biggestDRoomSize1 = roomSize;
			}
			if (biggestDRoomSize2 < roomSize && roomSize < biggestDRoomSize1) {
				biggestDRoom2 = al;
				biggestDRoomSize2 = roomSize;
			}
		}
		// Calcualte average
		for (double[] d : biggestTRoom1) {
			double theta = Math.atan2(d[1], d[0]);
			tSum1 += theta;
		}
		for (double[] d : biggestTRoom2) {
			double theta = Math.atan2(d[1], d[0]);
			tSum2 += theta;
		}
		for (double[] d : biggestDRoom1) {
			double distance = Math.sqrt(d[0]*d[0] + d[1]*d[1]);
			dSum1 += distance;
		}
		for (double[] d : biggestDRoom2) {
			double distance = Math.sqrt(d[0]*d[0] + d[1]*d[1]);
			dSum2 += distance;
		}
		double probableTheta1 = tSum1 / biggestTRoomSize1;
		double probableTheta2 = tSum2 / biggestTRoomSize2;
		double probableDistance1 = dSum1 / biggestDRoomSize1;
		double probableDistance2 = dSum2 / biggestDRoomSize2;

		// Massive sysouts
		System.out.println();
		System.out.println("Maxtheta: " + maxtheta);
		System.out.println("Mintheta: " + mintheta);
		System.out.println("1st probable theta count: " + biggestTRoomSize1);
		System.out.println("2nd probable theta count: " + biggestTRoomSize2);
		System.out.println("1st probable theta AVG: " + probableTheta1);		// Remember, Y-axis is inverted
		System.out.println("2nd probable theta AVG: " + probableTheta2);
		System.out.println();
		System.out.println("Maxdistance: " + maxdistance);
		System.out.println("Mindistance: " + mindistance);
		System.out.println("1st probable distance count: " + biggestDRoomSize1);
		System.out.println("2nd probable distance count: " + biggestDRoomSize2);
		System.out.println("1st probable distance AVG: " + probableDistance1);
		System.out.println("2nd probable distance AVG: " + probableDistance2);
		System.out.println();
		
		// Tiny file writes
		writer = new PrintWriter(PATH+"result.txt", "cp949");
		writer.println("Distance: " + probableDistance1);
		writer.println("Theta   : " + probableTheta1);
		writer.close();
	}
	
	/**
	 * Solve Least Square Problem and find the 'most appropriate' vector.
	 */
	public void findThetaDistance() {
		
	}
	
	public String doubleArrayToString(double[] ds) {
		String result = "";
		for(int i=0; i<ds.length; i++) {
			result += (ds[i] + " ");
		}
		return result;
	}
}
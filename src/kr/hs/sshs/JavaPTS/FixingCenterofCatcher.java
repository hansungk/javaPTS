package kr.hs.sshs.JavaPTS;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

import java.util.ArrayList;
import java.util.List;

public class FixingCenterofCatcher {

	static List<List<CvPoint>> centers;
	static int thresh = 10, thresh2=14;
	
	public static CvPoint findCatcher(IplImage img1, IplImage img2, IplImage img3, CvPoint ballfinal){
		centers = new ArrayList<List<CvPoint>>();
		centers.add(groupListMembers(CatcherDetect.main(img1),thresh,1));
		centers.add(groupListMembers(CatcherDetect.main(img2),thresh,1));
		centers.add(groupListMembers(CatcherDetect.main(img3),thresh,1));
		
		System.out.println("center1 : " + centers.get(0).size());
		System.out.println("center2 : " + centers.get(1).size());
		System.out.println("center3 : " + centers.get(2).size());
		List<CvPoint> merged = new ArrayList<CvPoint>();
		for(CvPoint pt : centers.get(0)){
			merged.add(pt);
		}
		for(CvPoint pt : centers.get(1)){
			merged.add(pt);
		}
		for(CvPoint pt : centers.get(2)){
			merged.add(pt);
		}
		
		System.out.println("merged size : " + merged.size());
		
		merged = groupListMembers(merged,thresh2,2);
		
		if(merged.size()==0) return null;
		
		CvPoint CenterOfCatcher;
		CenterOfCatcher=merged.get(0);
		
		for(CvPoint pt : merged){
			if(dist(pt,ballfinal)<dist(CenterOfCatcher,ballfinal))
					CenterOfCatcher = pt;
		}
		
		return CenterOfCatcher;
	}
	
	static List<CvPoint> groupListMembers(List<CvPoint> list, double disthresh, int numthresh){
		
		List<Integer> labels = new ArrayList<Integer>();
		List<CvPoint> temp = new ArrayList<CvPoint>();
		List<CvPoint> grouped = new ArrayList<CvPoint>();
		for(int q = 0; q<list.size(); q++){
			labels.add(q);
		}
		
		for(int i = 0 ; i<list.size()-1; i++){
			CvPoint Pi = list.get(i);
			for(int j = Math.min(i+1,list.size()-1); j<list.size(); j++){
				CvPoint Pj = list.get(j);
				if(dist(Pi,Pj)<disthresh){
					labels.set(j,labels.get(i));
				}
			}
		}
		
		int nthresh = numthresh;
		
		while(true){
			for(int q = 0; q<list.size()-1; q++){
				if(labels.get(q)!=q) continue;
				for(int i = 0 ; i<list.size()-1; i++){
					if(labels.get(i)==q) temp.add(list.get(i));
				}
				if(temp.size()>=nthresh)
					grouped.add(averagePoint(temp));
				temp.clear();
			}
			if(grouped.size()!=0 || nthresh==1) break;
			else nthresh--;
		}
		
		return grouped;
		
	}
	
	static CvPoint averagePoint(List<CvPoint> in){
		int x=0,y=0;
		
		for(int i = 0; i<in.size(); i++){
			x+=in.get(i).x();
			y+=in.get(i).y();
		}
		
		x=(int) Math.round(x/(double)in.size());
		y=(int) Math.round(y/(double)in.size());
		
		return new CvPoint(x,y);
		
	}
	
	static double dist(CvPoint a, CvPoint b){
		return Math.sqrt((a.x()-b.x())*(a.x()-b.x())+(a.y()-b.y())*(a.y()-b.y()));
	}
	
}

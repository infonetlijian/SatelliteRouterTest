package core;

import java.util.HashMap;
import java.util.Random;



import jat.orbit.SatelliteOrbit;

public class hello{
	public static void main(String[] args){
		
		
		double[] parameters = new double[6];
		

		
		Random random = new Random();
		parameters[0]= random.nextInt(9000)%(2000+1) + 2000;
		//this.parameters[0]=8000.0;
		parameters[1]= 0;//0.1偏心率，影响较大,e=c/a
		//parameters[2]= (360/NROF_PLANE)*(m/NROF_S_EACHPLANE);
		parameters[2] = random.nextInt(15);
		parameters[3] = random.nextInt(15);
		//parameters[3]= (360/NROF_S_EACHPLANE)*((m-(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE) - 1) + (360/NROF_SATELLITES)*(m/NROF_S_EACHPLANE);//0.0;
		parameters[4]= 0.0;//0.0;
		parameters[5]= 0.0;//0.0;
		
		SatelliteOrbit saot;
		saot=new SatelliteOrbit(parameters);
		
		double[][] coordinate = saot.getSatelliteCoordinate(1);
		
		Coord c1=new Coord(10.3,10.5);
		Coord c2=new Coord(100.9,10000.6);
		
		HashMap<Double, Double> test = new HashMap<Double, Double>();
		test.put(1.0, 0.0);
		double time1 = System.currentTimeMillis();
		
		System.out.printf("%.6f  ",time1);
		
		
		
		for(double var1 = 0; var1 < 10; var1 += 1)
		for (double var = 0; var < 100000000; var += 1){
			//JudgeNeighbors(c1,c2);
			//test.put(var1, var);//判断什么时候才会离开
			test.get(1.0);
		}
		double time2 = System.currentTimeMillis();
		System.out.printf("%.6f  ",time2-time1);
	
	
	}
	public static boolean JudgeNeighbors(Coord c1,Coord c2){

		double distance = c1.distance(c2);
		if (distance <= 5000)
			return true;
		else
			return false;
	}	
}

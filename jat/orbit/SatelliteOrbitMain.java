package jat.orbit;


public class SatelliteOrbitMain {
	
	static double tf=0.0;
	static double[][] coordinate = new double[1][3];
	private static SatelliteOrbit saot;
	
	public static void main(String[] args) {
		saot=new SatelliteOrbit();
		coordinate = saot.getSatelliteCoordinate(tf);
	}
}
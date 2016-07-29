package jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools;

public class Eye3D {

  private double theta;
  private double phi;

  public Eye3D() {
    theta = Math.PI/4;
    phi = Math.PI/4;
  }

  public Eye3D(double t,double p) {
    theta = t;
    phi = p;
  }

  public double getTheta() {
    return theta;
  }

  public double getPhi() {
    return phi;
  }

  public Object clone() {
    return new Eye3D(theta,phi);
  }

}
package jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools;

public class Coordinates2D {

  private Axe2D axe;
  private int[] Sc = new int[2];
  private double[] Pl = new double[2];

  public Coordinates2D(double[] xy,Axe2D ax) {
    Pl = xy;
    axe = ax;
    evalSc();
  }

  public Coordinates2D(double x,double y,Axe2D ax) {
    Pl[0] = x;
    Pl[1] = y;
    axe = ax;
    evalSc();
  }

  public Coordinates2D copy() {
    return new Coordinates2D(Pl[0],Pl[1],axe);
  }

  public double[] vector(Coordinates2D B) {
    double[] V = new double[2];
    V[0] = this.Pl[0] - B.Pl[0];
    V[1] = this.Pl[1] - B.Pl[1];
    return V;
  }

  public Coordinates2D addVector(double x,double y) {
    return new Coordinates2D(Pl[0] + x,Pl[1] + y,axe);
  }

  public Coordinates2D addVector(double[] xy) {
    return new Coordinates2D(Pl[0] + xy[0],Pl[1] + xy[1],axe);
  }

  public double[] getPl() {
    return Pl;
  }

  public int[] getSc() {
    return Sc;
  }

  public Axe2D getAxe() {
    return axe;
  }

  public void setPl(double[] xy) {
    Pl = xy;
    evalSc();
  }

  public void setPl(double x,double y) {
    Pl[0] = x;
    Pl[1] = y;
    evalSc();
  }

/*  public void setSc(int[] xy) {
    Sc = xy;
    evalPl();
  }*/

/*  private void evalPl() {
    Pl = axe.Sc2Pl(Sc);
  }*/

  private void evalSc() {
    Sc = axe.Pl2Sc(Pl);
  }
}


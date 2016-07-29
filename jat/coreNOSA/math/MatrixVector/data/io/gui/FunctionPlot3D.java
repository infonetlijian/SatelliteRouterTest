package jat.coreNOSA.math.MatrixVector.data.io.gui;

import jat.coreNOSA.math.MatrixVector.data.function.DoubleFunction;
import jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools.PlotAttributes;


public class FunctionPlot3D extends Plot3D {

  /**
	 * 
	 */
	private static final long serialVersionUID = 4708819612154821212L;

private DoubleFunction[] F;

  private double Xmin;
  private double Xmax;
  private double Ymin;
  private double Ymax;

  private static int nbPointsX = 20;
  private static int nbPointsY = 20;

  public FunctionPlot3D(DoubleFunction f, double xmin, double xmax, double ymin, double ymax) {
    setAppearence();
    setPlotAttributes();
    Xmin = xmin;
    Xmax = xmax;
    Ymin = ymin;
    Ymax = ymax;
    update(f);
  }

  public FunctionPlot3D(DoubleFunction[] f, double xmin, double xmax, double ymin, double ymax) {
    setAppearence();
    setPlotAttributes();
    Xmin = xmin;
    Xmax = xmax;
    Ymin = ymin;
    Ymax = ymax;
    update(f);
  }

  protected void setPlotAttributes() {
    PA = new PlotAttributes();
    PA.setTypeList(PIXEL);
    String[] leg = {"X","Y","Z"};
    PA.setLegend(leg);
  }

  public void update(DoubleFunction f) {
    checkArgNumber(f);
    F = new DoubleFunction[1];
    F[0] = f;

    setXYZ();
    update();
  }

  public void update(DoubleFunction[] f) {
    checkArgNumber(f);
    F = new DoubleFunction[f.length];
    for (int i = 0; i < f.length; i++) {
      F[i] = f[i];
    }

    setXYZ();
    update();
  }

  public void add(DoubleFunction f) {
    checkArgNumber(f);
    DoubleFunction[] F_tmp = new DoubleFunction[F.length + 1];
    for (int i = 0; i < F.length; i++) {
      F_tmp[i] = F[i];
    }
    F_tmp[F.length] = f;
    F = F_tmp;

    setXYZ();
    update();
  }

  public void setMinMax(double xmin, double xmax, double ymin, double ymax) {
    Xmin = xmin;
    Xmax = xmax;
    Ymin = ymin;
    Ymax = ymax;

    setXYZ();
    update();
  }

  private void setXYZ() {
    X = new double[F.length][nbPointsX*nbPointsY];
    Y = new double[F.length][nbPointsX*nbPointsY];
    Z = new double[F.length][nbPointsX*nbPointsY];
    widthX = new double[F.length][];
    widthY = new double[F.length][];
    widthZ = new double[F.length][];
    for (int i = 0; i < F.length; i++) {
      for (int j = 0; j < nbPointsX; j++) {
        for (int k = 0; k < nbPointsY; k++) {
          double[] xy = {Xmin + (Xmax - Xmin)*j/(nbPointsX-1),Ymin + (Ymax - Ymin)*k/(nbPointsY-1)};
          X[i][j+k*nbPointsX] = xy[0];
          Y[i][j+k*nbPointsX] = xy[1];
          Z[i][j+k*nbPointsX] = F[i].eval(xy);
        }
      }
      widthX[i] = new double[nbPointsX*nbPointsY];
      widthY[i] = new double[nbPointsX*nbPointsY];
      widthZ[i] = new double[nbPointsX*nbPointsY];
    }
  }

  /** Check if argNumber == 2.
  @param f   DoubleFunction.
   */

   private void checkArgNumber (DoubleFunction f) {
      f.checkArgNumber(2);
   }

  /** Check if argNumber == 2.
  @param F   DoubleFunction array.
   */

   private void checkArgNumber (DoubleFunction[] F) {
    for (int i = 0; i < F.length; i++)
      F[i].checkArgNumber(2);
   }


}

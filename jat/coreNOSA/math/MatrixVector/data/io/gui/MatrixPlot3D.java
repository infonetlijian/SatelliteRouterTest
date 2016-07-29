package jat.coreNOSA.math.MatrixVector.data.io.gui;

import jat.coreNOSA.math.MatrixVector.data.Matrix;
import jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools.PlotAttributes;


public class MatrixPlot3D extends Plot3D {

  /**
	 * 
	 */
	private static final long serialVersionUID = -8176264814304167999L;
private Matrix[] XYZ;

  public MatrixPlot3D(Matrix xyz) {
    setAppearence();
    setPlotAttributes();
    update(xyz);
  }

  public MatrixPlot3D(Matrix[] xyz) {
    setAppearence();
    setPlotAttributes();
    update(xyz);
  }

  public MatrixPlot3D(Matrix x,Matrix y,Matrix z) {
    setAppearence();
    setPlotAttributes();
    update(x,y,z);
  }

  protected void setPlotAttributes() {
    PA = new PlotAttributes();
    PA.setTypeList(DOT);
    String[] leg = {"X","Y","Z"};
    PA.setLegend(leg);
  }

  public void update(Matrix xyz) {
    checkColumnDimension(xyz);
    XYZ = new Matrix[1];
    XYZ[0] = xyz.copy();

    setXYZ();
    update();
  }

  public void update(Matrix[] xyz) {
    checkColumnDimension(xyz);
    XYZ = new Matrix[xyz.length];
    for (int i = 0; i < xyz.length; i++) {
      XYZ[i] = xyz[i].copy();
    }

    setXYZ();
    update();
  }

  public void add(Matrix xyz) {
    checkColumnDimension(xyz);
    Matrix[] XYZ_tmp = new Matrix[XYZ.length + 1];
    for (int i = 0; i < XYZ.length; i++) {
      XYZ_tmp[i] = XYZ[i];
    }
    XYZ_tmp[XYZ.length] = xyz.copy();
    XYZ = XYZ_tmp;

    setXYZ();
    update();
  }

  public void update(Matrix x,Matrix y,Matrix z) {
    checkDimensions(x,y,z);
    XYZ = new Matrix[1];
    Matrix xyz = new Matrix(y.getRowDimension(),3);
    xyz.setMatrix(0,0,x.copy());
    xyz.setMatrix(0,1,y.copy());
    xyz.setMatrix(0,2,z.copy());
    XYZ[0] = xyz;

    setXYZ();
    update();
 }

  private void TransposeIfNecessary() {
    for (int i = 0; i < XYZ.length; i++) {
      if (XYZ[i].getRowDimension()<XYZ[i].getColumnDimension()) {
        XYZ[i] = XYZ[i].transpose();
      }
    }
  }

  private void setXYZ() {
    TransposeIfNecessary();
    X = new double[XYZ.length][];
    Y = new double[XYZ.length][];
    Z = new double[XYZ.length][];
    widthX = new double[XYZ.length][];
    widthY = new double[XYZ.length][];
    widthZ = new double[XYZ.length][];
    for (int i = 0; i < XYZ.length; i++) {
      X[i] = XYZ[i].getColumnArrayCopy(0);
      Y[i] = XYZ[i].getColumnArrayCopy(1);
      Z[i] = XYZ[i].getColumnArrayCopy(2);
      widthX[i] = new double[XYZ[i].getColumnDimension()];
      widthY[i] = new double[XYZ[i].getColumnDimension()];
      widthZ[i] = new double[XYZ[i].getColumnDimension()];
    }
  }

  /** Check if ColumnDimension(xyz) == 3
  @param xyz   Matrix
   */

   private void checkColumnDimension (Matrix xyz) {
      xyz.checkColumnDimension(3);
   }

  /** Check if ColumnDimension(xxy) == 3
  @param xyz   Matrix
   */

   private void checkColumnDimension (Matrix[] xyz) {
    for (int i = 0; i < xyz.length; i++)
      xyz[i].checkColumnDimension(3);
   }

  /** Check if size(x) == size(y) == size(z)
  @param x   Matrix
  @param y   Matrix
  @param z   Matrix
   */

   private void checkDimensions(Matrix x,Matrix y,Matrix z) {
      x.checkColumnDimension(1);
      x.checkMatrixDimensions(y);
      x.checkMatrixDimensions(z);
   }

}

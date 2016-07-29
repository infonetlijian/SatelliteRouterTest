package jat.coreNOSA.math.MatrixVector.data.io.gui;

import jat.coreNOSA.math.MatrixVector.data.Matrix;
import jat.coreNOSA.math.MatrixVector.data.RandomMatrix;
import jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools.PlotAttributes;


public class MatrixHist2D extends Plot2D {

  /**
	 * 
	 */
	private static final long serialVersionUID = -6356121500445522355L;
private Matrix[] XY;
  private Matrix[] width;

  private int nbSlices;

  public MatrixHist2D(RandomMatrix x,int n) {
    setAppearence();
    setPlotAttributes();
    nbSlices = n;
    update(x);
  }

  public MatrixHist2D(RandomMatrix[] x,int n) {
    setAppearence();
    setPlotAttributes();
    nbSlices = n;
    update(x);
  }

  protected void setPlotAttributes() {
    PA = new PlotAttributes();
    PA.setTypeList(HIST);
    String[] leg = {"x",""};
    PA.setLegend(leg);
  }

  private void setSlicing(int nb) {

    X0 = XY[0].sumRows().get(0,0)/XY[0].getRowDimension();

    nbSlices = nb;
    int n = XY.length;
    int m;

    Matrix[] mat = new Matrix[n];
    Matrix temp;

    width = new Matrix[n];

    double colmin;
    double colmax;
    double colpas;

    for (int i=0;i<n;i++) {
      temp = new Matrix(nbSlices,2);
      m = XY[i].getRowDimension();
      colmin = XY[i].minRows().get(0,0);
      colmax = XY[i].maxRows().get(0,0);
      colpas = (colmax-colmin)/(nbSlices);

      width[i] = new Matrix(nbSlices,1);

      for (int j = 0; j < nbSlices; j++) {
        temp.set(j,0,(j+.5)*colpas+colmin);
        width[i].set(j,0,colpas);
      }

      for (int k = 0; k < m; k++) {
        Matrix d = temp.getColumn(0).dist(XY[i].getMatrix(k,k,0,0));
        int s = d.find(d.min().get(0,0))[0][0];
        temp.set(s,1,temp.get(s,1)+1);
      }
      mat[i] = temp.copy();
    }
    XY = mat;
  }

  public void update(RandomMatrix x) {
    checkColumnDimension(x);
    XY = new Matrix[1];
    XY[0] = x.copy();

    setXY();
    update();
  }


  public void update(RandomMatrix[] x) {
    checkColumnDimension(x);
    XY = new Matrix[x.length];
    for (int i = 0; i < x.length; i++) {
      XY[i] = x[i].copy();
    }

    setXY();
    update();
  }

  public void add(Matrix x) {
    checkColumnDimension(x);
    Matrix[] XY_tmp = new Matrix[XY.length + 1];
    for (int i = 0; i < XY.length; i++) {
      XY_tmp[i] = XY[i];
    }
    XY_tmp[XY.length] = x.copy();
    XY = XY_tmp;

    setXY();
    update();
  }

  public void setNumberSlices(int n) {
    nbSlices = n;

    setXY();
    update();
  }

  private void TransposeIfNecessary() {
    for (int i = 0; i < XY.length; i++) {
      if (XY[i].getRowDimension()<XY[i].getColumnDimension()) {
        XY[i] = XY[i].transpose();
      }
    }
  }

  private void setXY() {
    TransposeIfNecessary();
    setSlicing(nbSlices);
    X = new double[XY.length][];
    Y = new double[XY.length][];
    widthX = new double[XY.length][];
    widthY = new double[XY.length][];
    for (int i = 0; i < XY.length; i++) {
      X[i] = XY[i].getColumnArrayCopy(0);
      Y[i] = XY[i].getColumnArrayCopy(1);
      widthX[i] = width[i].getColumnArrayCopy(0);
      widthY[i] = new double[XY[i].getColumnDimension()];
    }
  }

  /** Check if ColumnDimension(x) == 1
  @param x   Matrix
   */

   private void checkColumnDimension (Matrix x) {
      x.checkColumnDimension(1);
   }

  /** Check if ColumnDimension(x) == 1
  @param x   Matrix
   */

   private void checkColumnDimension (Matrix[] x) {
    for (int i = 0; i < x.length; i++)
      x[i].checkColumnDimension(1);
   }
}

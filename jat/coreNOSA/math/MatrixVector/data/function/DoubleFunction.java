package jat.coreNOSA.math.MatrixVector.data.function;

import jat.coreNOSA.math.MatrixVector.data.io.gui.FrameView;
import jat.coreNOSA.math.MatrixVector.data.io.gui.FunctionPlot2D;
import jat.coreNOSA.math.MatrixVector.data.io.gui.FunctionPlot3D;
/**
 * <p>Titre : JAva MAtrix TOols</p>
 * <p>Description : </p>
 * @author Yann RICHET
 * @version 1.0
 */

public abstract class DoubleFunction {

  protected int argNumber;

  public abstract double eval(double[] values);

  public void checkArgNumber(int n) {
    if (argNumber != n) {
      throw new IllegalArgumentException("Number of arguments must equals " + argNumber);
    }
  }

   /** Plot the DoubleFunction in a JPanel
   @param Xmin  Min value in X.
   @param Xmax  Max value in X.
   @return      A FunctionPlot2D (extends a JPanel)
   */

  public FunctionPlot2D toPanelPlot2D(double Xmin, double Xmax) {
    return new FunctionPlot2D(this,Xmin,Xmax);
  }

   /** Plot the DoubleFunction in a JFrame
   @param Xmin  Min value in X.
   @param Xmax  Max value in X.
   */

  public void toFramePlot2D(double Xmin, double Xmax) {
   FrameView fv = new FrameView(toPanelPlot2D(Xmin,Xmax));
  }

   /** Plot the DoubleFunction in a JPanel
   @param Xmin  Min value in X.
   @param Xmax  Max value in X.
   @param Ymin  Min value in Y.
   @param Ymax  Max value in Y.
   @return      A FunctionPlot3D (extends a Swing JPanel)
   */

  public FunctionPlot3D toPanelPlot3D(double Xmin, double Xmax,double Ymin, double Ymax) {
    return new FunctionPlot3D(this,Xmin,Xmax,Ymin,Ymax);
  }

   /** Plot the DoubleFunction in a JFrame
   @param Xmin  Min value in X.
   @param Xmax  Max value in X.
   @param Ymin  Min value in Y.
   @param Ymax  Max value in Y.
   */

  public void toFramePlot3D(double Xmin, double Xmax,double Ymin, double Ymax) {
   FrameView fv = new FrameView(toPanelPlot3D(Xmin,Xmax,Ymin,Ymax));
  }
}
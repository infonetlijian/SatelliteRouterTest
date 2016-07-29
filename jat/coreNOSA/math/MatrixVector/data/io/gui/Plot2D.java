package jat.coreNOSA.math.MatrixVector.data.io.gui;

//import jat.matvec.data.Matrix;

import jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools.Axe2D;
import jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools.Coordinates2D;
import jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools.DataPlot2D;
import jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools.Grid2D;
import jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools.NotedPoint2D;
import jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools.PlotAttributes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

public abstract class Plot2D extends JPanel implements MouseListener, MouseMotionListener {

  /**
	 * 
	 */
	private static final long serialVersionUID = -6027686204743112203L;

private Dimension defaultSize = new Dimension(400,400);

  public static int PIXEL = DataPlot2D.PIXEL;
  public static int DOT = DataPlot2D.DOT;
  public static int LINE = DataPlot2D.LINE;
  public static int DOTLINE = DataPlot2D.DOTLINE;
  public static int BAR = DataPlot2D.BAR;
  public static int DOTBAR = DataPlot2D.DOTBAR;
  public static int HIST = DataPlot2D.HIST;

  protected DataPlot2D[] plots;
  protected Axe2D axe;
  protected Grid2D grid;
  protected PlotAttributes PA;
  protected NotedPoint2D np;

  protected double[][] X ;
  protected double[][] Y ;
  protected double X0 = 0;
  protected double Y0 = 0;
  protected double[][] widthX;
  protected double[][] widthY;

  protected void setAppearence() {
    setPreferredSize(defaultSize);
    setSize(defaultSize);
  }

  public void setPlotType(int i,int type) {
    PA.typeList[i] = type;
  }

  public void setPlotColor(int i,Color color) {
    PA.colorList[i] = color;
  }

  public void setPlotLegend(String[] legend) {
    PA.legend = legend;
  }

  public void update() {
    axe = new Axe2D(X0,Y0,X[0],Y[0],this,PA.legend);
    grid = new Grid2D(axe);
    plots = new DataPlot2D[X.length];
    np = new NotedPoint2D();
    for (int i=0;i<X.length;i++) {
      plots[i] = new DataPlot2D(X[i],Y[i],widthX[i],widthY[i],axe,PA.typeList[i],PA.colorList[i]);
    }

    addMouseListener(this);
    addMouseMotionListener(this);

    repaint();
  }

  public void paint(Graphics comp) {
    Graphics2D comp2D = (Graphics2D)comp;
    comp2D.setColor(getBackground());
    comp2D.fillRect(0,0,getSize().width,getSize().height);
    grid.draw(comp2D);
    axe.draw(comp2D);
    for (int i=0;i<plots.length;i++) {
      plots[i].draw(comp2D);
    }
    np.draw(comp2D);

    setBackground(Color.white);
  }

  public void mousePressed(MouseEvent e) {
  }
  public void mouseClicked(MouseEvent e) {
  }
  public void mouseDragged(MouseEvent e) {
  }
  public void mouseReleased(MouseEvent e) {
  }
  public void mouseEntered(MouseEvent e) {
  }
  public void mouseExited(MouseEvent e) {
  }
  public void mouseMoved(MouseEvent e) {

    np.setVisible(false);

    int Sc_x = e.getX();
    int Sc_y = e.getY();

    int[] Sc_XY;
    int Sc_X;
    int Sc_Y;

    double[] Pl_XY;
    double Pl_X;
    double Pl_Y;

    for (int i=0;i<plots.length;i++) {
      Coordinates2D[] all = plots[i].getCoord();
      for (int j = 0;j<all.length;j++) {
        Sc_XY = all[j].getSc();
        Sc_X = Sc_XY[0];
        Sc_Y = Sc_XY[1];
        if ((Math.abs(Sc_x-Sc_X)<5)&&(Math.abs(Sc_y-Sc_Y)<5)) {
          Pl_XY = all[j].getPl();
          Pl_X = Pl_XY[0];
          Pl_Y = Pl_XY[1];
          np = new NotedPoint2D(Pl_X,Pl_Y,axe,grid);
        }
      }
    }
    repaint();
    e.consume();
  }
}

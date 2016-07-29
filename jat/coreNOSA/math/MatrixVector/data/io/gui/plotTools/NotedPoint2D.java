package jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools;

//import javax.swing.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class NotedPoint2D {

  private int[] Sc = new int[2];;
  private double[] Pl = new double[2];
  private double[] Pl_0 = new double[2];

  private int[] Sc_X0 = new int[2];;
  private double[] Pl_X0 = new double[2];
  private int[] Sc_Y0 = new int[2];
  private double[] Pl_Y0 = new double[2];

  private boolean isVisible;

  private Axe2D axe;
  private Grid2D grid;

  public NotedPoint2D() {
    setVisible(false);
  }

  public NotedPoint2D(double x, double y,Axe2D ax,Grid2D g) {

    axe = ax;
    grid = g;

    Pl[0] = x;
    Pl[1] = y;

    Pl_0 = axe.getPl0();

    Pl_X0[0] = Pl_0[0];
    Pl_X0[1] = y;

    Pl_Y0[0] = x;
    Pl_Y0[1] = Pl_0[1];

    Pl2ScConvert();

    setVisible(true);
  }

  private void Pl2ScConvert() {
    Sc = axe.Pl2Sc(Pl);
    Sc_X0 = axe.Pl2Sc(Pl_X0);
    Sc_Y0 = axe.Pl2Sc(Pl_Y0);
  }

  public void setVisible(boolean b) {
    isVisible = b;
  }

  public void draw(Graphics2D comp2D) {
    if (isVisible) {
      //Pl2ScConvert();
      comp2D.setColor(Color.black);
      comp2D.setFont(new Font("Arial",Font.PLAIN,12));
      comp2D.drawLine(Sc[0],Sc[1],Sc_X0[0],Sc_X0[1]);
      comp2D.drawLine(Sc[0],Sc[1],Sc_Y0[0],Sc_Y0[1]);

      comp2D.drawString(new String("(" + grid.troncatedStringX(Pl[0],2) + "," + grid.troncatedStringY(Pl[1],2) +")"),Sc[0],Sc[1]);
      //comp2D.drawString(new String(""+Pl[0]),Sc_Y0[0],Sc_Y0[1]);
      //comp2D.drawString(new String(""+Pl[1]),Sc_X0[0],Sc_X0[1]);

    }
  }

}


package jat.coreNOSA.math.MatrixVector.data.io.gui.plotTools;

import java.awt.Color;

public class PlotAttributes {

  public static int PIXEL = 0;
  public static int DOT = 1;
  public static int LINE = 2;
  public static int DOTLINE = 3;
  public static int BAR = 4;
  public static int DOTBAR = 5;
  public static int HIST = 6;
  public static int GRID = 7;

  private int numberOfElements = 6;

  public static int dotSize = 8;

  public String[] legend = {"X","Y","Z"};
  public Color[] colorList = {Color.blue,Color.red,Color.green,Color.yellow,Color.pink,Color.orange};
  public int[] typeList = {DOT,DOT,DOT,DOT,DOT,DOT};

  public PlotAttributes() {
  }

  public PlotAttributes(String[] leg) {
    setLegend(leg);
  }

  public PlotAttributes(Color[] col) {
    setColorList(col);
  }

  public PlotAttributes(int[] typ) {
    setTypeList(typ);
  }


  public void setLegend(String[] args) {
    legend = new String[args.length];
    for (int i =0;i<args.length;i++) {
      legend[i] = args[i];
    }
  }

  public void setColorList(Color[] args) {
    colorList = new Color[args.length];
    for (int i =0;i<args.length;i++) {
      colorList[i] = args[i];
    }
  }

  public void setTypeList(int[] args) {
    typeList = new int[args.length];
    for (int i =0;i<args.length;i++) {
      typeList[i] = args[i];
    }
  }

  public void setTypeList(int arg) {
    typeList = new int[numberOfElements];
    for (int i =0;i<numberOfElements;i++) {
      typeList[i] = arg;
    }
  }
}
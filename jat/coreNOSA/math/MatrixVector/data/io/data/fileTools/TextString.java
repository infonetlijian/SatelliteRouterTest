package jat.coreNOSA.math.MatrixVector.data.io.data.fileTools;

import jat.coreNOSA.math.MatrixVector.data.Text;

public class TextString {

  private static int decimalSize = 10;

  private Text T;
  private String S;

  public TextString(Text t) {
    T = t;
    S = TextString.printText(T);
  }

  public TextString(String s) {
    S = s;
    T = readText(S);
  }

  public Text getText() {
    return T;
  }

  public String getString() {
    return S;
  }

  public static Text readText(String s) {
    return new Text(s);
  }

  public static String printText(Text t) {
    return t.getString();
  }

}
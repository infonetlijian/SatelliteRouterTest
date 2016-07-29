package jat.coreNOSA.math.MatrixVector.data.function;

import jat.coreNOSA.math.MatrixVector.data.Matrix;
import jat.coreNOSA.math.MatrixVector.data.io.data.MatrixFile;

import java.io.File;

public class InvokeDoubleFunction {

  File functionFile;
  File resultFile;

  public InvokeDoubleFunction(String fn,String rf) {
      functionFile = new File(fn);
      resultFile = new File(rf);
  }

  public InvokeDoubleFunction(File fn,File rf) {
      functionFile = fn;
      resultFile = rf;
  }

  public double eval() {
    try {
      Process p = Runtime.getRuntime().exec(functionFile.getName());
      p.waitFor();
      MatrixFile mf = new MatrixFile(resultFile);
      Matrix X = mf.getMatrix();
      return new Double(X.get(0,0)).doubleValue();
    } catch (Exception e) {
      System.out.println("Error : File " + resultFile +" unreadable : "+e);
      return Double.NaN;
    }
  }

}
package jat.coreNOSA.math.MatrixVector.data.function;


import jat.coreNOSA.math.MatrixVector.data.Matrix;
import jat.coreNOSA.math.MatrixVector.data.io.data.MatrixFile;

import java.io.File;

public class InvokeMatrixFunction {

  File functionFile;
  File resultFile;

  public InvokeMatrixFunction(String fn,String rf) {
      functionFile = new File(fn);
      resultFile = new File(rf);
  }

  public InvokeMatrixFunction(File fn,File rf) {
      functionFile = fn;
      resultFile = rf;
  }

  public Matrix eval() {
    try {
      Process p = Runtime.getRuntime().exec(functionFile.getName());
      p.waitFor();
      MatrixFile mf = new MatrixFile(resultFile);
      Matrix X = mf.getMatrix();
      return X;
    } catch (Exception e) {
      System.out.println("Error : File " + resultFile +" unreadable : "+e);
      return null;
    }
  }

}
package jat.coreNOSA.math.MatrixVector.data.function;

/**
 * <p>Titre : JAva MAtrix TOols</p>
 * <p>Description : </p>
 * @author Yann RICHET
 * @version 1.0
 */

public abstract class TestDoubleFunction {

  protected int argNumber;

  public abstract boolean eval(double[] values);

  public void checkArgNumber(int n) {
    if (argNumber != n) {
      throw new IllegalArgumentException("Number of arguments must equals " + n);
    }
  }
}
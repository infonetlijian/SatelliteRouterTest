package jat.coreNOSA.math.MatrixVector.data.function;

import jat.coreNOSA.math.MatrixVector.data.Matrix;
import jat.coreNOSA.math.MatrixVector.data.function.expressionParser.Evaluator;

public class DoubleFunctionExpression extends DoubleFunction {

  private String expression;
  private String[] variables;
  private Evaluator E = new Evaluator();

  public DoubleFunctionExpression(String exp,String[] vars) {
    argNumber = vars.length;
    setFunction(exp,vars);
  }

  public DoubleFunctionExpression(String exp,String vars) {
    argNumber = 1;
    String[] variable = new String[1];
    variable[0] = vars;
    setFunction(exp,variable);
  }

  private void setFunction(String exp,String[] vars) {
    expression = exp;
    variables = vars;
  }

  private void setVariableValues(double[] values) {
    for (int i = 0;i<variables.length;i++) {
      E.addVariable(variables[i],values[i]);
    }
    E.setExpression(expression);
  }

  private void setVariableValues(double value) {
    E.addVariable(variables[0],value);
    E.setExpression(expression);
  }

  private void setVariableValues(Matrix values) {
    for (int i = 0;i<variables.length;i++) {
      E.addVariable(variables[i],values.get(i,0));
    }
    E.setExpression(expression);
  }

  public double eval(double[] values) {
    checkArgNumber(values.length);
    setVariableValues(values);
    return ((Double)(E.getValue())).doubleValue();
  }

  public double eval(double value) {
    checkArgNumber(1);
    setVariableValues(value);
    return ((Double)(E.getValue())).doubleValue();
  }

  public double eval(Matrix values) {
    checkArgNumber(values.getRowDimension());
    setVariableValues(values);
    return ((Double)(E.getValue())).doubleValue();
  }
}

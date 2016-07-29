package jat.coreNOSA.math.MatrixVector.data.arrayTools;

   /** Shuffle algoritm.
   */

public class Shuffle {

/* ------------------------
   Class variables
 * ------------------------ */

   /** Number of elements to shuffle.
   */
  private int numOE;

   /** Array for internal storage of the order.
   */
  private int[] order;


/* ------------------------
   Constructors
 * ------------------------ */

   /** Construct a shuffled order.
   @param n    Size to shuffle.
   */

  public Shuffle(int n) {
    numOE = n;
    order = shuffle(numOE);
  }

/* ------------------------
   Public Methods
 * ------------------------ */

   /** Get the order of one line.
   @param i    order of the line i.
   @return  order shuffled.
   */

  public int getOrder(int i) {
    return order[i];
  }

   /** Get the order of the entire column.
   @return  orders shuffled.
   */

  public int[] getOrder() {
    return order;
  }

/* ------------------------
   Private Methods
 * ------------------------ */

   /** Shuffle the order.
   @param numOE    Size to shuffle.
   @return  order shuffled.
   */

  private int[] shuffle(int numOE) {
    int[] order_in = new int[numOE];
    for (int i = 0; i < numOE; i++) {
      order_in[i] = i;
    }

    int[] order_out = new int[0];

    for (int i = 0; i < numOE; i++) {
      int ind = randInt(order_in.length-1);
      int val = order_in[ind];
      order_out = put(order_out,val);
      order_in = push(order_in,ind);
    }

    return order_out;
  }

  private int[] put (int[] ind, int add) {
    int[] new_ind = new int[ind.length + 1];

    for (int i = 0; i < ind.length; i++) {
      new_ind[i] = ind[i];
    }
    new_ind[ind.length] = add;

    return new_ind;
  }

  private int[] push (int[] ind, int sub) {
    int[] new_ind = new int[ind.length - 1];

    if (sub == 0) {
      for (int i = 0; i < ind.length - 1; i++)
        new_ind[i] = ind[i+1];
    } else if (sub == ind.length) {
      for (int i = 0; i < ind.length - 1; i++)
        new_ind[i] = ind[i];
    } else {
      for (int i = 0; i < sub; i++)
        new_ind[i] = ind[i];
      for (int i = sub; i < ind.length - 1; i++)
        new_ind[i] = ind[i+1];
    }

    return new_ind;
  }

  /** Generate a random integer.
   @param i    Max of the random variable.
   @return      An int between 0 and i.
   */

  private static int randInt(int i) {
    double x = Math.random();
    int r = new Double(Math.floor((i+1)*x)).intValue();
    return r;
  }

}
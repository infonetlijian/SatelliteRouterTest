package jat.coreNOSA.math.MatrixVector.data.io.gui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class Progress extends JFrame {

  /**
	 * 
	 */
	private static final long serialVersionUID = 9153646141419362522L;
private JProgressBar progress;
  private int min;
  private int max;
  private int val;
  private JPanel pane;

  public Progress(int m,int M) {

    min = m;
    max = M;

    val = min;

    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    pane = new JPanel();

    progress = new JProgressBar(min,max);
    progress.setValue(val);
    progress.setString(null);

    pane.add(progress);
    this.setContentPane(pane);
    this.pack();
    this.setVisible(true);
  }

  public void setValue(int n) {
    val = n;
    progress.setValue(val);
    if (val>=max) setVisible(false);
  }

}
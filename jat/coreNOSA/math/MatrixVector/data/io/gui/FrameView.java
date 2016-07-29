package jat.coreNOSA.math.MatrixVector.data.io.gui;


import javax.swing.JFrame;
import javax.swing.JPanel;

public class FrameView extends JFrame {

  /**
	 * 
	 */
	private static final long serialVersionUID = 6513530503609461907L;

public FrameView(JPanel panel) {
    setContentPane(panel);
    pack();
    setVisible(true);
  }

  public FrameView(String title,JPanel panel) {
    super(title);
    setContentPane(panel);
    pack();
    setVisible(true);
  }

   public FrameView(JPanel[] panels) {
    JPanel panel = new JPanel();
    for (int i=0;i<panels.length;i++) {
      panel.add(panels[i]);
    }
    setContentPane(panel);
    pack();
    setVisible(true);
  }


}


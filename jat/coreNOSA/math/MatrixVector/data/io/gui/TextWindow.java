package jat.coreNOSA.math.MatrixVector.data.io.gui;

import jat.coreNOSA.math.MatrixVector.data.Text;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class TextWindow extends JPanel {

  /**
	 * 
	 */
	private static final long serialVersionUID = 7866316047060272256L;
private Text text;

  public TextWindow(Text t) {
    text  = t;
    toWindow();
  }

  public TextWindow(String s) {
    text = new Text(s);
    toWindow();
  }

  private void toWindow() {
    JTextArea textArea = new JTextArea(text.getString());
    JScrollPane scrollPane = new JScrollPane(textArea);
    add(scrollPane);
  }

}
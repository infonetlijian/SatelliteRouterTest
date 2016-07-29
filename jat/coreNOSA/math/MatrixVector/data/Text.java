package jat.coreNOSA.math.MatrixVector.data;

import jat.coreNOSA.math.MatrixVector.data.io.data.TextFile;
import jat.coreNOSA.math.MatrixVector.data.io.data.fileTools.MatrixString;
import jat.coreNOSA.math.MatrixVector.data.io.gui.FrameView;
import jat.coreNOSA.math.MatrixVector.data.io.gui.TextWindow;

import javax.swing.JPanel;

/**
<P>
   The Text Class is just designed to provide easy-to-use string operations
   like building log files, displaying text in a window, converting matrix to String format...

@author Yann RICHET
@version 2.0
*/


public class Text implements java.io.Serializable {

/* ------------------------
   Class variables
* ------------------------ */

  /**
	 * 
	 */
	private static final long serialVersionUID = -3421968800724612257L;
/** String for internal storage.
  @serial internal string storage.
  */
  private String string = new String("");

/* ------------------------
   Constructor
 * ------------------------ */

   /** Construct a text.
   @param str  String of the text.
   */
  public Text(String str) {
    setString(str);
  }

   /** Construct a text.
   @param X  Matrix to convert in text.
   */
  public Text(Matrix X) {
    setString(X);
  }

/* ------------------------
   Public Methods
 * ------------------------ */

   /** Provides access to the string of the text.
   @return String of the text.
   */
  public String getString() {
    String s = new String(string);
    return s;
  }

   /** Provides access to the string of the text.
   @param str  String of the text.
   */
  public void setString(String str) {
    string = str;
  }

  public void setString(Matrix X) {
    string = MatrixString.printMatrix(X);
  }


   /** Merge the two Texts.
   @param text    text to merge.
   */

  public void merge(Text text) {
    string = new String(string + text.getString());
  }

   /** Merge the Matrix.
   @param X    Matrix to merge.
   */

  public void merge(Matrix X) {
    Text text = new Text(X);
    string = new String(string + text.getString());
  }

   /** Merge the string.
   @param s    String to merge.
   */

  public void merge(String s) {
    string = new String(string + s);
  }

   /** Print the Text in the Command Line.
   */

  public void toCommandLine() {
    System.out.println(string);
  }

   /** Save the Text in a file.
   @param fileName    fileName.
   */

  public void toFile(String fileName) {
    TextFile mf = new TextFile(fileName,this);
  }

   /** Save the text in a file.
   @param file    file.
   */

  public void toFile(java.io.File file) {
    TextFile mf = new TextFile(file,this);
  }

   /** Load the text from a file.
   @param fileName    fileName.
   @return Text.
   */

  public static Text fromFile(String fileName) {
    TextFile mf = new TextFile(fileName);
    return mf.getText();
  }

   /** Save the text from a file.
   @param file    file.
   @return Text.
   */

  public static Text fromFile(java.io.File file) {
    TextFile mf = new TextFile(file);
    return mf.getText();
  }


   /** Display the text in a Window.
   @return JPanel.
   */

  public JPanel toPanel() {
    return new TextWindow(this);
  }

   /** Display the text in a Window in a Frame.
   @param title Title of the JFrame.
   */

  public void toFrame(String title) {
   FrameView fv = new FrameView(title,toPanel());
  }


}
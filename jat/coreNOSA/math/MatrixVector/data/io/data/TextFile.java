package jat.coreNOSA.math.MatrixVector.data.io.data;

import jat.coreNOSA.math.MatrixVector.data.Matrix;
import jat.coreNOSA.math.MatrixVector.data.Text;
import jat.coreNOSA.math.MatrixVector.data.io.data.fileTools.CharFile;

import java.io.File;

public class TextFile {

  private Text text = new Text("");
  private File file;

  public TextFile(File f) {
    file = f;
    if (file.exists())
      text = new Text(CharFile.fromFile(file));
    else
      text = new Text("");
  }

  public TextFile(File f, Text t) {
    text = t;
    file = f;
    CharFile.toFile(file,text.getString());
  }

  public TextFile(File f, String s) {
    text = new Text(s);
    file = f;
    CharFile.toFile(file,text.getString());
  }

  public TextFile(File f, Matrix X) {
    text = new Text(X);
    file = f;
    CharFile.toFile(file,text.getString());
  }

  public TextFile(String fileName) {
    file = new File(fileName);
    if (file.exists())
      text = new Text(CharFile.fromFile(file));
    else
      text = new Text("");
  }

  public TextFile(String fileName, Text t) {
    text = t;
    file = new File(fileName);
    CharFile.toFile(file,text.getString());
  }

  public TextFile(String fileName, String s) {
    text = new Text(s);
    file = new File(fileName);
    CharFile.toFile(file,text.getString());
  }

  public TextFile(String fileName, Matrix X) {
    text = new Text(X);
    file = new File(fileName);
    CharFile.toFile(file,text.getString());
  }

  public void append(Text t) {
    text = new Text(text.getString() + "\n" + t.getString());
    CharFile.toFile(file,text.getString());
  }

  public void append(String s) {
    text = new Text(text.getString() + "\n" + s);
    CharFile.toFile(file,text.getString());
  }

  public void append(Matrix X) {
    text = new Text(text.getString() + "\n" + new Text(X).getString());
    CharFile.toFile(file,text.getString());
  }

  public Text getText() {
    return text;
  }

  public File getFile() {
    return file;
  }

  public String getFileName() {
    return file.getName();
  }
}
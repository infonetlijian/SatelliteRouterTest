package jat.coreNOSA.math.MatrixVector.data.io.data.fileTools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * <p>Titre : JAva MAtrix TOols</p>
 * <p>Description : </p>
 * <p>Copyright : Copyright (c) 2002</p>
 * <p>Soci�t� : IRSN</p>
 * @author Yann RICHET
 * @version 1.0
 */

public class CharFile {

  public static String fromFile(File file)  {
    String string = new String("");
    try {
      FileReader fr = new FileReader(file);
      BufferedReader b = new BufferedReader(fr);
      boolean eof = false;
      while (!eof) {
        String line = b.readLine();
        if (line == null) {
          eof = true;
          string = string.substring(0,string.length()-1);
        } else
          string = string + line + "\n";
      }
      b.close();
    } catch (IOException e) {
      System.out.println("File " + file.getName() + " is unreadable.");
    }
    return string;
  }

  public static void toFile(File file,String s) {
    try {
      FileWriter fw = new FileWriter(file);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(s);
      bw.close();
    } catch (IOException e) {
      System.out.println("File " + file.getName() + " is unwritable.");
    }
  }

}
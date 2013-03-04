package de.guerda.licensereport;

import java.io.File;

import javax.swing.JFileChooser;

public class LicenseReport {

  /**
   * @param args
   */
  public static void main(String[] args) {
    JFileChooser tempJFileChooser = new JFileChooser();
    tempJFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int tempDialogResult = tempJFileChooser.showOpenDialog(null);
    if (tempDialogResult == JFileChooser.APPROVE_OPTION) {
      File tempSelectedFile = tempJFileChooser.getSelectedFile();
    }

  }

}

package de.guerda.licensereport;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * @author Philip Gillißen
 *
 */
public final class LicenseReportTask extends Task {

  Vector<FileSet> fileSets;
  private static final Pattern LICENSE_PATTERN = Pattern.compile("^Bundle-License: (.*)$");

  public LicenseReportTask() {
    fileSets = new Vector<>();
  }

  @Override
  public void execute() throws BuildException {
    validate();
    for (Iterator<FileSet> tmpIterator = fileSets.iterator(); tmpIterator.hasNext();) { // 2
      FileSet tempFileSet = tmpIterator.next();
      DirectoryScanner tempScanner = tempFileSet.getDirectoryScanner(getProject()); // 3
      String[] tempFiles = tempScanner.getIncludedFiles();
      for (String tmpFileName : tempFiles) {
        inspectJar(tempFileSet.getDir(), tmpFileName);
      }
    }
  }

  private void inspectJar(File aDir, String aString) {
    File tmpFile = new File(aDir, aString);
    if (!tmpFile.exists() || !tmpFile.canRead()) {
      throw new BuildException("File not found or not readable: " + aDir.getAbsolutePath() + aString);
    }
    StringBuffer tmpLicense = new StringBuffer();
    String tmpLicenseHead;
    Vector<String> tmpLines;

    // Search MANIFEST.MF for license information
    String tmpManifestFilename = "META-INF/MANIFEST.MF";
    tmpLines = findAndReadFileFromJar(tmpFile, tmpManifestFilename);

    for (String tmpLine : tmpLines) {
      Matcher tmpMatcher = LICENSE_PATTERN.matcher(tmpLine);
      if (tmpMatcher.matches()) {
        tmpLicense.append(tmpManifestFilename + ": " +tmpMatcher.group(1));
        break;
      }
    }

    // Search for META-INF/LICENSE.txt
    tmpLicenseHead = findFileAndExtractHeaderFromJar(tmpFile, "META-INF/LICENSE.txt");
    if (!isBlank(tmpLicenseHead)) {
      tmpLicense.append(tmpLicenseHead);
    }

    // Search for META-INF/LICENSE
    tmpLicenseHead = findFileAndExtractHeaderFromJar(tmpFile, "META-INF/LICENSE");
    if (!isBlank(tmpLicenseHead)) {
      tmpLicense.append(tmpLicenseHead);
    }

    // Search for LICENSE
    tmpLicenseHead = findFileAndExtractHeaderFromJar(tmpFile, "LICENSE");
    if (!isBlank(tmpLicenseHead)) {
      tmpLicense.append(tmpLicenseHead);
    }

    // Search for LICENSE.txt
    tmpLicenseHead = findFileAndExtractHeaderFromJar(tmpFile, "LICENSE.txt");
    if (!isBlank(tmpLicenseHead)) {
      tmpLicense.append(tmpLicenseHead);
    }

    // Search for LICENSE
    tmpLicenseHead = findFileAndExtractHeaderFromJar(tmpFile, "license/LICENSE.txt");
    if (!isBlank(tmpLicenseHead)) {
      tmpLicense.append(tmpLicenseHead);
    }

    if (tmpLicense.length() == 0) {
      System.out.printf("%50s\t%s\r\n", tmpFile.getName(), "No License Information Found");
    } else {
      System.out.printf("%50s\t%s\r\n", tmpFile.getName(), tmpLicense);
    }

  }

  private boolean isBlank(String aString) {
    int tmpLength;
    if (aString == null || (tmpLength = aString.length()) == 0) {
      return true;
    }
    for (int i = 0; i < tmpLength; i++) {
      if ((Character.isWhitespace(aString.charAt(i)) == false)) {
        return false;
      }
    }
    return true;
  }

  private String findFileAndExtractHeaderFromJar(File aFile, String aLicenseFilename) {
    StringBuffer tmpLicenseHead = new StringBuffer();
    Vector<String> tmpLines;
    tmpLines = findAndReadFileFromJar(aFile, aLicenseFilename);
    if (tmpLines.size() > 0) {
      for (int i = 0; i < tmpLines.size() && i <= 5; i++) {
        tmpLicenseHead.append(tmpLines.get(i).trim() + " ");
      }
    }
    if (tmpLicenseHead.toString().length() > 0) {
      return aLicenseFilename + ": " + tmpLicenseHead.toString();
    } else {
      return null;
    }
  }

  private Vector<String> findAndReadFileFromJar(File tmpFile, String tmpManifestFilename) {
    Vector<String> tmpResult = new Vector<>();
    String tmpJarFilePrefix = "jar:file:" + tmpFile.getAbsolutePath() + "!/";
    JarFile tmpJarFile;
    try {
      tmpJarFile = new JarFile(tmpFile);
      if (tmpJarFile.getJarEntry(tmpManifestFilename) != null) {
        try {
          URL url = new URL(tmpJarFilePrefix + tmpManifestFilename);
          InputStream tmpInputStream = url.openStream();
          InputStreamReader tmpInputStreamReader = new InputStreamReader(tmpInputStream);
          BufferedReader tmpBufferedReader = new BufferedReader(tmpInputStreamReader);
          String tmpData = null;
          while ((tmpData = tmpBufferedReader.readLine()) != null) {
            tmpResult.add(tmpData);
          }
        } catch (IOException e) {
          throw new BuildException("Could not read JAR file '" + tmpFile.getAbsolutePath() + "'!", e);
        }
      }
    } catch (IOException e1) {
      throw new BuildException("Could not read JAR file '" + tmpFile.getAbsolutePath() + "'!", e1);
    }
    return tmpResult;
  }

  private void validate() throws BuildException {
    if (fileSets.size() == 0) {
      throw new BuildException("No Files given");
    }
    System.out.println(fileSets.size() + " given filesets.");
  }

  // getter setter

  public void addFileset(FileSet fileset) {
    fileSets.add(fileset);
  }

}

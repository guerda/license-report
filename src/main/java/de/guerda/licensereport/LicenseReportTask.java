package de.guerda.licensereport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Philip Gillißen
 *
 */
public final class LicenseReportTask extends Task {

  private static final String NO_LICENSE_INFORMATION_FOUND = "No License Information Found";

  /**
   * Defines the count of lines which are read from a found text file.
   */
  private static final int COUNT_OF_LINES = 10;

  /**
   * Contains all {@link FileSet}s of JARs which should be inspected for
   * licenses.
   */
  private final Vector<FileSet> fileSets;

  /**
   * Contains the target directory for the results, in XML and HTML.
   */
  private File toDir;

  /**
   * Contains the pattern for the OSGI compatible license declaration in the
   * MANIFEST.MF file.
   */
  private static final Pattern LICENSE_PATTERN = Pattern.compile("^Bundle-License: (.*)$");

  protected Document document;
  protected File resultFile;
  protected Element librariesElement;

  private File htmlOutputFile;

  private HashMap<File, ArrayList<LicenseInformation>> licenseInformations;

  private Comparator<File> fileComparator;

  public LicenseReportTask() {
    fileSets = new Vector<FileSet>();

    fileComparator = new Comparator<File>() {
      @Override
      public int compare(File aO1, File aO2) {
        return aO1.getName().compareTo(aO2.getName());
      }
    };
  }

  @Override
  public void execute() throws BuildException {
    validate();
    initializeFiles();

    // Prepare file list
    ArrayList<File> tempFileList = new ArrayList<File>();
    for (Iterator<FileSet> tmpIterator = fileSets.iterator(); tmpIterator.hasNext();) { // 2
      FileSet tempFileSet = tmpIterator.next();
      DirectoryScanner tempScanner = tempFileSet.getDirectoryScanner(getProject()); // 3
      String[] tempFiles = tempScanner.getIncludedFiles();
      for (String tmpFileName : tempFiles) {
        if (!(tmpFileName.endsWith(".jar") || tmpFileName.endsWith(".JAR"))) {
          log("'" + tmpFileName + "' is not a JAR file!", Project.MSG_WARN);
        } else {
          tempFileList.add(new File(tempFileSet.getDir(), tmpFileName));
        }
      }
    }

    // Sort files
    Collections.sort(tempFileList, fileComparator);

    // Iterate over all collected files.
    for (File tempFile : tempFileList) {
      inspectJar(tempFile);
    }
    createXmlResultFile();
    createHtmlResultFile();
  }

  private void createHtmlResultFile() {
    try {
      DOMSource tmpSource = new DOMSource(document);
      JDOMResult tmpHtmlResult = new JDOMResult();
      InputStream tmpXslResource = LicenseReportTask.class.getClassLoader().getResourceAsStream("license-report.xsl");
      if (tmpXslResource == null) {
        throw new BuildException("Could not load XSL file!");
      }
      StreamSource tmpStreamSource = new StreamSource(tmpXslResource);
      // Create Transformer with XSL
      Transformer tmpTransformer = TransformerFactory.newInstance().newTransformer(tmpStreamSource);
      tmpTransformer.transform(tmpSource, tmpHtmlResult);

      // Create formatter with pretty output
      XMLOutputter tmpOutputter = new XMLOutputter();
      tmpOutputter.setFormat(Format.getPrettyFormat());
      // Write HTML output
      tmpOutputter.output(tmpHtmlResult.getDocument(), new FileWriter(htmlOutputFile));
    } catch (TransformerFactoryConfigurationError e) {
      throw new BuildException("Could not create HTML result file!", e);
    } catch (TransformerException e) {
      throw new BuildException("Could not create HTML result file!", e);
    } catch (IOException e) {
      throw new BuildException("Could not create HTML result file!", e);
    }
  }

  private void createXmlResultFile() {
    Set<File> tempKeySet = licenseInformations.keySet();
    ArrayList<File> tempFileList = new ArrayList<File>(tempKeySet);
    Collections.sort(tempFileList, fileComparator);

    for (File tempFile : tempFileList) {
      String tmpFileName = tempFile.getName();
      Node tmpLibraryElement = document.createElement("library");

      Element tmpNameElement = document.createElement("name");
      tmpNameElement.setTextContent(tmpFileName);
      tmpLibraryElement.appendChild(tmpNameElement);

      Element tmpInformationListElement = document.createElement("information-list");
      tmpLibraryElement.appendChild(tmpInformationListElement);

      for (LicenseInformation tempLicenseInformation : licenseInformations.get(tempFile)) {
        Element tmpInformationElement = document.createElement("information");
        tmpInformationListElement.appendChild(tmpInformationElement);

        String tmpSource = tempLicenseInformation.getSource();
        String tmpLicenseInformation = tempLicenseInformation.getLicenseInformation();

        Element tmpSourceElement = document.createElement("source");
        if (tmpSource != null) {
          tmpSourceElement.setTextContent(tmpSource);
        }
        tmpInformationElement.appendChild(tmpSourceElement);

        Element tmpLicenseElement = document.createElement("license");
        if (tmpLicenseInformation != null) {

          CDATASection tmpCDATASection = document.createCDATASection(tmpLicenseInformation);
          tmpLicenseElement.appendChild(tmpCDATASection);
        }
        tmpInformationElement.appendChild(tmpLicenseElement);

      }
      librariesElement.appendChild(tmpLibraryElement);
    }

    try {
      TransformerFactory tmpTransformerFactory = TransformerFactory.newInstance();
      Transformer tmpTransformer;
      tmpTransformer = tmpTransformerFactory.newTransformer();
      tmpTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
      tmpTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

      DOMSource tmpSource = new DOMSource(document);
      StreamResult tmpResult = new StreamResult(resultFile);

      tmpTransformer.transform(tmpSource, tmpResult);
    } catch (TransformerException e) {
      throw new BuildException("Could not create XML file '" + resultFile.getAbsolutePath() + "'!", e);
    }
  }

  protected void initializeFiles() {
    try {
      licenseInformations = new HashMap<File, ArrayList<LicenseInformation>>();

      DocumentBuilderFactory tmpFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder tmpBuilder = tmpFactory.newDocumentBuilder();
      document = tmpBuilder.newDocument();
      Element rootElement = document.createElement("license-report");
      rootElement.setAttribute("project-name", getProject().getName());
      rootElement.setAttribute("date", new Date().toString());
      document.appendChild(rootElement);
      librariesElement = document.createElement("libraries");
      rootElement.appendChild(librariesElement);

      resultFile = new File(toDir + "/license-report-results.xml");
      htmlOutputFile = new File(toDir + "/license-report-results.html");
      log(resultFile.getAbsolutePath(), Project.MSG_INFO);
    } catch (ParserConfigurationException e) {
      throw new BuildException("Could not create license report results file", e);
    }
  }

  protected void inspectJar(File aJarFile) {
    if (!aJarFile.exists() || !aJarFile.canRead()) {
      throw new BuildException("File not found or not readable: " + aJarFile.getAbsolutePath());
    }
    StringBuffer tmpLicense = new StringBuffer();
    String tmpLicenseHead;
    Vector<String> tmpLines;

    ArrayList<LicenseInformation> tempLicenseInformations = new ArrayList<LicenseInformation>();

    // Search MANIFEST.MF for license information
    String tmpManifestFilename = "META-INF/MANIFEST.MF";
    tmpLines = findAndReadFileFromJar(aJarFile, tmpManifestFilename);

    for (String tmpLine : tmpLines) {
      Matcher tmpMatcher = LICENSE_PATTERN.matcher(tmpLine);
      if (tmpMatcher.matches()) {
        tmpLicense.append(tmpMatcher.group(1));
        break;
      }
    }
    if (tmpLicense.length() > 0) {
      LicenseInformation tempLicenseInformation = new LicenseInformation("META-INF/MANIFEST.MF", tmpLicense.toString());
      tempLicenseInformations.add(tempLicenseInformation);
    }

    LicenseInformation tempLicenseInformation;
    // Search for META-INF/LICENSE.txt
    tempLicenseInformation = findFileInFileAndWriteTo(aJarFile, "META-INF/LICENSE.txt");
    if (tempLicenseInformation != null) {
      tempLicenseInformations.add(tempLicenseInformation);
    }

    // Search for META-INF/LICENSE
    tempLicenseInformation = findFileInFileAndWriteTo(aJarFile, "META-INF/LICENSE");
    if (tempLicenseInformation != null) {
      tempLicenseInformations.add(tempLicenseInformation);
    }

    // Search for LICENSE
    tempLicenseInformation = findFileInFileAndWriteTo(aJarFile, "LICENSE");
    if (tempLicenseInformation != null) {
      tempLicenseInformations.add(tempLicenseInformation);
    }

    // Search for LICENSE.txt
    tempLicenseInformation = findFileInFileAndWriteTo(aJarFile, "LICENSE.txt");
    if (tempLicenseInformation != null) {
      tempLicenseInformations.add(tempLicenseInformation);
    }

    // Search for LICENSE
    tempLicenseInformation = findFileInFileAndWriteTo(aJarFile, "license/LICENSE.txt");
    if (tempLicenseInformation != null) {
      tempLicenseInformations.add(tempLicenseInformation);
    }

    // Search for txt file with the same name
    String tmpPath = aJarFile.getAbsolutePath();
    // .jar -> .txt
    String tempTextFileName = tmpPath.substring(0, tmpPath.length() - 4) + ".txt";
    tmpLicenseHead = findAndReadFile(tempTextFileName);
    if (!isBlank(tmpLicenseHead)) {
      // Create only the relative path.
      String tempShortTextFileName = tempTextFileName.substring(aJarFile.getAbsolutePath().length() - aJarFile.getName().length());
      tempLicenseInformations.add(new LicenseInformation(tempShortTextFileName, tmpLicenseHead));
    }

    if (tempLicenseInformations.size() == 0) {
      System.out.printf("%50s\t%s\r\n", aJarFile.getName(), NO_LICENSE_INFORMATION_FOUND);
      tempLicenseInformations.add(new LicenseInformation(null, null));
      addResultToReport(aJarFile, tempLicenseInformations);
    } else {
      StringBuffer tempString = new StringBuffer();
      for (LicenseInformation tempLicenseInformation1 : tempLicenseInformations) {
        tempString.append(tempLicenseInformation1.toString() + ", ");
      }
      System.out.printf("%50s\t%s\r\n", aJarFile.getName(), tempString.toString());
      addResultToReport(aJarFile, tempLicenseInformations);
    }

  }

  /**
   * @param tmpFile
   * @param tmpLicense
   * @param aLicenseFilename
   */
  public LicenseInformation findFileInFileAndWriteTo(File tmpFile, String aLicenseFilename) {
    String tmpLicenseHead = findFileAndExtractHeaderFromJar(tmpFile, aLicenseFilename);
    if (!isBlank(tmpLicenseHead)) {
      return new LicenseInformation(aLicenseFilename, tmpLicenseHead);
    } else {
      return null;
    }
  }

  private String findAndReadFile(String aFileName) {
    File tmpFile = new File(aFileName);
    if (tmpFile.exists()) {
      StringBuffer tmpResult = new StringBuffer();

      FileReader tmpFileReader = null;
      BufferedReader tmpBufferedReader = null;
      try {
        tmpFileReader = new FileReader(tmpFile);
        tmpBufferedReader = new BufferedReader(tmpFileReader);
        for (int i = 0; i < COUNT_OF_LINES; i++) {
          String tmpData = tmpBufferedReader.readLine();
          if (tmpData == null) {
            break;
          }
          // Remove superficial characters
          tmpData = tmpData.replaceAll("\\*", "");
          tmpData = tmpData.replaceAll("=", "");
          tmpResult.append(tmpData);
        }
      } catch (IOException e) {
        throw new BuildException("Could not read file named '" + aFileName + "'!", e);
      } finally {
        if (tmpFileReader != null) {
          try {
            tmpFileReader.close();
          } catch (IOException e) {
          }
        }
        if (tmpBufferedReader != null) {
          try {
            tmpBufferedReader.close();
          } catch (IOException e) {
          }

        }
      }
      return tmpResult.toString();
    }
    return null;
  }

  private void addResultToReport(File aFile, ArrayList<LicenseInformation> aLicenseInformations) {
    licenseInformations.put(aFile, aLicenseInformations);
  }

  /**
   * Checks, if a String is null or empty.
   *
   * Copied from Apache Commons Lang StringUtils.isBlank()
   *
   * @param aString
   *          The {@link String} to check.
   * @return true, if the given String was empty or null
   */
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
      for (int i = 0; i < tmpLines.size() && i <= COUNT_OF_LINES; i++) {
        tmpLicenseHead.append(tmpLines.get(i).trim() + " ");
      }
    }
    if (tmpLicenseHead.toString().length() > 0) {
      return tmpLicenseHead.toString();
    } else {
      return null;
    }
  }

  private Vector<String> findAndReadFileFromJar(File tmpFile, String tmpManifestFilename) {
    Vector<String> tmpResult = new Vector<String>();
    String tmpJarFilePrefix = "jar:file:" + tmpFile.getAbsolutePath() + "!/";
    JarFile tmpJarFile = null;
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
    } finally {
      try {
        if (tmpJarFile != null) {
          tmpJarFile.close();
        }
      } catch (IOException e) {
      }
    }
    return tmpResult;
  }

  private void validate() throws BuildException {
    if (toDir == null) {
      throw new BuildException("toDir is required");
    }
    if (!toDir.isDirectory()) {
      throw new BuildException("toDir ('" + toDir.getAbsolutePath() + "')has to be a valid directory");
    }

    if (fileSets.size() == 0) {
      throw new BuildException("No Files given");
    }
    log(fileSets.size() + " given filesets.", Project.MSG_INFO);
  }

  // getter setter

  public void addFileset(FileSet fileset) {
    fileSets.add(fileset);
  }

  public File getToDir() {
    return toDir;
  }

  public void setToDir(File aToDir) {
    toDir = aToDir;
  }

  /**
   * Simple getter for {@link #licenseInformations}.
   * 
   * @return the {@link #licenseInformations}
   */
  public HashMap<File, ArrayList<LicenseInformation>> getLicenseInformations() {
    return licenseInformations;
  }

  /**
   * Simple setter for {@link #licenseInformations}.
   * 
   * @param aLicenseInformations
   *          - new value of {@link #licenseInformations}.
   */
  public void setLicenseInformations(HashMap<File, ArrayList<LicenseInformation>> aLicenseInformations) {
    licenseInformations = aLicenseInformations;
  }

}

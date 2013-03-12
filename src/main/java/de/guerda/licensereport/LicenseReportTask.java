package de.guerda.licensereport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
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

  private static final int COUNT_OF_LINES = 10;

  /**
   * Contains all {@link FileSet}s of JARs which should be inspected for
   * licenses.
   */
  private final Vector<FileSet> fileSets;

  /**
   * Contains the target directory for the results, in XML and HTML.
   */
  private String toDir;

  /**
   * Contains the pattern for the OSGI compatible license declaration in the
   * MANIFEST.MF file.
   */
  private static final Pattern LICENSE_PATTERN = Pattern.compile("^Bundle-License: (.*)$");

  private Document document;
  private File resultFile;
  private Element librariesElement;

  private File htmlOutputFile;

  public LicenseReportTask() {
    fileSets = new Vector<FileSet>();
  }

  @Override
  public void execute() throws BuildException {
    validate();
    initialize();

    for (Iterator<FileSet> tmpIterator = fileSets.iterator(); tmpIterator.hasNext();) { // 2
      FileSet tempFileSet = tmpIterator.next();
      DirectoryScanner tempScanner = tempFileSet.getDirectoryScanner(getProject()); // 3
      String[] tempFiles = tempScanner.getIncludedFiles();
      for (String tmpFileName : tempFiles) {
        inspectJar(tempFileSet.getDir(), tmpFileName);
      }
    }
    createXmlResultFile();
    createHtmlResultFile();
  }

  private void createHtmlResultFile() {
    try {
      DOMSource tmpSource = new DOMSource(document);
      JDOMResult tmpHtmlResult = new JDOMResult();
      InputStream tmpXslResource = LicenseReportTask.class.getResourceAsStream("../../../license-report.xsl");
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

  private void initialize() {
    try {
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

  private void inspectJar(File aDir, String aString) {
    if (!(aString.endsWith(".jar") || aString.endsWith(".JAR"))) {
      log("'" + aString + "' is not a JAR file!", Project.MSG_WARN);
    }
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
        tmpLicense.append(tmpManifestFilename + ": " + tmpMatcher.group(1));
        break;
      }
    }

    // Search for META-INF/LICENSE.txt
    findFileInFileAndWriteTo(tmpFile, tmpLicense, "META-INF/LICENSE.txt");

    // Search for META-INF/LICENSE
    findFileInFileAndWriteTo(tmpFile, tmpLicense, "META-INF/LICENSE");

    // Search for LICENSE
    findFileInFileAndWriteTo(tmpFile, tmpLicense, "LICENSE");

    // Search for LICENSE.txt
    findFileInFileAndWriteTo(tmpFile, tmpLicense, "LICENSE.txt");

    // Search for LICENSE
    findFileInFileAndWriteTo(tmpFile, tmpLicense, "license/LICENSE.txt");

    // Search for txt file with the same name
    String tmpPath = tmpFile.getAbsolutePath();
    tmpLicenseHead = findAndReadFile(tmpPath.substring(0, tmpPath.length() - 4) + ".txt"); // .jar
                                                                                           // ->
                                                                                           // .txt
    if (!isBlank(tmpLicenseHead)) {
      tmpLicense.append(tmpLicenseHead);
    }

    if (tmpLicense.length() == 0) {
      System.out.printf("%50s\t%s\r\n", tmpFile.getName(), "No License Information Found");
      addResultToReport(tmpFile.getName(), "No License Information Found");
    } else {
      System.out.printf("%50s\t%s\r\n", tmpFile.getName(), tmpLicense);
      addResultToReport(tmpFile.getName(), tmpLicense.toString());
    }

  }

  /**
   * @param tmpFile
   * @param tmpLicense
   * @param tmpLicenseFilename
   */
  public void findFileInFileAndWriteTo(File tmpFile, StringBuffer tmpLicense, String tmpLicenseFilename) {
    String tmpLicenseHead = findFileAndExtractHeaderFromJar(tmpFile, tmpLicenseFilename);
    if (!isBlank(tmpLicenseHead)) {
      tmpLicense.append(tmpLicenseHead);
    }
  }

  private String findAndReadFile(String aFileName) {
    File tmpFile = new File(aFileName);
    if (tmpFile.exists()) {
      StringBuffer tmpResult = new StringBuffer(tmpFile.getName() + ": ");

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

  private void addResultToReport(String aName, String aString) {
    Node tmpChild = document.createElement("library");
    Element tmpNameElement = document.createElement("name");
    tmpNameElement.setTextContent(aName);
    tmpChild.appendChild(tmpNameElement);

    Element tmpLicenseElement = document.createElement("license");
    CDATASection tmpCDATASection = document.createCDATASection(aString);
    tmpLicenseElement.appendChild(tmpCDATASection);
    tmpChild.appendChild(tmpLicenseElement);

    librariesElement.appendChild(tmpChild);
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
      return aLicenseFilename + ": " + tmpLicenseHead.toString();
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
    if (isBlank(toDir)) {
      throw new BuildException("No Output Directory (attribute 'toDir') given");
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

  public String getToDir() {
    return toDir;
  }

  public void setToDir(String aToDir) {
    toDir = aToDir;
  }

}

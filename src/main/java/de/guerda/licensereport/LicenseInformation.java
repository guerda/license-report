package de.guerda.licensereport;

import java.io.File;

public class LicenseInformation {

  private File file;

  private String source;

  private String licenseInformation;

  public LicenseInformation(File aFile, String aSource, String aLicenseInformation) {
    super();
    file = aFile;
    source = aSource;
    licenseInformation = aLicenseInformation;
  }

  public File getFile() {
    return file;
  }

  public void setFile(File aFile) {
    file = aFile;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String aSource) {
    source = aSource;
  }

  public String getLicenseInformation() {
    return licenseInformation;
  }

  public void setLicenseInformation(String aLicenseInformation) {
    licenseInformation = aLicenseInformation;
  }

  @Override
  public String toString() {
    return source + ": " + licenseInformation;
  }

}
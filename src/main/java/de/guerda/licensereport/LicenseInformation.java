package de.guerda.licensereport;

public class LicenseInformation {

  private String fileName;

  private String source;

  private String licenseInformation;

  public LicenseInformation(String aFileName, String aSource, String aLicenseInformation) {
    super();
    fileName = aFileName;
    source = aSource;
    licenseInformation = aLicenseInformation;
  }
  
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String aFileName) {
    fileName = aFileName;
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
  
}
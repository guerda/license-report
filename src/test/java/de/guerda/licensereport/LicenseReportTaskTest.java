package de.guerda.licensereport;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import org.junit.Before;
import org.junit.Test;

public class LicenseReportTaskTest {

  private LicenseReportTask task;

  @Before
  public void setUp() {
    task = new LicenseReportTask();

    File tempDir = new File("target/");
    System.out.println(tempDir.getAbsolutePath());

    // Add filesets
    FileSet tempFileset = new FileSet();
    tempFileset.setIncludes("**/*.jar");
    tempFileset.setDir(tempDir);
    task.addFileset(tempFileset);

    // Add project
    Project tempProject = new Project();
    tempProject.setName("license-report JUnit test");
    tempProject.setBaseDir(tempDir);
    task.setProject(tempProject);

    // Add output directory
    task.setToDir(tempDir.getAbsolutePath());
  }

  @Test
  public void testExecute() {
    task.execute();
  }
}

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
    FileSet tempFileset = new FileSet();
    tempFileset.setIncludes("**/*.jar");
    File tempDir = new File("target/");
    System.out.println(tempDir.getAbsolutePath());
    tempFileset.setDir(tempDir);
    Project tempProject = new Project();
    tempProject.setBaseDir(tempDir);
    task.setProject(tempProject);
    task.addFileset(tempFileset);
  }

  @Test
  public void testExecute() {
    task.execute();
  }
}

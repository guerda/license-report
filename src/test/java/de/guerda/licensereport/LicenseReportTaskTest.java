package de.guerda.licensereport;

import java.io.File;
import java.util.ArrayList;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.hamcrest.CoreMatchers;

import org.junit.Assert;
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
    task.setToDir(tempDir);

    task.initializeFiles();
  }

  @Test
  public void testExecute() {
    task.execute();
  }

  private void extractAndCheck(File tempJarFile) {
    task.inspectJar(tempJarFile);
    ArrayList<LicenseInformation> tempArrayList = task.getLicenseInformations().get(tempJarFile);
    Assert.assertNotNull(tempArrayList);
    int tempLength = tempArrayList.size();
    Assert.assertThat(tempLength, CoreMatchers.not(CoreMatchers.is(0)));
  }

  @Test
  public void testInspectJar1() {
    File tempJarFile = new File("target/test-classes/1.jar");
    extractAndCheck(tempJarFile);
  }

  @Test
  public void testInspectJar2() {
    File tempJarFile = new File("target/test-classes/2.jar");
    extractAndCheck(tempJarFile);
  }

  @Test
  public void testInspectJar3() {
    File tempJarFile = new File("target/test-classes/3.jar");
    extractAndCheck(tempJarFile);
  }

  @Test
  public void testInspectJar4() {
    File tempJarFile = new File("target/test-classes/4.jar");
    extractAndCheck(tempJarFile);
  }

  @Test
  public void testInspectJar5() {
    File tempJarFile = new File("target/test-classes/5.jar");
    extractAndCheck(tempJarFile);
  }

  @Test
  public void testInspectJar6() {
    File tempJarFile = new File("target/test-classes/6.jar");
    extractAndCheck(tempJarFile);
  }

  @Test
  public void testInspectJar7() {
    File tempJarFile = new File("target/test-classes/7.jar");
    extractAndCheck(tempJarFile);
  }

  @Test
  public void testInspectJar8() {
    File tempJarFile = new File("target/test-classes/8.jar");
    extractAndCheck(tempJarFile);
  }

  @Test
  public void testInspectJar9() {
    File tempJarFile = new File("target/test-classes/9.jar");
    task.inspectJar(tempJarFile);
    ArrayList<LicenseInformation> tempArrayList = task.getLicenseInformations().get(tempJarFile);
    Assert.assertNotNull(tempArrayList);
    int tempLength = tempArrayList.size();
    Assert.assertThat(tempLength, CoreMatchers.not(CoreMatchers.is(0)));
    Assert.assertThat(tempLength, CoreMatchers.is(2));
  }

}

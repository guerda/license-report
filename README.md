LicenseReport
=============
The LicenseReport is an [Apache Ant](http://ant.apache.org) plugin to create a
small report about all used licenses in a specific project.

As there was no solution for this problem, the project idea started on the
following question on [StackOverflow](http://stackoverflow.com):
[How can I compile a report showing all used licenses with Ant?](http://stackoverflow.com/questions/15024819/how-can-i-compile-a-report-showing-all-used-licenses-with-ant)

Features
--------
* Scanning all defined JAR files for license information
* Compiling the found results into an XML file
* Generating a nice HTML report for humans

Usage
-----
1. Generate the JAR file via `mvn clean package`
2. Include the generated JAR file (you'll find it at `target/license-report-x.x.jar`) into your Ant project
3. Put the following snippet in the head, so that the task definition is known. Be sure to adapt the paths: `<taskdef name="license-report" classpath="${basedir}\lib\license-report-1.0.jar" classname="de.guerda.licensereport.LicenseReportTask" />`
4. Call the `license-report` task with a snippet like this. Be sure to include all your JARs with the filesets.
    `<license-report>
      <fileset dir="${basedir}\lib">
        <include name="**/*.jar" />
      </fileset>
    </license-report>`    
5. Build your project as you normally do.
6. Enjoy your created report!

You can also see a demo of this task in the [license-report-demo project on Github](https://github.com/guerda/license-report-demo/)

License
-------
The LicenseReport is available under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
The full license text can be found in the file LICENSE.


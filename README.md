# renjin-hamcrest-maven-plugin
A maven plugin to execute R tests using the Renjin ScriptEngine

It executes R Hamcrest test located in src/test/R dir per default.
Testthat tests are also supported. 

In some regards it is not as advanced as the test plugin in the renjin-maven-plugin e.g.
tests are not forked and hence slightly slower; also a severely misbehaving test could crash the build but
it does provide more developer friendly output saving you from having to analyze log output
in the test result files. Also print() in the test will write to the console just like a
junit test would do. 

To use it add the following to your maven build plugins section:

```xml
<plugins>
  <plugin>
    <groupId>se.alipsa</groupId>
    <artifactId>renjin-test-maven-plugin</artifactId>
    <version>1.3</version>
    <configuration>
      <testFailureIgnore>true</testFailureIgnore>
    </configuration>
    <executions>
      <execution>
        <phase>test</phase>
        <goals>
          <goal>testR</goal>
        </goals>
      </execution>
    </executions>
    <dependencies>
      <dependency>
        <groupId>org.renjin</groupId>
        <artifactId>renjin-script-engine</artifactId>
        <version>${renjin.version}</version>
        <exclusions>
          <!-- optional but needed if you use e.g.slf4j (then use the jcl bridge instead) -->
          <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
          </exclusion>
        </exclusions>
      </dependency>
      <dependency>
        <groupId>org.renjin</groupId>
        <artifactId>hamcrest</artifactId>
        <version>${renjin.version}</version>
      </dependency>
    </dependencies>
  </plugin>
</plugins>
```

To use the latest code, build it with `mvn clean install` and then add the plugin to your pom as follows:  

```xml
<plugins>
  <plugin>
    <groupId>se.alipsa</groupId>
    <artifactId>renjin-test-maven-plugin</artifactId>
    <!-- match the version with the version in the plugin pom -->
    <version>1.3</version>
    <configuration>
      <testFailureIgnore>true</testFailureIgnore>
    </configuration>
    <executions>
      <execution>
        <phase>test</phase>
        <goals>
          <goal>testR</goal>
        </goals>
      </execution>
    </executions>
    <dependencies>
      <dependency>
        <groupId>org.renjin</groupId>
        <artifactId>renjin-script-engine</artifactId>
        <version>${renjin.version}</version>
        <exclusions>
          <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
          </exclusion>
        </exclusions>
      </dependency>
      <dependency>
        <groupId>org.renjin</groupId>
        <artifactId>hamcrest</artifactId>
        <version>${renjin.version}</version>
      </dependency>
    </dependencies>
  </plugin>
</plugins>
```
Where ${renjin.version} is the version of Renjin you want to use e.g. 0.9.2719

### Configuration
- reportOutputDirectory 
    - where the test logs will be, default to "${project.build.directory}/renjin-hamcrest-test-reports"
- testSourceDirectory 
    - where the test sources reside, defaults to "${project.basedir}/src/test/R
- testOutputDirectory
    - where the tests will be executed from, defaults to "${project.build.testOutputDirectory}"   
- skipTests
    - Whether to skip tests altogether, defaults to false  
- testFailureIgnore
    - Whether to halt the build on the first failure encountered or not, defaults to false
- runSourceScriptsBeforeTests
    -Whether to run the R scripts in src/main/R prior to running tests )useful for non-package projects), 
    defaults for false.
- sourceDirectory
    - where the main R scripts are, defaults to "${project.basedir}/src/main/R"
- printSuccess
    - excho "Success" after each test is successful, defaults to false
                
Example of overriding a few parameters:
````
    <build>
        <plugins>
            <plugin>
                <groupId>se.alipsa</groupId>
                <artifactId>renjin-test-maven-plugin</artifactId>
                <version>1.3</version>
                <configuration>
                    <outputDirectory>target/test-harness/project-to-test</outputDirectory>
                    <testSourceDirectory>R/test</testSourceDirectory>
                    <testFailureIgnore>true</testFailureIgnore>
                </configuration>
                <executions>
                  <execution>
                    <phase>test</phase>
                    <goals>
                      <goal>testR</goal>
                    </goals>
                  </execution>
                </executions>
                <dependencies>
                  <dependency>
                    <groupId>org.renjin</groupId>
                    <artifactId>renjin-script-engine</artifactId>
                    <version>${renjin.version}</version>
                    <exclusions>
                      <exclusion>
                        <groupId>commons-logging</groupId>
                        <artifactId>commons-logging</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                  <dependency>
                    <groupId>org.renjin</groupId>
                    <artifactId>hamcrest</artifactId>
                    <version>${renjin.version}</version>
                  </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
````              
# Generate a test report
You can use the surefire report plugin to generate a nice looking html report in the target/site dir.
Add something like the following to your maven pom:

```xml
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-report-plugin</artifactId>
        <version>3.0.0-M3</version>
        <configuration>
          <title>R Tests Report</title>
          <outputName>test-report</outputName>
          <reportsDirectories>${project.build.directory}/renjin-test-reports</reportsDirectories>
          <linkXRef>false</linkXRef>
        </configuration>
        <executions>
          <execution>
            <phase>test</phase>
            <goals>
              <goal>report-only</goal>
            </goals>
          </execution>
        </executions>
    </plugin>
    <!-- the site plugin will create formatting stuff e.g. css etc. --> 
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-site-plugin</artifactId>
        <version>3.7.1</version>
        <configuration>
          <generateReports>false</generateReports>
        </configuration>
        <executions>
          <execution>
            <phase>test</phase>
            <goals>
              <goal>site</goal>
            </goals>
          </execution>
        </executions>
    </plugin>
</plugins>
```      
Then run `mvn clean test site` or similar to generate the report

# Executing both hamcrest and testthat tests
If you have both in one project you need to add an additional execution target
```xml   
<plugin>
<groupId>se.alipsa</groupId>
<artifactId>renjin-test-maven-plugin</artifactId>
<version>1.3</version>
<executions>
  <execution>
    <phase>test</phase>
    <id>testthat</id>
    <goals>
      <goal>testR</goal>
    </goals>
    <configuration>
      <testFailureIgnore>true</testFailureIgnore>
      <testSourceDirectory>${project.basedir}/tests</testSourceDirectory>
    </configuration>
  </execution>
  <execution>
    <id>hamcrest</id>
    <phase>test</phase>
    <goals>
      <goal>testR</goal>
    </goals>
    <configuration>
      <testFailureIgnore>true</testFailureIgnore>
      <testSourceDirectory>${project.basedir}/src/test/R</testSourceDirectory>
    </configuration>
  </execution>
</executions>
<dependencies>
  <dependency>
    <groupId>org.renjin</groupId>
    <artifactId>renjin-script-engine</artifactId>
    <version>${renjin.version}</version>
    <exclusions>
      <exclusion>
        <groupId>commons-logging</groupId>
        <artifactId>commons-logging</artifactId>
      </exclusion>
    </exclusions>
  </dependency>
  <dependency>
    <groupId>org.renjin</groupId>
    <artifactId>hamcrest</artifactId>
    <version>${renjin.version}</version>
  </dependency>
  <dependency>
    <groupId>org.renjin.cran</groupId>
    <artifactId>testthat</artifactId>
    <version>2.1.1-b2</version>
  </dependency>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-log4j12</artifactId>
    <version>1.7.30</version>
  </dependency>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jcl-over-slf4j</artifactId>
    <version>1.7.30</version>
  </dependency>         
</dependencies>
</plugin>
``` 
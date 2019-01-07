# renjin-hamcrest-maven-plugin
A maven plugin to execute R hamcrest tests using the Renjin ScriptEngine

It executes R Hamcrest test located in src/test/R dir.

In some regards it is not as advanced as the test plugin in the renjin-maven-plugin e.g.
tests are not forked and hence a severely misbehaving test could crash the build but
it does provide more developer friendly output saving you from having to analyze log output
in the test result files. Also print() in the test will write to the console just like a
junit test would do. 

To use it, build it with `mvn clean install` and then add the plugin to your pom as follows:  

````
      <plugin>
        <groupId>se.alipsa</groupId>
        <artifactId>renjin-hamcrest-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
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
          <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>jcl-over-slf4j</artifactId>
            <version>1.7.25</version>
          </dependency>
          <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.25</version>
          </dependency>
          <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
            <version>1.7.25</version>
          </dependency>
        </dependencies>
      </plugin>
````

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
    - Whether to halt the build on the first faiure encountered or not, defaults to false
    
Example of overriding a few parameters:
````
    <build>
        <plugins>
            <plugin>
                <groupId>se.alipsa</groupId>
                <artifactId>renjin-hamcrest-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <configuration>
                    <outputDirectory>target/test-harness/project-to-test</outputDirectory>
                    <testSourceDirectory>R/test</testSourceDirectory>
                    <testFailureIgnore>true</testFailureIgnore>
                </configuration>
            </plugin>
        </plugins>
    </build>
````              


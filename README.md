# JAR Runner Maven Plugin
This is a Maven plugin to execute JAR artifacts from command line. Apache Maven manages a project's build, reporting and documentation using a project object model (POM). But it's lacking support for executing artifacts from command line using a single line of code. The JAR Runner Maven Plugin aims at changing that.

## Getting started
All you require to launch Maven artifacts is Maven itself - no POM. Then go ahead with the below example.

	mvn --quiet de.lars-sh:jar-runner-maven-plugin:run -Dartifact=com.github.spotbugs:spotbugs:LATEST

Maven is run, calls the `run` goal of the JAR Runner Maven Plugin, which itself resolves all dependencies of the artifact and launches the main class as described inside the JARs manifest. The Maven option `--quiet` simply hides away the maven-own output.

### Main Class and Arguments
For sure you can go even further and apply a custom main class and arguments.

	mvn --quiet de.lars-sh:jar-runner-maven-plugin:run -Dartifact=com.puppycrawl.tools:checkstyle:LATEST -DmainClass=com.puppycrawl.tools.checkstyle.Main -Darguments=-v

### Exit code
The returned exit code is handled by Maven. In case of success it's zero, in case of error it's non-zero, noting the original exit value as text output. Checkout the following example and proove it using either `echo $?` (Unix systems) or `echo %ERRORLEVEL%` (Windows systems).

	mvn --quiet de.lars-sh:jar-runner-maven-plugin:run -Dartifact=com.puppycrawl.tools:checkstyle:LATEST -DmainClass=com.puppycrawl.tools.checkstyle.Main

### Even further
There are more arguments available. Just checkout the `help` goal as shown below.

	mvn de.lars-sh:jar-runner-maven-plugin:help -Dgoal=run -Ddetail=true
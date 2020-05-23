# JAR Runner Maven Plugin
This is a Maven plugin to execute JAR artifacts from command line. Apache Maven manages a project's build, reporting and documentation using a project object model (POM). But it's lacking support for executing artifacts from command line using a single line of code. The JAR Runner Maven Plugin aims at changing that.

[Changelog](CHANGELOG.md)  |  [Plugin Documentation](https://lars-sh.github.io/jar-runner-maven-plugin/plugin-info.html)

## Getting started
All you require to launch Maven artifacts is Maven itself - no POM. Then go ahead with the below example.

	mvn --quiet de.lars-sh:jar-runner-maven-plugin:run -Dartifact=com.github.spotbugs:spotbugs:LATEST

Maven is executed, calls the `run` goal of the JAR Runner Maven Plugin, which itself resolves all dependencies of the artifact and launches the main class as described inside the JARs manifest. The Maven option `--quiet` simply hides away the maven-own output.

### Main Class
Some JAR files do not include a proper manifest or they include multiple main classes. Therefore you can apply a custom main class.

	mvn --quiet de.lars-sh:jar-runner-maven-plugin:run -Dartifact=com.puppycrawl.tools:checkstyle:LATEST -DmainClass=com.puppycrawl.tools.checkstyle.Main

### Arguments
You might add a single argument as follows.

	mvn --quiet de.lars-sh:jar-runner-maven-plugin:run -Dartifact=com.puppycrawl.tools:checkstyle:LATEST -DmainClass=com.puppycrawl.tools.checkstyle.Main -Darguments=--version

Use `,` or `],[` to separate multiple arguments from each other. Instead `[,]` can be used to represent an actual comma character.

### Run asynchronously
Coming back to the first example you may wonder why the command line stopped working until the SpotBugs windows were closed. To handle these cases you can set the `runAsync` argument to `true`.

	mvn --quiet de.lars-sh:jar-runner-maven-plugin:run -Dartifact=com.github.spotbugs:spotbugs:LATEST -DrunAsync=true

### Exit code
The applications exit code is inherited. In case of success it should be zero, in case of error it should be non-zero. Checkout the following example and prove it using either `echo $?` (Unix) or `echo %ERRORLEVEL%` (Windows).

### Even further
There are more arguments available. Just checkout the `help` goal as shown below.

	mvn de.lars-sh:jar-runner-maven-plugin:help -Dgoal=run -Ddetail=true
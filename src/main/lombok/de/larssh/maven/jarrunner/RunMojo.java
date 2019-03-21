package de.larssh.maven.jarrunner;

import java.util.List;
import java.util.Objects;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

/**
 * Mojo to launch a new JVM based on a given artifact with optional arguments.
 *
 * <p>
 * The given artifacts dependencies are resolved and a fully qualified class
 * path is created.
 *
 * <p>
 * The main class to execute can either be specified by argument or is taken
 * from the artifacts JAR.
 */
@NoArgsConstructor
@Mojo(name = "run", requiresDirectInvocation = true, requiresProject = false)
public class RunMojo extends AbstractMojo {
	/**
	 * Maven Session
	 */
	@NonFinal
	@Nullable
	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession mavenSession = null;

	/**
	 * Aether Repository System
	 */
	@NonFinal
	@Nullable
	@Component
	private RepositorySystem repositorySystem = null;

	/**
	 * Aether Repository System Session
	 */
	@NonFinal
	@Nullable
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repositorySystemSession = null;

	/**
	 * User argument: Artifact to execute
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "artifact", required = true)
	private String artifact = null;

	/**
	 * User argument: Main Class
	 *
	 * <p>
	 * Default: the artifacts JARs main class
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "mainClass")
	private String mainClass = null;

	/**
	 * User argument: Arguments
	 *
	 * <p>
	 * Default: none
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "arguments")
	private List<String> arguments = null;

	/**
	 * User argument: Class Path Format
	 *
	 * <p>
	 * Default: "%s"
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "classPathFormat")
	private String classPathFormat = null;

	/**
	 * User argument: Path to Java executable
	 *
	 * <p>
	 * Default: current Java executable
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "javaPath")
	private String javaPath = null;

	/**
	 * User argument: Java Options
	 *
	 * <p>
	 * Default: none
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "javaOptions")
	private List<String> javaOptions = null;

	/**
	 * User argument: Working Directory
	 *
	 * <p>
	 * Default: current working directory
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "workingDirectory")
	private String workingDirectory = null;

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("checkstyle:IllegalCatch")
	@SuppressFBWarnings(value = { "REC_CATCH_EXCEPTION", "WEM_WEAK_EXCEPTION_MESSAGING" },
			justification = "catching any exception at execution root")
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			new JarRunner(new Parameters(Objects.requireNonNull(mavenSession),
					Objects.requireNonNull(repositorySystem),
					Objects.requireNonNull(repositorySystemSession),
					artifact,
					mainClass,
					arguments,
					classPathFormat,
					javaPath,
					javaOptions,
					workingDirectory)).run();
		} catch (final MojoFailureException e) {
			throw e;
		} catch (final Exception e) {
			throw new MojoExecutionException("Unexpected exception thrown.", e);
		}
	}
}

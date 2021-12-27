package de.larssh.maven.jarrunner;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import de.larssh.utils.Nullables;
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
@SuppressWarnings("PMD.ImmutableField")
public class RunMojo extends AbstractMojo {
	/**
	 * Maven Session
	 */
	@NonFinal
	@Nullable
	@Parameter(defaultValue = "${session}", readonly = true)
	MavenSession mavenSession = null;

	/**
	 * Aether Repository System
	 */
	@NonFinal
	@Nullable
	@Component
	RepositorySystem repositorySystem = null;

	/**
	 * Aether Repository System Session
	 */
	@NonFinal
	@Nullable
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	RepositorySystemSession repositorySystemSession = null;

	/**
	 * Artifact to load
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "artifact", required = true)
	String artifact = null;

	/**
	 * Main class to execute
	 *
	 * <p>
	 * Default: the artifacts JARs main class
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "mainClass")
	String mainClass = null;

	/**
	 * List of arguments for the to-be-executed application
	 *
	 * <p>
	 * Default: none
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "arguments")
	String arguments = null;

	/**
	 * Run application asynchronously
	 *
	 * <p>
	 * If this argument equals "true" the JAR Runner instance stops right after
	 * starting the application. The applications streams will not be inherited.
	 *
	 * <p>
	 * Default: false
	 */
	@NonFinal
	@Parameter(property = "runAsync")
	boolean runAsync = false;

	/**
	 * Formatter value that allows modifying the class path. Substring "%s" is
	 * replaced with the generated class path.
	 *
	 * <p>
	 * Default: "%s"
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "classPathFormat")
	String classPathFormat = null;

	/**
	 * Path to the Java executable
	 *
	 * <p>
	 * Default: path to the Java executable used by Maven
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "javaPath")
	String javaPath = null;

	/**
	 * List of options for the Java VM
	 *
	 * <p>
	 * Default: none
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "javaOptions")
	String javaOptions = null;

	/**
	 * List of Maven repository URLs
	 *
	 * <p>
	 * User name and password can be inserted as URI user info, delimited by colon.
	 * Order is: user name, password converter (either base64 or plain), password. A
	 * repository ID can be set using the URI fragment.
	 *
	 * <p>
	 * Example: http://user:base64:cGFzc3dvcmQ=@repository.example.com/path#id
	 *
	 * <p>
	 * In case of multiple repositories with the same ID the first repository in
	 * order is used. Following repositories with the same ID are ignored.
	 * Repositories of user parameters are handled at first.
	 *
	 * <p>
	 * Repository layout and proxy cannot be set via user argument.
	 *
	 * <p>
	 * Default: none
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "repositories")
	String repositories = null;

	/**
	 * Ignore system repositories
	 *
	 * <p>
	 * Default: false
	 */
	@NonFinal
	@Parameter(property = "ignoreSystemRepositories")
	boolean ignoreSystemRepositories = false;

	/**
	 * Working Directory for the to-be-executed application
	 *
	 * <p>
	 * Default: current working directory
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "workingDirectory")
	String workingDirectory = null;

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings({
			"checkstyle:IllegalCatch",
			"PMD.AvoidCatchingGenericException",
			"PMD.AvoidRethrowingException" })
	@SuppressFBWarnings(value = { "REC_CATCH_EXCEPTION", "WEM_WEAK_EXCEPTION_MESSAGING" },
			justification = "catching any exception at execution root")
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			final JarRunner jarRunner = new JarRunner(new Parameters(Nullables.orElseThrow(mavenSession),
					Nullables.orElseThrow(repositorySystem),
					Nullables.orElseThrow(repositorySystemSession),
					artifact,
					mainClass,
					arguments,
					runAsync,
					classPathFormat,
					javaPath,
					javaOptions,
					repositories,
					ignoreSystemRepositories,
					workingDirectory));

			if (getLog().isInfoEnabled()) {
				getLog().info("Command: " + jarRunner.getCommandLine());
			}

			jarRunner.execute();
		} catch (final MojoFailureException e) {
			throw e;
		} catch (final Exception e) {
			throw new MojoExecutionException("Unexpected exception thrown.", e);
		}
	}

	/**
	 * This dummy method forces IDE automatisms to keep fields non-final.
	 */
	@SuppressWarnings({ "PMD.NullAssignment", "PMD.UnusedPrivateMethod" })
	@SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "dummy method")
	private void nonFinalDummy() {
		mavenSession = null;
		repositorySystem = null;
		repositorySystemSession = null;
		artifact = null;
		mainClass = null;
		arguments = null;
		runAsync = false;
		classPathFormat = null;
		javaPath = null;
		javaOptions = null;
		repositories = null;
		ignoreSystemRepositories = false;
		workingDirectory = null;
	}
}

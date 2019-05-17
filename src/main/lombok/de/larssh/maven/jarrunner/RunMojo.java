package de.larssh.maven.jarrunner;

import java.util.List;

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
	 * Artifact to load
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "artifact", required = true)
	private String artifact = null;

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
	private List<String> repositories = null;

	/**
	 * Ignore system repositories
	 *
	 * <p>
	 * Default: false
	 */
	@NonFinal
	@Parameter(property = "ignoreSystemRepositories")
	private boolean ignoreSystemRepositories = false;

	/**
	 * Main class to execute
	 *
	 * <p>
	 * Default: the artifacts JARs main class
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "mainClass")
	private String mainClass = null;

	/**
	 * List of arguments for the to-be-executed application
	 *
	 * <p>
	 * Default: none
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "arguments")
	private List<String> arguments = null;

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
	private String classPathFormat = null;

	/**
	 * Path to the Java executable
	 *
	 * <p>
	 * Default: path to the Java executable used by Maven
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "javaPath")
	private String javaPath = null;

	/**
	 * List of options for the Java VM
	 *
	 * <p>
	 * Default: none
	 */
	@NonFinal
	@Nullable
	@Parameter(property = "javaOptions")
	private List<String> javaOptions = null;

	/**
	 * Working Directory for the to-be-executed application
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
	@SuppressWarnings({
			"checkstyle:IllegalCatch",
			"PMD.AvoidCatchingGenericException",
			"PMD.AvoidRethrowingException" })
	@SuppressFBWarnings(value = { "REC_CATCH_EXCEPTION", "WEM_WEAK_EXCEPTION_MESSAGING" },
			justification = "catching any exception at execution root")
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			new JarRunner(new Parameters(Nullables.orElseThrow(mavenSession),
					Nullables.orElseThrow(repositorySystem),
					Nullables.orElseThrow(repositorySystemSession),
					artifact,
					mainClass,
					arguments,
					classPathFormat,
					javaPath,
					javaOptions,
					repositories,
					ignoreSystemRepositories,
					workingDirectory)).run();
		} catch (final MojoFailureException e) {
			throw e;
		} catch (final Exception e) {
			throw new MojoExecutionException("Unexpected exception thrown.", e);
		}
	}
}

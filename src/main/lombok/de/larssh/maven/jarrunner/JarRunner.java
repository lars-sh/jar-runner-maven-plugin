package de.larssh.maven.jarrunner;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes.Name;
import java.util.jar.JarInputStream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

import de.larssh.utils.SystemUtils;
import de.larssh.utils.io.ProcessBuilders;
import de.larssh.utils.text.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;

/**
 * Launches a new JVM based on a given artifact with optional arguments.
 *
 * <p>
 * This is the implementation for {@link RunMojo}. The work is done by
 * {@link #execute()}.
 */
@Getter
public final class JarRunner {
	/**
	 * Combines the class paths of {@code dependencyResult} with the optional class
	 * path format user argument.
	 *
	 * <p>
	 * The following code has already been prepared for maven-resolver-util v2:
	 *
	 * <pre>
	 * final NodeListGenerator nodeListGenerator = new NodeListGenerator();
	 * dependencyResult.getRoot().accept(new PreorderDependencyNodeConsumerVisitor(nodeListGenerator));
	 * final String classPath = nodeListGenerator.getClassPath();
	 * </pre>
	 *
	 * @param dependencyResult Resolved dependencies
	 * @param classPathFormat  formatter value allowing modifying the class path
	 * @return class path string
	 */
	private static String getClassPath(final DependencyResult dependencyResult,
			final Optional<String> classPathFormat) {
		final PreorderNodeListGenerator preorderNodeListGenerator = new PreorderNodeListGenerator();
		dependencyResult.getRoot().accept(preorderNodeListGenerator);
		final String classPath = preorderNodeListGenerator.getClassPath();
		return classPathFormat.map(format -> String.format(format, classPath)).orElse(classPath);
	}

	/**
	 * Determines the path to a Java executable by either user argument or current
	 * JVM instance.
	 *
	 * @param javaPath the path to the Java executable
	 * @return path to Java executable
	 */
	private static Path getJavaExecutable(final Optional<Path> javaPath) {
		return javaPath.orElseGet(SystemUtils::getJavaExecutable);
	}

	/**
	 * Determines the main class to call by either user argument or artifacts JAR
	 * manifest.
	 *
	 * @param mainClassParameter the main class to execute as per user input
	 * @param dependencyResult   Resolved dependencies
	 * @return main class to call
	 * @throws IOException          if any IO failure occurred
	 * @throws MojoFailureException if no main class is given and the artifacts JAR
	 *                              does not contain a main class in its manifest
	 */
	private static String getMainClass(final Optional<String> mainClassParameter,
			final DependencyResult dependencyResult) throws IOException, MojoFailureException {
		// by Argument
		if (mainClassParameter.isPresent()) {
			return mainClassParameter.get();
		}

		// by Manifest
		final Path jarFile = dependencyResult.getRoot().getArtifact().getFile().toPath();
		try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(jarFile))) {
			final String mainClass = jarInputStream.getManifest().getMainAttributes().getValue(Name.MAIN_CLASS);
			if (!Strings.isBlank(mainClass)) {
				return mainClass;
			}
		}

		// fail
		throw new MojoFailureException(Strings.format(
				"Could not find a %s entry inside the root JARs [%s] manifest. You can provide a main class yourself using -DmainClass=...",
				Name.MAIN_CLASS.toString(),
				jarFile.toAbsolutePath()));
	}

	/**
	 * Creates a list of repositories based on the repositories given by the user
	 * via parameter and the system repositories.
	 *
	 * <p>
	 * The user can leave parameters empty. The system repositories can be ignored
	 * using the system parameter "ignoreSystemRepositories".
	 *
	 * <p>
	 * In case of multiple repositories with the same ID the first repository in
	 * order is used. Following repositories with the same ID are ignored.
	 * Repositories of user parameters are handled at first.
	 *
	 * @param parameters the parameters object of {@link RunMojo}
	 * @return list of repositories
	 */
	@SuppressFBWarnings(value = "PSC_PRESIZE_COLLECTIONS",
			justification = "presizing collections is not worth it in this place")
	private static List<RemoteRepository> getRepositories(final Parameters parameters) {
		final Set<String> idsForExistanceCheck = new HashSet<>();

		// via Parameter
		final List<RemoteRepository> repositories = new ArrayList<>();
		for (final RemoteRepository repository : parameters.getRepositories()) {
			if (idsForExistanceCheck.add(repository.getId())) {
				repositories.add(repository);
			}
		}

		// via System
		if (!parameters.isIgnoreSystemRepositories()) {
			final List<RemoteRepository> systemRepositories
					= RepositoryUtils.toRepos(parameters.getMavenSession().getRequest().getRemoteRepositories());
			for (final RemoteRepository repository : systemRepositories) {
				if (idsForExistanceCheck.add(repository.getId())) {
					repositories.add(repository);
				}
			}
		}

		return unmodifiableList(repositories);
	}

	/**
	 * Resolves the dependencies for the artifact given by user argument.
	 *
	 * @param parameters the parameters object of {@link RunMojo}
	 * @return the resolved dependencies
	 * @throws DependencyResolutionException if resolving dependencies failed
	 */
	private static DependencyResult resolveDependencies(final Parameters parameters)
			throws DependencyResolutionException {
		final Dependency dependency = new Dependency(parameters.getArtifact(), DependencyScope.COMPILE.getValue());
		final CollectRequest collectRequest = new CollectRequest(dependency, getRepositories(parameters));
		final Collection<String> includedScopes
				= asList(DependencyScope.COMPILE.getValue(), DependencyScope.RUNTIME.getValue());
		final DependencyRequest dependencyRequest
				= new DependencyRequest(collectRequest, new ScopeDependencyFilter(includedScopes, null));
		return parameters.getRepositorySystem()
				.resolveDependencies(parameters.getRepositorySystemSession(), dependencyRequest);
	}

	/**
	 * Causes the current thread to wait, if necessary, until {@code process} has
	 * terminated. This method returns immediately if {@code process} already
	 * terminated. If {@code process} has not yet terminated, the calling thread
	 * will be blocked until {@code process} exits.
	 *
	 * @param process the process to wait for
	 * @return exit value of {@code process} (By convention, the value 0 indicates
	 *         normal termination.)
	 */
	@SuppressWarnings("java:S2142")
	private static int waitForWithoutInterrupting(final Process process) {
		try {
			return process.waitFor();
		} catch (@SuppressWarnings("unused") final InterruptedException ignore) {
			// Instead of rethrowing the InterruptedException we destroy the subprocess and
			// wait for it to end.
			process.destroy();
			return waitForWithoutInterrupting(process);
		}
	}

	/**
	 * Parameters object maintaining the injected system objects and user arguments
	 * of {@link RunMojo}
	 *
	 * @return parameters
	 */
	Parameters parameters;

	/**
	 * The command prepared to launch the artifact in a new JVM
	 *
	 * @return the process builder
	 */
	ProcessBuilder processBuilder;

	/**
	 * Prepares a command (in form of an internal {@link ProcessBuilder}) to launch
	 * a new JVM based on a given artifact with optional arguments.
	 *
	 * <p>
	 * The given artifacts dependencies are resolved and a fully qualified class
	 * path is created.
	 *
	 * <p>
	 * The main class to execute can either be specified by argument or is taken
	 * from the artifacts JAR.
	 *
	 * @param parameters the parameters used to create the command
	 * @throws DependencyResolutionException if resolving dependencies failed
	 * @throws IOException                   if any IO failure occurred
	 * @throws MojoFailureException          either if no main class is given and
	 *                                       the artifacts JAR does not contain a
	 *                                       main class in its manifest or the
	 *                                       started application stopped with an
	 *                                       exit value not equal to zero
	 */
	@SuppressFBWarnings(value = "COMMAND_INJECTION", justification = "command injection expected")
	public JarRunner(final Parameters parameters)
			throws DependencyResolutionException, MojoFailureException, IOException {
		this.parameters = parameters;

		// Resolve Dependencies
		final DependencyResult dependencyResult = resolveDependencies(parameters);

		// Build Java execution command
		final List<String> commands = new ArrayList<>();
		commands.add(getJavaExecutable(parameters.getJavaPath()).toString());
		commands.addAll(parameters.getJavaOptions());
		commands.add("-classpath");
		commands.add(getClassPath(dependencyResult, parameters.getClassPathFormat()));
		commands.add(getMainClass(parameters.getMainClass(), dependencyResult));
		commands.addAll(parameters.getArguments());

		// Build Java process
		processBuilder = new ProcessBuilder(commands);
		parameters.getWorkingDirectory().map(Path::toFile).ifPresent(processBuilder::directory);
	}

	/**
	 * Launches a new JVM based on the instances command.
	 *
	 * @throws IOException if any IO failure occurred
	 */
	@SuppressWarnings("PMD.DoNotTerminateVM")
	@SuppressFBWarnings(value = { "COMMAND_INJECTION", "DM_EXIT" },
			justification = "command is meant to be injected and exit code need to be passed")
	public void execute() throws IOException {
		if (getParameters().isRunAsync()) {
			getProcessBuilder().start();
		} else {
			final int exitStatus = waitForWithoutInterrupting(getProcessBuilder().inheritIO().start());
			if (exitStatus != 0) {
				System.exit(exitStatus);
			}
		}
	}

	/**
	 * Returns a command to launch this {@link JarRunner}.
	 *
	 * <p>
	 * The results of this method depend on the Operating System, as command line
	 * arguments on Unix and Windows are quoted and escaped differently.
	 *
	 * @return the escaped command
	 */
	public String getCommandLine() {
		return ProcessBuilders.toCommandLine(getProcessBuilder(), true);
	}
}

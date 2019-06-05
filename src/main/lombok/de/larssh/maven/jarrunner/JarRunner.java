package de.larssh.maven.jarrunner;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.io.File;
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

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

import de.larssh.utils.SystemUtils;
import de.larssh.utils.maven.AetherUtils;
import de.larssh.utils.maven.DependencyScope;
import de.larssh.utils.text.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Launches a new JVM based on a given artifact with optional arguments.
 *
 * <p>
 * This is the implementation for {@link RunMojo}. The work is done by
 * {@link #run()}.
 */
@Getter
@RequiredArgsConstructor
public class JarRunner {
	/**
	 * Parameters object maintaining the injected system objects and user arguments
	 * of {@link RunMojo}
	 *
	 * @return parameters
	 */
	Parameters parameters;

	/**
	 * Launches a new JVM based on a given artifact with optional arguments.
	 *
	 * <p>
	 * The given artifacts dependencies are resolved and a fully qualified class
	 * path is created.
	 *
	 * <p>
	 * The main class to execute can either be specified by argument or is taken
	 * from the artifacts JAR.
	 *
	 * @throws DependencyResolutionException if resolving dependencies failed
	 * @throws IOException                   if any IO failure occurred
	 * @throws MojoFailureException          either if no main class is given and
	 *                                       the artifacts JAR does not contain a
	 *                                       main class in its manifest or the
	 *                                       started application stopped with an
	 *                                       exit value not equal to zero
	 */
	@SuppressFBWarnings(value = "COMMAND_INJECTION", justification = "command is really meant to be injected")
	public void run() throws DependencyResolutionException, IOException, MojoFailureException {
		// Resolve Dependencies
		final DependencyResult dependencyResult = resolveDependencies();

		// Build Java execution command
		final List<String> commands = new ArrayList<>();
		commands.add(getJavaExecutable());
		commands.addAll(getParameters().getJavaOptions());
		commands.add("-classpath");
		commands.add(getClassPath(dependencyResult));
		commands.add(getMainClass(dependencyResult));
		commands.addAll(getParameters().getArguments());

		// Build Java process
		final ProcessBuilder builder = new ProcessBuilder(commands.toArray(new String[0]));
		getParameters().getWorkingDirectory().map(Path::toFile).ifPresent(builder::directory);

		// Execute Java process
		if (getParameters().isRunAsync()) {
			builder.start();
		} else {
			final int exitStatus = waitForWithoutInterrupting(builder.inheritIO().start());
			if (exitStatus != 0) {
				System.exit(exitStatus);
			}
		}
	}

	/**
	 * Combines the class paths of {@code dependencyResult} with the optional class
	 * path format user argument.
	 *
	 * @param dependencyResult Resolved dependencies
	 * @return class path string
	 */
	private String getClassPath(final DependencyResult dependencyResult) {
		final String classPath = AetherUtils.getClassPath(dependencyResult);
		return getParameters().getClassPathFormat().map(format -> String.format(format, classPath)).orElse(classPath);
	}

	/**
	 * Determines the path to a Java executable by either user argument or current
	 * JVM instance.
	 *
	 * @return path to Java executable
	 */
	private String getJavaExecutable() {
		// by Argument
		final Optional<Path> javaPath = getParameters().getJavaPath();
		if (javaPath.isPresent()) {
			return javaPath.get().toString();
		}

		// by Property
		return SystemUtils.getJavaExecutable().toString();
	}

	/**
	 * Determines the main class to call by either user argument or artifacts JAR
	 * manifest.
	 *
	 * @param dependencyResult Resolved dependencies
	 * @return main class to call
	 * @throws IOException          if any IO failure occurred
	 * @throws MojoFailureException if no main class is given and the artifacts JAR
	 *                              does not contain a main class in its manifest
	 */
	private String getMainClass(final DependencyResult dependencyResult) throws IOException, MojoFailureException {
		// by Argument
		final Optional<String> optionalMainClass = getParameters().getMainClass();
		if (optionalMainClass.isPresent()) {
			return optionalMainClass.get();
		}

		// by Manifest
		final File jarFile = dependencyResult.getRoot().getArtifact().getFile();
		try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(jarFile.toPath()))) {
			final String mainClass = jarInputStream.getManifest().getMainAttributes().getValue(Name.MAIN_CLASS);
			if (!Strings.isBlank(mainClass)) {
				return mainClass;
			}
		}

		// fail
		throw new MojoFailureException(Strings.format(
				"Could not find a %s entry inside the root JARs [%s] manifest. You can provide a main class yourself using -DmainClass=...",
				Name.MAIN_CLASS.toString(),
				jarFile.getAbsolutePath()));
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
	 * @return list of repositories
	 */
	@SuppressFBWarnings(value = "PSC_PRESIZE_COLLECTIONS",
			justification = "presizing collections is not worth it in this place")
	private List<RemoteRepository> getRepositories() {
		final Set<String> idsForExistanceCheck = new HashSet<>();

		// via Parameter
		final List<RemoteRepository> repositories = new ArrayList<>();
		for (final RemoteRepository repository : getParameters().getRepositories()) {
			if (idsForExistanceCheck.add(repository.getId())) {
				repositories.add(repository);
			}
		}

		// via System
		if (!getParameters().isIgnoreSystemRepositories()) {
			final List<RemoteRepository> systemRepositories
					= AetherUtils.getRemoteRepositories(getParameters().getMavenSession());
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
	 * @return the resolved dependencies
	 * @throws DependencyResolutionException if resolving dependencies failed
	 */
	private DependencyResult resolveDependencies() throws DependencyResolutionException {
		final Dependency dependency = new Dependency(getParameters().getArtifact(), DependencyScope.COMPILE.getValue());
		final CollectRequest collectRequest = new CollectRequest(dependency, getRepositories());
		final Collection<String> includedScopes
				= asList(DependencyScope.COMPILE.getValue(), DependencyScope.RUNTIME.getValue());
		final DependencyRequest dependencyRequest
				= new DependencyRequest(collectRequest, new ScopeDependencyFilter(includedScopes, null));
		return getParameters().getRepositorySystem()
				.resolveDependencies(getParameters().getRepositorySystemSession(), dependencyRequest);
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
	private int waitForWithoutInterrupting(final Process process) {
		try {
			return process.waitFor();
		} catch (final InterruptedException e) {
			return waitForWithoutInterrupting(process);
		}
	}
}

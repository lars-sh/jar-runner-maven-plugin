package de.larssh.maven.jarrunner;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import de.larssh.utils.Optionals;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Getter;

/**
 * Parameters object maintaining the injected system objects and user arguments
 * of {@link RunMojo}.
 */
@Getter
public class Parameters {
	/**
	 * User argument: Arguments
	 *
	 * <p>
	 * Default: none
	 *
	 * @return Arguments
	 */
	List<String> arguments;

	/**
	 * User argument: Artifact to execute
	 *
	 * <p>
	 * Required.
	 *
	 * @return Artifact
	 */
	Artifact artifact;

	/**
	 * User argument: Class Path Format
	 *
	 * <p>
	 * Default: "%s"
	 *
	 * @return Class Path Format
	 */
	Optional<String> classPathFormat;

	/**
	 * User argument: Path to Java executable
	 *
	 * <p>
	 * Default: current Java executable
	 *
	 * @return Path to Java executable
	 */
	Optional<Path> javaPath;

	/**
	 * User argument: Java Options
	 *
	 * <p>
	 * Default: none
	 *
	 * @return Java Options
	 */
	List<String> javaOptions;

	/**
	 * User argument: Main Class
	 *
	 * <p>
	 * Default: the artifacts JARs main class
	 *
	 * @return Main Class
	 */
	Optional<String> mainClass;

	/**
	 * Maven Session
	 *
	 * @return Maven Session
	 */
	MavenSession mavenSession;

	/**
	 * Aether Repository System
	 *
	 * @return Aether Repository System
	 */
	RepositorySystem repositorySystem;

	/**
	 * Aether Repository System Session
	 *
	 * @return Aether Repository System Session
	 */
	RepositorySystemSession repositorySystemSession;

	/**
	 * User argument: Working Directory
	 *
	 * <p>
	 * Default: current working directory
	 *
	 * @return Working Directory
	 */
	Optional<Path> workingDirectory;

	/**
	 * Constructor taking injected system objects and user arguments of
	 * {@link RunMojo}.
	 *
	 * <p>
	 * Arguments are validated and converted from Maven argument types to property
	 * types.
	 *
	 * @param mavenSession            Maven Session
	 * @param repositorySystem        Aether Repository System
	 * @param repositorySystemSession Aether Repository System Session
	 * @param artifact                Artifact
	 * @param classPathFormat         Class Path Format
	 * @param mainClass               Main Class
	 * @param arguments               Arguments
	 * @param javaPath                Java Path
	 * @param javaOptions             Java Options
	 * @param workingDirectory        Working Directory
	 */
	@SuppressWarnings("checkstyle:ParameterNumber")
	public Parameters(final MavenSession mavenSession,
			final RepositorySystem repositorySystem,
			final RepositorySystemSession repositorySystemSession,
			@Nullable final String artifact,
			@Nullable final String mainClass,
			@Nullable final List<String> arguments,
			@Nullable final String classPathFormat,
			@Nullable final String javaPath,
			@Nullable final List<String> javaOptions,
			@Nullable final String workingDirectory) {
		this.mavenSession = mavenSession;
		this.repositorySystem = repositorySystem;
		this.repositorySystemSession = repositorySystemSession;
		this.artifact = new DefaultArtifact(artifact);
		this.classPathFormat = Optionals.ofNonBlank(classPathFormat);
		this.mainClass = Optionals.ofNonBlank(mainClass);
		this.arguments = arguments == null ? emptyList() : unmodifiableList(new ArrayList<>(arguments));
		this.javaPath = Optionals.ofNonBlank(javaPath).map(Paths::get);
		this.javaOptions = javaOptions == null ? emptyList() : unmodifiableList(new ArrayList<>(javaOptions));
		this.workingDirectory = Optionals.ofNonBlank(workingDirectory).map(Paths::get);
	}
}

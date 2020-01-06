package de.larssh.maven.jarrunner;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import de.larssh.utils.Optionals;
import de.larssh.utils.SneakyException;
import de.larssh.utils.text.Patterns;
import de.larssh.utils.text.StringConverters;
import de.larssh.utils.text.Strings;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Parameters object maintaining the injected system objects and user arguments
 * of {@link RunMojo}.
 */
@Getter
@SuppressWarnings("PMD.DataClass")
public class Parameters {
	/**
	 * Format for repository IDs of user argument repositories without ID.
	 */
	private static final String REPOSITORY_ID_FORMAT_DEFAULT = "argument-%d";

	/**
	 * Layout of user argument repositories
	 */
	private static final String REPOSITORY_LAYOUT_DEFAULT = "default";

	/**
	 * Pattern of the user info part of user argument repository URIs
	 */
	private static final Pattern REPOSITORY_USER_INFO_PATTERN
			= Pattern.compile("^(?<userName>.*?)(:(?<converter>base64|plain):(?<password>.*))?$");

	/**
	 * Creates a list of {@link RemoteRepository} based on a list of repository URI
	 * strings of a user argument.
	 *
	 * @param uris list of repository URI strings
	 * @return list of repositories
	 */
	private static List<RemoteRepository> getRepositories(@Nullable final List<String> uris) {
		if (uris == null) {
			return emptyList();
		}

		final List<RemoteRepository> repositories = new ArrayList<>(uris.size());
		final int size = uris.size();
		for (int index = 0; index < size; index += 1) {
			repositories.add(getRepository(URI.create(uris.get(index)), index));
		}
		return unmodifiableList(repositories);
	}

	/**
	 * Creates a {@link RemoteRepository} based on a URI.
	 *
	 * @param uri   repository URI with optional user info for authentication
	 *              details and optional fragment for the repository ID
	 * @param index index of the repository in the list of user argument
	 *              repositories, used for generating a repository ID in case no URI
	 *              fragment is given
	 * @return a repository
	 */
	@SuppressWarnings("PMD.ShortVariable")
	private static RemoteRepository getRepository(final URI uri, final int index) {
		final String id = Strings.isBlank(uri.getFragment())
				? Strings.format(REPOSITORY_ID_FORMAT_DEFAULT, index + 1)
				: uri.getFragment();
		return new RemoteRepository.Builder(id, REPOSITORY_LAYOUT_DEFAULT, getRepositoryUrl(uri))
				.setAuthentication(getRepositoryAuthentication(uri))
				.build();
	}

	/**
	 * Creates a repository URL based on the user argument URI
	 *
	 * @param uri user argument URI
	 * @return repository URL
	 */
	@SuppressFBWarnings(value = "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS",
			justification = "converting checked to unchecked exceptions that must not be thrown")
	private static String getRepositoryUrl(final URI uri) {
		try {
			return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), null)
					.toString();
		} catch (final URISyntaxException e) {
			throw new SneakyException(e);
		}
	}

	/**
	 * Creates an {@link Authentication} object based on the user argument URI or
	 * {@code null} if no authentication information is given.
	 *
	 * @param uri user argument URI
	 * @return {@link Authentication} object or {@code null}
	 */
	@Nullable
	private static Authentication getRepositoryAuthentication(final URI uri) {
		if (Strings.isBlank(uri.getUserInfo())) {
			return null;
		}

		final Matcher matcher = Patterns.matches(REPOSITORY_USER_INFO_PATTERN, uri.getUserInfo())
				.orElseThrow(() -> new IllegalArgumentException(
						Strings.format("The user information part \"%s\" does not match the expected format.",
								uri.getUserInfo())));
		final String userName = matcher.group("userName");
		final String password
				= RepositoryPasswordConverter.convert(matcher.group("converter"), matcher.group("password"));
		return new AuthenticationBuilder().addUsername(userName).addPassword(password).build();
	}

	/**
	 * List of arguments for the to-be-executed application
	 *
	 * <p>
	 * Default: none
	 *
	 * @return List of arguments for the to-be-executed application
	 */
	List<String> arguments;

	/**
	 * Artifact to load
	 *
	 * <p>
	 * Required.
	 *
	 * @return Artifact to load
	 */
	Artifact artifact;

	/**
	 * Formatter value that allows modifying the class path. Substring "%s" is
	 * replaced with the generated class path.
	 *
	 * <p>
	 * Default: "%s"
	 *
	 * @return Class Path Format
	 */
	Optional<String> classPathFormat;

	/**
	 * Ignore system repositories
	 *
	 * <p>
	 * Default: false
	 *
	 * @return Ignore system repositories
	 */
	boolean ignoreSystemRepositories;

	/**
	 * Path to the Java executable
	 *
	 * <p>
	 * Default: path to the Java executable used by Maven
	 *
	 * @return Path to the Java executable
	 */
	Optional<Path> javaPath;

	/**
	 * List of options for the Java VM
	 *
	 * <p>
	 * Default: none
	 *
	 * @return List of options for the Java VM
	 */
	List<String> javaOptions;

	/**
	 * Main class to execute
	 *
	 * <p>
	 * Default: the artifacts JARs main class
	 *
	 * @return Main class to execute
	 */
	Optional<String> mainClass;

	/**
	 * Maven Session
	 *
	 * @return Maven Session
	 */
	MavenSession mavenSession;

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
	 *
	 * @return List of Maven repository URLs
	 */
	List<RemoteRepository> repositories;

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
	 * Run application asynchronously
	 *
	 * <p>
	 * If this argument equals "true" the JAR Runner instance stops right after
	 * starting the application. The applications stream will not be inherited.
	 *
	 * <p>
	 * Default: false
	 *
	 * @return {@code true} if the application shall run asynchronously
	 */
	boolean runAsync;

	/**
	 * Working Directory for the to-be-executed application
	 *
	 * <p>
	 * Default: current working directory
	 *
	 * @return Working Directory for the to-be-executed application
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
	 * @param mavenSession             Maven Session
	 * @param repositorySystem         Aether Repository System
	 * @param repositorySystemSession  Aether Repository System Session
	 * @param artifact                 Artifact
	 * @param mainClass                Main Class
	 * @param arguments                Arguments
	 * @param runAsync                 Run application asynchronously
	 * @param classPathFormat          Class Path Format
	 * @param javaPath                 Java Path
	 * @param javaOptions              Java Options
	 * @param repositories             List of Repositories
	 * @param ignoreSystemRepositories Ignore System Repositories
	 * @param workingDirectory         Working Directory
	 */
	@SuppressWarnings({ "checkstyle:ParameterNumber", "PMD.ExcessiveParameterList" })
	public Parameters(final MavenSession mavenSession,
			final RepositorySystem repositorySystem,
			final RepositorySystemSession repositorySystemSession,
			@Nullable final String artifact,
			@Nullable final String mainClass,
			@Nullable final List<String> arguments,
			final boolean runAsync,
			@Nullable final String classPathFormat,
			@Nullable final String javaPath,
			@Nullable final List<String> javaOptions,
			@Nullable final List<String> repositories,
			final boolean ignoreSystemRepositories,
			@Nullable final String workingDirectory) {
		this.mavenSession = mavenSession;
		this.repositorySystem = repositorySystem;
		this.repositorySystemSession = repositorySystemSession;
		this.artifact = new DefaultArtifact(artifact);
		this.mainClass = Optionals.ofNonBlank(mainClass);
		this.arguments = arguments == null ? emptyList() : unmodifiableList(new ArrayList<>(arguments));
		this.runAsync = runAsync;
		this.classPathFormat = Optionals.ofNonBlank(classPathFormat);
		this.javaPath = Optionals.ofNonBlank(javaPath).map(Paths::get);
		this.javaOptions = javaOptions == null ? emptyList() : unmodifiableList(new ArrayList<>(javaOptions));
		this.repositories = getRepositories(repositories);
		this.ignoreSystemRepositories = ignoreSystemRepositories;
		this.workingDirectory = Optionals.ofNonBlank(workingDirectory).map(Paths::get);
	}

	/**
	 * Converters used to load passwords for repositories.
	 */
	@Getter
	@RequiredArgsConstructor
	private enum RepositoryPasswordConverter {
		/**
		 * Base64 URL encoded password
		 *
		 * <p>
		 * The use of this converter does not secure passwords but makes them hardly
		 * readable for human.
		 */
		BASE64(StringConverters::decodeBase64Url),

		/**
		 * Plain password
		 *
		 * <p>
		 * The use of this converter is <b>not</b> recommended.
		 */
		PLAIN(Function.identity());

		/**
		 * Converts {@code password} using the converter with name
		 * {@code converterName}.
		 *
		 * @param converterName name of the converter to use
		 * @param password      password as given by the user
		 * @return plain password
		 */
		@Nullable
		public static String convert(@Nullable final String converterName, @Nullable final String password) {
			if (Strings.isBlank(password)) {
				return null;
			}
			for (final RepositoryPasswordConverter converter : values()) {
				if (converter.name().equalsIgnoreCase(converterName)) {
					return converter.getConverter().apply(password);
				}
			}
			throw new IllegalArgumentException(Strings.format(
					"Unknown password converter given. Allowed values: \"base64\" or \"plain\". Given: \"%s\"",
					converterName));
		}

		/**
		 * Converting function
		 *
		 * @return converting function
		 */
		Function<String, String> converter;
	}
}

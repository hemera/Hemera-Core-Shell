package hemera.core.shell.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import hemera.core.environment.config.Configuration;
import hemera.core.environment.enumn.EEnvironment;
import hemera.core.environment.util.UEnvironment;
import hemera.core.shell.enumn.EShell;
import hemera.core.utility.FileUtils;
import hemera.core.utility.shell.Shell;

import org.xml.sax.SAXException;

/**
 * <code>JSVCScriptGenerator</code> defines the singleton
 * implementation that provides the functionality to
 * generate shell scripts that execute the JSVC Apache
 * daemon process.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public enum JSVCScriptGenerator {
	/**
	 * The singleton instance.
	 */
	instance;

	/**
	 * Export both start and stop scripts based on the
	 * current environment setup to the runtime binary
	 * directory derived from the specified home.
	 * @param homeDir The <code>String</code> runtime
	 * home directory.
	 * @param config The <code>Configuration</code>
	 * used by the environment.
	 * @throws IOException If file processing failed.
	 * @throws ParserConfigurationException If parsing
	 * configuration failed.
	 * @throws SAXException If parsing configuration
	 * failed.
	 * @throws InterruptedException If the command was
	 * interrupted.
	 */
	public void exportScripts(final String homeDir, final Configuration config) throws IOException, SAXException, ParserConfigurationException, InterruptedException {
		final String binDir = UEnvironment.instance.getBinDir(homeDir);
		// Start script.
		final String startScriptContents = JSVCScriptGenerator.instance.generateStartScript(homeDir, config);
		final String startTarget = binDir + EShell.JSVCStartScriptFile.value;
		FileUtils.instance.writeAsString(startScriptContents, startTarget);
		Shell.instance.makeExecutable(startTarget);
		// Stop script.
		final String stopScriptContents = JSVCScriptGenerator.instance.generateStopScript(homeDir, config);
		final String stopTarget = binDir + EShell.JSVCStopScriptFile.value;
		FileUtils.instance.writeAsString(stopScriptContents, stopTarget);
		Shell.instance.makeExecutable(stopTarget);
	}

	/**
	 * Generate the JSVC stop script. This script will
	 * scan all internal libraries as well as all the
	 * deployed applications and their libraries.
	 * @param homeDir The <code>String</code> runtime
	 * home directory.
	 * @param config The <code>Configuration</code>
	 * used by the environment.
	 * @return The <code>String</code> script.
	 * @throws SAXException If loading configuration
	 * from file failed.
	 * @throws IOException If loading configuration
	 * from file failed.
	 * @throws ParserConfigurationException If loading
	 * configuration from file failed.
	 */
	private String generateStopScript(final String homeDir, final Configuration config) throws SAXException, IOException, ParserConfigurationException {
		final String header = this.buildHeader(homeDir);
		final String classpath = this.buildClasspath(homeDir);
		final String footer = this.buildFooter(homeDir, config);
		// Build script.
		final StringBuilder builder = new StringBuilder();
		builder.append(header).append(" -stop -wait 10 -cp ").append(classpath).append(" ");
		builder.append(footer);
		return builder.toString();
	}

	/**
	 * Generate the JSVC start script. This script will
	 * scan all internal libraries as well as all the
	 * deployed applications and their libraries.
	 * @param homeDir The <code>String</code> runtime
	 * home directory.
	 * @param config The <code>Configuration</code>
	 * used by the environment.
	 * @return The <code>String</code> script.
	 * @throws SAXException If loading configuration
	 * from file failed.
	 * @throws IOException If loading configuration
	 * from file failed.
	 * @throws ParserConfigurationException If loading
	 * configuration from file failed.
	 */
	private String generateStartScript(final String homeDir, final Configuration config) throws SAXException, IOException, ParserConfigurationException {
		final String header = this.buildHeader(homeDir);
		final String classpath = this.buildClasspath(homeDir);
		final String footer = this.buildFooter(homeDir, config);
		// Build script.
		final StringBuilder builder = new StringBuilder();
		builder.append(header).append(" -wait 10 -cp ").append(classpath).append(" ");
		builder.append(footer);
		return builder.toString();
	}

	/**
	 * Build the header section of the script.
	 * @param homeDir The <code>String</code> runtime
	 * home directory.
	 * @return The <code>String</code> header section
	 * of the script.
	 * @throws SAXException If loading configuration
	 * from file failed.
	 * @throws IOException If loading configuration
	 * from file failed.
	 * @throws ParserConfigurationException If loading
	 * configuration from file failed.
	 */
	private String buildHeader(final String homeDir) throws SAXException, IOException, ParserConfigurationException {
		final String binDir = UEnvironment.instance.getBinDir(homeDir);
		final Configuration config = UEnvironment.instance.getConfiguration(homeDir);
		final StringBuilder builder = new StringBuilder();
		builder.append("#!/bin/sh\n\n");
		// Export Java home based on operating system.
		if (UEnvironment.instance.isOSX()) {
			builder.append("export JAVA_HOME=$(/usr/libexec/java_home)\n");
		} else if (UEnvironment.instance.isLinux()) {
			builder.append("export JAVA_HOME=/usr/lib/jvm/default-java\n");
		}
		builder.append("export PATH=$JAVA_HOME:$PATH\n");
		// JSVC executable based on operating system.
		String jsvcFile = null;
		if (UEnvironment.instance.isOSX()) {
			jsvcFile = binDir + EShell.JSVCOSX.value;
		} else if (UEnvironment.instance.isLinux()) {
			jsvcFile = binDir + EShell.JSVCLinux.value;
		}
		builder.append(jsvcFile).append(" ");
		// Output file.
		final String logDir = UEnvironment.instance.getLogDir(homeDir);
		builder.append("-outfile ").append(logDir).append(EShell.JSVCOut.value).append(" ");
		// Error file.
		builder.append("-errfile ").append(logDir).append(EShell.JSVCError.value).append(" ");
		// JVM arguments.
		builder.append(" -jvm server -Xms").append(config.jvm.memoryMin).append(" -Xmx").append(config.jvm.memoryMax).append(" ");
		// File encoding.
		builder.append("-Dfile.encoding=").append(config.jvm.fileEncoding).append(" ");
		// PID file location.
		builder.append("-pidfile ").append(binDir).append(EEnvironment.JSVCPIDFile.value);
		return builder.toString();
	}

	/**
	 * Build the class path section of the script by
	 * scanning the binary directory, the applications
	 * modules directories and all the applications
	 * library directories.
	 * @return The <code>String</code> class path
	 * section.
	 */
	private String buildClasspath(final String homeDir) {
		final StringBuilder builder = new StringBuilder();
		// Scan internal library directory.
		final String libDir = UEnvironment.instance.getBinDir(homeDir);
		final List<File> libList = FileUtils.instance.getFiles(libDir, ".jar");
		this.appendFiles(builder, libList);
		// Scan applications modules directories and library directories.
		final String appsDir = UEnvironment.instance.getAppsDir(homeDir);
		final List<File> appsList = FileUtils.instance.getFiles(appsDir, ".jar");
		this.appendFiles(builder, appsList);
		// Remove the last path separator.
		builder.deleteCharAt(builder.length()-1);
		return builder.toString();
	}

	/**
	 * Append all the files in the given list to the
	 * given string builder with class path format.
	 * @param builder The <code>StringBuilder</code>
	 * to append to.
	 * @param files The <code>List</code> of all the
	 * <code>File</code> to append.
	 */
	private void appendFiles(final StringBuilder builder, final List<File> files) {
		final int size = files.size();
		for (int i = 0; i < size; i++) {
			final File file = files.get(i);
			builder.append(file.getAbsolutePath());
			builder.append(File.pathSeparator);
		}
	}
	
	/**
	 * Build the footer section of the script based on
	 * the specified environment configuration.
	 * @param homeDir The <code>String</code> home
	 * directory.
	 * @param config The <code>Configuration</code>.
	 * @return The footer <code>String</code> value.
	 */
	private String buildFooter(final String homeDir, final Configuration config) {
		final String launcher = (config.runtime.launcher!=null) ? config.runtime.launcher : EEnvironment.DefaultLauncher.value;
		final String configPath = UEnvironment.instance.getConfigurationFile(homeDir);
		final StringBuilder builder = new StringBuilder();
		builder.append(launcher).append(" ").append(configPath).append("\n");
		return builder.toString();
	}
}

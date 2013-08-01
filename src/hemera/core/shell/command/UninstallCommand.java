package hemera.core.shell.command;

import java.io.File;
import java.io.IOException;

import hemera.core.environment.util.UEnvironment;
import hemera.core.shell.enumn.ECommand;
import hemera.core.shell.enumn.EShell;
import hemera.core.shell.interfaces.ICommand;
import hemera.core.utility.FileUtils;
import hemera.core.utility.shell.Shell;
import hemera.core.utility.shell.ShellResult;

/**
 * <code>UninstallCommand</code> defines the logic that
 * un-installs the environment. It does not require any
 * arguments.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.6
 */
public class UninstallCommand implements ICommand {

	@Override
	public void execute(final String[] args) throws Exception {
		// Stop the service first.
		ECommand.Stop.execute(null);
		// Remove Hemera from environment profile.
		this.removeEnvPath();
		// Remove existing home directory.
		final String homePath = UEnvironment.instance.getInstalledHomeDir();
		FileUtils.instance.delete(homePath);
		// Print further instructions.
		System.out.println("Hemera has been uninstalled successfully.");
		System.out.println("Please run 'source /etc/profile' or restart your shell session to complete un-installation.");
	}

	/**
	 * Remove Hemera from the environment path.
	 * @throws IOException If any file processing failed.
	 * @throws InterruptedException If waiting for shell
	 * command failed.
	 */
	private void removeEnvPath() throws IOException, InterruptedException {
		// Read in entire file as a string.
		final File file = new File("/etc/profile");
		final String originalContents = FileUtils.instance.readAsString(file);
		// Remove any existing Hemera exports.
		final String[] tokens = originalContents.split("\n");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].contains(EShell.ShellAccessPath.value)) {
				tokens[i] = null;
			}
		}
		// Create new contents.
		final StringBuilder updatedContents = new StringBuilder();
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i] != null) updatedContents.append(tokens[i]).append("\n");
		}
		// Write the new contents to a temporary file.
		final String currentDir = FileUtils.instance.getCurrentJarDirectory();
		final String tempFile = currentDir + "temp.env";
		final File temp = FileUtils.instance.writeAsString(updatedContents.toString(), tempFile);
		// Execute command to update environment profile.
		final ShellResult result = Shell.instance.execute(new String[] {"mv", temp.getAbsolutePath(), file.getAbsolutePath()}, true);
		if (result.code != 0) throw new IOException("Removing Hemera environment path failed.\n" + result.output);
	}

	@Override
	public String getKey() {
		return "uninstall";
	}

	@Override
	public String getDescription() {
		return "Uninstall the runtime environment and remove all files.";
	}

	@Override
	public String[] getArgsDescription() {
		return null;
	}
}

package hemera.core.shell.command;

import hemera.core.environment.util.UEnvironment;
import hemera.core.shell.enumn.EShell;
import hemera.core.shell.interfaces.ICommand;
import hemera.core.utility.shell.Shell;
import hemera.core.utility.shell.ShellResult;

/**
 * <code>StartCommand</code> defines the unit of logic
 * that starts the Hemera runtime environment using the
 * generated JSVC script. This command does not require
 * any arguments.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class StartCommand implements ICommand {

	@Override
	public void execute(final String[] args) throws Exception {
		// Check if runtime is running.
		final boolean running = UEnvironment.instance.isRunning();
		if (running) {
			System.out.println("Runtime environment is already running.");
		} else {
			// Execute the script as root.
			System.out.println("Starting Hemera runtime environment...");
			final String binDir = UEnvironment.instance.getInstalledBinDir();
			final ShellResult result = Shell.instance.executeAsRoot(binDir+EShell.JSVCStartScriptFile.value);
			if (result.code != 0) {
				System.err.println("Executing JSVC script failed: " + result.code);
				System.err.println(result.output);
			}
			else System.out.println("Hemera runtime environment is now running.");
		}
	}

	@Override
	public String getKey() {
		return "start";
	}

	@Override
	public String getDescription() {
		return "Start the runtime environment if it is not running already.";
	}

	@Override
	public String[] getArgsDescription() {
		return null;
	}
}

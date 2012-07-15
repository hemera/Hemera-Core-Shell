package hemera.core.shell.command;

import hemera.core.environment.config.Configuration;
import hemera.core.environment.util.UEnvironment;
import hemera.core.shell.enumn.ECommand;
import hemera.core.shell.interfaces.ICommand;
import hemera.core.shell.util.JSVCScriptGenerator;
import hemera.core.utility.FileUtils;

/**
 * <code>UndeployCommand</code> defines the logic that
 * removes deployed a Hemera Application. It requires
 * the following arguments:
 * <p>
 * @param appName The <code>String</code> name of the
 * application to un-deploy.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class UndeployCommand implements ICommand {

	@Override
	public void execute(final String[] args) throws Exception {
		if (args == null || args.length < 1) {
			throw new IllegalArgumentException("Application name must be specified.");
		}
		final String appName = args[0];
		// Remove the application folder.
		final String path = UEnvironment.instance.getApplicationDir(appName);
		final boolean removed = FileUtils.instance.delete(path);
		if (!removed) {
			System.err.println("No such application: " + appName);
		} else {
			// Regenerate JSVC scripts.
			final String homeDir = UEnvironment.instance.getInstalledHomeDir();
			final Configuration config = UEnvironment.instance.getConfiguration(homeDir);
			JSVCScriptGenerator.instance.exportScripts(homeDir, config);
			System.out.println(appName + " successfully removed.");
			// Restart the runtime.
			ECommand.Restart.execute(null);
		}
	}

	@Override
	public String getKey() {
		return "undeploy";
	}

	@Override
	public String getDescription() {
		return "Undeploy the specified application.";
	}

	@Override
	public String[] getArgsDescription() {
		return new String[] {
				"appName", "The name of the application to undeploy. The name is case sensitive."
		};
	}
}

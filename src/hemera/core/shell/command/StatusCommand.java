package hemera.core.shell.command;

import hemera.core.environment.util.UEnvironment;
import hemera.core.shell.interfaces.ICommand;

/**
 * <code>StatusCommand</code> defines the unit of logic
 * that checks the running status of the Hemera runtime
 * environment. It does not require any arguments.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class StatusCommand implements ICommand {

	@Override
	public void execute(final String[] args) throws Exception {
		final boolean running = UEnvironment.instance.isRunning();
		if (running) {
			System.out.println("Hemera runtime environment is currently running.");
		} else {
			System.out.println("Hemera runtime environment is not running.");
		}
	}
	
	@Override
	public String getKey() {
		return "status";
	}

	@Override
	public String getDescription() {
		return "Check the running status of the runtime environment.";
	}

	@Override
	public String[] getArgsDescription() {
		return null;
	}
}

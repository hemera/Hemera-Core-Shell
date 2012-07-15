package hemera.core.shell.command;

import hemera.core.shell.enumn.ECommand;
import hemera.core.shell.interfaces.ICommand;

/**
 * <code>RestartCommand</code> defines the command that
 * stops then starts the runtime environment. This command
 * does not require any arguments.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class RestartCommand implements ICommand {

	@Override
	public void execute(final String[] args) throws Exception {
		System.out.println("Hemera Runtime Environment will restart now...");
		ECommand.Stop.execute(null);
		ECommand.Start.execute(null);
	}

	@Override
	public String getKey() {
		return "restart";
	}

	@Override
	public String getDescription() {
		return "Stop then start the runtime environment.";
	}

	@Override
	public String[] getArgsDescription() {
		return null;
	}
}

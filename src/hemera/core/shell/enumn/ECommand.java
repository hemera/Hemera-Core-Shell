package hemera.core.shell.enumn;

import hemera.core.shell.command.BundleCommand;
import hemera.core.shell.command.DeployCommand;
import hemera.core.shell.command.HelpCommand;
import hemera.core.shell.command.InstallCommand;
import hemera.core.shell.command.ListCommand;
import hemera.core.shell.command.RestartCommand;
import hemera.core.shell.command.StartCommand;
import hemera.core.shell.command.StatusCommand;
import hemera.core.shell.command.StopCommand;
import hemera.core.shell.command.UndeployCommand;
import hemera.core.shell.command.UninstallCommand;
import hemera.core.shell.interfaces.ICommand;

/**
 * <code>ECommand</code> defines the enumerations of
 * all the supported commands.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public enum ECommand implements ICommand {
	/**
	 * The install command.
	 */
	Install(new InstallCommand()),
	/**
	 * The help command.
	 */
	Help(new HelpCommand()),
	/**
	 * The bundle command.
	 */
	Bundle(new BundleCommand()),
	/**
	 * The deploy command.
	 */
	Deploy(new DeployCommand()),
	/**
	 * The un-deploy command.
	 */
	Undeploy(new UndeployCommand()),
	/**
	 * The start command.
	 */
	Start(new StartCommand()),
	/**
	 * The stop command.
	 */
	Stop(new StopCommand()),
	/**
	 * The restart command.
	 */
	Restart(new RestartCommand()),
	/**
	 * The list command.
	 */
	List(new ListCommand()),
	/**
	 * The status command.
	 */
	Status(new StatusCommand()),
	/**
	 * The un-install command.
	 */
	Uninstall(new UninstallCommand());
	
	/**
	 * Parse the given value into the corresponding
	 * command.
	 * @param value The <code>String</code> input.
	 * @return The <code>ECommand</code> instance.
	 * <code>null</code> if there is no such command.
	 */
	public static ECommand parse(final String value) {
		if (value.equals(ECommand.Install.command.getKey())) {
			return ECommand.Install;
		} else if (value.equals(ECommand.Help.command.getKey())) {
			return ECommand.Help;
		} else if (value.equals(ECommand.Bundle.command.getKey())) {
			return ECommand.Bundle;
		} else if (value.equals(ECommand.Deploy.command.getKey())) {
			return ECommand.Deploy;
		} else if (value.equals(ECommand.Undeploy.command.getKey())) {
			return ECommand.Undeploy;
		} else if (value.equals(ECommand.Start.command.getKey())) {
			return ECommand.Start;
		} else if (value.equals(ECommand.Stop.command.getKey())) {
			return ECommand.Stop;
		} else if (value.equals(ECommand.Restart.command.getKey())) {
			return ECommand.Restart;
		} else if (value.equals(ECommand.List.command.getKey())) {
			return ECommand.List;
		} else if (value.equals(ECommand.Status.command.getKey())) {
			return ECommand.Status;
		} else if (value.equals(ECommand.Uninstall.command.getKey())) {
			return ECommand.Uninstall;
		} else {
			return null;
		}
	}
	
	/**
	 * The <code>ICommand</code> instance.
	 */
	private final ICommand command;
	
	/**
	 * Constructor of <code>ECommand</code>.
	 * @param command The <code>ICommand</code>.
	 */
	private ECommand(final ICommand command) {
		this.command = command;
	}

	@Override
	public void execute(final String[] args) throws Exception {
		this.command.execute(args);
	}
	
	@Override
	public String getKey() {
		return this.command.getKey();
	}

	@Override
	public String getDescription() {
		return this.command.getDescription();
	}

	@Override
	public String[] getArgsDescription() {
		return this.command.getArgsDescription();
	}
}

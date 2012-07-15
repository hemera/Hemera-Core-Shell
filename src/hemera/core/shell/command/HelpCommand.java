package hemera.core.shell.command;

import hemera.core.shell.enumn.ECommand;
import hemera.core.shell.interfaces.ICommand;

/**
 * <code>HelpCommand</code> defines the unit of logic
 * that prints out all the available commands along
 * with their required arguments.
 * <p>
 * @param command The <code>String</code> specific
 * command to print out.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class HelpCommand implements ICommand {
	/**
	 * The <code>String</code> tab spacing.
	 */
	private static final String Tab = "    ";

	@Override
	public void execute(final String[] args) throws Exception {
		// If no arguments, print out all commands.
		if (args == null || args.length <= 0) this.printAll();
		// Otherwise print out individual command.
		else {
			final String key = args[0];
			this.printCommand(key);
		}
	}

	/**
	 * Print out the keys of all the commands.
	 */
	private void printAll() {
		System.out.println("List of commands:\n");
		final ECommand[] commands = ECommand.values();
		for (int i = 0; i < commands.length; i++) {
			final ECommand command = commands[i];
			System.out.println(HelpCommand.Tab + command.getKey());
		}
		System.out.println("\nRun \"hemera help COMMAND\" to see details of a single command.");
	}

	/**
	 * Print the details of the command with specified
	 * key.
	 * @param key The <code>String</code> command key.
	 */
	private void printCommand(final String key) {
		// Parse command.
		final ECommand command = ECommand.parse(key);
		if (command == null) {
			System.out.println("There is no such command: " + key);
		}
		else {
			// Print key.
			System.out.println("Key:");
			System.out.println(HelpCommand.Tab + command.getKey());
			System.out.println();
			// Print description.
			System.out.println("Description:");
			System.out.println(HelpCommand.Tab + command.getDescription());
			System.out.println();
			// Print arguments.
			final String[] args = command.getArgsDescription();
			System.out.println("Required Arguments:");
			if (args == null || args.length <= 0) {
				System.out.println(HelpCommand.Tab + "No arguments required");
			} else {
				final int last = args.length-1;
				final StringBuilder builder = new StringBuilder();
				for (int i = 0; i < args.length; i += 2) {
					builder.append(HelpCommand.Tab).append(args[i]);
					if (args[i+1] != null) {
						builder.append(": ").append(args[i+1]);
					}
					if (i+1 != last) builder.append("\n");
				}
				System.out.println(builder.toString());
			}
		}
	}

	@Override
	public String getKey() {
		return "help";
	}

	@Override
	public String getDescription() {
		return "Print out help information of commands.";
	}

	@Override
	public String[] getArgsDescription() {
		return new String[] {
				"If no argument is specified, list all supported commands", null,
				"command", "The key of the command to retrieve information on."
		};
	}
}

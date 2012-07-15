package hemera.core.shell;

import hemera.core.environment.enumn.EEnvironment;
import hemera.core.shell.enumn.ECommand;

/**
 * <code>Main</code> defines the utility that provides
 * the environment access to control the entire Hemera
 * runtime environment.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class Main {

	/**
	 * Environment entry point.
	 * @param args The <code>String</code> array of
	 * arguments.
	 */
	public static void main(final String[] args) {
		final int width = Main.printHeader();
		try {
			final ECommand command = Main.parse(args);
			Main.execute(command, args);
		} catch (final Exception e) {
			System.err.println(e.getMessage());
		}
		Main.printFooter(width);
	}
	
	/**
	 * Print the header.
	 * @return The <code>int</code> width.
	 */
	private static int printHeader() {
		final StringBuilder builder = new StringBuilder();
		// Determine middle line first.
		final int spacesCount = 20;
		final StringBuilder middleLine = new StringBuilder();
		middleLine.append("#");
		for (int i = 0; i < spacesCount; i++) middleLine.append(" ");
		middleLine.append("Hemera").append(" ").append(EEnvironment.Version.value);
		for (int i = 0; i < spacesCount; i++) middleLine.append(" ");
		middleLine.append("#");
		// Determine the overall width.
		final int width = middleLine.length();
		// Produce final print.
		for (int i = 0; i < width; i++) builder.append("#");
		builder.append("\n").append(middleLine.toString()).append("\n");
		for (int i = 0; i < width; i++) builder.append("#");
		builder.append("\n");
		// Output.
		System.out.println(builder.toString());
		return width;
	}
	
	/**
	 * Parse the input into a corresponding command.
	 * @param args The <code>String</code> array input
	 * arguments.
	 * @return The <code>ECommand</code> instance.
	 */
	private static ECommand parse(final String[] args) {
		if (args == null || args.length < 1) {
			throw new IllegalArgumentException("Please specify a command. Use \"hemera help\" to see all supported commands.");
		}
		final String input = args[0];
		final ECommand command = ECommand.parse(input);
		if (command == null) {
			final StringBuilder builder = new StringBuilder();
			builder.append("Unsupported command: ").append(input);
			// Append arguments.
			if (args.length > 1) {
				builder.append(" with arguments [");
				for (int i = 1; i < args.length; i++) {
					builder.append(args[i]);
					if (i != args.length-1) builder.append(" ");
					else builder.append("]");
				}
			}
			builder.append("\nUse \"hemera help\" to see all supported commands.");
			throw new IllegalArgumentException(builder.toString());
		}
		return command;
	}
	
	/**
	 * Execute the given command with given input
	 * arguments.
	 * @param command The <code>ECommand</code> instance.
	 * @param args The <code>String</code> array input
	 * arguments.
	 * @throws Exception If command execution failed.
	 */
	private static void execute(final ECommand command, final String[] args) throws Exception {
		final String[] arguments = (args.length>1) ? new String[args.length-1] : null;
		for (int i = 1; i < args.length; i++) {
			arguments[i-1] = args[i];
		}
		command.execute(arguments);
	}
	
	/**
	 * Print the footer.
	 * @param width The <code>int</code> header width.
	 */
	private static void printFooter(final int width) {
		final StringBuilder builder = new StringBuilder();
		builder.append("\n");
		for (int i = 0; i < width; i++) builder.append("#");
		System.out.println(builder.toString());
	}
}

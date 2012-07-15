package hemera.core.shell.interfaces;

/**
 * <code>ICommand</code> defines the interface of a
 * set of logic that can be executed based on given
 * arguments.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public interface ICommand {

	/**
	 * Execute the command with given arguments.
	 * @param args The <code>String</code> array of
	 * arguments.
	 * @throws Exception If any processing failed.
	 */
	public void execute(final String[] args) throws Exception;
	
	/**
	 * Retrieve the command key that is used to invoke
	 * this command.
	 * @return The <code>String</code> key.
	 */
	public String getKey();
	
	/**
	 * Retrieve the description of this command.
	 * @return The <code>String</code> description.
	 */
	public String getDescription();
	
	/**
	 * Retrieve all the arguments and their description
	 * in a array with the description following the name
	 * of the argument.
	 * @return The <code>String</code> array of all
	 * the arguments description.
	 */
	public String[] getArgsDescription();
}

package hemera.core.shell.enumn;

/**
 * <code>EShell</code> defines all the enumeration
 * values used by the shell environment.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public enum EShell {
	/**
	 * The Hemera Application Bundle file extension.
	 */
	BundleExtension(".hab"),
	/**
	 * The script file name.
	 */
	ScriptFile("hemera"),
	/**
	 * The environment access path set in the shell
	 * profile.
	 */
	ShellAccessPath("HEMERA"),
	/**
	 * The Mac OSX JSVC executable file name.
	 */
	JSVCOSX("jsvc-osx-v1.0.5"),
	/**
	 * The Linux JSVC executable file name.
	 */
	JSVCLinux("jsvc-linux-1.0.5"),
	/**
	 * The JSVC standard output file.
	 */
	JSVCOut("jsvc.out"),
	/**
	 * The JSVC error output file.
	 */
	JSVCError("jsvc.error"),
	/**
	 * The JSVC start script file name.
	 */
	JSVCStartScriptFile("hemera-jsvc-start"),
	/**
	 * The JSVC stop script file name.
	 */
	JSVCStopScriptFile("hemera-jsvc-stop"),
	/**
	 * The internal resources package path.
	 */
	InternalResourcesPath("hemera/core/shell/resources/");

	/**
	 * The <code>String</code> value.
	 */
	public final String value;

	/**
	 * Constructor of <code>EShell</code>.
	 * @param value The <code>String</code> value.
	 */
	private EShell(final String value) {
		this.value = value;
	}
}

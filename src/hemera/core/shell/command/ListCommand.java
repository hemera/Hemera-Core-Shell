package hemera.core.shell.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import hemera.core.environment.enumn.EEnvironment;
import hemera.core.environment.ham.HAM;
import hemera.core.environment.util.UEnvironment;
import hemera.core.shell.interfaces.ICommand;
import hemera.core.utility.FileUtils;

/**
 * <code>ListCommand</code> defines the command that
 * lists all the deployed application names. This
 * command does not require any arguments.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class ListCommand implements ICommand {

	@Override
	public void execute(final String[] args) throws Exception {
		// Scan all HAM files.
		final String appsDir = UEnvironment.instance.getInstalledAppsDir();
		final List<File> hamFiles = FileUtils.instance.getFiles(appsDir, EEnvironment.HAMExtension.value);
		final int hamSize = hamFiles.size();
		final ArrayList<String> apps = new ArrayList<String>(hamSize);
		for (int i = 0; i < hamSize; i++) {
			final File hamFile = hamFiles.get(i);
			// Parse HAM file.
			final Document document = FileUtils.instance.readAsDocument(hamFile);
			final HAM ham = new HAM(document);
			apps.add(ham.applicationName);
		}
		// Print out.
		final int size = apps.size();
		if (size <= 0) {
			System.out.println("There are no applications deployed.");
		} else {
			System.out.println(size + " deployed applications:");
			for (int i = 0; i < size; i++) {
				System.out.println("    " + apps.get(i));
			}
		}
	}

	@Override
	public String getKey() {
		return "list";
	}

	@Override
	public String getDescription() {
		return "List the names of all the deployed applications.";
	}

	@Override
	public String[] getArgsDescription() {
		return null;
	}
}

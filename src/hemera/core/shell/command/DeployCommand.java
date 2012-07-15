package hemera.core.shell.command;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import hemera.core.environment.config.Configuration;
import hemera.core.environment.enumn.EEnvironment;
import hemera.core.environment.ham.KHAM;
import hemera.core.environment.util.UEnvironment;
import hemera.core.shell.enumn.ECommand;
import hemera.core.shell.enumn.EShell;
import hemera.core.shell.enumn.KBundleManifest;
import hemera.core.shell.interfaces.ICommand;
import hemera.core.shell.util.JSVCScriptGenerator;
import hemera.core.utility.FileUtils;

/**
 * <code>DeployCommand</code> defines the logic that
 * deploys a Hemera Application Bundle. It requires
 * the following arguments:
 * <p>
 * @param bundlePath The <code>String</code> path to
 * the bundle file.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class DeployCommand implements ICommand {

	@Override
	public void execute(final String[] args) throws Exception {
		if (args == null || args.length < 1) {
			throw new IllegalArgumentException("Bundle file path must be specified.");
		}
		final String bundlePath = args[0];
		try {
			if (!bundlePath.endsWith(EShell.BundleExtension.value)) {
				throw new IllegalArgumentException("Invalid bundle file.");
			}
			final JarFile bundle = new JarFile(bundlePath);
			// Read in HAM document.
			final Document ham = this.readHAM(bundle);
			// Create application directory.
			final String appDir = this.createAppDir(ham);
			// Deploy bundle library files.
			System.out.println("Deploying libraries...");
			this.deployLibrary(appDir, bundle);
			// Deploy HAM file.
			System.out.println("Deploying HAM...");
			this.deployHAM(appDir, ham);
			// Deploy modules.
			System.out.println("Deploying modules...");
			this.deployModules(appDir, bundle, ham);
			// Update runtime scripts.
			System.out.println("Updating scripts...");
			final String homeDir = UEnvironment.instance.getInstalledHomeDir();
			final Configuration config = UEnvironment.instance.getConfiguration(homeDir);
			JSVCScriptGenerator.instance.exportScripts(homeDir, config);
			// Delete temp directory.
			FileUtils.instance.delete(UEnvironment.instance.getInstalledTempDir());
			System.out.println("Successfully deployed: " + this.parseAppName(ham));
			// Run stop command then run start command.
			System.out.println("Hemera Runtime Environment will restart now...");
			ECommand.Stop.execute(null);
			ECommand.Start.execute(null);
		} catch (final Exception e) {
			System.err.println("Deploying failed.");
			throw e;
		}
	}

	/**
	 * Read in the HAM XML document from the given
	 * application bundle.
	 * @param bundle The bundle <code>JarFile</code>.
	 * @return The HAM XML <code>Document</code>.
	 * @throws IOException If parsing bundle file
	 * failed.
	 * @throws SAXException If parsing HAM failed.
	 * @throws ParserConfigurationException If parsing
	 * HAM failed. 
	 */
	private Document readHAM(final JarFile bundle) throws IOException, SAXException, ParserConfigurationException {
		// Retrieve HAM entry.
		final Manifest manifest = bundle.getManifest();
		final String entryName = manifest.getMainAttributes().getValue(KBundleManifest.HAMFile.key);
		final ZipEntry entry = bundle.getEntry(entryName);
		final InputStream input = bundle.getInputStream(entry);
		// Parse into XML.
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(input);
	}

	/**
	 * Create the application directory. This will
	 * first delete the existing directory.
	 * @param ham The HAM XML <code>Document</code>.
	 * @return The <code>String</code> application
	 * directory.
	 */
	private String createAppDir(final Document ham) throws IOException {
		final String appName = this.parseAppName(ham);
		final String path = UEnvironment.instance.getApplicationDir(appName);
		// Delete existing first.
		FileUtils.instance.delete(path);
		// Create directory.
		final File appDir = new File(path);
		appDir.mkdirs();
		return path;
	}

	/**
	 * Deploy all the library files contained in the
	 * given bundle file excluding the ones that are
	 * already installed with the environment.
	 * @param appDir The <code>String</code> path of
	 * the application directory.
	 * @param bundle The bundle <code>JarFile</code>.
	 * @throws IOException If parsing bundle file
	 * failed.
	 */
	private void deployLibrary(final String appDir, final JarFile bundle) throws IOException {
		// Retrieve the lib Jar entry.
		final Manifest manifest = bundle.getManifest();
		final String libEntryName = manifest.getMainAttributes().getValue(KBundleManifest.LibraryJarFile.key);
		// Bundle may not contain any library files.
		if (libEntryName == null || libEntryName.isEmpty()) return;
		// Write the lib Jar file to temp directory.
		final String tempDir = UEnvironment.instance.getInstalledTempDir();
		final File libFile = FileUtils.instance.writeToFile(bundle, libEntryName, tempDir);
		// Write all the contents of the lib Jar file to application's
		// lib directory, excluding environment already installed.
		final String binDir = UEnvironment.instance.getInstalledBinDir();
		final String appLibDir = UEnvironment.instance.getApplicationLibDir(appDir);
		final List<File> existing = FileUtils.instance.getFiles(binDir);
		FileUtils.instance.writeAll(libFile, appLibDir, existing);
		// Delete temporary lib Jar file.
		libFile.delete();
	}

	/**
	 * Deploy the HAM configuration.
	 * @param appDir The <code>String</code> path of
	 * the application directory.
	 * @param ham The HAM XML <code>Document</code>.
	 * @throws IOException If file processing failed.
	 * @throws TransformerException If writing the
	 * XML document failed.
	 */
	private void deployHAM(final String appDir, final Document ham) throws IOException, TransformerException {
		final String appName = this.parseAppName(ham);
		// Write HAM to a temporary location.
		final String tempDir = UEnvironment.instance.getInstalledTempDir();
		final String tempTarget = tempDir + appName + EEnvironment.HAMExtension.value;
		final File tempFile = FileUtils.instance.writeDocument(ham, tempTarget);
		// Replace applications directory.
		final String contents = FileUtils.instance.readAsString(tempFile);
		final String updated = contents.replace(KHAM.PlaceholderAppsDir.tag, appDir);
		// Write to file.
		final StringBuilder builder = new StringBuilder();
		builder.append(appDir).append(appName).append(EEnvironment.HAMExtension.value);
		FileUtils.instance.writeAsString(updated, builder.toString());
	}

	/**
	 * Deploy all the modules contained in the given
	 * bundle file.
	 * @param appDir The <code>String</code> path of
	 * the application directory.
	 * @param bundle The bundle <code>JarFile</code>.
	 * @param ham The HAM XML <code>Document</code>.
	 * @throws IOException If parsing bundle file
	 * failed.
	 */
	private void deployModules(final String appDir, final JarFile bundle, final Document ham) throws IOException {
		final String tempDir = UEnvironment.instance.getInstalledTempDir();
		// Parse modules tag.
		final NodeList modulesList = ham.getElementsByTagName(KHAM.Modules.tag);
		if (modulesList == null || modulesList.getLength() != 1) {
			throw new IllegalArgumentException("Invalid HAM file. Must contain one modules tag.");
		}
		final Element modulesTag = (Element)modulesList.item(0);
		// Parse module tags.
		final NodeList moduleTags = modulesTag.getElementsByTagName(KHAM.Module.tag);
		if (moduleTags == null || moduleTags.getLength() <= 0) {
			throw new IllegalArgumentException("Invalid HAM file. Must contain at least one module tags.");
		}
		final int length = moduleTags.getLength();
		for (int i = 0; i < length; i++) {
			// Parse out the class name.
			final Element moduleTag = (Element)moduleTags.item(i);
			final NodeList classList = moduleTag.getElementsByTagName(KHAM.ModuleClassname.tag);
			if (classList == null || classList.getLength() != 1) {
				throw new IllegalArgumentException("Invalid HAM file. Each module tag must contain one class-name tag.");
			}
			final String classname = classList.item(0).getTextContent();
			// Use class name as the Jar file entry name.
			final String entryName = classname + ".jar";
			final File moduleFile = FileUtils.instance.writeToFile(bundle, entryName, tempDir);
			// Write all the contents of the module Jar file to the module directory.
			final String moduleDir = appDir + classname + File.separator;
			FileUtils.instance.writeAll(moduleFile, moduleDir);
			// Retrieve the resources Jar file.
			final File resourcesFile = new File(moduleDir+classname+"-resources.jar");
			if (resourcesFile.exists()) {
				// Create resources directory.
				final File resourcesDir = new File(moduleDir+"resources/");
				resourcesDir.mkdir();
				// Write all the contents of resources Jar file to resources directory.
				FileUtils.instance.writeAll(resourcesFile, resourcesDir.getAbsolutePath());
				// Remove resources Jar file.
				resourcesFile.delete();
			}
			// Delete the temporary module Jar file.
			moduleFile.delete();
		}
	}

	/**
	 * Retrieve the application name from the HAM
	 * document.
	 * @param ham The HAM XML <code>Document</code>.
	 * @return The <code>String</code> application
	 * name.
	 */
	private String parseAppName(final Document ham) {
		// Parse application name.
		final NodeList list = ham.getElementsByTagName(KHAM.ApplicationName.tag);
		if (list == null || list.getLength() != 1) {
			throw new IllegalArgumentException("Invalid HAM file. Must contain one application name.");
		}
		return list.item(0).getTextContent();
	}

	@Override
	public String getKey() {
		return "deploy";
	}

	@Override
	public String getDescription() {
		return "Deploy the specified application bundle, override existing application with the same name if there is one.";
	}

	@Override
	public String[] getArgsDescription() {
		return new String[] {
				"habFile", "The path to the Hemera Application Bundle (hab) file"
		};
	}
}

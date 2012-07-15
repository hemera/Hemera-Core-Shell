package hemera.core.shell.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.w3c.dom.Document;

import hemera.core.environment.enumn.EEnvironment;
import hemera.core.environment.ham.HAM;
import hemera.core.environment.hbm.HBM;
import hemera.core.environment.hbm.HBMModule;
import hemera.core.environment.util.UEnvironment;
import hemera.core.shell.enumn.EShell;
import hemera.core.shell.enumn.KBundleManifest;
import hemera.core.shell.interfaces.ICommand;
import hemera.core.utility.FileUtils;
import hemera.core.utility.Compiler;

/**
 * <code>BundleCommand</code> defines the logic that
 * creates a Hemera Application Bundle. It requires
 * the following arguments:
 * <p>
 * @param hbmPath The <code>String</code> path to the
 * <code>hbm</code> file.
 * @param bundlePath The <code>String</code> path to
 * put the final bundle file.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.0
 */
public class BundleCommand implements ICommand {

	@Override
	public void execute(final String[] args) throws Exception {
		if (args == null || args.length < 2) {
			throw new IllegalArgumentException("hbm file path and bundle target directory must be specified.");
		}
		final String hbmPath = args[0];
		final String bundlePath = args[1];
		final String tempPath = UEnvironment.instance.getInstalledTempDir();
		try {
			// Create the temp directory.
			FileUtils.instance.delete(tempPath);
			final File tempDir = new File(tempPath);
			tempDir.mkdirs();
			// Parse bundle from HBM file.
			System.out.println("Parsing Hemera Bundle Model (HBM) file...");
			final Document document = FileUtils.instance.readAsDocument(new File(hbmPath));
			final HBM bundle = new HBM(document);
			// Generate HAM file.
			System.out.println("Generating Hemera Application Model (HAM) file...");
			final Document ham = new HAM(bundle).toXML();
			final String hamTarget = tempPath + bundle.applicationName.toLowerCase() + EEnvironment.HAMExtension.value;
			final File hamFile = FileUtils.instance.writeDocument(ham, hamTarget);
			// Build modules.
			System.out.println("Building modules...");
			final List<File> moduleJars = this.buildModules(bundle, tempPath);
			// Package all the module library files into a single Jar file.
			System.out.println("Packaging module library files...");
			final File libJar = this.buildModuleLib(bundle.modules, tempPath);
			// Create a manifest file for the bundle.
			final Manifest manifest = this.createManifest(moduleJars, libJar, hamFile);
			// Package all module Jar files, module library Jar file, and
			// the HAM file into a single bundle Jar file.
			System.out.println("Packaging final bundle...");
			final String bundleTarget = FileUtils.instance.getValidDir(bundlePath) + bundle.applicationName + EShell.BundleExtension.value;
			final ArrayList<File> files = new ArrayList<File>();
			files.addAll(moduleJars);
			files.add(libJar);
			files.add(hamFile);
			FileUtils.instance.jarFiles(files, bundleTarget, manifest);
			// Remove temp directory.
			FileUtils.instance.delete(tempPath);
			System.out.println("Bundling completed: " + bundleTarget);
		} catch (final Exception e) {
			System.err.println("Bundling failed.");
			throw e;
		}
	}
	
	/**
	 * Build all the modules contained in the given
	 * bundle.
	 * @param bundle The <code>HBundle</code> node.
	 * @param tempDir The <code>String</code> temporary
	 * directory.
	 * @return The <code>List</code> of built module
	 * Jar <code>File</code>.
	 * @throws Exception If building modules failed.
	 */
	private List<File> buildModules(final HBM bundle, final String tempDir) throws Exception {
		final Compiler compiler = new Compiler();
		final int size = bundle.modules.size();
		final ArrayList<File> moduleJars = new ArrayList<File>(size);
		for (int i = 0; i < size; i++) {
			final HBMModule module = bundle.modules.get(i);
			final File moduleJar = this.buildModule(module, compiler, tempDir);
			moduleJars.add(moduleJar);
		}
		return moduleJars;
	}
	
	/**
	 * Build the HBM module node by packaging all the
	 * compiled module class files and its configuration
	 * file if there is one into a single Jar file under
	 * the bundler temp directory.
	 * @param module The <code>HBMModule</code> to be
	 * built.
	 * @param compiler The <code>Compiler</code> instance
	 * to compile the module classes.
	 * @param tempDir The <code>String</code> temporary
	 * directory.
	 * @return The packaged module Jar <code>File</code>.
	 * @throws Exception If any processing failed.
	 */
	private File buildModule(final HBMModule module, final Compiler compiler, final String tempDir) throws Exception {
		// Each module gets a separate build directory.
		final String buildDir = tempDir + module.classname + File.separator;
		// Compile classes.
		final String classDir = buildDir + "classes" + File.separator;
		compiler.compile(module.srcDir, module.libDir, classDir);
		// Package class files into a Jar file.
		final String classjarPath = buildDir + module.classname + ".jar";
		final ArrayList<File> classDirFileList = new ArrayList<File>(1);
		classDirFileList.add(new File(classDir));
		final File classjar = FileUtils.instance.jarFiles(classDirFileList, classjarPath);
		// Package class Jar file, configuration file and all resource files into a module Jar file.
		final String modulejarPath = tempDir + module.classname + ".jar";
		final ArrayList<File> modulefiles = new ArrayList<File>();
		modulefiles.add(classjar);
		if (module.configFile != null) {
			final File configFile = new File(module.configFile);
			if (!configFile.exists()) {
				throw new IllegalArgumentException("Module configuration file: " + module.configFile + " does not exist.");
			}
			modulefiles.add(configFile);
		}
		// Package module resource files into a Jar file.
		if (module.resourcesDir != null) {
			final List<File> resourceFiles = FileUtils.instance.getFiles(module.resourcesDir);
			final String resourceTarget = tempDir + module.classname + "-resources.jar";
			final File resourceJar = FileUtils.instance.jarFiles(resourceFiles, resourceTarget);
			modulefiles.add(resourceJar);
		}
		final File modulejar = FileUtils.instance.jarFiles(modulefiles, modulejarPath);
		// Remove the build directory.
		FileUtils.instance.delete(buildDir);
		return modulejar;
	}
	
	/**
	 * Package all the library files of all the modules
	 * in the given list into a single Jar file under
	 * the bundler temp directory.
	 * @param modules The <code>List</code> of all the
	 * <code>HBMModule</code> to check for library files.
	 * @param tempDir The <code>String</code> temporary
	 * directory.
	 * @return The library Jar <code>File</code>.
	 * @throws IOException If any file processing failed.
	 */
	private File buildModuleLib(final List<HBMModule> modules, final String tempDir) throws IOException {
		// Extract all the module library files.
		final ArrayList<File> libFiles = new ArrayList<File>();
		final int size = modules.size();
		for (int i = 0; i < size; i++) {
			final HBMModule module = modules.get(i);
			final List<File> modulelibs = FileUtils.instance.getFiles(module.libDir);
			// Exclude duplicates based on name.
			final int libsize = modulelibs.size();
			for (int j = 0; j < libsize; j++) {
				final File file = modulelibs.get(j);
				boolean contains = false;
				final int addedSize = libFiles.size();
				for (int k = 0; k < addedSize; k++) {
					if (libFiles.get(k).getName().equals(file.getName())) {
						contains = true;
						break;
					}
				}
				if (!contains) libFiles.add(file);
			}
		}
		// Package all files into a single Jar file.
		final String libjarPath = tempDir + "lib.jar";
		final File libjar = FileUtils.instance.jarFiles(libFiles, libjarPath);
		return libjar;
	}
	
	/**
	 * Create the bundle manifest file.
	 * @param modulejars The <code>List</code> of all
	 * module Jar <code>File</code> to be included in
	 * the final bundle file.
	 * @param libjar The <code>File</code> of the
	 * single Jar file containing all module library
	 * files to be included in the final bundle file.
	 * @param hamfile The HAM <code>File</code> to be
	 * included in the final bundle file.
	 * @return The <code>Manifest</code> instance.
	 */
	private Manifest createManifest(final List<File> modulejars, final File libjar, final File hamfile) {
		final Manifest manifest = new Manifest();
		// Must include the basic attributes.
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(Attributes.Name.SPECIFICATION_VENDOR, "Hemera");
		manifest.getMainAttributes().put(Attributes.Name.SPECIFICATION_VERSION, EEnvironment.Version.value);
		// Add HAM file attribute.
		manifest.getMainAttributes().putValue(KBundleManifest.HAMFile.key, hamfile.getName());
		// Add library Jar file attribute.
		manifest.getMainAttributes().putValue(KBundleManifest.LibraryJarFile.key, libjar.getName());
		return manifest;
	}

	@Override
	public String getKey() {
		return "bundle";
	}

	@Override
	public String getDescription() {
		return "Create a Hemera Application Bundle (hab).";
	}

	@Override
	public String[] getArgsDescription() {
		return new String[] {
				"hbmFile", "The path to the Hemera Bundle Model (hbm) file",
				"targetDir", "The directory to put the created bundle file"
		};
	}
}

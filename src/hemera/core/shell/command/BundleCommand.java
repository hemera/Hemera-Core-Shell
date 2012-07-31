package hemera.core.shell.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.w3c.dom.Document;

import hemera.core.environment.enumn.EEnvironment;
import hemera.core.environment.ham.HAM;
import hemera.core.environment.hbm.HBM;
import hemera.core.environment.hbm.HBMDependency;
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
			// Create the temporary directory.
			FileUtils.instance.delete(tempPath);
			final File tempDir = new File(tempPath);
			tempDir.mkdirs();
			// Parse bundle from HBM file.
			System.out.println("Parsing Hemera Bundle Model (HBM) file...");
			final Document document = FileUtils.instance.readAsDocument(new File(hbmPath));
			final HBM bundle = new HBM(document);
			// Process all shared dependencies.
			System.out.println("Processing shared dependencies...");
			final List<File> sharedDependencies = this.processDependencies(bundle, tempPath);
			// Generate HAM file.
			System.out.println("Generating Hemera Application Model (HAM) file...");
			final Document ham = new HAM(bundle).toXML();
			final String hamTarget = tempPath + bundle.applicationName.toLowerCase() + EEnvironment.HAMExtension.value;
			final File hamFile = FileUtils.instance.writeDocument(ham, hamTarget);
			// Build modules.
			System.out.println("Building modules...");
			final List<File> moduleJars = this.buildModules(bundle, sharedDependencies, tempPath);
			// Package all library files into a single Jar file.
			System.out.println("Packaging application library files...");
			final File libJar = this.buildAppLib(bundle, sharedDependencies, tempPath);
			// Package shared resource files into a Jar file.
			System.out.println("Packaging application shared resources...");
			File sharedResourceJar = null;
			if (bundle.shared != null && bundle.shared.resourcesDir != null) {
				final List<File> resourceFiles = FileUtils.instance.getFiles(bundle.shared.resourcesDir);
				final String resourceTarget = tempPath + "resources.jar";
				sharedResourceJar = FileUtils.instance.jarFiles(resourceFiles, resourceTarget);
			}
			// Create a manifest file for the bundle.
			final Manifest manifest = this.createManifest(moduleJars, libJar, sharedResourceJar, hamFile);
			// Package all module Jar files, application library Jar file, and
			// the HAM file into a single bundle Jar file.
			System.out.println("Packaging final bundle...");
			final ArrayList<File> files = new ArrayList<File>();
			final String bundleTarget = FileUtils.instance.getValidDir(bundlePath) + bundle.applicationName + EShell.BundleExtension.value;
			files.addAll(moduleJars);
			files.add(libJar);
			files.add(hamFile);
			if (sharedResourceJar != null) files.add(sharedResourceJar);
			FileUtils.instance.jarFiles(files, bundleTarget, manifest);
			// Remove temporary directory.
			FileUtils.instance.delete(tempPath);
			System.out.println("Bundling completed: " + bundleTarget);
		} catch (final Exception e) {
			System.err.println("Bundling failed.");
			throw e;
		}
	}

	/**
	 * Process the bundle shared dependencies.
	 * @param bundle The <code>HBM</code> bundle.
	 * @param tempDir The <code>String</code> path of
	 * the temporary directory.
	 * @return The <code>List</code> of all the bundle
	 * shared dependency Jar <code>File</code>.
	 * @throws Exception If compiling sources failed.
	 */
	private List<File> processDependencies(final HBM bundle, final String tempDir) throws Exception {
		final ArrayList<File> store = new ArrayList<File>();
		if (bundle.shared != null && bundle.shared.dependencies != null) {
			final int size = bundle.shared.dependencies.size();
			for (int i = 0; i < size; i++) {
				final HBMDependency dependency = bundle.shared.dependencies.get(i);
				final List<File> jarFiles = dependency.process(tempDir);
				store.addAll(jarFiles);
			}
		}
		return store;
	}

	/**
	 * Build all the modules contained in the given
	 * bundle.
	 * @param bundle The <code>HBundle</code> node.
	 * @param sharedDependencies The <code>List</code> of
	 * all the shared dependency <code>File</code>.
	 * @param tempDir The <code>String</code> temporary
	 * directory.
	 * @return The <code>List</code> of built module
	 * Jar <code>File</code>.
	 * @throws Exception If building modules failed.
	 */
	private List<File> buildModules(final HBM bundle, final List<File> sharedDependencies, final String tempDir) throws Exception {
		final Compiler compiler = new Compiler();
		final int size = bundle.modules.size();
		final ArrayList<File> moduleJars = new ArrayList<File>(size);
		for (int i = 0; i < size; i++) {
			final HBMModule module = bundle.modules.get(i);
			final File moduleJar = this.buildModule(bundle, module, sharedDependencies, compiler, tempDir);
			moduleJars.add(moduleJar);
		}
		return moduleJars;
	}

	/**
	 * Build the HBM module node by packaging all the
	 * compiled module class files and its configuration
	 * file if there is one into a single Jar file under
	 * the bundler temporary directory.
	 * @param bundle The <code>HBM</code> bundle.
	 * @param module The <code>HBMModule</code> to be
	 * built.
	 * @param sharedDependencies The <code>List</code> of
	 * all the shared dependency <code>File</code>.
	 * @param compiler The <code>Compiler</code> instance
	 * to compile the module classes.
	 * @param tempDir The <code>String</code> temporary
	 * directory.
	 * @return The packaged module Jar <code>File</code>.
	 * @throws Exception If any processing failed.
	 */
	private File buildModule(final HBM bundle, final HBMModule module, final List<File> sharedDependencies,
			final Compiler compiler, final String tempDir) throws Exception {
		// Each module gets a separate build directory.
		final String buildDir = tempDir + module.classname + File.separator;
		// Compile classes with both shared and module dependencies.
		final List<File> dependencies = new ArrayList<File>();
		dependencies.addAll(sharedDependencies);
		if (module.dependencies != null) {
			final int size = module.dependencies.size();
			for (int i = 0; i < size; i++) {
				final List<File> moduleDependencies = module.dependencies.get(i).process(tempDir);
				dependencies.addAll(moduleDependencies);
			}
		}
		final String classDir = buildDir + "classes" + File.separator;
		compiler.compile(module.srcDir, classDir, dependencies);
		// Package class files into a Jar file.
		final String classjarPath = buildDir + module.classname + ".jar";
		final ArrayList<File> classDirFileList = new ArrayList<File>(1);
		classDirFileList.add(new File(classDir));
		final File classjar = FileUtils.instance.jarFiles(classDirFileList, classjarPath);
		// Process module configuration.
		final File moduleConfig = module.processConfig(bundle.shared, tempDir);
		// Package class Jar file, processed configuration file and all resource files into a module Jar file.
		final String modulejarPath = tempDir + module.classname + ".jar";
		final ArrayList<File> modulefiles = new ArrayList<File>();
		modulefiles.add(classjar);
		if (moduleConfig != null) modulefiles.add(moduleConfig);
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
	 * Package all the shared library files and all the
	 * library files of all the modules into a single
	 * Jar file under the bundler temporary directory.
	 * @param bundle The <code>HBM</code> bundle.
	 * @param sharedDependencies The <code>List</code> of
	 * all the shared dependency <code>File</code>.
	 * @param tempDir The <code>String</code> temporary
	 * directory.
	 * @return The library Jar <code>File</code>.
	 * @throws Exception If any processing failed.
	 */
	private File buildAppLib(final HBM bundle, final List<File> sharedDependencies, final String tempDir) throws Exception {
		// Add all shared library files.
		final List<File> libFiles = new ArrayList<File>();
		libFiles.addAll(sharedDependencies);
		// Add all the module library files.
		final int moduleSize = bundle.modules.size();
		for (int i = 0; i < moduleSize; i++) {
			final HBMModule module = bundle.modules.get(i);
			if (module.dependencies == null) continue;
			final int size = module.dependencies.size();
			for (int j = 0; j < size; j++) {
				final HBMDependency dependency = module.dependencies.get(j);
				// Exclude duplicates based on name.
				final List<File> moduleDependencies = dependency.process(tempDir);
				final int libsize = moduleDependencies.size();
				for (int k = 0; k < libsize; k++) {
					final File file = moduleDependencies.get(k);
					boolean contains = false;
					final int addedSize = libFiles.size();
					for (int l = 0; l < addedSize; l++) {
						if (libFiles.get(l).getName().equals(file.getName())) {
							contains = true;
							break;
						}
					}
					if (!contains) libFiles.add(file);
				}
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
	 * @param sharedResourcesJar The <code>File</code>
	 * of the shared resources Jar. <code>null</code>
	 * if there are no shared resources.
	 * @param hamfile The HAM <code>File</code> to be
	 * included in the final bundle file.
	 * @return The <code>Manifest</code> instance.
	 */
	private Manifest createManifest(final List<File> modulejars, final File libjar, final File sharedResourcesJar,
			final File hamfile) {
		final Manifest manifest = new Manifest();
		// Must include the basic attributes.
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(Attributes.Name.SPECIFICATION_VENDOR, "Hemera");
		manifest.getMainAttributes().put(Attributes.Name.SPECIFICATION_VERSION, EEnvironment.Version.value);
		// Add HAM file attribute.
		manifest.getMainAttributes().putValue(KBundleManifest.HAMFile.key, hamfile.getName());
		// Add library Jar file attribute.
		manifest.getMainAttributes().putValue(KBundleManifest.LibraryJarFile.key, libjar.getName());
		// Add shared resources Jar file attribute.
		if (sharedResourcesJar != null) {
			manifest.getMainAttributes().putValue(KBundleManifest.SharedResourcesJarFile.key, sharedResourcesJar.getName());
		}
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

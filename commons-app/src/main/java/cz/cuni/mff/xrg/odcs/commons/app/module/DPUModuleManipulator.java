package cz.cuni.mff.xrg.odcs.commons.app.module;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.dpu.config.DPUConfigurable;
import cz.cuni.mff.xrg.odcs.commons.app.auth.ShareType;
import cz.cuni.mff.xrg.odcs.commons.app.dpu.DPUExplorer;
import cz.cuni.mff.xrg.odcs.commons.app.dpu.DPUTemplateRecord;
import cz.cuni.mff.xrg.odcs.commons.app.dpu.DPUType;
import cz.cuni.mff.xrg.odcs.commons.app.facade.DPUFacade;
import cz.cuni.mff.xrg.odcs.commons.app.facade.ModuleFacade;

/**
 * Class provide one-place access to create/update/delete actions for DPUs. It
 * takes care about functionality connected with {@link DPUFacade} as well with {@link ModuleFacade}.
 * 
 * @author Petyr
 */
public class DPUModuleManipulator {

    private static final Logger LOG = LoggerFactory
            .getLogger(DPUModuleManipulator.class);

    @Autowired
    private DPUFacade dpuFacade;

    @Autowired
    private ModuleFacade moduleFacade;

    @Autowired
    private DPUExplorer dpuExplorer;

    @Autowired
    private ModuleChangeNotifier notifier;

    @Autowired
    private List<DPUValidator> validators;

    /**
     * Create {@link DPUTemplateRecord} for given DPU. If creation is successful
     * the DPU is loaded into application and is presented in DPU directory. The
     * visibility of new DPU is set to {@link ShareType#PRIVATE}.
     * <p>
     * Use setters to set additional DPU fields like name, description, visibility, ..., as those are not set by the {@link #create} method.
     * </p>
     * <p>
     * In case of error use {@link DPUCreateException#getMessage()} to get the description that can be shown to user.
     * </p>
     * <p>
     * When {@link DPUTemplateRecord} is loaded and instance is created then execute validators to validate new DPU. If one of them fails, the create method
     * fails and the DPU is not created.
     * </p>
     * 
     * @param sourceFile
     *            File from which load the dpu.
     * @param name
     *            DPU's name. If name is null then name from MANIFEST is used
     * @return new instance of {@link DPUTemplateRecord}
     * @throws DPUCreateException
     */
    public DPUTemplateRecord create(File sourceFile,
            String name) throws DPUCreateException {
        if (sourceFile == null) {
            // failed to load file
            throw new DPUCreateException(
                    "The DPU's file has not been loaded properly.");
        }
        if (!sourceFile.exists()) {
			LOG.error("Unable to find dpu: " + name);
			throw new DPUCreateException("Unable to find dpu: " + name);
		}
        
        // get directory name and also validate the DPU's file name
        final String newDpuFileName = sourceFile.getName();
        final String newDpuDirName =  getDirectoryName(newDpuFileName);
        // prepare directory secure that this method
        // will not continue for same jar-file twice .. use synchronisation
        // over file system.
        File newDPUDir = new File(moduleFacade.getDPUDirectory(), newDpuDirName);
        final File newDPUFile = new File(newDPUDir, newDpuFileName);
        
        DPUTemplateRecord newTemplate;
        boolean isChild = false;
        if (newDPUDir.exists()) {
            DPUTemplateRecord parent = dpuFacade.getByJarName(newDpuFileName);

            if (parent == null) {
                throw new DPUCreateException("DPU " + newDpuFileName + " already exists but with different version.");
            }

            if (name == null || parent.getName().equals(name)) { // the same jarFileName and name
                throw new DPUCreateException("DPU " + newDpuFileName + " with name " + name + " already exists.");
            }
            
            DPUTemplateRecord dpuWithSameName = dpuFacade.getByName(name);
            if (dpuWithSameName != null && newDpuFileName.equals(dpuWithSameName.getJarName())) {
                throw new DPUCreateException("Child DPU " + newDpuFileName + " with name " + name + " already exists.");
            }

            newTemplate = dpuFacade.createTemplate(name, null);
            newTemplate.setParent(parent);
            isChild = true;
		} else {
			prepareDirectory(newDPUDir);
			
			// copy
			try {
				FileUtils.copyFile(sourceFile, newDPUFile);
			} catch (IOException e) {
				LOG.error("Failed to copy file, when creating new DPU.", e);
				// release
				try {
					FileUtils.deleteDirectory(newDPUDir);
				} catch (IOException ex) {
					LOG.error("Failed to delete directory after DPU.", ex);
				}
				// failed to copy file
				throw new DPUCreateException("Failed to create DPU file.");
			}
			
			// we need dpu template to work wit DPUs
			newTemplate = dpuFacade.createTemplate(name, null);
		}

        newTemplate.setJarDirectory(newDpuDirName);
        newTemplate.setJarName(newDpuFileName);

        // try to load bundle
        final String dpuRelativePath = newDpuDirName + File.separator
                + newDpuFileName;
        Object dpuObject = null;
        try {
            dpuObject = moduleFacade.getInstance(newTemplate);
        } catch (ModuleException e) {
            LOG.error("Failed to load new DPU bundle.", e);
            String msg = "Failed to load DPU bacause of exception:" + e.getMessage();
            release(newDPUFile, newDPUDir, newTemplate, msg, isChild);
        } catch (Throwable t) {
        	String msg = "Failed to load DPU bacause of !unexpected! exception:" + t.getMessage();
            LOG.error(msg, t);
            release(newDPUFile, newDPUDir, newTemplate, msg, isChild);
        }

        final String jarDescription = dpuExplorer.getJarDescription(newTemplate);
        String dpuName;
        if (name == null) {
            dpuName = dpuExplorer.getBundleName(newTemplate);
        } else {
            dpuName = name;
        }

        // check type ..
        final DPUType dpuType = dpuExplorer.getType(dpuObject, dpuRelativePath);
        if (dpuType == null) {
            release(newDPUFile, newDPUDir, newTemplate, "DPU has unspecified type.", isChild);
        }

        // is configurable
        if (dpuObject instanceof DPUConfigurable) {
            DPUConfigurable configurable = (DPUConfigurable) dpuObject;
            try {
                newTemplate.setRawConf(configurable.getDefaultConfiguration());
            } catch (DPUConfigException e) {
                // failed to load default configuration .. 
                String msg = "Failed to obtain DPU's default configuration.";
                release(newDPUFile, newDPUDir, newTemplate, msg, isChild);
            }
        }

        // set other DPUs variables
        newTemplate.setType(dpuType);
        newTemplate.setDescription("");
        newTemplate.setName(dpuName);
        newTemplate.setJarDescription(jarDescription);
        newTemplate.setShareType(ShareType.PRIVATE);

        // validate
        if (validators != null) {
            try {
                for (DPUValidator item : validators) {
                    item.validate(newTemplate, dpuObject);
                }
            } catch (Throwable e) {
                release(newDPUFile, newDPUDir, newTemplate, e.getMessage(), isChild);
            }
        }

        // and save it into DB
        try {
            dpuFacade.save(newTemplate);
        } catch (Throwable e) {
            String msg = "Failed to save new DPU: " + e.getMessage();
            release(newDPUFile, newDPUDir, newTemplate, msg, isChild);
        }

        // notify the rest of the application
        notifier.created(newTemplate);

        // return new DPUTempateRecord
        return newTemplate;
    }
    
    /**
     * Cleaning up after failed dpu creation
     * 
     * @param newDPUFile
     * @param newDPUDir
     * @param newTemplate
     * @param msg
     * @param isChild
     * @throws DPUCreateException
     */
    private void release(File newDPUFile, File newDPUDir, DPUTemplateRecord newTemplate, String msg, boolean isChild)
    		throws DPUCreateException {
    	// release
        if (!isChild) { // child is pointing to the same jar, so no deleting if its not a parent
        	newDPUFile.delete();
        	try {
        		FileUtils.deleteDirectory(newDPUDir);
        	} catch (IOException ex) {
        		LOG.error("Failed to delete directory after DPU.", ex);
        	}
		}
        moduleFacade.unLoad(newTemplate);
        throw new DPUCreateException(msg);
	}

	/**
     * Try to replace jar-file for given DPU with given file. During the replace
     * the given DPU is inaccessible through the {@link ModuleFacade}. If the
     * new jar file can not be used, the old jar file is preserved and the {@link DPUReplaceException} is thrown.
     * When {@link DPUTemplateRecord} is loaded and instances is created then
     * execute validators to validate new DPU. If one of them failed the create
     * method failed and the DPU is not created.
     * 
     * @param dpu
     *            DPU to replace.
     * @param sourceDpuFile
     *            File that should replace given DPU's jar file.
     * @throws DPUReplaceException
     */
    public void replace(DPUTemplateRecord dpu,
            File sourceDpuFile) throws DPUReplaceException {
        // get file to the source and to the originalDPU
        final File originalDpuFile = new File(moduleFacade.getDPUDirectory(),
                dpu.getJarPath());
        final String directoryName = dpu.getJarDirectory();
        // validate input DPU's name
        try {
            getDirectoryName(sourceDpuFile.getName());
        } catch (DPUCreateException e) {
            throw new DPUReplaceException(e.getMessage());
        }
        // prepare the paths for new DPU
        final String newDpuName = sourceDpuFile.getName();
        final File newDpuFile = new File(moduleFacade.getDPUDirectory()
                + File.separator + directoryName, newDpuName);
        final String newRelativePath = directoryName + File.separator
                + sourceDpuFile.getName();

        // check that the DPU directory exist
        final File newDPUDir = new File(moduleFacade.getDPUDirectory(),
                directoryName);
        if (newDPUDir.exists()) {
            // ok exist
        } else {
            // no directory .. create it
            newDPUDir.mkdir();
        }

        // we have to lock bundle here ..
        // this prevent every other user .. from working with DPUs in give
        // directory, so we are the only one here .. we can do what we want :)

        // the backend should keep it's copy loaded .. so it do not
        // try to access the directory either
        moduleFacade.beginUpdate(dpu);

        // we need a backup of the original file here
        createBackUp(originalDpuFile);

        // copy new file, can replace the old one possibly
        try {
            FileUtils.copyFile(sourceDpuFile, newDpuFile);
        } catch (IOException e) {
            // failed to copy DPU
            LOG.error("Failed to copy new DPU jar file {}",
                    newDpuFile.getPath(), e);
            // recover
            recoverFromBackUp(originalDpuFile);
            moduleFacade.endUpdate(dpu, true);
            throw new DPUReplaceException("Can't copy new DPU jar file.");
        }

        // now we try to load new instance of DPU
        Object newDpuInstance = null;

        // we can use update
        try {
            // update
            newDpuInstance = moduleFacade.update(directoryName, newDpuName);
        } catch (ModuleException e) {
            LOG.warn("Failed to load bunle during replace.", e);
            // recover
            recoverFromBackUp(originalDpuFile);
            // finish update and unload remove DPU record
            moduleFacade.endUpdate(dpu, true);
            // the old bundle will be loaded with first user request
            throw new DPUReplaceException(
                    "Can't load instance from new bundle. Exception: "
                            + e.getMessage());
        } catch (Throwable e) {
            LOG.warn("Unexpected exception! Failed to load bunle during replace.", e);
            // recover
            recoverFromBackUp(originalDpuFile);
            // finish update and unload remove DPU record
            moduleFacade.endUpdate(dpu, true);
            // the old bundle will be loaded with first user request
            throw new DPUReplaceException(
                    "Can't load instance from new bundle. !Unexpected! exception: "
                            + e.getMessage());
        }

        // if we are here we have backUp bundle which has been uninstalled
        // we have new bundle which is functional

        // now we can examine the DPU
        DPUType dpuType = dpuExplorer.getType(newDpuInstance, newRelativePath);
        if (dpuType == dpu.getType()) {
            // type match .. we can continue
        } else {
            // we store message about type here
            String typeMessage;
            if (dpuType == null) {
                typeMessage = "New DPU has unspecified type. Check the DPU's annotations";
            } else {
                typeMessage = "New DPU has different type then the old one.";
            }
            // recover
            recoverFromBackUp(originalDpuFile);
            // finish update and unload remove DPU record
            moduleFacade.endUpdate(dpu, true);
            // DPU type changed .. we do not allow this
            throw new DPUReplaceException(
                    "New DPU has different type then the old one. "
                            + typeMessage);
        }

        // get new data from manifest.mf and save this changes
        final String jarDescription = dpuExplorer.getJarDescription(dpu);
        dpu.setJarDescription(jarDescription);
        dpu.setJarName(newDpuName);

        // validate
        if (validators != null) {
            try {
                for (DPUValidator item : validators) {
                    item.validate(dpu, newDpuInstance);
                }
            } catch (DPUValidatorException e) {
                // recover
                recoverFromBackUp(originalDpuFile);
                // finish update and unload remove DPU record
                moduleFacade.endUpdate(dpu, true);
                throw new DPUReplaceException(e.getMessage());
            } catch (Throwable e) {
                LOG.error("Unexpected exception occure during DPu validations.", e);
                // recover
                recoverFromBackUp(originalDpuFile);
                // finish update and unload remove DPU record
                moduleFacade.endUpdate(dpu, true);
                throw new DPUReplaceException(e.getMessage());
            }
        }
        dpuFacade.save(dpu);

        // we delete the backup
        deleteBackUp(originalDpuFile);

        // here we can unlock the bundle
        // we else say mofuleFacade to not drop the current module
        // as the replace has been successful
        moduleFacade.endUpdate(dpu, false);

        // notify the rest of the application
        notifier.updated(dpu);
    }

    /**
     * Delete given DPU record from database, unload it's bundle if loaded and
     * also delete it's jar file.
     * 
     * @param dpu
     *            DPU to delete.
     */
    public void delete(DPUTemplateRecord dpu) {
        dpuFacade.delete(dpu);
        if (dpu.getParent() == null) {
            moduleFacade.delete(dpu);
            // 	notify the rest of the application
            notifier.deleted(dpu);
            // and delete the directory if DPU is root
            final File dpuDirectory = new File(moduleFacade.getDPUDirectory(),
                    dpu.getJarDirectory());

            try {
                FileUtils.deleteDirectory(dpuDirectory);
            } catch (IOException e) {
                LOG.error("Failed to delete directory after DPU remove.", e);
            }
        } else {
            // non-root do not delete jar
        }
    }

    /**
     * Prepare directory with given name in DPU's directory. In case of failure
     * throws exception.
     * 
     * @param newDpuDir
     *            Name of directory that should be created.
     * @throws DPUCreateException
     */
    protected void prepareDirectory(File newDpuDir)
            throws DPUCreateException {
        
        // create directory
        if (newDpuDir.mkdir()) {
            // ok, directory has been created
        } else {
            // failed
            throw new DPUCreateException(String.format(
                    "Failed to create DPU's directory: '%s'", newDpuDir.getName()));
        }
    }

    /**
     * Validate the sourcePath. If the sourcePath is in right format then return
     * the name for DPU's directory otherwise throws exception.
     * 
     * @param sourceFileName
     *            Name of DPU's jar file.
     * @return directory for DPU
     * @throws DPUCreateException
     */
    protected static String getDirectoryName(String sourceFileName)
            throws DPUCreateException {
        // the name must be in format: NAME-.*.jar
        final Pattern pattern = Pattern
                .compile("(.+)-(\\d(\\.\\d+)+).*\\.jar");
        final Matcher matcher = pattern.matcher(sourceFileName);
        if (matcher.matches()) {
            // 0 - original, 1 - name, 2 - version
            return matcher.group(1);
        } else {
            throw new DPUCreateException(
                    "DPU's name must be in format NAME-VERSION.*.jar");
        }
    }

    /**
     * Return file for the backUp to given DPU's jar file.
     * 
     * @param originalDPU
     *            The original DPU's jar file.
     * @return file for the backup of given DPU's jar file
     */
    protected File createBackUpName(File originalDPU) {
        return new File(originalDPU.toString() + ".backup");
    }

    /**
     * Try to create backup for given file. If the file do not exist then log
     * this information but do not thrown.
     * 
     * @param originalDpu
     *            The original DPU's jar file.
     * @throws DPUReplaceException
     */
    protected void createBackUp(File originalDpu) throws DPUReplaceException {
        File originalDpuBackUp = createBackUpName(originalDpu);
        // check if original file exist
        if (originalDpu.exists()) {
            // yes, continue
        } else {
            // nothing to backup .. end
            LOG.warn("Original DPU does not exist.");
            return;
        }
        // check if backup is not already used
        if (originalDpuBackUp.exists()) {
            // try to delete it
            if (originalDpuBackUp.delete()) {
                // if we fail .. we can do nothing ..
                LOG.warn("Failed to delete previous DPU backup file: {}",
                        originalDpuBackUp.getPath());
            }
        }
        if (originalDpu.renameTo(originalDpuBackUp)) {
            // we have backup .. we can continue
        } else {
            // no backup no continue .. if we continue then we can lose the only
            // functional version we have
            LOG.error("Failed to create backup file: {}",
                    originalDpuBackUp.getPath());
            throw new DPUReplaceException(
                    "Failed to create original DPU backup.");
        }
    }

    /**
     * I there is no backup then nothing happened. If there is backup, then try
     * to use it to recover original DPU's jar file. The function does not
     * require the originalDpu to has been deleted.
     * 
     * @param originalDpu
     *            The original DPU's jar file.
     */
    protected void recoverFromBackUp(File originalDpu) {
        File originalDpuBackUp = createBackUpName(originalDpu);
        if (originalDpuBackUp.exists()) {
            // remove new file
            if (originalDpu.exists()) {
                originalDpu.delete();
            }
            // use backup
            originalDpuBackUp.renameTo(originalDpu);
        }
    }

    /**
     * Delete backUp if exist.
     * 
     * @param originalDpu
     *            The original DPU's jar file.
     */
    protected void deleteBackUp(File originalDpu) {
        File originalDpuBackUp = createBackUpName(originalDpu);
        originalDpuBackUp.delete();
    }

}

package cz.cuni.mff.xrg.odcs.commons.module.dialog;

import java.util.Arrays;

import cz.cuni.mff.xrg.odcs.commons.configuration.ConfigException;
import cz.cuni.mff.xrg.odcs.commons.configuration.DPUConfigObject;
import cz.cuni.mff.xrg.odcs.commons.module.config.ConfigWrap;
import cz.cuni.mff.xrg.odcs.commons.web.AbstractConfigDialog;

/**
 * 
 * Class which should be used by DPU developer as a base class from which his
 * DPU's configuration dialog is derived.
 * 
 * @author petyr
 * @param <C> Particular configuration object of the DPU
 */
public abstract class BaseConfigDialog<C extends DPUConfigObject>
		extends AbstractConfigDialog<C> {

	/**
	 * Used to convert configuration object into byte array and back.
	 */
	private ConfigWrap<C> configWrap;

	/**
	 * Last valid configuration that is in dialog. Is used to detect changes in
	 * configuration by function {@link #hasConfigChanged()}.
	 */
	private byte[] lastSetConfig;
	
	public BaseConfigDialog(Class<C> configClass) {
		this.configWrap = new ConfigWrap<>(configClass);
		this.lastSetConfig = null;
	}

	@Override
	public void setConfig(byte[] conf) throws ConfigException {
		C config = configWrap.deserialize(conf);
		if (config == null) {
			// null -> try to use default configuration
			config = configWrap.createInstance();
			if (config == null) {
				throw new ConfigException(
						"Missing configuration and failed to create default."
								+ "No configuration loaded into dialog.");
			}
		}
		// in every case set the configuration
		setConfiguration(config);
		lastSetConfig = conf;
		if (!config.isValid()) {
			// notify for invalid configuration
			throw new ConfigException(
					"Invalid configuration loaded into dialog.");
		}
	}

	@Override
	public byte[] getConfig() throws ConfigException {
		C configuration = getConfiguration();
		// check for validity before saving
		if (configuration == null || !configuration.isValid()) {
			throw new ConfigException("Invalid configuration.");
		} else {
			lastSetConfig = configWrap.serialize(getConfiguration());
			return lastSetConfig;
		}
	}

	@Override
	public String getToolTip() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public boolean hasConfigChanged() {
		byte[] configByte = null;
		try {
			C config = getConfiguration();			
			configByte = configWrap.serialize(config);
		} catch (ConfigException e) {
			// exception according to definition return false
			return false;
		}
		
		// just compare, if comparison is true .. then
		// the configuration is the same so return false 
		return !Arrays.equals(configByte, lastSetConfig);
	}
	
	/**
	 * Set dialog interface according to passed configuration. If the passed
	 * configuration is invalid ConfigException can be thrown.
	 * 
	 * @param conf Configuration object.
	 * @throws ConfigException
	 */
	protected abstract void setConfiguration(C conf) throws ConfigException;

	/**
	 * Get configuration from dialog. In case of presence invalid configuration
	 * in dialog throw ConfigException.
	 * 
	 * @return getConfiguration object.
	 */
	protected abstract C getConfiguration() throws ConfigException;
}
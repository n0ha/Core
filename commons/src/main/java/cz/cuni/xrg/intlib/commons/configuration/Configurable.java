package cz.cuni.xrg.intlib.commons.configuration;


/**
 * Interface describes object that can be configured by using configuration.
 * 
 * @author Petyr
 *
 */
public interface Configurable <C extends DPUConfigObject> {

	/**
	 * Configure object based on passed object. If the object does not
	 * contains valid configuration ConfigException should be thrown.
	 * @param c Object with configuration.
	 * @throws ConfigException In case of invalid configuration.
	 */
	void configure(C c) throws ConfigException;

	/**
	 * Return object configuration. 
	 * @return Object configuration.
	 */
	C getConfiguration();
}

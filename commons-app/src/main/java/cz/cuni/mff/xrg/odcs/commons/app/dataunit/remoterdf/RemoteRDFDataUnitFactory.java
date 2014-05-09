package cz.cuni.mff.xrg.odcs.commons.app.dataunit.remoterdf;

import cz.cuni.mff.xrg.odcs.commons.app.dataunit.ManagableRdfDataUnit;
import cz.cuni.mff.xrg.odcs.commons.app.dataunit.RDFDataUnitFactory;

public class RemoteRDFDataUnitFactory implements RDFDataUnitFactory {
	private String url;
	
	private String user;
	
	private String password;
	
	@Override
	public ManagableRdfDataUnit create(String dataUnitName, String dataGraph) {
		return new RemoteRDFDataUnit(url, user, password, dataUnitName, dataGraph);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
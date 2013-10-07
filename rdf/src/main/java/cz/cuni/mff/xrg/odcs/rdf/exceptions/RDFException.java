package cz.cuni.mff.xrg.odcs.rdf.exceptions;

/**
 *
 * Exception is thrown when RDF operation (extract,transform, load) cause
 * problems - was not executed successfully.
 *
 * @author Jiri Tomes
 */
public class RDFException extends RDFDataUnitException {

	public RDFException() {
		super();
	}

	public RDFException(Throwable cause) {
		super(cause);
	}

	public RDFException(String message) {
		super(message);
	}

	public RDFException(String message, Throwable cause) {
		super(message, cause);
	}
}
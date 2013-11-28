package cz.cuni.mff.xrg.odcs.extractor.file;

import cz.cuni.mff.xrg.odcs.commons.data.DataUnitException;
import cz.cuni.mff.xrg.odcs.commons.dpu.DPUContext;
import cz.cuni.mff.xrg.odcs.commons.dpu.DPUException;
import cz.cuni.mff.xrg.odcs.commons.dpu.annotation.AsExtractor;
import cz.cuni.mff.xrg.odcs.commons.dpu.annotation.OutputDataUnit;
import cz.cuni.mff.xrg.odcs.commons.message.MessageType;
import cz.cuni.mff.xrg.odcs.commons.module.dpu.ConfigurableBase;
import cz.cuni.mff.xrg.odcs.commons.web.AbstractConfigDialog;
import cz.cuni.mff.xrg.odcs.commons.web.ConfigDialogProvider;
import cz.cuni.mff.xrg.odcs.rdf.enums.FileExtractType;
import cz.cuni.mff.xrg.odcs.rdf.enums.HandlerExtractType;
import cz.cuni.mff.xrg.odcs.rdf.enums.RDFFormatType;
import cz.cuni.mff.xrg.odcs.rdf.exceptions.RDFException;
import cz.cuni.mff.xrg.odcs.rdf.handlers.StatisticalHandler;
import cz.cuni.mff.xrg.odcs.rdf.interfaces.RDFDataUnit;

import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jiri Tomes
 * @author Petyr
 */
@AsExtractor
public class FileExtractor extends ConfigurableBase<FileExtractorConfig>
		implements ConfigDialogProvider<FileExtractorConfig> {

	private final Logger LOG = LoggerFactory.getLogger(FileExtractor.class);

	@OutputDataUnit
	public RDFDataUnit rdfDataUnit;

	public FileExtractor() {
		super(FileExtractorConfig.class);
	}

	@Override
	public void execute(DPUContext context) throws DataUnitException, DPUException {

		final String baseURI = "";
		final FileExtractType extractType = config.getFileExtractType();
		final String path = config.getPath();
		final String fileSuffix = config.getFileSuffix();
		final boolean onlyThisSuffix = config.isOnlyThisSuffix();

		boolean useStatisticHandler = config.isUseStatisticalHandler();
		boolean failWhenErrors = config.isFailWhenErrors();

		final HandlerExtractType handlerExtractType = HandlerExtractType
				.getHandlerType(useStatisticHandler, failWhenErrors);

		RDFFormatType formatType = config.getRDFFormatValue();
		final RDFFormat format = RDFFormatType.getRDFFormatByType(formatType);

		LOG.debug("extractType: {}", extractType);
		LOG.debug("format: {}", format);
		LOG.debug("path: {}", path);
		LOG.debug("fileSuffix: {}", fileSuffix);
		LOG.debug("baseURI: {}", baseURI);
		LOG.debug("onlyThisSuffix: {}", onlyThisSuffix);
		LOG.debug("useStatisticHandler: {}", useStatisticHandler);

		try {
			rdfDataUnit.extractFromFile(extractType, format, path, fileSuffix,
					baseURI, onlyThisSuffix, handlerExtractType);

			if (useStatisticHandler && StatisticalHandler.hasParsingProblems()) {

				String problems = StatisticalHandler
						.getFindedGlobalProblemsAsString();
				StatisticalHandler.clearParsingProblems();

				context.sendMessage(MessageType.WARNING,
						"Statistical and error handler has found during parsing problems triples (these triples were not added)",
						problems);
			}
		} catch (RDFException e) {
			context.sendMessage(MessageType.ERROR, e.getMessage());
			throw new DPUException(e.getMessage(), e);
		}
		final long triplesCount = rdfDataUnit.getTripleCount();
		LOG.info("Extracted {} triples", triplesCount);
	}

	@Override
	public AbstractConfigDialog<FileExtractorConfig> getConfigurationDialog() {
		return new FileExtractorDialog();
	}
}

package cz.cuni.mff.xrg.odcs.frontend.container.accessor;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Embedded;
import java.util.Arrays;
import java.util.List;

import cz.cuni.mff.xrg.odcs.commons.app.pipeline.Pipeline;
import cz.cuni.mff.xrg.odcs.commons.app.pipeline.PipelineExecution;
import cz.cuni.mff.xrg.odcs.commons.app.pipeline.PipelineExecutionStatus;
import cz.cuni.mff.xrg.odcs.commons.app.facade.PipelineFacade;
import cz.cuni.mff.xrg.odcs.frontend.auxiliaries.DecorationHelper;
import cz.cuni.mff.xrg.odcs.frontend.doa.container.ClassAccessor;
import cz.cuni.mff.xrg.odcs.frontend.container.DataTimeCache;
import cz.cuni.mff.xrg.odcs.frontend.gui.views.Utils;
import java.text.DateFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PipelineAccessor implements ClassAccessor<Pipeline> {

	private static final Logger LOG = LoggerFactory.getLogger(PipelineAccessor.class);
		
	private List<String> all = Arrays.asList("id", "name", "description", "duration", "lastExecTime", "lastExecStatus");
	
	private List<String> sortable = Arrays.asList("id", "name");
	
	private List<String> filtrable = Arrays.asList("id", "name", "description");
	
	@Autowired
	PipelineFacade pipelineFacade;
	
	/**
	 * Cache for last pipeline execution, so we do not load from DB every time
	 * table cell with duration, start, ..., is needed.
	 */
	private DataTimeCache<PipelineExecution> execCache = new DataTimeCache<>();

	@Override
	public List<String> all() {
		return all;
	}

	@Override
	public List<String> sortable() {
		return sortable;
	}

	@Override
	public List<String> filtrable() {
		return filtrable;
	}

	@Override
	public List<String> visible() {
		return all;
	}

	@Override
	public List<String> toFetch() {
		return null;
	}
	
	@Override
	public Class<Pipeline> getEntityClass() {
		return Pipeline.class;
	}

	@Override
	public String getColumnName(String id) {
		switch (id) {
			case "id":
				return "id";
			case "name":
				return "name";
			case "description":
				return "description";
			case "duration":
				return "Last run time";
			case "lastExecTime":
				return "Last execution time";
			case "lastExecStatus":
				return "Last status";
			default:
				return id;
		}
	}

	@Override
	public Object getValue(Pipeline object, String id) {
		switch (id) {
			case "id":
				return object.getId();
			case "name":
				String name = object.getName();
				return name.length() > Utils.getColumnMaxLenght() ? name.substring(0, Utils.getColumnMaxLenght() - 3) + "..." : name;
			case "description":
				String description = object.getDescription();
				return description.length() > Utils.getColumnMaxLenght() ? description.substring(0, Utils.getColumnMaxLenght() - 3) + "..." : description;
			case "duration":
				PipelineExecution latestExec = pipelineFacade.getLastExec(object, PipelineExecutionStatus.FINISHED);
				return DecorationHelper.getDuration(latestExec);
			case "lastExecTime":
				return getLastExecutionTime(object);
			case "lastExecStatus":
				return getLastExecutionStatus(object);
			default:
				return null;
		}
	}

	@Override
	public Class<?> getType(String id) {
		switch (id) {
			case "id":
				return Integer.class;
			case "name":
			case "description":
			case "duration":
			case "lastExecTime":
				return String.class;
			case "lastExecStatus":
				return Embedded.class;
			default:
				return null;
		}
	}

	/**
	 * Clears the pipeline cache.
	 */
	public void clearExecCache() {
		execCache.invalidate();
	}

	/**
	 * Get last pipeline execution from cache. If execution is not found in
	 * cache, it is loaded from DB and cached.
	 *
	 * @param ppl pipeline
	 * @return last execution for given pipeline
	 */
	private PipelineExecution getLastExecution(Pipeline ppl) {
		PipelineExecution exec = execCache.get(ppl.getId());
		if (exec == null) {
			exec = pipelineFacade.getLastExec(ppl);
			execCache.set(ppl.getId(), exec);
		}
		return exec;
	}

	Object getLastExecutionTime(Pipeline ppl) {
		PipelineExecution latestExec = getLastExecution(ppl);
		if (latestExec != null) {
			if (latestExec.getStart() == null) {
				// no start time for last execution
				LOG.warn("The start time for execuiton id: {} is null", latestExec.getId());
				return null;
			}			
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
			return df.format(latestExec.getStart());
		} else {
			return null;
		}
	}

	Object getLastExecutionStatus(Pipeline ppl) {
		PipelineExecution latestExec = getLastExecution(ppl);
		if (latestExec != null) {
			PipelineExecutionStatus type = latestExec.getStatus();
			if (type != null) {
				ThemeResource img = DecorationHelper.getIconForExecutionStatus(type);
				Embedded emb = new Embedded(type.name(), img);
				emb.setDescription(type.name());
				return emb;
			} else {
				return null;
			}
		} else {
			return null;
		}

	}
}

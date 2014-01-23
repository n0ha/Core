package cz.cuni.mff.xrg.odcs.frontend.gui.components;

import com.vaadin.data.Container;
import com.vaadin.data.util.filter.Compare;
import cz.cuni.mff.xrg.odcs.frontend.gui.tables.RecordsTable;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.*;
import com.vaadin.ui.TabSheet.Tab;
import cz.cuni.mff.xrg.odcs.commons.app.facade.DPUFacade;

import cz.cuni.mff.xrg.odcs.commons.app.dpu.DPUInstanceRecord;
import cz.cuni.mff.xrg.odcs.commons.app.execution.log.DbLogRead;
import cz.cuni.mff.xrg.odcs.commons.app.execution.log.Log;
import cz.cuni.mff.xrg.odcs.commons.app.execution.message.DbMessageRecord;
import cz.cuni.mff.xrg.odcs.commons.app.execution.message.MessageRecord;
import cz.cuni.mff.xrg.odcs.commons.app.facade.LogFacade;
import cz.cuni.mff.xrg.odcs.commons.app.pipeline.PipelineExecution;
import cz.cuni.mff.xrg.odcs.commons.app.pipeline.PipelineExecutionStatus;
import cz.cuni.mff.xrg.odcs.commons.app.facade.PipelineFacade;
import cz.cuni.mff.xrg.odcs.frontend.AppEntry;
import cz.cuni.mff.xrg.odcs.frontend.auxiliaries.DecorationHelper;
import cz.cuni.mff.xrg.odcs.frontend.auxiliaries.RefreshManager;
import cz.cuni.mff.xrg.odcs.frontend.container.accessor.MessageRecordAccessor;
import cz.cuni.mff.xrg.odcs.frontend.container.accessor.NewLogAccessor;
import cz.cuni.mff.xrg.odcs.frontend.doa.container.CachedSource;
import cz.cuni.mff.xrg.odcs.frontend.gui.tables.LogTable;
import cz.cuni.mff.xrg.odcs.frontend.gui.tables.OpenLogsEvent;
import cz.cuni.mff.xrg.odcs.frontend.gui.views.Utils;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shows complex debug information about current pipeline execution. Shows
 * information about whole run or if specific DPU is selected only information
 * related to this DPU. Top table shows events which occurred during pipeline
 * execution. DPU selection is available if the pipeline is in debug mode.
 * Bottom part consists of tabs. Log tab shows log messages, which can be
 * filtered by level. Browse tab shows triples from graph which selected DPU
 * created. Query tab allows to query data from graphs which were created during
 * pipeline execution.
 *
 * @author Petyr
 * @author Bogo
 */
public class DebuggingView extends CustomComponent {

	private static final Logger LOG = LoggerFactory.getLogger(DebuggingView.class);
	
	private VerticalLayout mainLayout;
	
	private PipelineExecution pipelineExec;
	
	private DPUInstanceRecord debugDpu;
	
	private boolean isInDebugMode;

	private Tab queryTab;
	
	private Tab logsTab;
	
	private TabSheet tabs;
	
	private Browse browse;

	private boolean isFromCanvas;
	
	private Embedded iconStatus;
	
	private CheckBox refreshAutomatically = null;
	
	private boolean isInitialized = false;

	// - - - - - - - - - - - - - - - - - - - -
	
	private LogTable logTable;

	private RecordsTable msgTable;

	@Autowired
	private DbMessageRecord dbMsg;

	@Autowired
	private DbLogRead dbLogs;

	@Autowired
	private PipelineFacade pipelineFacade;
	
	@Autowired
	private DPUFacade dpuFacade;
	
	// - - - - -
	
	private CachedSource<MessageRecord> msgSource;

	private CachedSource<Log> logSource;

	private final List<Container.Filter> msgCoreFilters = new LinkedList<>();

	private final List<Container.Filter> logCoreFilters = new LinkedList<>();
	
	@Autowired
	private Utils utils;
	
	@Autowired
	private LogFacade logFacade;

	public DebuggingView() {
		// empty ctor .. nothing is done on creation
	}

	public final void initialize(PipelineExecution exec,
			DPUInstanceRecord dpu, boolean debug, boolean isFromCanvas) {

		// set properties
		this.isFromCanvas = isFromCanvas;

		// bind to data sources
		{
			// create sources
			logSource = new CachedSource<>(dbLogs, new NewLogAccessor(), logCoreFilters);
			msgSource = new CachedSource<>(dbMsg, new MessageRecordAccessor(), msgCoreFilters);

			// create tables
			logTable = new LogTable(logSource, logFacade, utils.getPageLength());
			msgTable = new RecordsTable(msgSource, utils.getPageLength());
		}

		// building require some thing to be set 
		// like queryView = new RDFQueryView(pipelineExec);
		this.pipelineExec = exec;
		
		// build gui layout 
		buildMainLayout();
		setCompositionRoot(mainLayout);

		// set 
		setExecution(exec, dpu);

		isInitialized = true;
	}

	public boolean isInitialized() {
		return isInitialized;
	}

	/**
	 * Builds main layout.
	 */
	public final void buildMainLayout() {

		mainLayout = new VerticalLayout();

		if (isFromCanvas) {
			HorizontalLayout topLine = new HorizontalLayout();
			Label labelPipelineStatus = new Label("Pipeline status:");
			topLine.addComponent(labelPipelineStatus);
			iconStatus = new Embedded();
			iconStatus.setImmediate(true);
			topLine.addComponent(iconStatus);
			mainLayout.addComponent(topLine);
		}

		tabs = new TabSheet();
		tabs.setSizeFull();

		msgTable.addListener(new Listener() {
			@Override
			public void componentEvent(Event event) {
				if (event.getClass() == OpenLogsEvent.class) {
					// we open tab with log's here
					OpenLogsEvent ole = (OpenLogsEvent) event;
					// we get activ dpu
					debugDpu = dpuFacade.getDPUInstance(ole.getDpuId());
					// we set active dpu
					logTable.setDpu(debugDpu);
					// and we do refresh of table
					logTable.refresh(pipelineExec);
					// and finally change the tab
					tabs.setSelectedTab(logsTab);
				}
			}
		});
		msgTable.setWidth("100%");

		tabs.addTab(msgTable, "Events");

		HorizontalLayout optionLine = new HorizontalLayout();
		optionLine.setWidth(100, Unit.PERCENTAGE);

		//if (!isRunFinished()) {
		refreshAutomatically = new CheckBox("Refresh automatically", true);
		refreshAutomatically.setImmediate(true);
		refreshAutomatically.setVisible(false);
		optionLine.addComponent(refreshAutomatically);
		optionLine.setComponentAlignment(refreshAutomatically, Alignment.MIDDLE_RIGHT);
		//}
		mainLayout.addComponent(optionLine);

		VerticalLayout logLayout = new VerticalLayout();
		logLayout.addComponent(logTable);
		logLayout.setSizeFull();
		logsTab = tabs.addTab(logLayout, "Log");
		
		browse = new Browse(pipelineExec);
		if (debugDpu != null) {
			browse.setDpu(debugDpu);
		}
		queryTab = tabs.addTab(browse, "Browse/Query");

		mainLayout.setSizeFull();
		mainLayout.addComponent(tabs);

		fillContent(false);
	}

	/**
	 * Fills DebuggingView with data, obtained from objects passed in
	 * constructor.
	 * 
	 * @param doRefresh If true then the refresh is done
	 */
	public void fillContent(boolean doRefresh) {

		if (isFromCanvas) {
			// update execution icon .. 
			ThemeResource icon = DecorationHelper.getIconForExecutionStatus(pipelineExec.getStatus());
			iconStatus.setSource(icon);
			iconStatus.setDescription(pipelineExec.getStatus().name());
		}

		LOG.trace("Tables refresh start");
		if (doRefresh) {
			// refresh data .. 
			logSource.invalidate();
			msgSource.invalidate();
			// refresh tables
			logTable.refresh(pipelineExec);
			msgTable.refresh();
		}
		LOG.trace("Tables refresh done");
		
		// refresh of query View
		if (isInDebugMode && isRunFinished()) {
			queryTab.setEnabled(true);
			browse.refreshDPUs(pipelineExec);
		} else {
			// no query possibility if we are in debug mode
			// or the pipeline is not finished yet
			queryTab.setEnabled(false);
		}

		// hide the refresh automaticaly check box if the pipeline 
		// execution is finished
		refreshAutomatically.setVisible(!isRunFinished());
	}

	/**
	 * Refresh content for current data.
	 */
	public void refresh() {
		// refresh out pipeline execution .. 
		pipelineExec = pipelineFacade.getExecution(pipelineExec.getId());
		// refresh the content
		fillContent(true);
	}

	/**
	 * Sets execution and debug node about which debug info should be shown.
	 *
	 * @param execution New execution.
	 * @param instance New debug node.
	 *
	 */
	public void setExecution(PipelineExecution execution, DPUInstanceRecord instance) {
		this.pipelineExec = execution;
		this.isInDebugMode = execution.isDebugging();
		this.debugDpu = instance;
		// update core filters
		msgCoreFilters.clear();
		logCoreFilters.clear();
		msgCoreFilters.add(new Compare.Equal("execution.id", execution.getId()));
		logCoreFilters.add(new Compare.Equal("execution", execution.getId()));

		//set page lenght
		logTable.setPageLength(utils.getPageLength());
		msgTable.setPageLength(utils.getPageLength());
		// update the log table
		logTable.setExecution(pipelineExec, instance);
		msgTable.setExecution(execution);
				
		// update content, but do not refresh data in tables
		// as they have already been refresh by setting the executions
		fillContent(false);

		if (!isRunFinished()) {
			// add us to the refresh manager, so we got some refresh events
			((AppEntry)UI.getCurrent()).getRefreshManager().addListener(
					RefreshManager.DEBUGGINGVIEW, 
					RefreshManager.getDebugRefresher(this, execution, pipelineFacade));
		}
	}

	/**
	 * Returns whether given execution is finished.
	 *
	 * @return True is the execution is finished.
	 */
	public boolean isRunFinished() {
		return !(pipelineExec.getStatus() == PipelineExecutionStatus.QUEUED
				|| pipelineExec.getStatus() == PipelineExecutionStatus.RUNNING
				|| pipelineExec.getStatus() == PipelineExecutionStatus.CANCELLING);
	}

	/**
	 * Return true if the content is automatically refreshed.
	 *
	 * @return
	 */
	public boolean isRefreshingAutomatically() {
		return refreshAutomatically.getValue();
	}

	/**
	 * Resizes log area after window with DebuggingView was resized.
	 *
	 * @param height New height of log text area.
	 */
	public void resize(float height) {
		// TODO Resize the content here .. 
	}

	/**
	 * Fires refresh request event.
	 */
	protected void fireRefreshRequest() {
		Collection<Listener> ls = (Collection<Listener>) this.getListeners(Component.Event.class);
		for (Listener l : ls) {
			l.componentEvent(new Event(this));
		}
	}

	public void setActiveTab(String tabName) {
		int tabIdx;
		switch (tabName) {
			case "Events":
				tabIdx = 0;
				break;
			case "Log":
				tabIdx = 1;
				break;
			case "Browse":
				tabIdx = 2;
				break;
			default:
				tabIdx = 0;
		}
		tabs.setSelectedTab(tabIdx);
	}

}

package cz.cuni.xrg.intlib.frontend.gui.views;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Window.CloseListener;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.Validator;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.*;

import cz.cuni.xrg.intlib.commons.app.dpu.DPUInstanceRecord;
import cz.cuni.xrg.intlib.commons.app.dpu.DPUTemplateRecord;
import cz.cuni.xrg.intlib.commons.app.dpu.VisibilityType;
import cz.cuni.xrg.intlib.commons.app.module.ModuleException;
import cz.cuni.xrg.intlib.commons.app.pipeline.Pipeline;
import cz.cuni.xrg.intlib.commons.app.pipeline.graph.Node;
import cz.cuni.xrg.intlib.commons.configuration.Config;
import cz.cuni.xrg.intlib.commons.configuration.ConfigException;
import cz.cuni.xrg.intlib.commons.web.AbstractConfigDialog;
import cz.cuni.xrg.intlib.frontend.auxiliaries.App;
import cz.cuni.xrg.intlib.frontend.auxiliaries.dpu.DPUTemplateWrap;
import cz.cuni.xrg.intlib.frontend.gui.ViewComponent;
import cz.cuni.xrg.intlib.frontend.gui.components.DPUCreate;
import cz.cuni.xrg.intlib.frontend.gui.components.DPUTree;
import cz.cuni.xrg.intlib.frontend.gui.components.IntlibPagedTable;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.dialogs.ConfirmDialog;

/**
 * @author Maria Kukhar
 */

class DPU extends ViewComponent {

	/*
	 * @AutoGenerated private AbsoluteLayout mainLayout;
	 * 
	 * @AutoGenerated private Label label; private Label lblUri; private
	 * TextField txtUri; private Button btnOpenDialog;
	 */
	private VerticalLayout mainLayout;
	private VerticalLayout verticalLayoutData;
	private VerticalLayout verticalLayoutConfigure;
	private VerticalLayout verticalLayoutInstances;
	private VerticalLayout dpuDetailLayout;
	private DPUTree dpuTree;
	private TextField dpuName;
	private TextArea dpuDescription;
	private TabSheet tabSheet;	
	private OptionGroup groupVisibility;
	private GridLayout dpuLayout;
	private HorizontalLayout buttonDpuBar;
	private Config conf;
	private HorizontalLayout layoutInfo;
	private IntlibPagedTable instancesTable;
	private IndexedContainer tableData;
	
	static String[] visibleCols = new String[]{"id", "name", "description",
		"author", "actions"};

	static String[] headers = new String[]{"Id", "Name", "Description",
		"Author", "Actions"};
	

	/**
	 * Wrap for selected DPUTemplateRecord.
	 */
	private DPUTemplateWrap selectedDpuWrap = null;
	
	private static final Logger LOG = LoggerFactory.getLogger(ViewComponent.class);
	
	/*- VaadinEditorProperties={"grid":"RegularGrid,20","showGrid":true,"snapToGrid":true,"snapToObject":true,"movingGuides":false,"snappingDistance":10} */

	/**
	 * The constructor should first build the main layout, set the composition
	 * root and then do any custom initialization.
	 * 
	 * The constructor will not be automatically regenerated by the visual
	 * editor.
	 */
	public DPU() {

	}

	@AutoGenerated
	private VerticalLayout buildMainLayout() {
		// common part: create layout
		mainLayout = new VerticalLayout();
		mainLayout.setImmediate(true);
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		mainLayout.setWidth("100%");
		mainLayout.setHeight("100%");
		mainLayout.setStyleName("mainLayout");


		// top-level component properties
	//	setSizeUndefined();
		setWidth("100%");
		setHeight("100%");

		// Buttons on the top: "Create DPU", "Import DPU", "Export All"
		HorizontalLayout buttonBar = new HorizontalLayout();
	//	buttonBar.setWidth("100%");

		Button buttonCreateDPU = new Button();
		buttonCreateDPU.setCaption("Create DPU template");
		buttonCreateDPU.setHeight("25px");
		buttonCreateDPU.setWidth("150px");
		buttonCreateDPU
				.addClickListener(new com.vaadin.ui.Button.ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {
						DPUCreate createDPU = new DPUCreate();
						App.getApp().addWindow(createDPU);
						createDPU.addListener(new CloseListener() {
							
							@Override
							public void windowClose(CloseEvent e) {
								// TODO Auto-generated method stub
								DPUCreate.uploadInfoWindow.close();
								dpuTree.refresh();

							}
						});
					}
				});
		buttonBar.addComponent(buttonCreateDPU);

		Button buttonImportDPU = new Button();
		buttonImportDPU.setCaption("Import DPU template");
		buttonImportDPU.setHeight("25px");
		buttonImportDPU.setWidth("150px");
		buttonImportDPU
				.addClickListener(new com.vaadin.ui.Button.ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {

					}
				});
		buttonBar.addComponent(buttonImportDPU);

		Button buttonExportAll = new Button();
		buttonExportAll.setCaption("Export All");
		buttonExportAll.setHeight("25px");
		buttonExportAll.setWidth("150px");
		buttonExportAll
				.addClickListener(new com.vaadin.ui.Button.ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {

					}
				});
		buttonBar.addComponent(buttonExportAll);

		mainLayout.addComponent(buttonBar);
	
		GridLayout dpuLayout = buildDpuLayout();
		mainLayout.addComponent(dpuLayout);

		return mainLayout;
	}

	/**
	 * Building layout contains DPURecord tree and DPURecord details.
	 */
	@SuppressWarnings({ "serial", "deprecation" })
	private GridLayout buildDpuLayout() {

		dpuLayout = new GridLayout(3, 1);
		dpuLayout.setSpacing(true);
	//	dpuLayout.setWidth("100%");
	//	dpuLayout.setHeight("100%");
		dpuLayout.setHeight(630, Unit.PIXELS);
		dpuLayout.setRowExpandRatio(0, 0.01f);
		dpuLayout.setRowExpandRatio(1, 0.99f);
				
		layoutInfo = new HorizontalLayout();
		layoutInfo.setHeight("100%");
		layoutInfo.setWidth("100%");
		Label infoLabel = new Label();
		infoLabel.setImmediate(false);
		infoLabel.setWidth("-1px");
		infoLabel.setHeight("-1px");
		infoLabel.setValue("<br><br><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Select DPURecord from the DPURecord tree for displaying it's details.");
		infoLabel.setContentMode(ContentMode.HTML);
		layoutInfo.addComponent(infoLabel);
		
		

		dpuTree = new DPUTree();
		dpuTree.addItemClickListener(new ItemClickListener() {

			@Override
			public void itemClick(ItemClickEvent event) {

				if(event.getItemId().getClass() != DPUTemplateRecord.class) {
					dpuLayout.removeComponent(dpuDetailLayout);
					dpuLayout.removeComponent(layoutInfo);
					dpuLayout.addComponent(layoutInfo, 2, 0);
					return;
				}

					DPUTemplateRecord selectedDpu = (DPUTemplateRecord) event
							.getItemId();

					if ((selectedDpu != null) && (selectedDpu.getId() != null)) {
						// crate new wrap
						selectedDpuWrap = new DPUTemplateWrap(selectedDpu);

						dpuLayout.removeComponent(dpuDetailLayout);
						dpuLayout.removeComponent(layoutInfo);
						dpuDetailLayout = buildDPUDetailLayout();
						dpuLayout.addComponent(dpuDetailLayout, 1, 0);

						String selectedDpuName = selectedDpuWrap
								.getDPUTemplateRecord().getName();
						String selecteDpuDescription = selectedDpuWrap
								.getDPUTemplateRecord().getDescription();
						VisibilityType selecteDpuVisibility = selectedDpuWrap
								.getDPUTemplateRecord().getVisibility();
						dpuName.setValue(selectedDpuName);
						dpuDescription.setValue(selecteDpuDescription);

						groupVisibility.setValue(selecteDpuVisibility);
						groupVisibility.setEnabled(true);
						if (selecteDpuVisibility == VisibilityType.PUBLIC) {
							groupVisibility.setValue(selecteDpuVisibility);
							groupVisibility.setEnabled(false);
						} else {
							groupVisibility.setValue(selecteDpuVisibility);
							groupVisibility.setEnabled(true);
						}
				} else {
					dpuLayout.removeComponent(dpuDetailLayout);
					dpuLayout.removeComponent(layoutInfo);
					dpuLayout.addComponent(layoutInfo, 2, 0);

				}
			}
		});

		
		dpuLayout.addComponent(dpuTree,0,0);
		dpuLayout.addComponent(layoutInfo, 2, 0);

		return dpuLayout;
	}

	/**
	 * Create dialog with detail for DPU in {@link #selectedDpu}
	 * @return
	 */
	private VerticalLayout buildDPUDetailLayout() {

		dpuDetailLayout = new VerticalLayout();
		dpuDetailLayout.setImmediate(true);
		dpuDetailLayout.setStyleName("dpuDetailLayout");
	//	dpuDetailLayout.setWidth("100.0%");
	//	dpuDetailLayout.setHeight("100%");
	//	dpuDetailLayout.setHeight(630, Unit.PIXELS);
		dpuDetailLayout.setMargin(true);

		tabSheet = new TabSheet();
		tabSheet.setWidth(630, Unit.PIXELS);
		tabSheet.setHeight(350, Unit.PIXELS);

		verticalLayoutData = buildVerticalLayoutData();
		Tab dataTab = tabSheet.addTab(verticalLayoutData, "General");

		verticalLayoutConfigure = new VerticalLayout();
		verticalLayoutConfigure.setImmediate(false);
//		verticalLayoutConfigure.setWidth("100.0%");
//		verticalLayoutConfigure.setHeight("100%");
		verticalLayoutConfigure.setMargin(true);
		tabSheet.addTab(verticalLayoutConfigure, "Template Configuration");
		tabSheet.setSelectedTab(dataTab);

		if (selectedDpuWrap != null) {
			AbstractConfigDialog<Config> configDialog = null;
			
			try {
				configDialog = selectedDpuWrap.getDialog();
			} catch (ModuleException ex) {
				Notification.show(
						"Failed to load configuration dialog",
						ex.getMessage(), Type.ERROR_MESSAGE);
				LOG.error("Can't load DPU '{}'", selectedDpuWrap.getDPUTemplateRecord().getId(), ex);
			} catch (FileNotFoundException ex) {
				Notification.show(
						"File not found",
						ex.getMessage(), Type.ERROR_MESSAGE);
				LOG.error("Can't load DPU '{}'", selectedDpuWrap.getDPUTemplateRecord().getId(), ex);
			} catch (ConfigException ex) {
				Notification.show(
						"Failed to load configuration. Dialog default configuration is used.",
						ex.getMessage(), Type.WARNING_MESSAGE);
				LOG.error("Can't load configuration '{}'", selectedDpuWrap.getDPUTemplateRecord().getId(), ex);
			}
			
			verticalLayoutConfigure.removeAllComponents();
			if (configDialog == null) {
				// use some .. dummy component
			}  else {
				// add dialog
				verticalLayoutConfigure.addComponent(configDialog);
			}
		}

		// Pipelines using the given DPU : DPU instances tab
		verticalLayoutInstances = buildVerticalLayoutInstances();
		tabSheet.addTab(verticalLayoutInstances, "DPU instances");

		dpuDetailLayout.addComponent(tabSheet);
		buttonDpuBar = buildDPUButtonBur();
		dpuDetailLayout.addComponent(buttonDpuBar);

		return dpuDetailLayout;
	}

	private VerticalLayout buildVerticalLayoutData() {

		// common part: create layout
		verticalLayoutData = new VerticalLayout();
		verticalLayoutData.setImmediate(false);
		verticalLayoutData.setWidth("100.0%");
		verticalLayoutData.setHeight("100%");
		verticalLayoutData.setMargin(true);

		GridLayout dpuSettingsLayout = new GridLayout(2, 5);
		dpuSettingsLayout.setStyleName("dpuSettingsLayout");
		dpuSettingsLayout.setMargin(true);
		dpuSettingsLayout.setSpacing(true);
		dpuSettingsLayout.setWidth("100%");
		dpuSettingsLayout.setHeight("100%");
		dpuSettingsLayout.setColumnExpandRatio(0, 0.10f);
		dpuSettingsLayout.setColumnExpandRatio(1, 0.90f);

		Label nameLabel = new Label("Name:");
		nameLabel.setImmediate(false);
		nameLabel.setWidth("-1px");
		nameLabel.setHeight("-1px");
		dpuSettingsLayout.addComponent(nameLabel, 0, 0);
		dpuName = new TextField();
		dpuName.setImmediate(true);
		dpuName.setWidth("200px");
		dpuName.setHeight("-1px");
		dpuName.addValidator(new Validator() {

			@Override
			public void validate(Object value) throws InvalidValueException {
				if (value.getClass() == String.class
						&& !((String) value).isEmpty()) {
					return;
				}
				throw new InvalidValueException("Name must be filled!");
			}
		});
		dpuSettingsLayout.addComponent(dpuName, 1, 0);
		Label descriptionLabel = new Label("Description:");
		descriptionLabel.setImmediate(false);
		descriptionLabel.setWidth("-1px");
		descriptionLabel.setHeight("-1px");
		dpuSettingsLayout.addComponent(descriptionLabel, 0, 1);
		dpuDescription = new TextArea();
		dpuDescription.setImmediate(true);
		dpuDescription.setWidth("100%");
		dpuDescription.setHeight("60px");
		dpuSettingsLayout.addComponent(dpuDescription, 1, 1);

		Label visibilityLabel = new Label("Visibility:");
		dpuSettingsLayout.addComponent(visibilityLabel, 0, 2);

		groupVisibility = new OptionGroup();
		groupVisibility.addStyleName("horizontalgroup");
		groupVisibility.addItem(VisibilityType.PRIVATE);
		groupVisibility.addItem(VisibilityType.PUBLIC);

		dpuSettingsLayout.addComponent(groupVisibility, 1, 2);
		
		
		// JAR path
		HorizontalLayout jarPathLayout = new HorizontalLayout();
		jarPathLayout.setImmediate(false);
		jarPathLayout.setSpacing(true);
		jarPathLayout.setHeight("100%");

		dpuSettingsLayout.addComponent(new Label("JAR path:"),0,3);
		Label jPath = new Label( selectedDpuWrap.getDPUTemplateRecord().getJarPath() );
		dpuSettingsLayout.addComponent(jPath,1,3);
		
		// created in "buildDPUDetailLayout"
		
		dpuSettingsLayout.addComponent(new Label("Description of JAR:"),0,4);
		TextArea jDescription = new TextArea(selectedDpuWrap.getDPUTemplateRecord().getJarDescription());
		jDescription.setReadOnly(true);
		jDescription.setWidth("100%");
		jDescription.setHeight("100%");		
		dpuSettingsLayout.addComponent(jDescription,1,4);
		
		
		

		verticalLayoutData.addComponent(dpuSettingsLayout);
		
		

		return verticalLayoutData;
	}

	private VerticalLayout buildVerticalLayoutInstances() {

		// common part: create layout
		verticalLayoutInstances = new VerticalLayout();
		verticalLayoutInstances.setImmediate(false);
		verticalLayoutInstances.setWidth("100.0%");
//		verticalLayoutInstances.setHeight("300px");
		verticalLayoutInstances.setMargin(true);
		
		tableData = getTableData();
		
		instancesTable = new IntlibPagedTable();
		instancesTable.setSelectable(true);
		instancesTable.setCaption("Pipelines:");
		instancesTable.setContainerDataSource(tableData);
		
		instancesTable.setWidth("100%");
//		instancesTable.setHeight("200%");
		instancesTable.setImmediate(true);
		instancesTable.setVisibleColumns(visibleCols); // Set visible columns
		instancesTable.setColumnHeaders(headers);
		
		instancesTable.addGeneratedColumn("actions",
				new actionColumnGenerator());
		
		verticalLayoutInstances.addComponent(instancesTable);
		verticalLayoutInstances.addComponent(instancesTable.createControls());
		instancesTable.setPageLength(6);

	return verticalLayoutInstances;
}

	private HorizontalLayout buildDPUButtonBur() {

		buttonDpuBar = new HorizontalLayout();
		buttonDpuBar.setWidth("100%");
		buttonDpuBar.setHeight(30, Unit.PIXELS);
		buttonDpuBar.setSpacing(false);

		Button buttonCopyDPU = new Button();
		buttonCopyDPU.setCaption("Copy");
		buttonCopyDPU.setHeight("25px");
		buttonCopyDPU.setWidth("100px");
		buttonCopyDPU
				.addClickListener(new com.vaadin.ui.Button.ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {
					}
				});
		buttonDpuBar.addComponent(buttonCopyDPU);
		buttonDpuBar.setExpandRatio(buttonCopyDPU, 0.85f);
		buttonDpuBar
				.setComponentAlignment(buttonCopyDPU, Alignment.BOTTOM_LEFT);

		Button buttonDeleteDPU = new Button();
		buttonDeleteDPU.setCaption("Delete");
		buttonDeleteDPU.setHeight("25px");
		buttonDeleteDPU.setWidth("100px");
		buttonDeleteDPU
				.addClickListener(new com.vaadin.ui.Button.ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {
						ConfirmDialog.show(UI.getCurrent(),
								"Delete this DPU template?",
								new ConfirmDialog.Listener() {
							@Override
							public void onClose(ConfirmDialog cd) {
								if (cd.isConfirmed()) {
									deleteDPU();
									
								}
							}
						});



					}
				});
		buttonDpuBar.addComponent(buttonDeleteDPU);
		buttonDpuBar.setExpandRatio(buttonDeleteDPU, 0.85f);
		buttonDpuBar.setComponentAlignment(buttonDeleteDPU,
				Alignment.BOTTOM_LEFT);

		Button buttonExportDPU = new Button();
		buttonExportDPU.setCaption("Export");
		buttonExportDPU.setHeight("25px");
		buttonExportDPU.setWidth("100px");
		buttonExportDPU
				.addClickListener(new com.vaadin.ui.Button.ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {

					}
				});
		buttonDpuBar.addComponent(buttonExportDPU);
		buttonDpuBar.setExpandRatio(buttonExportDPU, 2.55f);
		buttonDpuBar.setComponentAlignment(buttonExportDPU,
				Alignment.BOTTOM_LEFT);

		Button buttonSaveDPU = new Button();
		buttonSaveDPU.setCaption("Save");
		buttonSaveDPU.setHeight("25px");
		buttonSaveDPU.setWidth("100px");
		buttonSaveDPU
				.addClickListener(new com.vaadin.ui.Button.ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {
						if (!dpuName.isValid()) {
							Notification.show("Failed to save DPURecord", "Mandatory fields should be filled", Notification.Type.ERROR_MESSAGE);
							return;
						}
						
						if ((selectedDpuWrap != null )
								&& (selectedDpuWrap.getDPUTemplateRecord().getId() != null)) {
							selectedDpuWrap.getDPUTemplateRecord().setName(dpuName.getValue());
							selectedDpuWrap.getDPUTemplateRecord().setDescription(dpuDescription
									.getValue());
							selectedDpuWrap.getDPUTemplateRecord()
									.setVisibility((VisibilityType) groupVisibility
											.getValue());
							
							// save configuration
							try {
								selectedDpuWrap.saveConfig();
								
							} catch (ConfigException e) {
								selectedDpuWrap.getDPUTemplateRecord().setConf(null);
//								Notification.show(
//										"Failed to save DPURecord.", "Mandatory fields should be filled", Type.ERROR_MESSAGE);

//								LOG.error("Can't save configuration '{}'", selectedDpuWrap.getDPUTemplateRecord().getId(), e);
							}	

							// store into DB
							selectedDpuWrap.save();
							Notification.show("DPURecord was saved",
									Notification.Type.HUMANIZED_MESSAGE);
							
							dpuTree.refresh();

						}

					}
				});
		buttonDpuBar.addComponent(buttonSaveDPU);
		buttonDpuBar.setComponentAlignment(buttonSaveDPU,
				Alignment.BOTTOM_RIGHT);
		dpuDetailLayout.addComponent(buttonDpuBar);

		return buttonDpuBar;
	}
	
	public  IndexedContainer getTableData() {

		IndexedContainer result = new IndexedContainer();

		for (String p : visibleCols) {

				result.addContainerProperty(p, String.class, "");
			}

		List<DPUInstanceRecord> instances = App.getDPUs()
				.getAllDPUInstances();
		for (DPUInstanceRecord item : instances) {

			if (item.getTemplate().getId() == selectedDpuWrap.getDPUTemplateRecord().getId()) {
				List<Pipeline> pipelines = App.getApp().getPipelines()
						.getAllPipelines();

				for (Pipeline pitem : pipelines) {

					Set<Node> nodes = pitem.getGraph()
							.getNodes();

					for (Node nitem : nodes) {

						if (nitem.getDpuInstance().getTemplate().getId() == item.getTemplate().getId()) {
							
							Object num = result.addItem();
							

							result.getContainerProperty(num, "id").setValue(pitem.getId().toString());
							result.getContainerProperty(num, "name").setValue(pitem.getName());
							result.getContainerProperty(num, "description").setValue(pitem.getDescription());
							result.getContainerProperty(num, "author").setValue("");

						}
					}

				}
				break;
			}

		}

		return result;
	}
	
	public void deleteDPU() {
		List<DPUInstanceRecord> instances = App.getDPUs()
				.getAllDPUInstances();
		List<Pipeline> pipelines = App.getApp().getPipelines()
				.getAllPipelines();

		int fl = 0, i = 0;
		int j = 0;
		String[] pipeName = new String[pipelines.size()];
		for (DPUInstanceRecord item : instances) {

			if (item.getTemplate().getId() == selectedDpuWrap.getDPUTemplateRecord().getId()) {
				fl = 1;

				for (Pipeline pitem : pipelines) {

					Set<Node> nodes = pitem.getGraph()
							.getNodes();

					for (Node nitem : nodes) {

						if (nitem.getDpuInstance().getTemplate().getId() == item.getTemplate().getId()) {
							pipeName[j] = pitem.getName()
									.toString();
							j++;
						}
					}

				}
				break;
			}

		}
		if (fl == 0) {

			App.getApp().getDPUs().delete(selectedDpuWrap.getDPUTemplateRecord());
			dpuTree.refresh();
			dpuDetailLayout.removeAllComponents();
			Notification.show("DPURecord was removed",
					Notification.Type.HUMANIZED_MESSAGE);
		} else {
			String names = "";

			for (i = 0; i < j; i++) {
				if (i != j - 1)
					names = names + " " + pipeName[i] + ",";
				else
					names = names + " " + pipeName[i] + ".";

			}

			if (j > 1)
				Notification
						.show("DPURecord can not be removed because it has been used in Pipelines: ",
								names,
								Notification.Type.WARNING_MESSAGE);
			else
				Notification
						.show("DPURecord can not be removed because it has been used in Pipeline: ",
								names,
								Notification.Type.WARNING_MESSAGE);

		}
	}
	

	@Override
	public void enter(ViewChangeEvent event) {
		buildMainLayout();
		setCompositionRoot(mainLayout);
	}

	class actionColumnGenerator implements com.vaadin.ui.Table.ColumnGenerator {
		private ClickListener clickListener = null;
		@Override
		public Object generateCell(final Table source, final Object itemId,
				Object columnId) {
			
			HorizontalLayout layout = new HorizontalLayout();
			
			Button detailButton = new Button();
			detailButton.setCaption("Detail");
			detailButton
			.addClickListener(new com.vaadin.ui.Button.ClickListener() {
				@Override
				public void buttonClick(ClickEvent event) {
					// open scheduler dialog


					

				}
			});
			layout.addComponent(detailButton);

			Button deleteButton = new Button();
			deleteButton.setCaption("Delete");
			layout.addComponent(deleteButton);
			
			
			Button statusButton = new Button();
			statusButton.setCaption("Status");
			layout.addComponent(statusButton);

			return layout;
		}

	}
}

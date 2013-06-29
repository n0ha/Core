package cz.cuni.xrg.intlib.frontend.gui.components;

import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.TabIndexState;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.InlineDateField;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button.ClickEvent;

import cz.cuni.xrg.intlib.commons.app.execution.ExecutionStatus;
import cz.cuni.xrg.intlib.commons.app.pipeline.Pipeline;
import cz.cuni.xrg.intlib.frontend.auxiliaries.App;
import cz.cuni.xrg.intlib.frontend.auxiliaries.ContainerFactory;
import cz.cuni.xrg.intlib.frontend.gui.ViewComponent;
import cz.cuni.xrg.intlib.frontend.gui.ViewNames;
import cz.cuni.xrg.intlib.frontend.gui.components.IntlibPagedTable;


public class SchedulePipeline extends CustomComponent {

	@AutoGenerated
	private GridLayout mainLayout;
	@AutoGenerated
	private Label label;
	private GridLayout autoLayout;
	private GridLayout afterLayout;
	
	private String  afterSelect;
	private String  automatically;
	private Container container; 
	private Container comboboxData;
	private HorizontalLayout labelInerval;
	private TextField pipeFilter;
	private TwinColSelect selectPipe;
	

	/*- VaadinEditorProperties={"grid":"RegularGrid,20","showGrid":true,"snapToGrid":true,"snapToObject":true,"movingGuides":false,"snappingDistance":10} */

	/**
	 * The constructor should first build the main layout, set the
	 * composition root and then do any custom initialization.
	 *
	 * The constructor will not be automatically regenerated by the
	 * visual editor.
	 */
	public SchedulePipeline() {
		
		buildMainLayout();
		setCompositionRoot(mainLayout);

	}

	@AutoGenerated
	private GridLayout buildMainLayout() {
		// common part: create layout
		mainLayout = new GridLayout(2,3);
		mainLayout.setImmediate(false);
//		mainLayout.setWidth("100%");
//		mainLayout.setHeight("100%");
		mainLayout.setSpacing(true);
		mainLayout.setMargin(true);

		mainLayout.setWidth(800, Unit.PIXELS);
//		mainLayout.setHeight(680, Unit.PIXELS);
				
		// top-level component properties
//		setWidth("100%");
//		setHeight("100%");
		
		container = ContainerFactory.CreatePipelines(App.getApp()
				.getPipelines().getAllPipelines());
		
		comboboxData = getComboboxData(App.getApp()
				.getPipelines().getAllPipelines());
		

		
		HorizontalLayout layoutPipeline = new HorizontalLayout();
		layoutPipeline.setSpacing(true);
		layoutPipeline.setMargin(false);
				
		ComboBox comboPipeline = new ComboBox();
		comboPipeline.setImmediate(true);
		comboPipeline.setContainerDataSource(comboboxData);
		comboPipeline.setNullSelectionAllowed(false);
		comboPipeline.setItemCaptionPropertyId("name");
				
		layoutPipeline.addComponent(new Label("Pipeline "));		
		layoutPipeline.addComponent(comboPipeline);
		layoutPipeline.addComponent(new Label(" was selected for scheduling."));
		
		mainLayout.addComponent(layoutPipeline, 0, 0);
		
		automatically = "Schedule the pipeline to run automatically in fixed interval.";
		afterSelect ="Schedule the pipeline to after selected pipeline finishes.";
		OptionGroup scheduleType = new OptionGroup();
		scheduleType.setImmediate(true);
		scheduleType.addItem(automatically);
		scheduleType.addItem(afterSelect);
		scheduleType.setValue(automatically);
		scheduleType.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				// TODO Auto-generated method stub
				if(event.getProperty().getValue().toString()== afterSelect){
					
					mainLayout.removeComponent(1, 1);
					afterLayout = buildAfterLayout();
					mainLayout.addComponent(afterLayout,1,1);
					
				}
				else{
					mainLayout.removeComponent(1, 1);
					mainLayout.addComponent(autoLayout,1,1);
				}
				
			}
		});
		
		
		mainLayout.addComponent(scheduleType,0,1);
		autoLayout = buildAutoLayout();
		mainLayout.addComponent(autoLayout,1,1);

		
		Button createRule = new Button();
		createRule.setCaption("Create scheduler rule");
		createRule.setImmediate(true);
		mainLayout.addComponent(createRule,1,2);
		mainLayout.setComponentAlignment(createRule, Alignment.BOTTOM_RIGHT);		

		
		return mainLayout;
	}
	
	private GridLayout buildAfterLayout(){
		
		afterLayout = new GridLayout(2,2);
		afterLayout.setImmediate(false);
//		afterLayout.setWidth("600px");
		afterLayout.setHeight("300px");
		afterLayout.setSpacing(true);
		
		afterLayout.setColumnExpandRatio(0, 0.2f);
		afterLayout.setColumnExpandRatio(1, 0.8f);

		
		afterLayout.addComponent(new Label("Select pipeline:"),0,0);
		
		
		pipeFilter = new TextField();
		pipeFilter.setImmediate(true);
		pipeFilter.setCaption("Pipeline:");
		pipeFilter.setInputPrompt("name of pipeline");
		pipeFilter.setWidth("110px");
		pipeFilter.setTextChangeEventMode(TextChangeEventMode.LAZY);
		pipeFilter.addTextChangeListener(new TextChangeListener() {
			@Override
			public void textChange(TextChangeEvent event) {


			}
		});

		afterLayout.addComponent(pipeFilter, 1, 0);
		
		
		selectPipe = new TwinColSelect();
       
        selectPipe.setContainerDataSource(container);
        selectPipe.setItemCaptionPropertyId("name");

        selectPipe.setNullSelectionAllowed(true);
        selectPipe.setMultiSelect(true);
        selectPipe.setImmediate(true);
        selectPipe.setWidth("333px");
        selectPipe.setLeftColumnCaption("Available pipelines");
        selectPipe.setRightColumnCaption("Selected pipelines"); 
		
		/*IntlibPagedTable tablePipe = new IntlibPagedTable();
		tablePipe.setWidth("300px");
//		tablePipe.setHeight("400px");
		tablePipe.setSelectable(true);
		tablePipe.setPageLength(10);
		// assign data source
		tablePipe.setContainerDataSource(container);
		// set columns
		tablePipe.setVisibleColumns(new String[] { "name" });
		
		// add column
		tablePipe.addGeneratedColumn("", new actionColumnGenerator());

		
		VerticalLayout layoutTable = new VerticalLayout();
		layoutTable.setSpacing(true);
		
		
		layoutTable.addComponent(tablePipe);
		layoutTable.addComponent(tablePipe.createControls());
		tablePipe.setPageLength(10); */
		afterLayout.addComponent(selectPipe,1,1);
        
//		afterLayout.setComponentAlignment(selectPipe, Alignment.TOP_LEFT);		

		
				
		return afterLayout;
		
	}
	
	private GridLayout buildAutoLayout(){
		
		autoLayout = new GridLayout(2,3);
		autoLayout.setImmediate(false);
//		autoLayout.setWidth("100%");
		autoLayout.setSpacing(true);
//		autoLayout.setWidth("500px");
		autoLayout.setHeight("300px");
		autoLayout.setColumnExpandRatio(0, 0.6f);
		autoLayout.setColumnExpandRatio(1, 0.4f);

		
		autoLayout.addComponent(new Label("Date and time of first execution:"),0,0);

		
		InlineDateField date = new InlineDateField();
		date.setValue(new java.util.Date());
		date.setResolution(date.RESOLUTION_SEC);
//		date.setDateFormat("yyyy-MM-dd HH:mm:ss");
		autoLayout.addComponent(date,1,0);
		
		CheckBox justOnce = new CheckBox();
		justOnce.setCaption("Just once");
		justOnce.setValue(false);
		justOnce.setImmediate(true);
		justOnce.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				// TODO Auto-generated method stub
				if(event.getProperty().getValue().equals(true)){
					labelInerval.setEnabled(false);
				}
				else{
					labelInerval.setEnabled(true);
				}
				
			}
		});
		
		autoLayout.addComponent(justOnce,1,1);
		
		
		autoLayout.addComponent(new Label("Interval:"),0,2);
		
		labelInerval = new HorizontalLayout();
		labelInerval.setSpacing(true);
		labelInerval.addComponent(new Label("every"));
		
		TextField tfEvery = new TextField();
		tfEvery.setWidth("50px");
		tfEvery.setImmediate(true);

		
		labelInerval.addComponent(tfEvery);
		
		ComboBox comboEvery = new ComboBox();
		comboEvery.setNullSelectionAllowed(false);
		comboEvery.setImmediate(true);
		comboEvery.addItem("minutes");
		comboEvery.addItem("hours");
		comboEvery.addItem("days");
		comboEvery.addItem("months");
		comboEvery.setValue("days");

		labelInerval.addComponent(comboEvery);
		
		autoLayout.addComponent(labelInerval,1,2);

		
		return autoLayout;
		
	}
	
	public static IndexedContainer getComboboxData(List<Pipeline> data) {

		IndexedContainer result = new IndexedContainer();

		result.addContainerProperty("name", String.class, "");


		for (Pipeline item : data)
		{	

			Object num = result.addItem();
			
			
			result.getContainerProperty(num, "name").setValue(item.getName());
	
		}

		return result;
	}
	


	
	
	class actionColumnGenerator implements com.vaadin.ui.Table.ColumnGenerator {

		@Override
		public Object generateCell(final Table source, final Object itemId,
				Object columnId) {
			HorizontalLayout layout = new HorizontalLayout();

			CheckBox selectPipe = new CheckBox();
			layout.addComponent(selectPipe);
			// get item
	
			return layout;
		}

	}


	public void resize(float height) {
		float newLogHeight = height - 325;
		if(newLogHeight < 400) {
			newLogHeight = 400;
		}

	}

}


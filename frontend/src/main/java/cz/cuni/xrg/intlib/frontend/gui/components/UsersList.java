package cz.cuni.xrg.intlib.frontend.gui.components;


import java.util.List;
import java.util.Set;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
import cz.cuni.xrg.intlib.commons.app.pipeline.PipelineExecutionStatus;
import cz.cuni.xrg.intlib.commons.app.user.Role;
import cz.cuni.xrg.intlib.commons.app.user.User;
import cz.cuni.xrg.intlib.frontend.auxiliaries.App;

/**
 * GUI for User List  which opens from the Administrator menu. Contains table with
 * users and button for user creation.
 *
 *
 * @author Maria Kukhar
 */

public class UsersList {
	
	 private IntlibPagedTable usersTable;
	 private VerticalLayout usersListLayout;
	 private static String[] visibleCols = new String[]{"id", "user", "role",
	        "total_pipelines", "actions"};
	 private static String[] headers = new String[]{"Id", "User Name", "Role(s)",
	        "Total Pipelines", "Actions"};
	 private IndexedContainer tableData;


	public VerticalLayout buildUsersListLayout(){
		
		
		usersListLayout = new VerticalLayout();
		usersListLayout.setMargin(true);
		usersListLayout.setSpacing(true);
		usersListLayout.setWidth("100%");

		usersListLayout.setImmediate(true);
		
		
		//Layout for buttons Add new user and Clear Filters on the top.
		HorizontalLayout topLine = new HorizontalLayout();
		topLine.setSpacing(true);
		topLine.setWidth(100, Unit.PERCENTAGE);

		Button addUserButton = new Button();
		addUserButton.setCaption("Create new user");
		addUserButton
				.addClickListener(new com.vaadin.ui.Button.ClickListener() {

					private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				
				boolean newUser = true;
				// open usercreation dialog
				UserCreate user = new UserCreate(newUser);
				App.getApp().addWindow(user);
				user.addCloseListener(new CloseListener() {

					private static final long serialVersionUID = 1L;

					@Override
					public void windowClose(CloseEvent e) {
						refreshData();
					}
				});
			}
		});
		topLine.addComponent(addUserButton);
		topLine.setComponentAlignment(addUserButton, Alignment.MIDDLE_RIGHT);

		Button buttonDeleteFilters = new Button();
		buttonDeleteFilters.setCaption("Clear Filters");
		buttonDeleteFilters.setHeight("25px");
		buttonDeleteFilters.setWidth("110px");
		buttonDeleteFilters
				.addClickListener(new com.vaadin.ui.Button.ClickListener() {

					private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				usersTable.resetFilters();
				usersTable.setFilterFieldVisible("actions", false);
			}
		});
		topLine.addComponent(buttonDeleteFilters);
		topLine.setComponentAlignment(buttonDeleteFilters, Alignment.MIDDLE_RIGHT);

		Label topLineFiller = new Label();
		topLine.addComponentAsFirst(topLineFiller);
		topLine.setExpandRatio(topLineFiller, 1.0f);
		usersListLayout.addComponent(topLine);

		
		
        tableData = getTableData(App.getApp().getUsers().getAllUsers());

        //table with pipeline execution records
        usersTable = new IntlibPagedTable();
        usersTable.setSelectable(true);
        usersTable.setContainerDataSource(tableData);
        usersTable.setWidth("100%");
        usersTable.setHeight("100%");
        usersTable.setImmediate(true);
        usersTable.setVisibleColumns((Object[])visibleCols); // Set visible columns
        usersTable.setColumnHeaders(headers);

        //Actions column. Contains actions buttons: Debug data, Show log, Stop.
        usersTable.addGeneratedColumn("actions",
                new actionColumnGenerator());

        usersListLayout.addComponent(usersTable);
        usersListLayout.addComponent(usersTable.createControls());
        usersTable.setPageLength(10);
        usersTable.setFilterDecorator(new filterDecorator());
        usersTable.setFilterBarVisible(true);
        usersTable.setFilterFieldVisible("actions", false);

		

		
		return usersListLayout;
	}
	
	/**
	 * Calls for refresh table {@link #schedulerTable}.
	 */
	private void refreshData() {
		int page = usersTable.getCurrentPage();
		tableData = getTableData(App.getApp().getUsers().getAllUsers());
		usersTable.setContainerDataSource(tableData);
		usersTable.setCurrentPage(page);
		usersTable.setVisibleColumns((Object[])visibleCols);
		usersTable.setFilterFieldVisible("actions", false);

	}
	
    /**
     * Container with data for {@link #usersTable}
     *
     * @param data. List of users
     * @return result. IndexedContainer with data for users table
     */
	@SuppressWarnings("unchecked")
	public static IndexedContainer getTableData(List<User> data) {

		IndexedContainer result = new IndexedContainer();
		
		for (String p : visibleCols) {
			// setting type of columns
			switch (p) {
				case "id":
					result.addContainerProperty(p, Long.class, null);
					break;
				case "total_pipelines":
					result.addContainerProperty(p, Integer.class, 0);
					break;
				default:
					result.addContainerProperty(p, String.class, "");
					break;
			}

		}


		for (User item : data)  {
			Object num = result.addItem();
			
			Set<Role> roles = item.getRoles();
			String roleStr = new String();
			int i = 0;
			for(Role role:roles){
				i++;
				if (i < roles.size())
					roleStr = roleStr + role.toString() + ", ";
				else
					roleStr = roleStr + role.toString() + ". ";
			}
			
			result.getContainerProperty(num, "id").setValue(item.getId());
			result.getContainerProperty(num, "user").setValue(item.getUsername());
			result.getContainerProperty(num, "role").setValue(roleStr);
			result.getContainerProperty(num, "total_pipelines").setValue(0);


		}

		return result;

	}
	

    
	/**
	 * Generate column "actions" in the table {@link #usersTable}.
	 *
	 * @author Maria Kukhar
	 *
	 */
	class actionColumnGenerator implements CustomTable.ColumnGenerator {

		private static final long serialVersionUID = 1L;


		@Override
		public Object generateCell(final CustomTable source, final Object itemId,
				Object columnId) {

			HorizontalLayout layout = new HorizontalLayout();

		
			//Delete button. Delete user's record from Database.
			Button deleteButton = new Button();
			deleteButton.setCaption("Delete");
/*			deleteButton.addClickListener(new ClickListener() {
				@Override
				public void buttonClick(ClickEvent event) {

					userId = (Long) tableData.getContainerProperty(itemId, "id")
							.getValue();
					Schedule schedule = App.getApp().getSchedules().getSchedule(userId);
					App.getApp().getSchedules().delete(schedule);
					refreshData();
				}
			});*/
			layout.addComponent(deleteButton);
			
			//Delete button. Delete user's record from Database.
			Button changeButton = new Button();
			changeButton.setCaption("Change settings");
			changeButton.addClickListener(new ClickListener() {
				
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(ClickEvent event) {

					
					boolean newUser = false;
					// open usercreation dialog
					UserCreate user = new UserCreate(newUser);
					App.getApp().addWindow(user);
					user.addCloseListener(new CloseListener() {

						private static final long serialVersionUID = 1L;

						@Override
						public void windowClose(CloseEvent e) {
							refreshData();
						}
					});
				
					
				}
			});
			
			layout.addComponent(changeButton);

			return layout;
		}
	}
	
	private class filterDecorator extends IntlibFilterDecorator {

		private static final long serialVersionUID = 1L;

		@Override
        public String getEnumFilterDisplayName(Object propertyId, Object value) {
            if (propertyId == "role") {
                return ((PipelineExecutionStatus) value).name();
            }
            return super.getEnumFilterDisplayName(propertyId, value);
        }
	};
}

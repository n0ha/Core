package cz.cuni.mff.xrg.odcs.frontend.gui.components;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.ItemSorter;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.BaseTheme;

import cz.cuni.mff.xrg.odcs.commons.app.auth.ShareType;
import cz.cuni.mff.xrg.odcs.commons.app.dpu.DPURecord;
import cz.cuni.mff.xrg.odcs.commons.app.dpu.DPUTemplateRecord;
import cz.cuni.mff.xrg.odcs.commons.app.dpu.transfer.ExportService;
import cz.cuni.mff.xrg.odcs.commons.app.facade.DPUFacade;
import cz.cuni.mff.xrg.odcs.frontend.auxiliaries.SimpleTreeFilter;

/**
 * Tree showing available DPUs. Contains filters by accessibility and name. It
 * is possible to make nodes draggable and to add custom click listeners.
 * 
 * @author Bogo
 */
@org.springframework.stereotype.Component
@Scope("prototype")
public class DPUTree extends CustomComponent {

	private static final long serialVersionUID = -8635330869349339394L;

	VerticalLayout layoutTree;

    VerticalLayout mainLayout;

    Tree dpuTree;

    Button btnMinimize;

    Button btnExpand;

    Button buttonCreateDPU;
    
    private Button exportButton;

    GridLayout filterBar;

    boolean isExpandable = false;

    private Filter visibilityFilter;

    @Autowired
    private DPUFacade dpuFacade;

    @Autowired
    private DPUCreate createDPU;
    
    @Autowired
    private ExportService exportService;

    private HorizontalLayout topLine;

    private Window.CloseListener createDPUCloseListener;

    /**
     * Creates new DPUTree.
     */
    public DPUTree() {
    }

    @PostConstruct
    private void initialize() {
        buildMainLayout();

        visibilityFilter = new Filter() {
			private static final long serialVersionUID = -1761725398586311364L;

			@Override
            public boolean passesFilter(Object itemId, Item item) throws UnsupportedOperationException {
                if (itemId.getClass() != DPUTemplateRecord.class) {
                    return true;
                }
                DPUTemplateRecord dpu = (DPUTemplateRecord) itemId;
                if (dpu == null || dpu.getShareType() == null) {
                    return false;
                }
                return ShareType.PRIVATE.equals(dpu.getShareType());
            }

            @Override
            public boolean appliesToProperty(Object propertyId) {
                return true;
            }
        };

        createDPUCloseListener = new Window.CloseListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void windowClose(Window.CloseEvent e) {
                //refresh DPU tree after closing DPU Template creation dialog 
                refresh();
            }
        };

        setCompositionRoot(mainLayout);
    }

    /**
     * Builds layout.
     */
    private void buildMainLayout() {

        mainLayout = new VerticalLayout();

        layoutTree = new VerticalLayout();
        layoutTree.setSpacing(true);
        layoutTree.setImmediate(true);
        layoutTree.setHeight("100%");
        layoutTree.setMargin(true);
        mainLayout.setStyleName("dpuTreeLayout");

        //Expandable part of the component
        topLine = new HorizontalLayout();
        topLine.setWidth(100, Unit.PERCENTAGE);
        Label lblTree = new Label("DPU Templates Tree");
        lblTree.setWidth(160, Unit.PIXELS);
        topLine.addComponent(lblTree);
        btnMinimize = new Button();
        btnMinimize.addClickListener(new Button.ClickListener() {
			private static final long serialVersionUID = -5401352180513254192L;

			@Override
            public void buttonClick(Button.ClickEvent event) {
                setTreeState(false);
            }
        });
        btnMinimize.setStyleName(BaseTheme.BUTTON_LINK);
        btnMinimize.setIcon(new ThemeResource("icons/collapse.svg"));
        btnMinimize.setDescription("Minimize DPU tree");
        topLine.addComponent(btnMinimize);
        topLine.setExpandRatio(btnMinimize, 1.0f);
        btnExpand = new Button();
        btnExpand.addClickListener(new Button.ClickListener() {
			private static final long serialVersionUID = 2099560102850977680L;

			@Override
            public void buttonClick(Button.ClickEvent event) {
                setTreeState(true);
            }
        });
        btnExpand.setStyleName(BaseTheme.BUTTON_LINK);
        btnExpand.setIcon(new ThemeResource("icons/expand.svg"));
        btnExpand.setDescription("Expand DPU tree");
        btnExpand.setVisible(false);
        topLine.addComponent(btnExpand);
        topLine.setExpandRatio(btnExpand, 1.0f);
        topLine.setComponentAlignment(btnExpand, Alignment.TOP_RIGHT);
        topLine.setVisible(isExpandable);
        mainLayout.addComponent(topLine);
        
        buttonCreateDPU = new Button();
        buttonCreateDPU.setCaption("Create DPU template");
        buttonCreateDPU.setHeight("25px");
        buttonCreateDPU.setWidth("150px");
        buttonCreateDPU
                .addClickListener(new Button.ClickListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void buttonClick(Button.ClickEvent event) {
                        //Open the dialog for DPU Template creation
                        if (!UI.getCurrent().getWindows().contains(createDPU)) {
                            createDPU.initClean();
                            UI.getCurrent().addWindow(createDPU);
                            createDPU.removeCloseListener(createDPUCloseListener);
                            createDPU.addCloseListener(createDPUCloseListener);
                        } else {
                            createDPU.bringToFront();
                        }

                    }
                });
        
        exportButton = new Button("Export DPU templates");
        exportButton.setHeight("25px");
        exportButton.setWidth("150px");

        exportButton.addClickListener(new Button.ClickListener() {
			private static final long serialVersionUID = 6941128812967827740L;

			@Override
			public void buttonClick(ClickEvent event) {
				DPUTemplatesExport exportWindow = 
						new DPUTemplatesExport(dpuFacade.getAllTemplates(), exportService);
				UI.getCurrent().addWindow(exportWindow);
			}
		});
        
        buttonCreateDPU.setVisible(isExpandable);
//        exportButton.setVisible(isExpandable);
        mainLayout.addComponent(buttonCreateDPU);
        mainLayout.addComponent(exportButton);
        
        // DPURecord tree filters
        filterBar = new GridLayout(2, 2);
        filterBar.setSpacing(false);

        CheckBox onlyMyDPU = new CheckBox();
        onlyMyDPU.setCaption("Only private DPU templates");
        onlyMyDPU.setStyleName("private");
        onlyMyDPU.addValueChangeListener(new Property.ValueChangeListener() {
			private static final long serialVersionUID = 9200593350155429057L;

			@Override
            public void valueChange(Property.ValueChangeEvent event) {
                boolean onlyMy = (boolean) event.getProperty().getValue();
                Container.Filterable f = (Container.Filterable) dpuTree.getContainerDataSource();
                if (onlyMy) {
                    f.addContainerFilter(visibilityFilter);
                } else {
                    f.removeContainerFilter(visibilityFilter);
                }
            }
        });
        filterBar.addComponent(onlyMyDPU, 0, 0, 1, 0);

        TextField treeFilter = new TextField();
        treeFilter.setImmediate(false);
        treeFilter.setInputPrompt("Type to filter tree");
        treeFilter.addTextChangeListener(new FieldEvents.TextChangeListener() {
			private static final long serialVersionUID = -8569331871410134460L;
			SimpleTreeFilter filter = null;

            @Override
            public void textChange(FieldEvents.TextChangeEvent event) {
                Container.Filterable f = (Container.Filterable) dpuTree
                        .getContainerDataSource();

                // Remove old filter
                if (filter != null) {
                    f.removeContainerFilter(filter);
                }

                // Set new filter
                filter = new SimpleTreeFilter(event.getText(), true, false);
                f.addContainerFilter(filter);

            }
        });

        filterBar.addComponent(treeFilter, 1, 1);
        filterBar.setSizeUndefined();
        layoutTree.addComponent(filterBar);
        layoutTree.setExpandRatio(filterBar, 0.05f);

        // DPURecord tree 
        dpuTree = new Tree();
        dpuTree.setImmediate(true);
        dpuTree.setHeight("100%");
        //	dpuTree.setHeight(600, Unit.PIXELS);
        dpuTree.setStyleName("dpuTree");
        dpuTree.setItemStyleGenerator(new Tree.ItemStyleGenerator() {
			private static final long serialVersionUID = -3033372681114948667L;

			@Override
            public String getStyle(Tree source, Object itemId) {
                DPUTemplateRecord dpu = (DPUTemplateRecord) itemId;
                if (dpu.getShareType() == ShareType.PRIVATE) {
                    return "private";
                } else {
                    return "public";
                }
            }
        });
        ((HierarchicalContainer) dpuTree.getContainerDataSource()).setIncludeParentsWhenFiltering(true);
        ((HierarchicalContainer) dpuTree.getContainerDataSource()).setItemSorter(new ItemSorter() {
			private static final long serialVersionUID = -3394104490891279840L;

			@Override
            public void setSortProperties(Container.Sortable container, Object[] propertyId, boolean[] ascending) {
                //Ignore
            }

            @Override
            public int compare(Object itemId1, Object itemId2) {
                DPUTemplateRecord first = (DPUTemplateRecord) itemId1;
                DPUTemplateRecord second = (DPUTemplateRecord) itemId2;
                if (first.getId() == null && second.getId() == null) {
                    return 0;
                } else {
                    return first.getName().compareTo(second.getName());
                }
            }
        });

        layoutTree.addComponent(dpuTree);
        layoutTree.setComponentAlignment(dpuTree, Alignment.TOP_LEFT);
        layoutTree.setExpandRatio(dpuTree, 0.95f);
        mainLayout.addComponent(layoutTree);
    }

    /**
     * Fill DPU tree with data.
     */
    public void fillTree() {
        Container.Filterable f = (Container.Filterable) dpuTree.getContainerDataSource();
        Collection<Filter> filters = new LinkedList<>(f.getContainerFilters());
        f.removeAllContainerFilters();
        fillTree(dpuTree);
        for (Object itemId : dpuTree.rootItemIds()) {
            dpuTree.expandItemsRecursively(itemId);
        }
        for (Filter filter : filters) {
            f.addContainerFilter(filter);
        }
    }

    /**
     * Adds custom ItemClickListener to the DPUTRee.
     * 
     * @param itemClickListener
     *            {@link ItemClickListener} to add
     *            to DPU tree.
     */
    public void addItemClickListener(
            ItemClickEvent.ItemClickListener itemClickListener) {
        dpuTree.addItemClickListener(itemClickListener);
    }

    /**
     * Reloads the contents of the DPUTree.
     */
    public void refresh() {
        fillTree();
        markAsDirty();
    }

    /**
     * Fills tree with available DPUs.
     * 
     * @param tree
     *            {@link Tree} to fill.
     */
    private void fillTree(Tree tree) {

        tree.removeAllItems();

        DPURecord rootExtractor = new DPUTemplateRecord("Extractors", null);
        tree.addItem(rootExtractor);
        DPURecord rootTransformer = new DPUTemplateRecord("Transformers", null);
        tree.addItem(rootTransformer);
        DPURecord rootLoader = new DPUTemplateRecord("Loaders", null);
        tree.addItem(rootLoader);
        DPURecord rootQuality = new DPUTemplateRecord("Quality", null);
        tree.addItem(rootQuality);


        List<DPUTemplateRecord> dpus = dpuFacade.getAllTemplates();
        for (DPUTemplateRecord dpu : dpus) {
            if (dpu.getType() != null) {
                tree.addItem(dpu);
                DPUTemplateRecord parent = dpu.getParent();
                if (parent != null) {
//					DPUTemplateRecord parent = null;
//					for(DPUTemplateRecord candidate : dpus) {
//						if(candidate.getId() == parentId) {
//							parent = candidate;
//							break;
//						}
//					}
                    tree.setParent(dpu, parent);
                } else {
                    switch (dpu.getType()) {
                        case EXTRACTOR:
                            tree.setParent(dpu, rootExtractor);
                            break;
                        case TRANSFORMER:
                            tree.setParent(dpu, rootTransformer);
                            break;
                        case LOADER:
                            tree.setParent(dpu, rootLoader);
                            break;
                        case QUALITY:
                            tree.setParent(dpu, rootQuality);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown type: " + dpu.getType());
                    }
                }
            }

            for (Object itemId : tree.rootItemIds()) {
                tree.expandItemsRecursively(itemId);
            }
        }

        ((HierarchicalContainer) tree.getContainerDataSource()).sort(null, null);
    }

    private void setTreeState(boolean isStateExpanded) {
        btnMinimize.setVisible(isExpandable && isStateExpanded);
        btnExpand.setVisible(isExpandable && !isStateExpanded);
        buttonCreateDPU.setVisible(isExpandable && isStateExpanded);
        exportButton.setVisible(isExpandable && isStateExpanded);
        layoutTree.setVisible(isStateExpanded);
        mainLayout.setSizeUndefined();
    }

    /**
     * Sets nodes of the tree drag-able.
     * 
     * @param dragable
     *            True if the nodes should be drag-able, false otherwise.
     */
    public void setDragable(boolean dragable) {
        if (dragable) {
            dpuTree.setDragMode(Tree.TreeDragMode.NODE);
        } else {
            dpuTree.setDragMode(Tree.TreeDragMode.NONE);
        }
    }

    /**
     * Set DPU tree expandable.
     * 
     * @param expandable
     */
    public void setExpandable(boolean expandable) {
        this.isExpandable = expandable;
        topLine.setVisible(isExpandable);
        buttonCreateDPU.setVisible(isExpandable);
    }

    @Override
    public Collection<?> getListeners(Class<?> eventType) {
        if (eventType == ItemClickEvent.class) {
            return dpuTree.getListeners(eventType);
        }
        return super.getListeners(eventType);
    }
}

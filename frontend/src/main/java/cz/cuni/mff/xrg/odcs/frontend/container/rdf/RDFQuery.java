package cz.cuni.mff.xrg.odcs.frontend.container.rdf;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Graph;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.lazyquerycontainer.NestingBeanItem;
import org.vaadin.addons.lazyquerycontainer.Query;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.ui.Notification;

import cz.cuni.mff.xrg.odcs.commons.app.dataunit.rdf.ManagableRdfDataUnit;
import cz.cuni.mff.xrg.odcs.rdf.enums.SPARQLQueryType;
import cz.cuni.mff.xrg.odcs.rdf.exceptions.InvalidQueryException;
import cz.cuni.mff.xrg.odcs.rdf.help.RDFTriple;
import cz.cuni.mff.xrg.odcs.rdf.query.utils.QueryPart;
import cz.cuni.mff.xrg.odcs.rdf.query.utils.QueryRestriction;
import eu.unifiedviews.dataunit.DataUnitException;
import eu.unifiedviews.dataunit.rdf.RDFDataUnit;
import eu.unifiedviews.helpers.dataunit.rdfhelper.RDFHelper;

/**
 * Implementation of {@link Query} interface for RDF queries. Just read-only
 * access to data is supported.
 *
 * @author Bogo
 */
public class RDFQuery implements Query {

    private static final Logger LOG = LoggerFactory.getLogger(RDFQuery.class);

    private static final String ALL_STATEMENT_QUERY = "CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}";

    private String baseQuery;

    private int batchSize;

    private RDFQueryDefinition queryDefinition;

    private ArrayList<Item> cachedItems;

    private ManagableRdfDataUnit repository;

    /**
     * Constructor.
     *
     * @param queryDefinition Query definition.
     */
    public RDFQuery(RDFQueryDefinition queryDefinition) {
        this.baseQuery = queryDefinition.getBaseQuery();
        this.batchSize = queryDefinition.getBatchSize();
        this.queryDefinition = queryDefinition;
        this.repository = RepositoryFrontendHelper
                .getRepository(queryDefinition.getInfo(), queryDefinition.getDpu(), queryDefinition.getDataUnit());

    }

    /**
     * Size of the query result.
     *
     * @return Size of the result.
     */
    @Override
    public int size() {

        if (repository == null) {
            throw new RuntimeException("Unable to load RDFDataUnit.");
        }
        try {
            int count;
            String filteredQuery = setWhereCriteria(baseQuery);
            LOG.debug("Size query started...");
            if (isAllStatementQuery(filteredQuery)) {
                count = (int) this.getResultSizeForDataCollection(repository);
            } else {
                count = (int) this.getResultSizeForQuery(repository, filteredQuery);
            }
            LOG.debug("Size query finished!");
            return count;
        } catch (InvalidQueryException ex) {
            Notification.show("Query Validator",
                    "Query is not valid: "
                    + ex.getCause().getMessage(),
                    Notification.Type.ERROR_MESSAGE);
            LOG.debug("Size query exception", ex);
        } finally {
            repository.release();
        }
        return 0;
    }

    private boolean isAllStatementQuery(String query) {
        return query.equalsIgnoreCase(ALL_STATEMENT_QUERY);
    }

    /**
     * Load batch of items.
     *
     * @param startIndex Starting index of the item list.
     * @param count Count of the items to be retrieved.
     * @return List of items.
     */
    @Override
    public List<Item> loadItems(int startIndex, int count) {
        LOG.debug(String.format("Loading %d items from %d started...", count,
                startIndex));
        if (cachedItems != null) {
            LOG.debug("Using cached items.");
            return cachedItems.subList(startIndex, startIndex + count);
        }

        if (repository == null) {
            LOG.debug("Unable to load RDFDataUnit.");
            throw new RuntimeException("Unable to load RDFDataUnit.");
        }

        String filteredQuery = setWhereCriteria(baseQuery);

        QueryRestriction restriction = new QueryRestriction(filteredQuery);
        restriction.setLimit(batchSize);
        //String query = baseQuery + String.format(" LIMIT %d", batchSize);
        int offset = startIndex / batchSize;
        if (offset > 0) {
            //query += String.format(" OFFSET %d", offset * batchSize);
            restriction.setOffset(offset * batchSize);
        }
        String query = restriction.getRestrictedQuery();
        SPARQLQueryType type;
        Object data = null;
        RepositoryConnection connection = null;
        try {
            type = getQueryType(query);
            Graph graph = null;
            connection = repository.getConnection();
            switch (type) {
                case SELECT:
                    data = RepositoryFrontendHelper.executeSelectQueryAsTuples(connection, query, RDFHelper.getGraphsURISet(repository));
                    break;
                case CONSTRUCT:
                    graph = RepositoryFrontendHelper.executeConstructQuery(connection, query, RDFHelper.getDatasetWithDefaultGraphs(repository));
                    data = getRDFTriplesData(graph);
                    break;
                case DESCRIBE:
                    String resource = query.substring(query.indexOf('<') + 1,
                            query.indexOf('>'));
                    URIImpl uri = new URIImpl(resource);
                    graph = RepositoryFrontendHelper.describeURI(connection, RDFHelper.getGraphsURISet(repository), uri);
                    data = getRDFTriplesData(graph);
                    break;
                default:
                    return null;
            }

            List<Item> items = new ArrayList<>();
            switch (type) {
                case SELECT:
                    TupleQueryResult result = (TupleQueryResult) data;
                    int id = 0;
                    if (result != null) {
                        while (result.hasNext()) {
                            List<String> names = result.getBindingNames();
                            if (names.size() > 0) {
                                Item item = toItem(names, result.next(), ++id);
                                if (item != null) {
                                    items.add(item);
                                }
                            }
                        }
                        result.close();
                    }
                    break;
                case CONSTRUCT:
                    for (RDFTriple triple : (List<RDFTriple>) data) {
                        items.add(toItem(triple));
                    }
                    break;
                case DESCRIBE:
                    cachedItems = new ArrayList<>();
                    for (RDFTriple triple : (List<RDFTriple>) data) {
                        cachedItems.add(toItem(triple));
                    }
                    LOG.debug(
                            "Loading of items finished, whole result preloaded and cached.");
                    return cachedItems.subList(startIndex, startIndex + count);
            }

            LOG.debug("Loading of items finished.");
            return items;
        } catch (InvalidQueryException ex) {
            Notification.show("Query Validator",
                    "Query is not valid: "
                    + ex.getCause().getMessage(),
                    Notification.Type.ERROR_MESSAGE);
        } catch (QueryEvaluationException ex) {
            Notification.show("Query Evaluation",
                    "Error in query evaluation: "
                    + ex.getCause().getMessage(),
                    Notification.Type.ERROR_MESSAGE);
        } catch (DataUnitException e) {
            Notification.show("DataUnit problem",
                    "Error: " + e.getCause().getMessage(),
                    Notification.Type.ERROR_MESSAGE);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (RepositoryException ex) {
                    Notification.show("Connection problem",
                            "Could close connection to frontend repository: "
                            + ex.getCause().getMessage(),
                            Notification.Type.ERROR_MESSAGE);
                }
            }
            // close reporistory
            repository.release();
        }
        return null;
    }

    private String setWhereCriteria(String query) {
        List<Filter> filters = queryDefinition.getFilters();
        return RepositoryFrontendHelper.filterRDFQuery(query, filters);
    }

    private Item toItem(RDFTriple triple) {
        return new NestingBeanItem(triple, queryDefinition.getMaxNestedPropertyDepth(), queryDefinition
                .getPropertyIds());
    }

    private Item toItem(List<String> headers, BindingSet binding, int id) {
        return new BindingSetItem(headers, binding, id);
    }

    /**
     * Override from {@link Query}. Not applicable for our use.
     *
     * @param list
     * @param list1
     * @param list2
     */
    @Override
    public void saveItems(List<Item> list, List<Item> list1, List<Item> list2) {
        throw new UnsupportedOperationException(
                "RDFLazyQueryContainer is read-only.");
    }

    /**
     * Override from {@link Query}. Not applicable for our use.
     *
     * @return not supported
     */
    @Override
    public boolean deleteAllItems() {
        throw new UnsupportedOperationException(
                "RDFLazyQueryContainer is read-only.");
    }

    /**
     * Override from {@link Query}. Not applicable for our use.
     *
     * @return not supported
     */
    @Override
    public Item constructItem() {
        throw new UnsupportedOperationException(
                "RDFLazyQueryContainer is read-only.");
    }

    private List<RDFTriple> getRDFTriplesData(Graph graph) {

        List<RDFTriple> triples = new ArrayList<>();

        int count = 0;

        for (Statement next : graph) {
            String subject = next.getSubject().stringValue();
            String predicate = next.getPredicate().stringValue();
            String object = next.getObject().stringValue();

            count++;

            RDFTriple triple = new RDFTriple(count, subject, predicate, object);
            triples.add(triple);
        }

        return triples;
    }

    private SPARQLQueryType getQueryType(String query) throws InvalidQueryException {
        if (query.length() < 9) {
            //Due to expected exception format in catch block
            throw new InvalidQueryException(new InvalidQueryException(
                    "Invalid query: " + query));
        }
        QueryPart queryPart = new QueryPart(query);
        SPARQLQueryType type = queryPart.getSPARQLQueryType();

        return type;
    }

    private long getSizeForConstruct(RDFDataUnit rdfDataUnit, String constructQuery) throws InvalidQueryException {
        long size = 0;

        RepositoryConnection connection = null;

        try {
            connection = rdfDataUnit.getConnection();

            GraphQuery graphQuery = connection.prepareGraphQuery(
                    QueryLanguage.SPARQL,
                    constructQuery);
            graphQuery.setDataset(RDFHelper.getDatasetWithDefaultGraphs(rdfDataUnit));
            try {
                GraphQueryResult result = graphQuery.evaluate();

                // its quicker to iterate through result, than init model and ask model the size 
                while (result.hasNext()) {
                    ++size;
                    result.next();
                }
                result.close();

            } catch (QueryEvaluationException ex) {

                throw new InvalidQueryException(
                        "This query is probably not valid. " + ex
                        .getMessage(),
                        ex);
            }

        } catch (MalformedQueryException ex) {
            throw new InvalidQueryException(
                    "This query is probably not valid as construct query. "
                    + ex.getMessage(), ex);
        } catch (RepositoryException ex) {
            LOG.error("Connection to RDF repository failed. {}",
                    ex.getMessage(), ex);
        } catch (DataUnitException ex) {
            LOG.error("DataUnit problem. {}",
                    ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (RepositoryException ex) {
                    LOG.warn(
                            "Failed to close connection to RDF repository while querying. {}",
                            ex.getMessage(), ex);
                }
            }
        }

        return size;
    }

    private long getSizeForSelect(RDFDataUnit rdfDataUnit, QueryPart queryPart) throws InvalidQueryException {

        final String sizeVar = "selectSize";

        final String sizeQuery = String.format(
                "%s SELECT (count(*) AS ?%s) WHERE {%s}", queryPart
                .getQueryPrefixes(), sizeVar,
                queryPart.getQueryWithoutPrefixes());
        RepositoryConnection connection = null;
        try {
            connection = rdfDataUnit.getConnection();

            TupleQuery tupleQuery = connection.prepareTupleQuery(
                    QueryLanguage.SPARQL, sizeQuery);
            tupleQuery.setDataset(RDFHelper.getDatasetWithDefaultGraphs(rdfDataUnit));
            try {
                TupleQueryResult tupleResult = tupleQuery.evaluate();
                if (tupleResult.hasNext()) {
                    String selectSize = tupleResult.next()
                            .getValue(sizeVar).stringValue();
                    long resultSize = Long.parseLong(selectSize);

                    tupleResult.close();
                    return resultSize;
                }
                throw new InvalidQueryException(
                        "Query: " + queryPart.getQuery() + " has no bindings for information about its size");
            } catch (QueryEvaluationException ex) {
                throw new InvalidQueryException(
                        "This query is probably not valid. " + ex
                        .getMessage(),
                        ex);
            }

        } catch (MalformedQueryException ex) {
            throw new InvalidQueryException(
                    "This query is probably not valid. "
                    + ex.getMessage(), ex);
        } catch (RepositoryException ex) {
            LOG.error("Connection to RDF repository failed. {}",
                    ex.getMessage(), ex);
        } catch (DataUnitException ex) {
            LOG.error("DataUnit problem. {}",
                    ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (RepositoryException ex) {
                    LOG.warn(
                            "Failed to close connection to RDF repository while querying. {}",
                            ex.getMessage(), ex);
                }
            }
        }

        return 0;
    }

    /**
     * For Browsing all data in graph return its size {count of rows}.
     */
    private long getResultSizeForDataCollection(RDFDataUnit rdfDataUnit) throws InvalidQueryException {
        final String selectQuery = "SELECT ?x ?y ?z WHERE {?x ?y ?z}";
        return getSizeForSelect(rdfDataUnit, new QueryPart(selectQuery));
    }

    /**
     * For given valid SELECT of CONSTRUCT query return its size {count of rows
     * returns for given query).
     *
     * @param query Valid SELECT/CONTRUCT query for asking.
     * @return size for given valid query as long.
     * @throws InvalidQueryException if query is not valid.
     */
    private long getResultSizeForQuery(RDFDataUnit rdfDataUnit, String query) throws InvalidQueryException {

        long size = 0;

        QueryPart queryPart = new QueryPart(query);
        SPARQLQueryType type = queryPart.getSPARQLQueryType();

        switch (type) {
            case SELECT:
                size = getSizeForSelect(rdfDataUnit, queryPart);
                break;
            case CONSTRUCT:
            case DESCRIBE:
                size = getSizeForConstruct(rdfDataUnit, query);
                break;
            case UNKNOWN:
                throw new InvalidQueryException(
                        "Given query: " + query + "have to be SELECT or CONSTRUCT type.");
        }

        return size;

    }
}

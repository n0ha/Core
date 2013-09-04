package cz.cuni.xrg.intlib.commons.app.rdf.namespace;

import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade for managing persisted entities of {@link NamespacePrefix}.
 * 
 * @author Jan Vojt
 */
public class NamespacePrefixFacade {
	
	private static final Logger LOG = LoggerFactory.getLogger(NamespacePrefixFacade.class);
	
	/**
	 * Entity manager for accessing database with persisted objects
	 */
	@PersistenceContext
	private EntityManager em;
	
	/**
	 * Namespace prefix factory.
	 * 
	 * @param name
	 * @param URI
	 * @return 
	 */
	public NamespacePrefix createPrefix(String name, String URI) {
		return new NamespacePrefix(name, URI);
	}
	
	/**
	 * Fetch all RDF namespace prefixes defined in application.
	 * 
	 * @return 
	 */
	public List<NamespacePrefix> getAllPrefixes() {

		@SuppressWarnings("unchecked")
		List<NamespacePrefix> resultList = Collections.checkedList(
				em.createQuery("SELECT e FROM NamespacePrefix e").getResultList(),
				NamespacePrefix.class
		);

		return resultList;
	}
	
	/**
	 * Fetch a single namespace RDF prefix given by ID.
	 * 
	 * @param id
	 * @return 
	 */
	public NamespacePrefix getPrefix(long id) {
		return em.find(NamespacePrefix.class, id);
	}
	
	/**
	 * Find prefix with given name in persistent storage.
	 * 
	 * @param name
	 * @return 
	 */
	public NamespacePrefix getPrefixByName(String name) {
		Query q = em.createQuery("SELECT e FROM NamespacePrefix e WHERE e.name = :name")
				.setParameter("name", name);
		
		NamespacePrefix prefix = null;
		try {
			prefix = (NamespacePrefix) q.getSingleResult();
		} catch (NoResultException ex) {
			LOG.info(String.format("RDF namespace prefix with username %s was not found.", name));
		}
		
		return prefix;
	}
	
	/**
	 * Persists given RDF namespace prefix. If it is persisted already, all changes
	 * performed on object are updated.
	 * 
	 * @param prefix namespace prefix to persist or update
	 */
	@Transactional
	public void save(NamespacePrefix prefix) {
		if (prefix.getId() == null) {
			em.persist(prefix);
		} else {
			em.merge(prefix);
		}
	}
	
	/**
	 * Deletes RDF namespace prefix from persistent storage.
	 * 
	 * @param prefix 
	 */
	@Transactional
	public void delete(NamespacePrefix prefix) {
		NamespacePrefix px;
		if (!em.contains(prefix)) {
			px = getPrefix(prefix.getId());
		} else {
			px = prefix;
		}
		if (px == null) {
			LOG.warn(String.format(
				"Namespace prefix with name %s could not be deleted, because it was not found.",
				prefix.getName()
			));
		} else {
			em.remove(px);
		}
	}
	
}

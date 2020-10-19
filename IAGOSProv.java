package fr;

import java.util.Arrays;
import java.util.HashMap;

import javax.xml.datatype.DatatypeConfigurationException;

import org.openprovenance.prov.interop.Formats;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Activity;
import org.openprovenance.prov.model.Agent;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.Entity;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.WasAssociatedWith;
import org.openprovenance.prov.model.WasAttributedTo;
import org.openprovenance.prov.model.WasGeneratedBy;

import fr.iagos.mongo.domain.util.DataOperation;

public class IAGOSProv {

	public static final String OBO_NAMESPACE = "http://purl.obolibrary.org/obo/";
	public static final String OBO_PREFIX = "obo";
	public static final String OBO_TERM_FILE = "SIO_000396";
	public static final String OBO_TERM_QAQC = "NCIT_C15311";

	public static final String IAGOS_NAMESPACE = "http://iagos.org/";
	public static final String IAGOS_PREFIX = "iagos";
	public static final String PIDINST_NAMESPACE = "http://pidinst.org/";
	public static final String PIDINST_PREFIX = "pidinst";
	public static final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String RDFS_PREFIX = "rdfs";

	public static final String XSD_PREFIX = "xsd";
	public static final String XSD_TYPE_STRING = "string";
	public static final String PROV_PREFIX = "prov";
	public static final String PROV_TYPE = "type";
	public static final String PROV_LABEL = "label";
	public static final String PROV_TYPE_COLLECTION = "Collection";
	public static final String PROV_TYPE_SOFTWARE = "SoftwareAgent";
	public static final String PROV_TYPE_PERSON = "Person";

	private final ProvFactory pFactory;
	private final Namespace ns;
	private final String EXAMPLE_FLIGHT_NAME = "2014020313095402";

	public IAGOSProv(ProvFactory pFactory) {
		this.pFactory = pFactory;
		ns = new Namespace();
		ns.addKnownNamespaces();
		ns.register(OBO_PREFIX, OBO_NAMESPACE);
		ns.register(IAGOS_PREFIX, IAGOS_NAMESPACE);
		ns.register(PIDINST_PREFIX, PIDINST_NAMESPACE);
		ns.register(RDFS_PREFIX, RDFS_NAMESPACE);
	}

	public QualifiedName qn(String prefix, String name) {
		return ns.qualifiedName(prefix, name, pFactory);
	}

	public QualifiedName qnXsdString() {
		return ns.qualifiedName(XSD_PREFIX, XSD_TYPE_STRING, pFactory);
	}

	public Document makeDocument() throws DatatypeConfigurationException {
		HashMap<String, Entity> entities = new HashMap<>();
		HashMap<String, Agent> agents = new HashMap<>();
		HashMap<String, Activity> activities = new HashMap<>();
		HashMap<String, WasGeneratedBy> wgbs = new HashMap<>();
		HashMap<String, WasAttributedTo> wats = new HashMap<>();
		HashMap<String, WasAssociatedWith> waws = new HashMap<>();
		for (DataOperation operation : iagosOperations.keySet()) {
			// Generate entity dataset.
			Entity entity = pFactory.newEntity(
					qn(IAGOS_PREFIX, "data/" + generateUuid(EXAMPLE_FLIGHT_NAME + "l" + operation.getLevel())),
					Arrays.asList(
							pFactory.newAttribute(qn(PROV_PREFIX, PROV_TYPE), qn(OBO_PREFIX, OBO_TERM_FILE),
									qnXsdString()),
							pFactory.newAttribute(qn(PROV_PREFIX, PROV_LABEL),
									getFileName(EXAMPLE_FLIGHT_NAME, operation.getLevel()), qnXsdString())));
			entities.put(operation.getLevel(), entity);
			// Generate agent.
			Agent agent = pFactory
					.newAgent(
							qn(IAGOS_PREFIX,
									"software/" + generateUuid(
											operation.getSoftwareName() + operation.getSoftwareVersion())),
							Arrays.asList(
									pFactory.newAttribute(qn(PROV_PREFIX, PROV_LABEL), operation.getSoftwareName()),
									qnXsdString()),
							pFactory.newAttribute(qn(PROV_PREFIX, PROV_TYPE), qn(PROV_PREFIX, PROV_TYPE_SOFTWARE),
									qnXsdString()));
			agents.put(operation.getLevel(), agent);
			// Generate activity.
			Activity activity = pFactory.newActivity(
					qn(IAGOS_PREFIX, "activity/" + generateUuid(operation.getName() + "activity")), operation.getDate(),
					operation.getDate(),
					Arrays.asList(
							pFactory.newAttribute(qn(PROV_PREFIX, PROV_LABEL), operation.getName(), qnXsdString()),
							pFactory.newAttribute(qn(PROV_PREFIX, PROV_TYPE), qn(OBO_PREFIX, OBO_TERM_QAQC),
									qnXsdString())));
			activities.put(operation.getLevel(), activity);
			WasGeneratedBy wgb = pFactory.newWasGeneratedBy(null, entity.getId(), activity.getId());
			WasAttributedTo wat = pFactory.newWasAttributedTo(null, entity.getId(), activity.getId());
			WasAssociatedWith waw = pFactory.newWasAssociatedWith(null, activity.getId(), agent.getId());
			wgbs.put(operation.getLevel(), wgb);
			wats.put(operation.getLevel(), wat);
			waws.put(operation.getLevel(), waw);
		}

		Document document = pFactory.newDocument();
		document.getStatementOrBundle().addAll(entities.values());
		document.getStatementOrBundle().addAll(agents.values());
		document.getStatementOrBundle().addAll(activities.values());
		document.getStatementOrBundle().addAll(wgbs.values());
		document.getStatementOrBundle().addAll(wats.values());
		document.getStatementOrBundle().addAll(waws.values());
		document.getStatementOrBundle()
				.addAll(Arrays.asList(pFactory.newWasDerivedFrom(entities.get("0").getId(), entities.get("0A").getId()),
						pFactory.newWasDerivedFrom(entities.get("1").getId(), entities.get("0").getId()),
						pFactory.newWasDerivedFrom(entities.get("2").getId(), entities.get("1").getId())));

		document.setNamespace(ns);
		return document;
	}

	public void doConversions(Document document, String file) {
		InteropFramework intF = new InteropFramework();
		intF.writeDocument(file, document);
		intF.writeDocument(System.out, Formats.ProvFormat.TURTLE, document);
	}

	public static void main(String[] args) throws DatatypeConfigurationException {
		String outPath = "XXX/iagos.ttl";
		IAGOSProv little = new IAGOSProv(InteropFramework.getDefaultFactory());
		Document document = little.makeDocument();
		little.doConversions(document, outPath);

	}
}

package refnet;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import io.FileWriters;
import io.Logger;
import util.Consolidator;
import util.NameGenerator;

/**
 * This class hold a network generated from the netdb database and formats
 * attributes before addition to RefLinks.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @version 1.0
 */
public class RefNetwork {
	private Logger logger;
	private HashMap<String, RefLink> refLinks;
	private HashMap<String, RefNode> _refNodes;
	// private HashSet<RefNode> refNodes;
	public WKTReader wktReader;
	public GeometryFactory geometryFactory;
	private NameGenerator nmg;
	public static final double tolerance = 0.0000000001d;
	public static final double toleranceMax = 0.1000000000d;
	public static final int SRID = 3006;

	/**
	 * Default constructor. Creates a RefNetwork, i.e. a 'true' network with
	 * nodes and edges given the information in the ResultSet.
	 * 
	 * Assumes SRID 3006.
	 * 
	 * Will create a Logger to write info, errors, exceptions, discards etc.
	 * 
	 * @param refLinkParts
	 *            ResultSet with columns (REFLINK_OID, MEASURE_FROM, MEASURE_TO,
	 *            REFNODE_OID_FROM, REFNODE_OID_TO, GEOM [wkt-formatted
	 *            LineString]).
	 * @throws SQLException
	 *             if any get... from ResultSet went wrong or if the set is
	 *             empty.
	 * @throws ParseException
	 *             if the GEOM-string was refused.
	 * @throws ClassCastException
	 *             if GEOM-string was not refused but could not be cast to a
	 *             com.vividsolutions.jts.geom.LineString.
	 * @throws IOException
	 *             if a Logger couldn't be created.
	 */
	public RefNetwork(ResultSet refLinkParts, String path, String fileName)
			throws SQLException, ParseException, ClassCastException, IOException {

		this.logger = new Logger(path, fileName);
		System.out.println("Info will be written to log: " + path + fileName);

		PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
		this.geometryFactory = new GeometryFactory(pm, RefNetwork.SRID);
		this.wktReader = new WKTReader(this.geometryFactory);

		this.refLinks = new HashMap<String, RefLink>();
		this._refNodes = new HashMap<String, RefNode>();
		// this.refNodes = new HashSet<RefNode>();

		this.nmg = new NameGenerator();

		String nodeFromOid, nodeToOid;

		String refLinkOid;
		LineString refLinkPartGeometry;
		double refLinkPartMeasureFrom;
		double refLinkPartMeasureTo;
		RefNode reflinkPartRefNodeFrom;
		RefNode reflinkPartRefNodeTo;

		while (refLinkParts.next()) {
			// Get value of each column in result-row.
			refLinkOid = refLinkParts.getString("REFLINK_OID");
			refLinkPartGeometry = (LineString) wktReader.read(refLinkParts.getString("GEOM"));
			refLinkPartMeasureFrom = refLinkParts.getDouble("MEASURE_FROM");
			refLinkPartMeasureTo = refLinkParts.getDouble("MEASURE_TO");

			/*
			 * tempNode = refLinkParts.getString("REFNODE_OID_FROM");
			 * reflinkPartRefNodeFrom = new RefNode(tempNode,
			 * refLinkPartGeometry.getStartPoint(), false);
			 * 
			 * tempNode = refLinkParts.getString("REFNODE_OID_TO");
			 * reflinkPartRefNodeTo = new RefNode(tempNode,
			 * refLinkPartGeometry.getEndPoint(), false);
			 */

			// Create part for storage with link
			try {
				nodeFromOid = refLinkParts.getString("REFNODE_OID_FROM");
				nodeToOid = refLinkParts.getString("REFNODE_OID_TO");

				if (this._refNodes.containsKey(nodeFromOid)) {
					reflinkPartRefNodeFrom = this._refNodes.get(nodeFromOid);
				} else {
					reflinkPartRefNodeFrom = new RefNode(nodeFromOid, refLinkPartGeometry.getStartPoint(), false);
				}

				if (this._refNodes.containsKey(nodeToOid)) {
					reflinkPartRefNodeTo = this._refNodes.get(nodeToOid);
				} else {
					reflinkPartRefNodeTo = new RefNode(nodeToOid, refLinkPartGeometry.getEndPoint(), false);
				}

				RefLinkPart newRefLinkPart = new RefLinkPart(refLinkOid, refLinkPartGeometry, refLinkPartMeasureFrom,
						refLinkPartMeasureTo, reflinkPartRefNodeFrom.getOid(), reflinkPartRefNodeTo.getOid());

				if (this.refLinks.containsKey(refLinkOid)) {
					if (!this.refLinks.get(refLinkOid).addRefLinkPart(newRefLinkPart, this.geometryFactory)) {
						// TODO: Should keep the integrity of the network.
						this.logger.log(new String[] { "Warning: The latest RefLinkPart with REFLINK_OID " + refLinkOid
								+ " AND MEASURE_FROM " + refLinkPartMeasureFrom + "AND MEASURE TO "
								+ refLinkPartMeasureTo
								+ " was illegal as it occupied space already occupied by another RefLinkPart." });
					}
				} else {
					RefLink newRefLink = new RefLink(refLinkOid, newRefLinkPart);
					this.refLinks.put(refLinkOid, newRefLink);
				}

				this._refNodes.put(nodeFromOid, reflinkPartRefNodeFrom);
				this._refNodes.put(nodeToOid, reflinkPartRefNodeTo);

				/*
				 * if (!(this.refNodes.contains(reflinkPartRefNodeTo))) {
				 * this.refNodes.add(reflinkPartRefNodeTo); }
				 * 
				 * if (!(this.refNodes.contains(reflinkPartRefNodeFrom))) {
				 * this.refNodes.add(reflinkPartRefNodeFrom); }
				 */
			} catch (IllegalArgumentException iae) {
				this.logger.log(new String[] { "RefNetwork: Could not create RefLinkPart (Illegal Argument:",
						iae.getMessage() });
			}
		}
	}

	/**
	 * Returns the a list of all OID of all RefLinks in the network.
	 * 
	 * @return an arraylist of all unique OID Strings without any specific
	 *         order.
	 */
	public ArrayList<String> getRefLinks() {
		return new ArrayList<String>(this.refLinks.keySet());
	}

	/**
	 * Default way of adding a list of attributes to the network. NOTE: The
	 * method assumes that each entry in attributes have been added to the list
	 * through util.Consolidate.
	 */
	public void addAttribute(ArrayList<Attribute> attributes, String refLinkOid) {
		if ((!(attributes.isEmpty())) && (this.refLinks.containsKey(attributes.get(0).getOid()))) {

			RefLink rl = this.refLinks.get(attributes.get(0).getOid());
			HashSet<RefNode> nodesToAdd = new HashSet<RefNode>();

			for (int i = 0; i < attributes.size(); i++) {
				try {
					/*
					 * this.refNodes.addAll(rl.addAttributeByGeom(attributes.get
					 * (i), this.geometryFactory, this.nmg,
					 * RefNetwork.tolerance, true, this.logger));
					 */
					nodesToAdd = rl.addAttributeByGeom(attributes.get(i), this.geometryFactory, this.nmg,
							RefNetwork.tolerance, true, this.logger);

					for (RefNode nodeToAdd : nodesToAdd) {
						if (!(this._refNodes.containsKey(nodeToAdd.getOid()))) {
							this._refNodes.put(nodeToAdd.getOid(), nodeToAdd);
						}
					}
				} catch (IllegalArgumentException e1) {
					/*
					 * Could be java.lang.IllegalArgumentException: Invalid
					 * number of points in LineString (found 1 - must be 0 or >=
					 * 2) from GeometryOps: lgb.endLine() failed. This has
					 * happened for e.g. REFLINK_OID 16499:2200
					 */
					this.logger.log(new String[] {
							"RefNetwork could not add attribute with tolerance " + RefNetwork.tolerance + " for "
									+ rl.getOid() + ":",
							"         Attribute: " + attributes.get(i).toCSVStringWithoutAttributes(),
							"         will try with increased tolerance..." });

					double sl = RefNetwork.tolerance * 10.0000000000d;
					while (true) {
						try {
							/*
							 * this.refNodes.addAll(rl.addAttributeByGeom(
							 * attributes.get(i), this.geometryFactory,
							 * this.nmg, sl, false, this.logger));
							 */
							nodesToAdd = rl.addAttributeByGeom(attributes.get(i), this.geometryFactory, this.nmg,
									RefNetwork.tolerance, true, this.logger);

							for (RefNode nodeToAdd : nodesToAdd) {
								if (!(this._refNodes.containsKey(nodeToAdd.getOid()))) {
									this._refNodes.put(nodeToAdd.getOid(), nodeToAdd);
								}
							}

							this.logger.log(new String[] { "         succeded at a tolerance of " + sl + " m." });
							break;
						} catch (IllegalArgumentException e2) {
							sl = sl * 1.1000000000d;
							/*
							 * In SWEREF 99 TM, this should be 0.1 m. (And at
							 * the most, 216 iterations)..
							 */
							if (sl > RefNetwork.toleranceMax) {
								this.logger.log(new String[] { "         FAILED to add the attribute." });
								break;
							}
						}
					}
				}
				this.refLinks.put(rl.getOid(), rl);
			}
		}
	}

	/**
	 * Default way of adding returned query of attributes to the network.
	 * Consolidates the attributes by OID, attribute values and geometry before
	 * calling the actual method that adds the attributes to the network.
	 * 
	 * OBSERVE: If the Consolidation method is to function properly, the process
	 * demands that the result set got: SORT BY REFLINK_OID ASC, MEASURE_FROM
	 * ASC.
	 * 
	 * @throws SQLException
	 *             if a database access error occurs or this method is called on
	 *             a closed result set
	 */
	public void addAttribute(ResultSet attr) throws SQLException {

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		Attribute attribute = null;
		Attribute attributeNoGeom = null;
		boolean hasAttributeNoGeom = false;
		String prevOid = null;
		String currOid = null;
		String refLinkOid, attributeGeomStr;
		LineString attributeGeom;
		double attributeFromMeasure, attributeToMeasure;
		Double vel;
		Integer velDir, lanes, classification, unallowedDriveDir;

		while (attr.next()) {
			currOid = attr.getString("REFLINK_OID");

			// 1. Check to see if data is useful.
			if (this.refLinks.containsKey(currOid)) {
				attributeGeomStr = attr.getString("GEOM");
				// 2. Get data
				refLinkOid = attr.getString("REFLINK_OID");
				attributeFromMeasure = attr.getDouble("MEASURE_FROM");
				attributeToMeasure = attr.getDouble("MEASURE_TO");

				// 3. Find the correct attribute field.
				try {
					vel = attr.getDouble("speed");
				} catch (SQLException se) {
					vel = null;
				}

				try {
					velDir = attr.getInt("speed_direction");
				} catch (SQLException se) {
					velDir = null;
				}

				try {
					lanes = attr.getInt("lanes");
				} catch (SQLException se) {
					lanes = null;
				}

				try {
					classification = attr.getInt("functional_road_class");
				} catch (SQLException se) {
					classification = null;
				}

				try {
					unallowedDriveDir = attr.getInt("forbidden_direction");
				} catch (SQLException se) {
					unallowedDriveDir = null;
				}

				// 4. If the attribute has geometry, it is business as usual.
				if ((attributeGeomStr != null) && (!attributeGeomStr.equals("POINT EMPTY"))) {
					try {
						// 5. Create new attribute
						attributeGeom = (LineString) wktReader.read(attributeGeomStr);

						attribute = new Attribute(refLinkOid, attributeGeom, attributeFromMeasure, attributeToMeasure,
								vel, velDir, lanes, classification, unallowedDriveDir);

						if ((prevOid != null) && (currOid.equals(prevOid))) {
							/*
							 * 6. If this is not the first attribute in the
							 * resultset and if the OID equals the previous OID,
							 * try to consolidate the attributes (one common
							 * geometry).
							 */
							
							boolean sameAttributes = false;
							for(Attribute otherAttribute : attributes)
							{
								if(otherAttribute.getOid().equals(attribute.getOid()) &&
								   otherAttribute.getGeometry().equalsExact(attribute.getGeometry(),0.1) &&	
								   otherAttribute.getMeasureFrom() == attribute.getMeasureFrom() &&
								   otherAttribute.getMeasureTo() == attribute.getMeasureTo() &&
								   ((otherAttribute.getVelocity() == null && attribute.getVelocity() == null) ||
								    otherAttribute.getVelocity().equals(attribute.getVelocity())) &&
								   ((otherAttribute.getNumberOfLanes() == null && attribute.getNumberOfLanes() == null) ||
								   otherAttribute.getNumberOfLanes().equals(attribute.getNumberOfLanes())) &&
								   ((otherAttribute.getFunctionalRoadClass() == null && attribute.getFunctionalRoadClass() == null) ||
								   otherAttribute.getFunctionalRoadClass().equals(attribute.getFunctionalRoadClass())))
								{
									 if(otherAttribute.getUnallowedDriverDir() != null && attribute.getUnallowedDriverDir() != null &&
									   !otherAttribute.getUnallowedDriverDir().equals(attribute.getFunctionalRoadClass()))
									 {
										 otherAttribute.setUnallowedDriverDir(3);
										 sameAttributes = true;
									 }
									 if(otherAttribute.getVelocityDirection() != null && attribute.getVelocityDirection() != null &&
									    !otherAttribute.getVelocityDirection().equals(attribute.getVelocityDirection()))
									 {
										 otherAttribute.setVelocityDirection(3);
										 sameAttributes = true;
										 
									 }
								}
							}
							
							if(!sameAttributes)
							{
								attributes = Consolidator.Consolidate(attributes, attribute, this.geometryFactory);
							}
						} else if (attributes.isEmpty()) {
							/*
							 * 7. if the list has been emptied, then this is the
							 * first attribute of this OID.
							 */
							attributes.add(attribute);
						} else {
							/*
							 * 8. Otherwise the list isn't empty, but the new
							 * attribute has another RefLink parent than the
							 * items already in the list (different OID). If
							 * this is the case, we first try to salvage
							 * possible attributes without geometries, then we
							 * add the attribute list to the RefLink before
							 * moving on to the new OID.
							 */
							if (hasAttributeNoGeom) {
								attributes = Consolidator.ConsolidateWithoutGeom(attributes, attributeNoGeom,
										this.geometryFactory, this.logger);
								attributeNoGeom = null;
								hasAttributeNoGeom = false;
							}
							this.addAttribute(attributes, currOid);
							attributes.clear();
							attributes.add(attribute);
						}

						prevOid = attr.getString("REFLINK_OID");

					} catch (ParseException pe) {
						this.logger.log(new String[] {
								"Consolidator through RefNetwork: Skipping one attribute (ParseException): "
										+ currOid });
					} catch (IllegalArgumentException iae) {
						this.logger.log(
								new String[] { "RefNetwork: Skipping one attribute (Illegal arguments): " + currOid });
					}
				} else {
					try {
						attributeNoGeom = new Attribute(refLinkOid, null, attributeFromMeasure, attributeToMeasure, vel,
								velDir, lanes, classification, unallowedDriveDir);
						hasAttributeNoGeom = true;
					} catch (IllegalArgumentException iae) {
						this.logger.log(new String[] {
								"RefNetwork: Skipping one attribute (Illegal arguments, also lacked geometry): "
										+ currOid });
					}
				}
			}
		}

		if (hasAttributeNoGeom) {
			attributes = Consolidator.ConsolidateWithoutGeom(attributes, attributeNoGeom, this.geometryFactory,
					this.logger);
			attributeNoGeom = null;
			hasAttributeNoGeom = false;
		}
		this.addAttribute(attributes, currOid);
		attributes.clear();
		attributes.add(attribute);
	}

	/**
	 * Returns the number of RefLinkParts of this network.
	 * 
	 * @return the number of RefLinkParts of this network.
	 */
	public int getNumberOfParts() {
		int n = 0;

		for (RefLink rf : this.refLinks.values()) {
			n = n + rf.getNbParts();
		}

		return n;
	}

	/**
	 * Prints all of the RefLinkParts in the network to the console.
	 * 
	 * @param withAttributes
	 *            determines if the attributes also should be printed.
	 */
	public void print(boolean withAttributes) {
		String heading = "REFLINK_OID;MEASURE_FROM;MEASURE_TO;REFNODE_OID_FROM;REFNODE_OID_TO;GEOM;GEOMETRIC_LENGTH";

		if (withAttributes) {
			heading = heading + ";FUNKTIONELL_VAGKLASS;HASTIGHET;KORFALT;FORBJUDEN_FARDRIKTNING;HASTIGHET_RIKTNING";
		}

		System.out.println(heading);

		for (RefLink rf : this.refLinks.values()) {
			System.out.println(rf.getRefLinkPartsAsCSVStringWithNewRow(withAttributes));
		}
	}

	/**
	 * Writes all nodes in the Network to a ;-separated file.
	 * 
	 * @param path
	 *            the system path where the new file should be saved.
	 * @param fileName
	 *            the name of the new file, should, but does not have to, be
	 *            ended with ".csv"
	 */
	public void writeNodesToFile(String path, String fileName) {
		try {
			FileWriters fw = new FileWriters(path, fileName);

			fw.FileWritersAppendRow("REFNODE_OID;EXTENDED;GEOM");

			for (RefNode rn : this._refNodes.values()) {
				fw.FileWritersAppendRow(rn.toCsvString());
			}
			fw.destroy();

		} catch (IOException e) {
			this.logger.log(new String[] { "Failed to write refnodes to file" });
		}
	}

	/**
	 * Writes all RefLinkParts in the network to a file.
	 * 
	 * @param path
	 *            the system path where the new file should be saved.
	 * @param fileName
	 *            the name of the new file, should, but does not have to, be
	 *            ended with ".csv"
	 * @param withAttributes
	 *            true if the user wishes to write with any attributes.
	 */
	public void writeToFile(String path, String fileName, boolean withAttributes) {
		try {
			FileWriters fw = new FileWriters(path, fileName);
			String heading = "REFLINK_OID;MEASURE_FROM;MEASURE_TO;REFNODE_OID_FROM;REFNODE_OID_TO;GEOM;GEOMETRIC_LENGTH";

			if (withAttributes) {
				heading = heading + ";FUNKTIONELL_VAGKLASS;HASTIGHET;KORFALT;FORBJUDEN_FARDRIKTNING;HASTIGHET_RIKTNING";
			}

			fw.FileWritersAppendRow(heading);

			for (RefLink rf : this.refLinks.values()) {
				int idx = 0;

				while (idx < rf.getNbParts()) {
					fw.FileWritersAppendRow(rf.refLinkPartAsCSVString(idx, withAttributes));

					idx++;
				}
			}

			fw.destroy();

		} catch (IOException e) {
			this.logger.log(new String[] { "Failed to write network to file" });
		}
	}

	/**
	 * Walks through the list of RefLinks and 'cleans' them, i.e. fills out
	 * null-valued fields (attributes) of the RefLinkParts of each RefLink and
	 * then joins consecutive Parts by attributes.
	 */
	public void clean() {
		
		this.align();
		
		RefLink value;
		ArrayList<String> remove;
		for (Map.Entry<String, RefLink> entry : this.refLinks.entrySet()) {
			value = entry.getValue();
			remove = value.clean(this.geometryFactory, this._refNodes);

			this.refLinks.put(entry.getKey(), value);

			for (String rm : remove) {
				if (this._refNodes.containsKey(rm)) {
					if (this._refNodes.get(rm).nbIncoming() == 1 && this._refNodes.get(rm).nbOutgoing() == 1)
						this._refNodes.remove(rm);
				}
			}
		}
		
		this.forbidTurns();
	}

	/**
	 * Aligns all Parts of all RefLinks to be given <from, to> in the driving
	 * direction and then set the incoming and outgoing links of all nodes.
	 */
	private void align() {
		ArrayList<String> from, to;
		RefNode n;

		for (RefLink r : this.refLinks.values()) {
			r.align(this.geometryFactory);
			from = r.getNodesFrom();
			to = r.getNodesTo();

			for (String f : from) {
				if (this._refNodes.containsKey(f)) {
					n = this._refNodes.get(f);
					n.setOutgoing(r.getOid());
					this._refNodes.put(f, n);
				}
			}

			for (String t : to) {
				if (this._refNodes.containsKey(t)) {
					n = this._refNodes.get(t);
					n.setIncoming(r.getOid());
					this._refNodes.put(t, n);
				}
			}
		}
	}

	private void forbidTurns() {
		/*
		String o;
		RefNode n;
		
		for (RefLink r : this.refLinks.values()) {
			o = r.getEndNodeOid();
			if (o != null) {
				n = this._refNodes.get(o);
				
				if (n.nbOutgoing() > 1) {
					// TODO.
				}
			}
		}
		*/
	}

	public void closeLogger() {
		this.logger.destroy();
	}
}
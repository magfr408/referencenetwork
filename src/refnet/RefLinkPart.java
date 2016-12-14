package refnet;

import java.util.HashMap;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * The true edge-representation of the network. Holds start - and end node,
 * extends Part.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @version 1.0
 */
public class RefLinkPart extends Part {
	private String nodeFrom;
	private String nodeTo;

	/**
	 * Constructor when road network is loaded. I.e. no attributes are
	 * available.
	 * 
	 * @param refLinkOid
	 *            should be OID of parent RefLink.
	 * @param geometry
	 *            JTS LineString
	 * @param measureFrom
	 *            is a value equal to 0 but less than 1 that represents the
	 *            relative starting position of this part on the RefLink parent.
	 * @param measureTo
	 *            is a value greater than 0 and less than or equal to 1 that
	 *            represents the relative end position of this part on the
	 *            RefLink parent.
	 * @param nodeFromOid
	 *            start node at this parts origin.
	 * @param nodeToOid
	 *            end node at this parts end.
	 */
	public RefLinkPart(String refLinkOid, LineString geometry, double measureFrom, double measureTo, String nodeFromOid,
			String nodeToOid) throws IllegalArgumentException {

		this(refLinkOid, geometry, measureFrom, measureTo, nodeFromOid, nodeToOid, null, null, null, null, null);
	}

	/**
	 * @param refLinkOid
	 *            the String representation of the parent RefLink.
	 * @param geometry
	 *            is a jts.geom.Geometry LineString object.
	 * @param measureFrom
	 *            is a value equal to 0 but less than 1 that represents the
	 *            relative starting position of this part on the RefLink parent.
	 * @param measureTo
	 *            is a value greater than 0 and less than or equal to 1 that
	 *            represents the relative end position of this part on the
	 *            RefLink parent.
	 * @param nodeFromOid
	 * @param nodeToOid
	 * @param velocity
	 *            posted speed limit in kmph.
	 * @param velocityDirection
	 *            Direction of posted speed limit.
	 * @param nbLanes
	 *            number of lanes on this reflinkpart.
	 * @param functionalRoadClass
	 *            int value between 0 and 9 (& null).
	 * @param unallDriveDir
	 *            int value between 1 and 3 (& null).
	 * @throws IllegalArgumentException
	 *             if any parameter has an illegal value (outside value range).
	 */
	public RefLinkPart(String refLinkOid, LineString geometry, double measureFrom, double measureTo, String nodeFromOid,
			String nodeToOid, Double velocity, Integer velocityDirection, Integer nbLanes, Integer functionalRoadClass,
			Integer unallDriveDir) throws IllegalArgumentException {

		super(refLinkOid, geometry, measureFrom, measureTo, velocity, velocityDirection, nbLanes, functionalRoadClass,
				unallDriveDir);

		this.nodeFrom = nodeFromOid;
		this.nodeTo = nodeToOid;
	}

	public String getNodeFrom() {
		return this.nodeFrom;
	}

	public String getNodeTo() {
		return this.nodeTo;
	}

	/**
	 * Replace the current destination node oid.
	 */
	public void setNodeTo(String oid) {
		this.nodeTo = oid;
	}

	/**
	 * Replace the current origin node oid.
	 */
	public void setNodeFrom(String oid) {
		this.nodeFrom = oid;
	}

	/**
	 * @return all fields of this object as a ;-separated String. The Geometry
	 *         will be in WKT-format.
	 */
	public String toCSVString(boolean withAttribute) {
		if (withAttribute) {
			return (this.getOid() + ";" 
					+ String.valueOf(this.getMeasureFrom()) + ";"
					+ String.valueOf(this.getMeasureTo()) + ";" 
					+ this.nodeFrom + ";" 
					+ this.nodeTo + ";"
					+ this.getGeometryAsStr() + ";" 
					+ String.valueOf(this.getLength()) + ";"
					+ String.valueOf(this.getFunctionalRoadClass()) + ";" 
					+ String.valueOf(this.getVelocity()) + ";"
					+ String.valueOf(this.getNumberOfLanes()) + ";" 
					+ String.valueOf(this.getUnallowedDriverDir()) + ";"
					+ String.valueOf(this.getVelocityDirection()));
		} else {
			return (this.getOid() + ";" 
					+ String.valueOf(this.getMeasureFrom()) + ";"
					+ String.valueOf(this.getMeasureTo()) + ";" 
					+ this.nodeFrom + ";" 
					+ this.nodeTo + ";"
					+ this.getGeometryAsStr() + ";" 
					+ String.valueOf(this.getLength()));
		}

	}

	/**
	 * Replace all null attribute (lanes, vel etc.) fields of this object with
	 * those of other. Some values are only replaced iff the object has the same
	 * direction as other.
	 * @deprecated
	 */
	public void replaceNullAttributesBy(RefLinkPart other) {
		if ((this.getFunctionalRoadClass() == null) && (other.getFunctionalRoadClass() != null)) {
			this.setClassification(other.getFunctionalRoadClass());
		}

		if ((this.getNumberOfLanes() == null) && (other.getNumberOfLanes() != null)) {
			this.setLanes(other.getNumberOfLanes());
		}

		if (this.getNodeFrom().equals(other.getNodeTo()) || (this.getNodeTo().equals(other.getNodeFrom()))) {
			if ((this.getUnallowedDriverDir() == null) && (other.getUnallowedDriverDir() != null)) {
				this.setUnallowedDriverDir(other.getUnallowedDriverDir());
			}

			if ((this.getVelocity() == null) && (other.getVelocity() != null)) {
				this.setVelocity(other.getVelocity());
			}

			if ((this.getVelocityDirection() == null) && (other.getVelocityDirection() != null)) {
				this.setVelocityDirection(other.getVelocityDirection());
			}
		}
	}

	/**
	 * Replace all null attribute fields (lanes, vel etc.) <b>except</b> for the
	 * unallowed driving direction of this object with those of other. Some
	 * values are only replaced iff the object has the same direction as other.
	 * <br>
	 * </br>
	 * <i>Note</i>: This method replaces old version since it did not consider
	 * the connectivity of the RefNetwork. In some instances, a RefLink can be
	 * cut by another RefLinkPart as indicated by below.
	 * 
	 * <pre>
	 * 1 ________  _________ 3
	 * 2 ________/
	 * </pre>
	 * 
	 * In the example, RefLinkPart 3 could have a forbidden driving direction =
	 * null (travel is allowed in both directions) but RefLinkPart 1 could have
	 * a driving direction = 2 (cannot drive against direction). This happens
	 * sometimes when e.g. roundabouts are drawn, No 1 and 3 have the same
	 * RefLinkOID and is therefore considered to be part of the same RefLink,
	 * whilst No 2 have another OID (and leads out of the roundabout). Thus, if
	 * we take the null value of 3 and replace it with the value of 1, then
	 * traffic cannot move from 2 to 3.
	 * 
	 * @param other
	 */
	public void replaceNullAttributesWith(RefLinkPart other) {
		if ((this.getFunctionalRoadClass() == null) && (other.getFunctionalRoadClass() != null)) {
			this.setClassification(other.getFunctionalRoadClass());
		}

		if ((this.getNumberOfLanes() == null) && (other.getNumberOfLanes() != null)) {
			this.setLanes(other.getNumberOfLanes());
		}
		
		if (this.getNodeFrom().equals(other.getNodeTo()) || (this.getNodeTo().equals(other.getNodeFrom()))) {
			if ((this.getVelocity() == null) && (other.getVelocity() != null)) {
				this.setVelocity(other.getVelocity());
			}
	
			if ((this.getVelocityDirection() == null) && (other.getVelocityDirection() != null)) {
				this.setVelocityDirection(other.getVelocityDirection());
			}
		}
	}
	/**
	 * Returns true if all attributes of this equals those of other.
	 */
	public boolean propertyEqual(RefLinkPart other) {
		boolean retval = true;

		if (!this.getOid().equals(other.getOid())) {
			retval = false;
		}
		
		if (this.getFunctionalRoadClass() != null && other.getFunctionalRoadClass() != null) {
			if (!(this.getFunctionalRoadClass().equals(other.getFunctionalRoadClass()))) {
				return false;
			}
		} else if (!(this.getFunctionalRoadClass() == null && other.getFunctionalRoadClass() == null)) {
			return false;
		}
		
		if (this.getNumberOfLanes() != null && other.getNumberOfLanes() != null) {
			if (!(this.getNumberOfLanes().equals(other.getNumberOfLanes()))) {
				return false;
			}
		} else if (!(this.getNumberOfLanes() == null && other.getNumberOfLanes() == null)) {
			return false;
		}
		
		if (this.getUnallowedDriverDir() != null && other.getUnallowedDriverDir() != null) {
			if (!(this.getUnallowedDriverDir().equals(other.getUnallowedDriverDir()))) {
				return false;
			}
		} else if (!(this.getUnallowedDriverDir() == null && other.getUnallowedDriverDir() == null)) {
			return false;
		}
		
		if (this.getVelocity() != null && other.getVelocity() != null) {
			if (!(this.getVelocity().equals(other.getVelocity()))) {
				return false;
			}
		} else if (!(this.getVelocity() == null && other.getVelocity() == null)) {
			return false;
		}
		
		if (this.getVelocityDirection() != null && other.getVelocityDirection() != null) {
			if (!(this.getVelocityDirection().equals(other.getVelocityDirection()))) {
				return false;
			}
		} else if (!(this.getVelocityDirection() == null && other.getVelocityDirection() == null)) {
			return false;
		}
		
		return retval;
	}

	/**
	 * Returns true if all direction based attributes indicates that they are
	 * with the travel direction.
	 */
	public boolean aligned() {
		if (this.getUnallowedDriverDir() != null && this.getUnallowedDriverDir().intValue() == 1) {
			return false;
		} else if (this.getVelocityDirection() != null && this.getVelocityDirection().intValue() == 2) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Align this object so that it is given "in the travel direction". Reverses
	 * geometry, reverses node order and update driving direction as well as
	 * velocity direction
	 * 
	 * @param measureMax
	 *            the largest value of measure from/to of the parent RefLink.
	 *            I.e. MAX(MAX(measure_to, measure_from)).
	 */
	public void align(GeometryFactory gf) {
		if (!this.aligned()) {
			if (this.reverseGeom(gf)) {
				String oldTo = this.getNodeTo();

				this.setNodeTo(this.getNodeFrom());
				this.setNodeFrom(oldTo);

				// After check of "aligned()" we know that vals are not null
				this.setUnallowedDriverDir(2);
				this.setVelocityDirection(1);
				
				//Update measure to/from
				double measureToOld = this.getMeasureTo();
				// FIXME: Precision errors?
				this.setMeasureTo(1.0000000000000d-this.getMeasureFrom());
				this.setMeasureFrom(1.0000000000000d-measureToOld);
			}
		}
	}
}
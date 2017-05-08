package refnet;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import util.GeometryOps;

/**
 * Fundamental building block of the smallest possible representation of a link
 * in the network. Common denominator of RefLinkParts and Attributes.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @version 1.0
 */
public class Part {
	private final String refLinkOid;
	private double measureFrom;
	private double measureTo;
	private LineString geom;
	private double length;
	private Double vel;
	private Integer velDirection;
	private Integer lanes;
	private Integer functionalRoadClass;
	private Integer unallowedDriveDir;

	/**
	 * Default constructor.
	 * 
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
	 * @param velocity
	 *            posted speed limit in kmph.
	 * @param velocityDirection
	 *            Direction of posted speed limit.
	 * @param nbLanes
	 *            number of lanes on this reflinkpart.
	 * @param functionalRoadClass
	 *            int value between 0 and 9 (& null).
	 * @param unallDriveDir
	 *            int value between 1 and 3 (& null). 1 = cannot drive from from
	 *            node (to to node), 2 = cannot drive from to node (from to
	 *            node), 3 = cannot drive in any direction, null = both
	 *            directions open. I.e. forbidded driving direction is (1 =
	 *            with, 2 = against, 3 = both, null = neither).
	 * @throws IllegalArgumentException
	 *             if any parameter has an illegal value (outside value range).
	 */
	public Part(String refLinkOid, LineString geometry, double measureFrom, double measureTo, Double velocity,
			Integer velocityDirection, Integer nbLanes, Integer functionalRoadClass, Integer unallDriveDir) {

		if ((Double.compare(measureFrom, measureTo) == 0) && (Double.compare(measureFrom, measureTo) > 0)) {
			throw new IllegalArgumentException("measureFrom must be less than measureTo.");
		} else if ((velocity != null) && (Double.compare(0.0d, velocity) > 0)) {
			throw new IllegalArgumentException("velocity must be null or >= 0.0D.");
		} else if (velocityDirection != null) {
			if ((velocityDirection.intValue() < 1) || (velocityDirection.intValue() > 3)) {
				throw new IllegalArgumentException("velocityDirection must be null or 1, 2 or 3.");
			}
		} else if (nbLanes != null) {
			if (nbLanes.intValue() < 0) {
				throw new IllegalArgumentException("Number of lanes must be null or positive.");
			}
		} else if (functionalRoadClass != null) {
			if ((functionalRoadClass.intValue() < 0) || (functionalRoadClass.intValue() > 9)) {
				throw new IllegalArgumentException("Functional Road Class must be null or in 0-9.");
			}
		} else if (unallDriveDir != null) {
			if ((unallDriveDir.intValue() < 1) || (unallDriveDir.intValue() > 3)) {
				throw new IllegalArgumentException("unallDriveDir must be null or 1, 2 or 3.");
			}
		}

		this.refLinkOid = refLinkOid;
		this.measureFrom = measureFrom;
		this.measureTo = measureTo;
		this.geom = geometry;
		try {
			this.length = this.geom.getLength();
		} catch (java.lang.NullPointerException npe) {
			this.length = 0.0d;
		}
		this.vel = velocity;
		this.velDirection = velocityDirection;
		this.lanes = nbLanes;
		this.functionalRoadClass = functionalRoadClass;
		this.unallowedDriveDir = unallDriveDir;
	}

	/**
	 * The different kinds of attributes, could be useful...
	 * 
	 * @author Magnus Fransson
	 * @version 1.0
	 */
	public enum AttributeTypes {
		SPEED, LANES, FUNCTIONAL_ROADCLASS, FORBIDDEN_DRIVER_DIRECTION;
	}

	public String getOid() {
		return this.refLinkOid;
	}

	/**
	 * Returns the length of this objects geometry in meters.
	 */
	public double getLength() {
		return this.length;
	}

	/**
	 * Returns the start of this part relative to the parent RefLinks geometry.
	 */
	public double getMeasureFrom() {
		return this.measureFrom;
	}

	/**
	 * Returns the end of this part relative to the parent RefLinks geometry.
	 */
	public double getMeasureTo() {
		return this.measureTo;
	}

	/**
	 * Returns this objects geometry.
	 */
	public LineString getGeometry() {
		return this.geom;
	}

	/**
	 * Returns a WKT-formatted representation of this objects geometry.
	 */
	public String getGeometryAsStr() {
		return this.geom.toText();
	}

	/**
	 * Sets the Geometry and length of this object.
	 * 
	 * @param WKT-String
	 *            representation of the new Geometry on the format 'LINESTRING
	 *            (x1 y1, x2, y2, ..., xn, yn)'
	 * @throws ParseException
	 *             if the conversion from String to Geometry failed.
	 */
	public void setGeometry(String geometryStr) throws ParseException {
		this.geom = (LineString) new WKTReader().read(geometryStr);
		this.length = this.geom.getLength();
	}

	/**
	 * Sets the Geometry and length of this object.
	 * 
	 * @param geometry
	 *            the new Geometry.
	 */
	public void setGeometry(LineString geometry) {
		this.geom = geometry;
		this.length = this.geom.getLength();
	}

	/**
	 * Update the relative end of this object.
	 */
	public void setMeasureTo(double meas) {
		this.measureTo = meas;
	}

	/**
	 * Update the relative start of this object.
	 */
	public void setMeasureFrom(double meas) {
		this.measureFrom = meas;
	}

	public Double getVelocity() {
		return this.vel;
	}

	public Integer getVelocityDirection() {
		return this.velDirection;
	}

	public Integer getNumberOfLanes() {
		return this.lanes;
	}

	public Integer getFunctionalRoadClass() {
		return this.functionalRoadClass;
	}

	public Integer getUnallowedDriverDir() {
		return this.unallowedDriveDir;
	}

	/**
	 * Replace the current posted speed limit.
	 */
	public void setVelocity(double velocity) {
		if (velocity >= 0) {
			this.vel = velocity;
		}
	}

	/**
	 * Replace the current direction of the posted speed limit.
	 */
	public void setVelocityDirection(Integer velocityDirection) {
		if ((velocityDirection >= 1) && (velocityDirection <= 3)) {
			this.velDirection = velocityDirection;
		}
	}

	/**
	 * Replace the current number of lanes.
	 */
	public void setLanes(int nbLanes) {
		if (nbLanes >= 0) {
			this.lanes = nbLanes;
		}
	}

	/**
	 * Replace the current functional road class.
	 */
	public void setClassification(int classific) {
		if ((classific >= 0) && (classific <= 9)) {
			this.functionalRoadClass = classific;
		}
	}

	/**
	 * Replace the current forbidden driving direction.
	 */
	public void setUnallowedDriverDir(int unaDriveDir) {
		if ((unaDriveDir >= 1) && (unaDriveDir <= 3)) {
			this.unallowedDriveDir = unaDriveDir;
		}
	}

	/**
	 * Adds the values of the attribute to the objects corresponding fields. If
	 * the value equals (==) null, it is ignored.
	 */
	public void addAttribute(AttributePart attribute) {
		if (attribute.getNumberOfLanes() != null) {
			this.setLanes(attribute.getNumberOfLanes());
		}

		if (attribute.getVelocity() != null) {
			this.setVelocity(attribute.getVelocity());
		}

		if (attribute.getVelocityDirection() != null) {
			this.setVelocityDirection(attribute.getVelocityDirection());
		}
		if (attribute.getFunctionalRoadClass() != null) {
			this.setClassification(attribute.getFunctionalRoadClass());
		}

		if (attribute.getUnallowedDriverDir() != null) {
			this.setUnallowedDriverDir(attribute.getUnallowedDriverDir());
		}
	}

	/**
	 * Returns true if the geometry of this and other are exactly equal, i.e. of
	 * the same class, has the same points and the points are in the same order.
	 */
	public boolean geomEquals(Part other) {
		return this.geom.equalsExact(other.getGeometry());
	}

	/**
	 * Returns true if both the end and start points of this objects geometry
	 * lies within the geometry of other with a tolerance of 0.000000001.
	 */
	public boolean geomIsWithin(Part other, GeometryFactory gf) {
		boolean start = this.geomStartsWithin(other, gf);
		boolean end = this.geomEndsWithin(other, gf);

		if (start && end) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns true if both the end and start points of this objects geometry
	 * lies completely within the geometry of other with a tolerance of
	 * 0.000000001. Completely within suggests that the start and end points of
	 * this geometry did not equal the equivalent points of other.
	 */
	public boolean geomIsCompletelyWithin(Part other, GeometryFactory gf) {
		if (this.geomIsWithin(other, gf)) {
			if (!(this.geom.getStartPoint().equalsExact(other.getGeometry().getStartPoint()))) {
				if (!(this.geom.getEndPoint().equalsExact(other.getGeometry().getEndPoint()))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns true if the start point of this geometry lies on some segment of
	 * the geometry of other with a tolerance of 0.000000001.
	 */
	public boolean geomStartsWithin(Part other, GeometryFactory gf) {
		return GeometryOps.spansPoint(other.getGeometry(), this.geom.getStartPoint(), gf, 0.0000000001d, true);
	}

	/**
	 * Returns true if the end point of this geometry lies on some segment of
	 * the geometry of other with a tolerance of 0.000000001.
	 */
	public boolean geomEndsWithin(Part other, GeometryFactory gf) {
		return GeometryOps.spansPoint(other.getGeometry(), this.geom.getEndPoint(), gf, 0.0000000001d, true);
	}

	/**
	 * Given that this and other refers to the LineString geometry of the
	 * respective objects then the method returns true if at least one statement
	 * holds:
	 * 
	 * 1) The start point of this is within other but is not equal to the end
	 * point of other. 2) The start point of other is within this but is not
	 * equal to the end point of this. 3) The end point of this is within other
	 * but is not equal to the start point of other. 4) The end point of other
	 * is within this but is not equal to the start point of this. 5) This
	 * equals other. 6) The start point of this equals the start point of other.
	 * 7) The start point of other equals the start point of this. 8) The end
	 * point of this equals the end point of other. 9) The end point of other
	 * equals the end point of this.
	 */
	public boolean hasCommonGeometry(Part other, GeometryFactory gf) {

		if (this.geom.getStartPoint().equalsExact(other.getGeometry().getEndPoint())) {
			return false;
		} else if (other.getGeometry().getStartPoint().equalsExact(this.geom.getEndPoint())) {
			return false;
		} else {
			if (this.geomEquals(other)) {
				return true;
			} else if (this.geomStartsWithin(other, gf)) {
				return true;
			} else if (other.geomStartsWithin(this, gf)) {
				return true;
			} else if (this.geomEndsWithin(other, gf)) {
				return true;
			} else if (other.geomEndsWithin(this, gf)) {
				return true;
			} else if (this.geomStartsWithin(other, gf)) {
				return true;
			} else if (other.geomStartsWithin(this, gf)) {
				return true;
			} else if (other.getGeometry().getStartPoint().equalsExact(this.geom.getStartPoint())) {
				return true;
			} else if (other.getGeometry().getEndPoint().equalsExact(this.geom.getEndPoint())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Reverse the order of the Points of this objects geometry.
	 * Returns true if the reverse was successful.
	 */
	public boolean reverseGeom(GeometryFactory gf) {
		LineString L = GeometryOps.reverse(this.getGeometry(), gf);
		if (L != null) {
			this.setGeometry(L);
			return true;
		} else {
			return false;
		}
	}
}
package refnet;

import java.util.HashMap;
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import refnet.Attribute.AttributeType;
import util.GeometryOps;

/**
 * Fundamental building block of the smallest possible representation of a link
 * in the network. Common denominator of RefLinkParts and Attributes.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @author Modified by Rasmus Ringdahl Linköpings University
 * @version 2.0
 */
public class Part 
{
	private final String refLinkOid;
	private double measureFrom;
	private double measureTo;
	private LineString geom;
	private double length;
	protected HashMap<AttributeType, Attribute> attributes;

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
	 * @param attributes
	 *            HashMap with attributes.
	 * @throws IllegalArgumentException
	 *             if any parameter has an illegal value (outside value range).
	 */
	public Part(String refLinkOid, LineString geometry, double measureFrom, double measureTo,
				HashMap<AttributeType, Attribute> attributes) 
	{

		if ((Double.compare(measureFrom, measureTo) == 0) && (Double.compare(measureFrom, measureTo) > 0)) 
		{
			throw new IllegalArgumentException("measureFrom must be less than measureTo.");
		} 
		
		// Adding attributes to the part.
		this.attributes = new HashMap<AttributeType, Attribute>();
		
		if (attributes != null)
		{
			// Validating all the attributes.
			attributes.values().stream().forEach(t -> t.validate());
			
			// Setting the attributes.
			for ( Entry<AttributeType, Attribute>  entry : attributes.entrySet())
			{
				this.attributes.put(entry.getKey(), new Attribute(entry.getValue().getType(), 
																  entry.getValue().getDirection(),
																  entry.getValue().getValue()));	
			}
		}

		this.refLinkOid = refLinkOid;
		this.measureFrom = measureFrom;
		this.measureTo = measureTo;
		this.geom = geometry;
		
		if (this.geom == null)
		{
			this.length = 0.0d;
		}
		else
		{
			this.length = this.geom.getLength();
		} 
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

	public Attribute getAttribute(Attribute.AttributeType type) 
	{
		return this.attributes.get(type);
	}

	public HashMap<AttributeType, Attribute> getAttributeMap() 
	{
		return this.attributes;
	}

	/**
	 * Adds the values of the attribute to the objects corresponding fields. If
	 * the value equals (==) null, it is ignored.
	 */
	public void addAttribute(AttributePart attributePart) 
	{
		for( Entry<AttributeType, Attribute> entry : attributePart.getAttributeMap().entrySet())
		{
			// Checks for attribute existence.
			if(entry.getValue() != null)
			{
				// Replaces the value if the attribute exists.
				this.attributes.put(entry.getKey(), entry.getValue());
			}
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
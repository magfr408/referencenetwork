package refnet;

import java.util.HashMap;

import com.vividsolutions.jts.geom.LineString;

import refnet.Attribute.AttributeType;

/**
 * Shell to Part which is used to compare the attributes of Parts.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @author Modified by Rasmus Ringdahl Linköpings University
 * @version 2.0
 */
public class AttributePart extends Part 
{

	/**
	 * {@inheritDoc}
	 */
	public AttributePart(String refLinkOid, LineString geometry, double measureFrom, double measureTo, 
						 HashMap<AttributeType, Attribute> attributes) 
														throws IllegalArgumentException 
	{

		super(refLinkOid, geometry, measureFrom, measureTo, attributes);
	}

	/**
	 * @return all the fields of this object as a ;-separated String. The
	 *         Geometry will be in WKT-format.
	 */
	public String toCSVStringWithoutAttributes() 
	{
		return (this.getOid() + ";" 
				+ String.valueOf(this.getMeasureFrom()) + ";" 
				+ String.valueOf(this.getMeasureTo()) + ";" 
				+ this.getGeometryAsStr());
	}

	/**
	 * Returns true if all attributes of this equals those of other.
	 */
	public boolean propertyEqual(AttributePart other) 
	{
		boolean retval = true;
		
		// Checking if the other AttributePart has the same OID.
		if (!this.getOid().equals(other.getOid())) {
			retval = false;
		}

		// Checking if the other AttributePart has the same attributes.
		if (!this.attributes.equals(other.attributes))
		{
			return false;
		}
		

		return retval;
	}
}

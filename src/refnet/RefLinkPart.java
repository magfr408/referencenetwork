package refnet;

import java.util.HashMap;
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import refnet.Attribute.AttributeType;
import refnet.Attribute.DirectionCategories;

/**
 * The true edge-representation of the network. Holds start - and end node,
 * extends Part.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @author Modified by Rasmus Ringdahl Linköpings University
 * @version 2.0
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
			String nodeToOid) throws IllegalArgumentException 
	{

		this(refLinkOid, geometry, measureFrom, measureTo, nodeFromOid, nodeToOid, null);
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
	 * @param attributes
	 *            HashMap with attributes.
	 * @throws IllegalArgumentException
	 *             if any parameter has an illegal value (outside value range).
	 */
	public RefLinkPart(String refLinkOid, LineString geometry, double measureFrom, double measureTo, String nodeFromOid,
			String nodeToOid, HashMap<AttributeType, Attribute> attributes) 
					throws IllegalArgumentException 
	{

		super(refLinkOid, geometry, measureFrom, measureTo, attributes);

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
	public String toCSVString(boolean withAttribute, AttributeType[] usedAttributes) 
	{
		// Creating a string builder.
		StringBuilder strBuilder = new StringBuilder();
		
		// Appending all mandatory columns.
		strBuilder.append(this.getOid() + ";" );
		strBuilder.append(String.valueOf(this.getMeasureFrom()) + ";");
		strBuilder.append(String.valueOf(this.getMeasureTo()) + ";");
		strBuilder.append(this.nodeFrom + ";");
		strBuilder.append(this.nodeTo + ";");
		strBuilder.append(this.getGeometryAsStr() + ";");
		strBuilder.append(String.valueOf(this.getLength()));
		
		// Appending all attribute columns.
		if (withAttribute && usedAttributes.length > 0) 
		{
			strBuilder.append(";");
			for (int i = 0 ; i < usedAttributes.length ; i++)
			{
				strBuilder.append(String.valueOf(this.getAttribute(usedAttributes[i]).getValue()));
				
				if (i < usedAttributes.length -1)
				{
					strBuilder.append(";");
				}
			}
		}
		return strBuilder.toString();
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
	public void replaceNullAttributesWith(RefLinkPart other) 
	{
		for (Entry<AttributeType, Attribute> entry : other.attributes.entrySet())
		{
			if (this.attributes.get(entry.getKey()) == null)
			{
				// Ignoring Forbidden driving direction.
				if (entry.getKey().equals(AttributeType.FORBIDDEN_DRIVER_DIRECTION))
				{
					continue;
				}
				// Adding attributes that has a specified direction.
				else if (!entry.getValue().getDirection().equals(DirectionCategories.NOT_SPECIFIED))
				{
					if(this.getNodeFrom().equals(other.getNodeTo()) || (this.getNodeTo().equals(other.getNodeFrom())))
					{
						this.attributes.put(entry.getKey(), entry.getValue());
					}
				}
				// Adding attributes that has no specified direction.
				else
				{
					this.attributes.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}
	/**
	 * Returns true if all attributes of this equals those of other.
	 */
	public boolean propertyEqual(RefLinkPart other) 
	{

		if (!this.getOid().equals(other.getOid())) 
		{
			return false;
		}
		
		// Checking if the other AttributePart has the same attributes.
		if (!this.attributes.equals(other.attributes))
		{
			return false;
		}
		
		return true;
	}

	/**
	 * Returns true if all direction based attributes indicates that they are
	 * with the travel direction.
	 */
	public boolean aligned() 
	{
		// Checking direction of the Forbidden driving direction attribute.
		if (this.attributes.get(AttributeType.FORBIDDEN_DRIVER_DIRECTION) != null 
			&& this.attributes.get(AttributeType.FORBIDDEN_DRIVER_DIRECTION).getDirection().equals(DirectionCategories.WITH)) 
		{
			return false;
		} 
		
		// Checking direction of all other attributes.
		for (Entry<AttributeType, Attribute> entry : this.attributes.entrySet())
		{
			if (!entry.getKey().equals(AttributeType.FORBIDDEN_DRIVER_DIRECTION))
			{
				if (entry.getValue().getDirection().equals(DirectionCategories.AGAINST))
				{
					return false;
				}
			}
		}
		
		return true;
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
	public void align(GeometryFactory gf) 
	{
		if (!this.aligned()) 
		{
			if (this.reverseGeom(gf)) 
			{
				String oldTo = this.getNodeTo();

				this.setNodeTo(this.getNodeFrom());
				this.setNodeFrom(oldTo);

				// After check of "aligned()" we know that vals are not null
				for (Entry<AttributeType, Attribute> entry : this.attributes.entrySet())
				{
					if (entry.getValue().getType().equals(AttributeType.FORBIDDEN_DRIVER_DIRECTION))
					{
						entry.getValue().setDirection(DirectionCategories.AGAINST);
					}
					//TODO: Limit to only directed attributes?
					else
					{
						entry.getValue().setDirection(DirectionCategories.WITH);
					}
				}
				
				//Update measure to/from
				double measureToOld = this.getMeasureTo();
				// FIXME: Precision errors?
				this.setMeasureTo(1.0000000000000d-this.getMeasureFrom());
				this.setMeasureFrom(1.0000000000000d-measureToOld);
			}
		}
	}
}
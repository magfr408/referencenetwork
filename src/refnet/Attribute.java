package refnet;
/**
 * This Class contains a specific Attribute of the NVDB network.
 * 
 * @author Rasmus Ringdahl Linköpings University
 *
 */
public class Attribute 
{
	/**
	 * The different kinds of attributes.
	 * 
	 * @author Magnus Fransson
	 * @author Rasmus Ringdahl
	 */
	public enum AttributeType
	{
		BRIDGE_AND_TUNNEL,
		FUNCTIONAL_ROADCLASS,
		LIMITED_VEHICLE_LENGTH,
		LIMITED_VEHICLE_WIDTH,
		LIVING_STREET,
		MOTORWAY,
		MOTORWAY_WITHOUT_A_CENTRAL_RESERVATION,
		LANES,
		PEDESTRIAN_STREET, 
		FORBIDDEN_DRIVER_DIRECTION,
		ROAD_MANAGER,
//		ROAD_TRAFFIC_NETWORK, TODO: Add this?
		ROAD_WIDTH,
		SPEED_LIMIT,
		URBAN_AREA,
		WEARING_COURSE;
	}

	/**
	 * This is the type of attribute.
	 */
	private AttributeType type;
	
	/**
	 * This is the direction of the attribute:
	 * 1 = with the geometry
	 * 2 = against the geometry
	 * 3 = both directions.
	 * TODO: Change to boolean?
	 */
	private Integer direction;
	
	/**
	 * The value of the attribute.
	 */
	private Object value;
	
	
	/**
	 * This is the constructor for an attribute.
	 * @param type
	 * @param direction
	 * @param value
	 */
	public Attribute(AttributeType type, Integer direction, Object value)
	{
		this.type = type;
		this.direction = direction;
		this.value = value;
	}
	
	/**
	 * This method gets the type of attribute.
	 * 
	 * @return type.
	 */
	public AttributeType getType() {return this.type;}
	
	/**
	 * The method gets the direction of the attribute.
	 * @return direction (1 = with the geometry, 2 = against the geometry, 3 = both directions).
	 */
	public Integer getDirection() {return this.direction;}
	
	/**
	 * This method gets the attribute value.
	 * @return attribute value as an Object.
	 */
	public Object getValue() {return this.value;}
	
	@Override 
	public boolean equals(Object other) 
	{
		// Class type check.
		if (!(other instanceof Attribute)) {return false;}
		
		// Attribute type check.
		if (this.type != ((Attribute)other).type) { return false;}
		
		// Direction check.
		if (this.direction != ((Attribute)other).direction) {return false;}
		
		// Value check.
		if (this.value != ((Attribute)other).value) {return false;}
		
		return true;
	}
	
}	

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
//		LIVING_STREET, TODO: Add this?
		MOTORWAY,
		MOTORWAY_WITHOUT_A_CENTRAL_RESERVATION,
		NUMBER_OF_LANES,
//		PEDESTRIAN_STREET, TODO: Add this? 
		FORBIDDEN_DRIVER_DIRECTION,
		ROAD_MANAGER,
//		ROAD_TRAFFIC_NETWORK, TODO: Add this?
		ROAD_WIDTH,
		ROUNDABOUT,
		SPEED_LIMIT,
		URBAN_AREA,
		WEARING_COURSE;
	}

	public enum DirectionCategories
	{
		WITH,
		AGAINST,
		WITH_AND_AGAINST,
		NOT_SPECIFIED
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
	private DirectionCategories direction;
	
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
	public Attribute(AttributeType type, DirectionCategories direction, Object value)
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
	public DirectionCategories getDirection() {return this.direction;}
	
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
	
	
	public void validate()
	{
		switch (this.type)
		{
			// Validation of functional road class.
			case FUNCTIONAL_ROADCLASS:
			
				// Checking for Integer.
				if (!(this.value instanceof Integer)) 
				{
					throw new IllegalArgumentException("Attribute of type " + this.type.name() 
													 + " must have a value of type " + Integer.class.getName());
				}
				
				// Checking for value in range [0 , 9].
				if (((Integer)this.value).intValue() < 0 || ((Integer)this.value).intValue() > 9) 
				{
					throw new IllegalArgumentException("Attribute of type " + this.type.name() 
					 								 + " must have a value between 0 and 9.");
				}
				
				break;
			
			// Validation of number of lanes.
			case NUMBER_OF_LANES:
				
				// Checking for integer.
				if (!(this.value instanceof Integer)) 
				{
					throw new IllegalArgumentException("Attribute of type " + this.type.name() 
					 + " must have a value of type " + Integer.class.getName());
				}
				
				// Checking for positive values.
				if (((Integer)this.value).intValue() < 0) 
				{
					throw new IllegalArgumentException("Attribute of type " + this.type.name() 
					 								 + " must have a positive value.");
				}
				
				break;
				
			// Validation of forbidden driver direction.
			case FORBIDDEN_DRIVER_DIRECTION:
				
				// Checking for boolean.
				if (!(this.value instanceof Boolean)) 
				{
					throw new IllegalArgumentException("Attribute of type " + this.type.name() 
					 + " must have a value of type " + Boolean.class.getName());
				}
				
				break;
				
			// Validation of speed limit.
			case SPEED_LIMIT:
				
				// Checking for Double
				if (!(this.value instanceof Double)) 
				{
					throw new IllegalArgumentException("Attribute of type " + this.type.name() 
					 + " must have a value of type " + Double.class.getName());
				}
				
				// Checking for positive values.
				if (((Double)this.value).doubleValue() < 0) 
				{
					throw new IllegalArgumentException("Attribute of type " + this.type.name() 
					 + " must have a positive value.");
				}
				
				break;
				
			default:
				throw new UnsupportedOperationException(this.type + " is not supported in the validation yet.");
				
		}
	}

	public void setDirection(DirectionCategories direction) 
	{
		
		this.direction = direction;
		
	}

}	

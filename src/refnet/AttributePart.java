package refnet;

import com.vividsolutions.jts.geom.LineString;

/**
 * Shell to Part which is used to compare the attributes of Parts.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @version 1.0
 */
public class AttributePart extends Part {

	/**
	 * {@inheritDoc}
	 */
	public AttributePart(String refLinkOid, LineString geometry, double measureFrom, double measureTo, Double velocity,
			Integer velocityDirection, Integer nbLanes, Integer classificationNo, Integer unallowedDirection) throws IllegalArgumentException {

		super(refLinkOid, geometry, measureFrom, measureTo, velocity, velocityDirection, nbLanes, classificationNo,
				unallowedDirection);
	}

	/**
	 * @return all the fields of this object as a ;-separated String. The
	 *         Geometry will be in WKT-format.
	 */
	public String toCSVStringWithoutAttributes() {
		return (this.getOid() + ";" + String.valueOf(this.getMeasureFrom()) + ";" + String.valueOf(this.getMeasureTo())
				+ ";" + this.getGeometryAsStr());
	}

	/**
	 * Returns true if all attributes of this equals those of other.
	 */
	public boolean propertyEqual(AttributePart other) {
		boolean retval = true;

		if (!this.getOid().equals(other.getOid())) {
			retval = false;
		}

		if (!(((this.getFunctionalRoadClass() == null) && (other.getFunctionalRoadClass() == null))
				|| (this.getFunctionalRoadClass().equals(other.getFunctionalRoadClass())))) {
			retval = false;
		}

		if (!(((this.getNumberOfLanes() == null) && (other.getNumberOfLanes() == null))
				|| (this.getNumberOfLanes().equals(other.getNumberOfLanes())))) {
			retval = false;
		}

		if (!(((this.getUnallowedDriverDir() == null) && (other.getUnallowedDriverDir() == null))
				|| (this.getUnallowedDriverDir().equals(other.getUnallowedDriverDir())))) {
			retval = false;
		}

		if (!(((this.getVelocity() == null) && (other.getVelocity() == null))
				|| (this.getVelocity().equals(other.getVelocity())))) {
			retval = false;
		}

		if (!(((this.getVelocityDirection() == null) && (other.getVelocityDirection() == null))
				|| (this.getVelocityDirection().equals(other.getVelocityDirection())))) {
			retval = false;
		}

		return retval;
	}
}

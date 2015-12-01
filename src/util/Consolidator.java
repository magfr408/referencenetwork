package util;

import java.util.ArrayList;
import java.util.Collections;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import io.Logger;
import refnet.Attribute;
import refnet.CustomComparator;
import refnet.RefLinkPart;

public class Consolidator {

	/**
	 * Add attribute to the list of attributes and then sorts attributes in
	 * ascending order. If the attribute "equals" (same attribute values) some
	 * other attribute in the list, and their geometries overlap, then they are
	 * appended to each other, creating a longer attribute.
	 */
	public static ArrayList<Attribute> Consolidate(ArrayList<Attribute> attributes, Attribute attribute,
			GeometryFactory gf) {
		return Consolidator.addToList(attributes, attribute, gf);
	}

	/**
	 * Add attribute attributeWithoutGeom to the list of attributes and then
	 * sorts attributes in ascending order.
	 */
	public static ArrayList<Attribute> ConsolidateWithoutGeom(ArrayList<Attribute> attributes,
			Attribute attributeWithoutGeom, GeometryFactory geometryFactory, Logger logger) {

		Collections.sort(attributes, new CustomComparator());

		LineString L = null;

		if (attributes.size() > 1) {
			for (int i = 1; i < attributes.size(); i++) {
				// Step 1: Find part before and part after.
				if (!(attributes.get(i).getGeometry().getStartPoint()
						.equalsExact(attributes.get(i - 1).getGeometry().getEndPoint()))) {
					// Step 2: Create new LineString with new method in
					// GeometryOps.
					L = GeometryOps.lineStringBetween(attributes.get(i - 1).getGeometry(),
							attributes.get(i).getGeometry(), geometryFactory);
					break;
				}
			}
		} else {
			// TODO: Get Geometry from reflink part....
			logger.log(new String[] {
					"Consolidator: Could not create geometry for attribute (it is probably first or last in the list): "
							+ attributeWithoutGeom.getOid() + ";" + attributeWithoutGeom.getMeasureFrom() + ";"
							+ attributeWithoutGeom.getMeasureTo() });
		}

		if (L != null) {
			// Step 3: Add geometry to attribute.
			attributeWithoutGeom.setGeometry(L);
			// Step 4: Add attribute to list.
			return Consolidator.addToList(attributes, attributeWithoutGeom, geometryFactory);
		}

		return attributes;
	}

	static ArrayList<Attribute> addToList(ArrayList<Attribute> attributes, Attribute attribute, GeometryFactory gf) {
		boolean add = true;

		for (int i = 0; i < attributes.size(); i++) {

			if (attributes.get(i).propertyEqual(attribute)) {
				// L1 has the same endpoint as L2:s startpoint...
				if (attributes.get(i).getGeometry().getEndPoint()
						.equalsExact(attribute.getGeometry().getStartPoint())) {
					attribute.setGeometry(
							GeometryOps.append(attributes.get(i).getGeometry(), attribute.getGeometry(), gf, false));
					attribute.setMeasureFrom(attributes.get(i).getMeasureFrom());

					attributes.set(i, attribute);
					add = false; // Already added.
					// L2 has the same endpoint as L1:s startpoint...
				} else if (attribute.getGeometry().getEndPoint()
						.equalsExact(attributes.get(i).getGeometry().getStartPoint())) {
					attribute.setGeometry(
							GeometryOps.append(attribute.getGeometry(), attributes.get(i).getGeometry(), gf, false));
					attribute.setMeasureTo(attributes.get(i).getMeasureTo());

					attributes.set(i, attribute);
					add = false; // Already added.
				}
			}
		}

		if (add) {
			attributes.add(attribute);
		}

		Collections.sort(attributes, new CustomComparator());

		return attributes;
	}

	/**
	 * Add refLinkPart to the list of RefLinkParts and then sorts refLinkParts
	 * in ascending order. If the refLinkPart "equals" (same attribute values)
	 * some other refLinkPart in the list, and if their geometries overlap, then
	 * they are appended to each other, creating a 'longer' RefLinkPart.
	 */
	public static ArrayList<RefLinkPart> Consolidate(ArrayList<RefLinkPart> refLinkParts, RefLinkPart refLinkPart,
			GeometryFactory gf) {
		return Consolidator.addToList(refLinkParts, refLinkPart, gf);
	}

	static ArrayList<RefLinkPart> addToList(ArrayList<RefLinkPart> refLinkParts, RefLinkPart refLinkPart,
			GeometryFactory gf) {
		boolean add = true;

		for (int i = 0; i < refLinkParts.size(); i++) {

			if (refLinkParts.get(i).propertyEqual(refLinkPart)) {
				// L1 has the same endpoint as L2:s startpoint...
				if (refLinkParts.get(i).getGeometry().getEndPoint()
						.equalsExact(refLinkPart.getGeometry().getStartPoint())) {
					refLinkPart.setGeometry(GeometryOps.append(refLinkParts.get(i).getGeometry(),
							refLinkPart.getGeometry(), gf, false));
					refLinkPart.setMeasureFrom(refLinkParts.get(i).getMeasureFrom());
					refLinkPart.setNodeFrom(refLinkParts.get(i).getNodeFrom());

					refLinkParts.set(i, refLinkPart);
					add = false; // Already added.
					break;
					// L2 has the same endpoint as L1:s startpoint...
				} else if (refLinkPart.getGeometry().getEndPoint()
						.equalsExact(refLinkParts.get(i).getGeometry().getStartPoint())) {
					refLinkPart.setGeometry(GeometryOps.append(refLinkPart.getGeometry(),
							refLinkParts.get(i).getGeometry(), gf, false));
					refLinkPart.setMeasureTo(refLinkParts.get(i).getMeasureTo());
					refLinkPart.setNodeTo(refLinkParts.get(i).getNodeTo());

					refLinkParts.set(i, refLinkPart);
					add = false; // Already added.
					break;
				}
			}
		}

		if (add) {
			refLinkParts.add(refLinkPart);
		}

		Collections.sort(refLinkParts, new CustomComparator());

		return refLinkParts;
	}
}

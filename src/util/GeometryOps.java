package util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LinearGeometryBuilder;

/**
 * Common static geometry operations.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @version 1.0
 */
public class GeometryOps {

	/**
	 * Creates a 2-point LineString. First Point is the end point of L1. Second
	 * Point is the start Point of L2. Returns a new LineString if all went
	 * well, returns null if it could not be created.
	 */
	public static LineString lineStringBetween(LineString L1, LineString L2, GeometryFactory gf) {
		LinearGeometryBuilder lgb = new LinearGeometryBuilder(gf);

		Coordinate[] c1 = L1.getCoordinates();
		Coordinate[] c2 = L2.getCoordinates();

		int c = c1.length - 1;

		lgb.add(c1[c], false);
		lgb.add(c2[0], false);

		lgb.endLine();

		try {
			LineString L = (LineString) lgb.getGeometry();

			if (L.getNumPoints() != 2) {
				return null;
			} else {
				return L;
			}
		} catch (ClassCastException cce) {
			return null;
		}
	}

	/**
	 * Returns true if the distance from P2 to any segment of L1 is smaller than
	 * tolerance.
	 */
	public static boolean spansPoint(LineString L1, Point P2, GeometryFactory gf, double tolerance, boolean allowSlack)
			throws IllegalArgumentException {

		LinearGeometryBuilder lgb = new LinearGeometryBuilder(gf);
		Coordinate[] coordinates = L1.getCoordinates();

		// This just adds the coordinates in the right sequence (hopefully).
		for (int i = 0; i < coordinates.length; i++) {

			// Don't add duplicate coordinates.
			lgb.add(coordinates[i], false);

			if (i < (coordinates.length - 1)) {
				LineSegment ls = new LineSegment(coordinates[i], coordinates[i + 1]);

				if (Math.abs(ls.distance(P2.getCoordinate())) < tolerance) {
					lgb.add(P2.getCoordinate(), false);
					return true;
				}
			}
		}

		lgb.endLine();

		LineString returnval = (LineString) lgb.getGeometry();

		if (returnval.getNumPoints() == L1.getNumPoints() && allowSlack) {
			return GeometryOps.spansPoint(returnval, P2, gf, tolerance * 10.0000000000d, false);
		} else {
			return false;
		}
	}

	/**
	 * Adds L2 to the end of L1. <b>conditional</b> checks to make sure that the
	 * end point of L1 is equal to the start point of L2.
	 */
	public static LineString append(LineString L1, LineString L2, GeometryFactory gf, boolean conditional)
			throws IllegalArgumentException {

		LinearGeometryBuilder lgb1 = new LinearGeometryBuilder(gf);

		Coordinate[] coordinates1 = L1.getCoordinates();
		Coordinate[] coordinates2 = L2.getCoordinates();

		for (int i = 0; i < coordinates1.length; i++) {
			lgb1.add(coordinates1[i], true);
		}

		if (conditional) {
			if (L1.getEndPoint().equalsExact(L2.getStartPoint())) {
				for (int i = 1; i < coordinates2.length; i++) {
					lgb1.add(coordinates2[i], false);
				}
			}
		} else {
			for (int i = 0; i < coordinates2.length; i++) {
				lgb1.add(coordinates2[i], false);
			}
		}

		lgb1.endLine();

		return (LineString) lgb1.getGeometry();
	}

	/**
	 * Projects P on L by checking the distance from P to each LineSegment of L.
	 * If the distance is less than <b>tolerance</b> then the point is added
	 * (once) to the LineString coordinates.
	 * 
	 * @param L
	 *            <b>LineString</b> object on which the <b>Point</b> is
	 *            projected.
	 * @param P
	 *            <b>Point</b> object which is projected.
	 * @param gf
	 *            <b>GeometryFactory</b> should have been instantiated by new
	 *            GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING))
	 * @param tolerance,
	 *            suggested value is 0.0000000001d.
	 * @param allowSlack,
	 *            will roll back tolerance to
	 *            <p>
	 *            tolerance*10.0000000000d
	 *            </p>
	 *            .
	 * @return L with an extra point in the right place if the projection was
	 *         successful. I.e. P2 was on the line of a LineSegment.
	 */
	public static LineString projectPoint(LineString L, Point P, GeometryFactory gf, double tolerance,
			boolean allowSlack) throws IllegalArgumentException {

		LinearGeometryBuilder lgb1 = new LinearGeometryBuilder(gf);
		Coordinate[] coordinates = L.getCoordinates();

		// This just adds the coordinates in the right sequence (hopefully).
		for (int i = 0; i < coordinates.length; i++) {

			// Don't add duplicate coordinates.
			lgb1.add(coordinates[i], false);

			if (i < (coordinates.length - 1)) {
				LineSegment ls = new LineSegment(coordinates[i], coordinates[i + 1]);

				if (Math.abs(ls.distance(P.getCoordinate())) < tolerance) {
					lgb1.add(P.getCoordinate(), false);
				}
			}
		}

		lgb1.endLine();

		LineString returnval = (LineString) lgb1.getGeometry();

		if (returnval.getNumPoints() == L.getNumPoints() && allowSlack) {
			return GeometryOps.projectPoint(returnval, P, gf, tolerance * 10.0000000000d, false);
		} else {
			return returnval;
		}
	}

	/**
	 * Splits the LineString L at each point p of P that could be "projected" on
	 * L given that p was within the tolerated distance of L.
	 * 
	 * @param L
	 *            the LineString object that will be separated.
	 * @param P
	 *            list of Points at which L is separated.
	 * @param gf
	 * @param tolerance
	 *            the maximum allowed distance between each Point of P and L.
	 * @param allowSlack
	 * @return an array of LineStrings of length |P|+1, or null if the split was
	 *         unsuccessful, i.e. if the processed cast a
	 *         java.lang.ClassCaseException at LineString linestring =
	 *         (LineString) Geometry object.
	 */
	public static LineString[] splitBy(LineString L, Point[] P, GeometryFactory gf, double tolerance,
			boolean allowSlack) {
		LineString[] retval = new LineString[(P.length + 1)];

		for (int i = 0; i < P.length; i++) {
			L = GeometryOps.projectPoint(L, P[i], gf, tolerance, allowSlack);
		}

		Coordinate[] C = new Coordinate[L.getCoordinates().length];

		for (int i = 0; i < C.length; i++) {
			C[i] = L.getCoordinateN(i);
		}

		int idx = 0;

		LinearGeometryBuilder lgb = new LinearGeometryBuilder(gf);

		for (int i = 0; i < C.length; i++) {
			lgb.add(C[i], false);

			// End of first LineString.
			if (idx < P.length) {
				if (C[i].equals(P[idx].getCoordinates()[0])) {
					lgb.endLine();
					lgb.add(C[i], false);
					idx++;
				}
			}
		}

		lgb.endLine();

		try {
			MultiLineString M = (MultiLineString) lgb.getGeometry();

			if (M.getNumGeometries() == retval.length) {
				for (int i = 0; i < retval.length; i++) {
					try {
						retval[i] = (LineString) M.getGeometryN(i).clone();
					} catch (java.lang.ClassCastException cce1) {
						System.out.println("GeometryOps: Classcast exception @2, returning null.");

						return null;
					}
				}
			}
		} catch (java.lang.ClassCastException cce2) {
			System.out.println("GeometryOps: Classcast exception @1, returning null.");
			return null;
		}
		return retval;
	}

	/**
	 * Reverses the order of the Coordinates of L.
	 * 
	 * @param L
	 *            LineString to be reversed.
	 * @param gf
	 * @return a new LineString (L with reversed coordinate order) or null if
	 *         something went wrong.
	 */
	public static LineString reverse(LineString L, GeometryFactory gf) {
		Coordinate[] C = L.getCoordinates();

		LinearGeometryBuilder lgb = new LinearGeometryBuilder(gf);

		for (int i = C.length - 1; i >= 0; i--) {
			lgb.add(C[i]);
		}
		lgb.endLine();

		try {
			return (LineString) lgb.getGeometry();
		} catch (IllegalArgumentException iae) {
			return null;
		} catch (ClassCastException cce) {
			return null;
		}
	}
}

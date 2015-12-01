package refnet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import io.Logger;
import util.NameGenerator;
import util.Consolidator;
import util.GeometryOps;

/**
 * Owner of all parts of the network that has the same OID. Manages the addition
 * of attributes.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @version 1.0
 */
public class RefLink {
	private final String oid;
	private ArrayList<RefLinkPart> refLinkParts;
	private int nbParts;

	public RefLink(String linkOid, RefLinkPart firstRefLinkPart) {
		this.oid = linkOid;

		this.refLinkParts = new ArrayList<RefLinkPart>();

		this.addFirstRefLinkPart(firstRefLinkPart);
	}

	/**
	 * Aligns all Parts of the object so that they are given in the travel
	 * direction if they are unidirectional.
	 */
	public void align(GeometryFactory geometryFactory) {
		ArrayList<RefLinkPart> unaligned = new ArrayList<RefLinkPart>(this.refLinkParts);
		ArrayList<RefLinkPart> aligned = new ArrayList<RefLinkPart>();

		Collections.sort(unaligned, new CustomComparator());

		RefLinkPart r;

		for (int n = 0; n < this.nbParts; n++) {
			r = unaligned.get(n);

			if (!r.aligned()) {
				r.align(geometryFactory);
			}

			aligned.add(r);
		}

		this.refLinkParts = new ArrayList<RefLinkPart>(aligned);
	}

	/**
	 * Checks for null-value fields in the list of RefLinkParts and tries to
	 * fill those with the values of other parts and then does a union of all
	 * sequential RefLinkParts iff: they are property equal and if a from-node
	 * can be removed (only has one incoming and one outgoing link).
	 */
	public ArrayList<String> clean(GeometryFactory geometryFactory, HashMap<String, RefNode> nodes) {

		ArrayList<String> removedNodes = new ArrayList<String>();

		ArrayList<RefLinkPart> dirty = new ArrayList<RefLinkPart>(this.refLinkParts);
		ArrayList<RefLinkPart> clean = new ArrayList<RefLinkPart>();

		Collections.sort(dirty, new CustomComparator());

		RefLinkPart r1, r2;

		if (dirty.size() > 1) {
			for (int n = 0; n < (dirty.size() - 1); n++) {
				r1 = dirty.get(n);
				r2 = dirty.get(n + 1);

				r1.replaceNullAttributesBy(r2);
			}

			r1 = dirty.get(dirty.size() - 1);
			r2 = dirty.get(dirty.size() - 2);
			r1.replaceNullAttributesBy(r2);

			clean.add(dirty.get(0));

			RefNode from;

			int nb = clean.size();

			for (int n = 1; n < dirty.size(); n++) {
				from = nodes.get(dirty.get(n).getNodeFrom());

				if ((from.nbIncoming() == 1) && (from.nbOutgoing() == 1)) {
					clean = Consolidator.Consolidate(clean, dirty.get(n), geometryFactory);
					if (clean.size() == nb) {
						removedNodes.add(from.getOid());
					}
				} else {
					clean.add(dirty.get(n));
				}

				nb = clean.size();
			}
		} else {
			clean = new ArrayList<RefLinkPart>(dirty);
		}

		this.refLinkParts = new ArrayList<RefLinkPart>(clean);
		this.nbParts = clean.size();

		return removedNodes;
	}

	/**
	 * Returns the OID of this RefLink
	 */
	public String getOid() {
		return this.oid;
	}

	/**
	 * Add the first <b>RefLinkPart</b> if none available.
	 */
	private void addFirstRefLinkPart(RefLinkPart firstPart) {
		if (this.refLinkParts.isEmpty()) {
			this.refLinkParts.add(firstPart);
			this.nbParts = 1;
		}
	}

	/**
	 * Adds a new <b>RefLinkPart</b> to the list of parts. Guarantees that new
	 * part does not overlap previous parts by rejecting it.
	 * 
	 * @param newPart
	 * @return true if new RefLinkPart was approved.
	 */
	public boolean addRefLinkPart(RefLinkPart newPart, GeometryFactory gf) {

		for (RefLinkPart oldPart : this.refLinkParts) {
			if (oldPart.hasCommonGeometry(newPart, gf)) {
				return false;
			}
		}

		this.refLinkParts.add(newPart);
		this.nbParts = this.nbParts + 1;
		return true;
	}

	/**
	 * Adds an <b>Attribute</b> to the object by a "slice and dice RefLinkParts"
	 * method. The following cases apply for all <b>RefLinkPart</b>s rlp: If the
	 * rlp is within the attribute, or if they are geometrically equal, the rlp
	 * inherits the attribute properties lanes, speed etc. without any
	 * alternations to the rlp geometry. If the attribute is within the rlp, or
	 * if they overlap in some sense, the rlp will be split by either the
	 * attribute start point, end point or both. The part of the rlp that
	 * overlapped the attribute will inherit the attribute properties. The nodes
	 * and measures gets altered in this process.
	 * 
	 * @param attribute
	 * @param gf
	 * @param nmg
	 * @param tolerance
	 *            expresses the tolerated distance between the geometries when
	 *            instersections are checked.
	 * @param allowSlack
	 *            if this is true, then the tolerance can be increased until the
	 *            parts do intersect at that distance. The final tolerance is
	 *            posted in Console.
	 * @return HashSet<RefNode> new nodes generated with <b>nmg</b> to be added
	 *         to RefNetworks list of nodes.
	 */
	public HashSet<RefNode> addAttributeByGeom(Attribute attribute, GeometryFactory gf, NameGenerator nmg,
			double tolerance, boolean allowSlack, Logger logger) {

		HashSet<RefNode> newNodes = new HashSet<RefNode>();

		ListIterator<RefLinkPart> iterator = this.refLinkParts.listIterator();

		while (iterator.hasNext()) {

			RefLinkPart rlp = iterator.next();

			boolean rlpEndEqualsAttrStart = (attribute.getGeometry().getStartPoint())
					.equalsExact((rlp.getGeometry().getEndPoint()));
			boolean rlpStartEqualsAttrEnd = (attribute.getGeometry().getEndPoint())
					.equalsExact((rlp.getGeometry().getStartPoint()));
			boolean rlpStartAndEndEqualsAttrStartAndEnd = false;

			if ((attribute.getGeometry().getStartPoint()).equalsExact((rlp.getGeometry().getStartPoint()), tolerance)
					&& (attribute.getGeometry().getEndPoint()).equalsExact((rlp.getGeometry().getEndPoint()),
							tolerance)) {
				rlpStartAndEndEqualsAttrStartAndEnd = true;
			}

			if ((rlp.geomIsWithin(attribute, gf)) || rlpStartAndEndEqualsAttrStartAndEnd) {
				// Trivial case, just add the property.
				rlp.addAttribute(attribute);
				// set() replaces the last object returned by next()
				iterator.set(rlp);

			} else if (attribute.geomIsCompletelyWithin(rlp, gf)) {
				// Split in 3.

				Point[] P = new Point[] { attribute.getGeometry().getStartPoint(),
						attribute.getGeometry().getEndPoint() };

				RefNode n1 = new RefNode(attribute.getOid() + ":" + nmg.newName(), P[0], true);
				RefNode n2 = new RefNode(attribute.getOid() + ":" + nmg.newName(), P[1], true);

				LineString[] L = GeometryOps.splitBy(rlp.getGeometry(), P, gf, tolerance, allowSlack);

				if (L != null) {
					RefLinkPart rlpMiddle = new RefLinkPart(rlp.getOid(), L[1], attribute.getMeasureFrom(),
							attribute.getMeasureTo(), n1.getOid(), n2.getOid(), rlp.getVelocity(),
							rlp.getVelocityDirection(), rlp.getNumberOfLanes(), rlp.getFunctionalRoadClass(),
							rlp.getUnallowedDriverDir());
					rlpMiddle.addAttribute(attribute);

					RefLinkPart rlpLast = new RefLinkPart(rlp.getOid(), L[2], attribute.getMeasureTo(),
							rlp.getMeasureTo(), n2.getOid(), rlp.getNodeTo(), rlp.getVelocity(),
							rlp.getVelocityDirection(), rlp.getNumberOfLanes(), rlp.getFunctionalRoadClass(),
							rlp.getUnallowedDriverDir());

					rlp.setGeometry(L[0]);
					rlp.setMeasureTo(attribute.getMeasureFrom());
					rlp.setNodeTo(n1.getOid());

					iterator.set(rlp);
					iterator.add(rlpMiddle);
					iterator.add(rlpLast);
					this.nbParts = this.nbParts + 2;

					if (!newNodes.contains(n1)) {
						newNodes.add(n1);
					}

					if (!newNodes.contains(n2)) {
						newNodes.add(n2);
					}
				} else {
					logger.log(
							new String[] {
									"RefLink: (Case1) Failed to add attribute: "
											+ attribute.toCSVStringWithoutAttributes() + " ...",
									" 	... on RefLinkPart " + rlp.toCSVString(false) });
				}
			} else if (rlp.geomEndsWithin(attribute, gf) && !(rlpEndEqualsAttrStart)) {
				// split in 2.

				Point[] P = new Point[] { attribute.getGeometry().getStartPoint() };

				RefNode n1 = new RefNode(attribute.getOid() + ":" + nmg.newName(), P[0], true);

				LineString[] L = GeometryOps.splitBy(rlp.getGeometry(), P, gf, tolerance, allowSlack);

				if (L != null) {
					RefLinkPart rlpLast = new RefLinkPart(rlp.getOid(), L[1], attribute.getMeasureFrom(),
							rlp.getMeasureTo(), n1.getOid(), rlp.getNodeTo(), rlp.getVelocity(),
							rlp.getVelocityDirection(), rlp.getNumberOfLanes(), rlp.getFunctionalRoadClass(),
							rlp.getUnallowedDriverDir());
					rlpLast.addAttribute(attribute);

					rlp.setGeometry(L[0]);
					rlp.setMeasureTo(attribute.getMeasureFrom());
					rlp.setNodeTo(n1.getOid());

					iterator.set(rlp);
					iterator.add(rlpLast);
					this.nbParts = this.nbParts + 1;

					if (!newNodes.contains(n1)) {
						newNodes.add(n1);
					}
				} else {
					logger.log(
							new String[] {
									"RefLink: (Case3) Failed to add attribute: "
											+ attribute.toCSVStringWithoutAttributes() + " ...",
									" 	... on RefLinkPart " + rlp.toCSVString(false) });

				}
			} else if (attribute.geomEndsWithin(rlp, gf) && !(rlpStartEqualsAttrEnd)) {
				// split in 2.

				/*
				 * Observe: This if-block is last since
				 * attribute.geomEndsWithin(rlp...) is true for 3 cases and not
				 * the desired 2.
				 */

				Point[] P = new Point[] { attribute.getGeometry().getEndPoint() };

				RefNode n2 = new RefNode(attribute.getOid() + ":" + nmg.newName(), P[0], true);

				LineString[] L = GeometryOps.splitBy(rlp.getGeometry(), P, gf, tolerance, allowSlack);

				if (L != null) {
					RefLinkPart rlpFirst = new RefLinkPart(rlp.getOid(), L[0], rlp.getMeasureFrom(),
							attribute.getMeasureTo(), rlp.getNodeFrom(), n2.getOid(), rlp.getVelocity(),
							rlp.getVelocityDirection(), rlp.getNumberOfLanes(), rlp.getFunctionalRoadClass(),
							rlp.getUnallowedDriverDir());
					rlpFirst.addAttribute(attribute);

					rlp.setGeometry(L[1]);
					rlp.setMeasureFrom(attribute.getMeasureTo());
					rlp.setNodeFrom(n2.getOid());

					iterator.set(rlp);
					iterator.add(rlpFirst);
					this.nbParts = this.nbParts + 1;

					if (!newNodes.contains(n2)) {
						newNodes.add(n2);
					}
				} else {
					logger.log(
							new String[] {
									"RefLink: (Case2) Failed to add attribute: "
											+ attribute.toCSVStringWithoutAttributes() + " ...",
									" 	... on RefLinkPart " + rlp.toCSVString(false) });
				}
			}
		}
		return newNodes;
	}

	/**
	 * Get the number of <b>RefLinkParts</b> on the RefLink.
	 */
	public int getNbParts() {
		return this.nbParts;
	}

	/**
	 * Get the one <b>RefLinkPart</b> as a ;-separated String without new line.
	 */
	public String refLinkPartAsCSVString(int idx, boolean withAttributes) {
		if (idx < this.refLinkParts.size()) {
			return this.refLinkParts.get(idx).toCSVString(withAttributes);
		} else {
			return null;
		}
	}

	/**
	 * Get the whole list of <b>RefLinkPart</b>s as a String with new line
	 * (System.lineSeparator).
	 */
	public String getRefLinkPartsAsCSVStringWithNewRow(boolean withAttributes) {
		String str = "";

		int idx = 0;

		while (this.refLinkParts.size() > idx) {
			str = str + this.refLinkParts.get(idx).toCSVString(withAttributes);

			if (idx < (this.refLinkParts.size() - 1)) {
				str = str + System.lineSeparator();
			}
			idx++;
		}

		return str;
	}

	/**
	 * Returns a list of all from node oids.
	 */
	public ArrayList<String> getNodesFrom() {
		ArrayList<String> from = new ArrayList<String>();

		for (RefLinkPart r : this.refLinkParts) {
			from.add(r.getNodeFrom());
		}

		return from;
	}

	/**
	 * Returns a list of all to node oids.
	 */
	public ArrayList<String> getNodesTo() {
		ArrayList<String> to = new ArrayList<String>();

		for (RefLinkPart r : this.refLinkParts) {
			to.add(r.getNodeTo());
		}

		return to;
	}
	
	public String getEndNodeOid() {
		double to = 0.0d;
		String n = null;
		
		for (RefLinkPart p : this.refLinkParts) {
			if (p.getMeasureTo() > to) {
				n = p.getNodeTo();
				to = p.getMeasureTo();
			}
		}
		
		return n;
	}
	
	public String getStartNodeOid() {
		double from = 1.0d;
		String n = null;
		
		for (RefLinkPart p : this.refLinkParts) {
			if (p.getMeasureFrom() < from) {
				n = p.getNodeFrom();
				from = p.getMeasureFrom();
			}
		}
		
		return n;
	}
}
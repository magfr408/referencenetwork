package refnet;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Point;

import util.Pair;

/**
 * A network node, which is just a String with some additions.
 * 
 * @author Magnus Fransson <magnus.fransson@sweco.se>
 * @author Rasmus Ringdahl <rasmus.ringdahl@liu.se>
 * @version 1.0
 */
public class RefNode {
	private final String oid;
	private final boolean extended;
	private Point point;
	private ArrayList<String> incoming;
	private ArrayList<String> outgoing;
	private ArrayList<Pair<String, String>> forbiddenTurns;

	public RefNode(String nodeOid, Point P, boolean createdDuringRunTime) {
		this.oid = nodeOid;
		this.extended = createdDuringRunTime;
		this.point = (Point) P.clone();

		this.incoming = new ArrayList<String>();
		this.outgoing = new ArrayList<String>();
		this.forbiddenTurns = new ArrayList<Pair<String, String>>();
	}

	/**
	 * Used as key in RefNetwork.RefNodes through RefNode.equals.
	 */
	@Override
	public int hashCode() {
		int hash = 7;

		hash = 17 * hash + this.oid.hashCode() + Boolean.valueOf(this.extended).hashCode();

		return hash;
	}

	/**
	 * Compares the hashcode of this to other
	 */
	@Override
	public boolean equals(Object other) {
		if ((other == null) || !(other instanceof RefNode)) {
			return false;
		} else {
			if (this.hashCode() == other.hashCode()) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Returns true if this RefNode was created during attribute assignment.
	 */
	public boolean extended() {
		return this.extended;
	}

	/**
	 * Returns the String identifier of this object.
	 */
	public String getOid() {
		return this.oid;
	}

	/**
	 * Returns a ;-separated String with OID;t/f;Geometry. Second value is 't'
	 * if this node was created during attribute assignment. The Geometry is
	 * WKT-formatted.
	 */
	public String toCsvString() {
		return this.oid + ";" + ((this.extended) ? "t" : "f") + ";" + this.point.toText();
	}
	
	/**
	 * Adds a String OID to the list of incoming links.
	 */
	public void setIncoming(String refLinkOid) {
		if (!(this.incoming.contains(refLinkOid))) {
			this.incoming.add(refLinkOid);
		}
	}

	/**
	 * Adds a String OID to the list of outgoing links.
	 */
	public void setOutgoing(String refLinkOid) {
		if (!(this.outgoing.contains(refLinkOid))) {
			this.outgoing.add(refLinkOid);
		}
	}

	/**
	 * Removes an OID String from the list of incoming links.
	 */
	public void removeIncoming(String refLinkOid) {
		if (this.incoming.contains(refLinkOid)) {
			this.incoming.remove(refLinkOid);
		}
	}
	
	/**
	 * Removes an OID String from the list of outgoing links.
	 */
	public void removeOutGoing(String refLinkOid) {
		if (this.outgoing.contains(refLinkOid)) {
			this.outgoing.remove(refLinkOid);
		}
	}
	
	
	/**
	 * returns the number of outgoing links.
	 */
	public int nbIncoming() {
		return this.incoming.size();
	}

	/**
	 * returns the number of outgoing links.
	 */
	public int nbOutgoing() {
		return this.outgoing.size();
	}

	/**
	 * Note turn from <b>from</b> to <b>to</b> as forbidden.
	 */
	public void forbidTurn(String from, String to) {
		if (!(from.equals(to))) {
			this.forbiddenTurns.add(new Pair<String, String>(from, to));
		}
	}

	/**
	 * Returns true if a turn is forbidden
	 */
	public boolean forbiddenTurn(String from, String to) {
		return this.forbiddenTurns.contains(new Pair<String, String>(from, to));
	}
}
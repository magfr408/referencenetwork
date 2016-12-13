package refnet;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.vividsolutions.jts.io.ParseException;

import io.SQLDatabaseReader;
import refnet.RefNetwork;

/**
 * This class manages the build process of a RefNetwork, including attribute
 * selection and addition.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @version 1.0
 */
public class BuildRefNet {

	private RefNetwork refNet;
	private SQLDatabaseReader dbr;

	public BuildRefNet(String host, int port, String name, String user, String password, int today,
			String[] regions, String loggerPath, String logFileName) {

		this.dbr = new SQLDatabaseReader(host, port, name, user, password, 1);

		this.refNet = createRefNetwork(this.dbr, today, regions, loggerPath,
		 logFileName);
		//this.refNet = createRefNetworkSodraLanken(this.dbr, loggerPath, logFileName);
		System.out.println("Info: Done loading and sanity-checking data from DB.");
		System.out.println("Info: Loaded " + this.networkSize() + " parts.");
	}

	/**
	 * Calls the creation of a new RefNetwork with parts which lies within a
	 * whole region (län).
	 * 
	 * @param dbr
	 * @param today
	 * @param region
	 * @return
	 */
	public static RefNetwork createRefNetwork(SQLDatabaseReader dbr, int today, String[] regions, String loggerPath,
			String logFileName) {

		ResultSet result = null;

		try {
			System.out.println("Info: Loading data from DB.");
			result = dbr.getNetworkByRegion(today, regions);
			RefNetwork rn = new RefNetwork(result, loggerPath, logFileName);
			return rn;
		} catch (SQLException se) {
			System.out.println("BuildRefNet: Failed to retrieve data from DB, shutting down.");
			dbr.closeConnection();
			se.printStackTrace();
			System.exit(0);
		} catch (ParseException pe) {
			System.out.println("BuildRefNet: Failed to parse WKT-string to Geometry, shutting down.");
			dbr.closeConnection();
			pe.printStackTrace();
			System.exit(0);
		} catch (ClassCastException cce) {
			System.out.println(
					"BuildRefNet: Loaded geometry other than LINESTRING from DB. Don't know what to do. Shutting down.");
			dbr.closeConnection();
			cce.printStackTrace();
			cce.printStackTrace();
			System.exit(0);
		} catch (IOException ioe) {
			System.out.println("BuildRefNet: Could not create logger. Don't know what to do. Shutting down.");
			dbr.closeConnection();
			ioe.printStackTrace();
			ioe.printStackTrace();
			System.exit(0);
		} finally {
			try {
				result.close();
			} catch (SQLException e) {
				/* Nothing */
			}
		}

		return null;
	}

	/**
	 * Used for testing.
	 * @param dbr
	 * @param today YYYYMMDD integer
	 * @param polygonWKT bounding polygon of selection
	 * @param SRID SRID of bounding polygon
	 * @param loggerPath
	 * @param logFileName
	 * @return
	 */
	public static RefNetwork createRefNetwork(SQLDatabaseReader dbr, int today, String polygonWKT, int SRID, String loggerPath, String logFileName) {

		ResultSet result = null;

		try {
			System.out.println("Info: Loading data from DB.");
			result = dbr.getNetworkByPolygon(today, polygonWKT, SRID);
			RefNetwork rn = new RefNetwork(result, loggerPath, logFileName);
			return rn;
		} catch (SQLException se) {
			System.out.println("BuildRefNet: Failed to retrieve data from DB, shutting down.");
			dbr.closeConnection();
			se.printStackTrace();
			System.exit(0);
		} catch (ParseException pe) {
			System.out.println("BuildRefNet: Failed to parse WKT-string to Geometry, shutting down.");
			dbr.closeConnection();
			pe.printStackTrace();
			System.exit(0);
		} catch (ClassCastException cce) {
			System.out.println(
					"BuildRefNet: Loaded geometry other than LINESTRING from DB. Don't know what to do. Shutting down.");
			dbr.closeConnection();
			cce.printStackTrace();
			cce.printStackTrace();
		} catch (IOException ioe) {
			System.out.println("BuildRefNet: Could not create logger. Don't know what to do. Shutting down.");
			dbr.closeConnection();
			ioe.printStackTrace();
			ioe.printStackTrace();
			System.exit(0);
		} finally {
			try {
				result.close();
			} catch (SQLException e) {
				/* Nothing */
			}
		}

		return null;		
	}
	
	/**
	 * Used for testing.
	 */
	public static RefNetwork createRefNetworkSodraLanken(SQLDatabaseReader dbr, String loggerPath, String logFileName) {

		ResultSet result = null;

		try {
			System.out.println("Info: Loading data from DB.");
			result = dbr.getNetworkSodraLanken();
			RefNetwork rn = new RefNetwork(result, loggerPath, logFileName);
			return rn;
		} catch (SQLException se) {
			System.out.println("BuildRefNet: Failed to retrieve data from DB, shutting down.");
			dbr.closeConnection();
			se.printStackTrace();
			System.exit(0);
		} catch (ParseException pe) {
			System.out.println("BuildRefNet: Failed to parse WKT-string to Geometry, shutting down.");
			dbr.closeConnection();
			pe.printStackTrace();
			System.exit(0);
		} catch (ClassCastException cce) {
			System.out.println(
					"BuildRefNet: Loaded geometry other than LINESTRING from DB. Don't know what to do. Shutting down.");
			dbr.closeConnection();
			cce.printStackTrace();
			cce.printStackTrace();
		} catch (IOException ioe) {
			System.out.println("BuildRefNet: Could not create logger. Don't know what to do. Shutting down.");
			dbr.closeConnection();
			ioe.printStackTrace();
			ioe.printStackTrace();
			System.exit(0);
		} finally {
			try {
				result.close();
			} catch (SQLException e) {
				/* Nothing */
			}
		}

		return null;
	}

	public void clean() {
		this.refNet.clean();
	}

	/**
	 * Adds one attribute to the network.
	 * 
	 * @param today
	 *            integer on the format YYYYMMDD
	 * @param region
	 *            "länskod", Stockholms län = "AB"
	 * @param attributeType
	 *            "FUNCTIONAL_ROAD_CLASS", "LANES",
	 *            "FORBIDDEN_DRIVER_DIRECTION", "SPEED".
	 */
	public void addAttribute(int today, String[] region, String attributeType) {

		System.out.println("Loading one attribute " + attributeType + " from DB. It will be added to the network.");
		ResultSet res = null;

		try {
			if (attributeType.equalsIgnoreCase("FUNCTIONAL_ROAD_CLASS")) {
				res = this.dbr.getClassificationAll(today, region);
			} else if (attributeType.equalsIgnoreCase("LANES")) {
				res = this.dbr.getLanesAll(today, region);
			} else if (attributeType.equalsIgnoreCase("FORBIDDEN_DRIVER_DIRECTION")) {
				res = this.dbr.getForbiddedTravelDirectionAll(today, region);
			} else if (attributeType.equalsIgnoreCase("SPEED")) {
				res = this.dbr.getSpeedLimitKmPHWithDirectionAll(today, region);
			}

			// TODO: other attributes?

			this.refNet.addAttribute(res);
			
		} catch (SQLException e) {
			e.printStackTrace();
			this.closeConnections();
		} finally {

			System.out.println("Info: Network now has " + this.networkSize() + " parts.");
			try {
				res.close();
			} catch (SQLException e) {
				/* ignored */ }
		}
	}

	/**
	 * Prints all Parts of all RefLinks in the RefNetwork to the terminal.
	 * 
	 * @param withAttributes
	 *            indicates if the parts should be printed with attribute
	 *            fields.
	 */
	public void printParts(boolean withAttributes) {
		this.refNet.print(withAttributes);
	}

	/**
	 * @return the total number of parts in the RefNetwork.
	 */
	public int networkSize() {
		return this.refNet.getNumberOfParts();
	}

	public void writeRefNodesToFile(String path, String fileName) {
		System.out.println("Info: Writing RefNodes to file.");
		this.refNet.writeNodesToFile(path, fileName);
		System.out.println("Info: Done writing data to file.");
	}

	public void writeRefNetworkToFile(String path, String fileName, boolean withAttributes) {
		if (withAttributes) {
			System.out.println("Info: Writing data to file with attributes.");
		} else {
			System.out.println("Info: Writing data to file without attributes.");
		}
		this.refNet.writeToFile(path, fileName, withAttributes);
		System.out.println("Info: Done writing data to file.");
	}

	public static void main(String[] args) {

		String[] attributes = new String[] { "FUNCTIONAL_ROAD_CLASS", "LANES", "FORBIDDEN_DRIVER_DIRECTION", "SPEED" };

		String[] regions = {"E"};
		int today = 20160603;

		BuildRefNet builder = new BuildRefNet("localhost", 5455, "mms", "XXXX", "XXXX", today, regions,
				"C:\\Users\\rasri17\\Desktop\\", "log.txt");

		for (int i = 0; i < attributes.length; i++) {
			builder.addAttribute(today, regions, attributes[i]);
		}

		builder.writeRefNetworkToFile("C:\\Users\\rasri17\\Desktop\\refnet\\",
				"refnet_E-lan_dirty_" + today + ".csv", true);
		builder.writeRefNodesToFile("C:\\Users\\rasri17\\Desktop\\refnet\\",
				"refnodes_E-lan_dirty_" + today + ".csv");

		builder.clean();

		builder.writeRefNetworkToFile("C:\\Users\\rasri17\\Desktop\\refnet\\",
				"refnet_E-lan_clean_" + today + ".csv", true);
		builder.writeRefNodesToFile("C:\\Users\\rasri17\\Desktop\\refnet\\",
				"refnodes_E-lan_clean_" + today + ".csv");

		builder.close();
	}

	private void close() {
		this.closeNetwork();
		this.closeConnections();
	}

	private void closeNetwork() {
		this.refNet.closeLogger();
	}

	public void closeConnections() {
		this.dbr.closeConnection();
	}
}

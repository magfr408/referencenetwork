package io;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Simple SQL database reader class with prepared statements for retrieving data
 * from NetDB.
 * 
 * @author Magnus Fransson, magnus.fransson@sweco.se
 * @version 1.0
 */
public class SQLDatabaseReader {
	private String connectionURL;
	private Connection conn;
	private PreparedStatement psFunctionalRoadClass;
	private PreparedStatement psFunctionalRoadClassLong;
	private PreparedStatement psLanesLong;
	private PreparedStatement psForbiddenDriveDirLong;
	private PreparedStatement psSpeedLong;
	private PreparedStatement psNetwork;
	private PreparedStatement psNetworkSodraLanken;
	private static final String queryFunctionalRoadClass = 
			"SELECT \"RLID\" AS REFLINK_OID, "
		  + "\"STARTAVST\" AS MEASURE_FROM, "
		  + "\"SLUTAVST\" AS MEASURE_TO, "
		  + "\"KLASS\"::integer AS functional_road_class, "
		  + "ST_AsText(ST_LineMerge (ST_Force2D(geom))) AS GEOM "
		  + "FROM nvdb.functional_road_class "
		  + "WHERE \"RLID\" IN (?) "
		  + "AND ? between \"FRAN_DATUM\" AND \"TILL_DATUM\" "
		  + "ORDER BY MEASURE_FROM ASC";
	
	private static final String quryFunctionalRoadClassLong = 
			"SELECT \"RLID\" AS REFLINK_OID, "
		  + "\"STARTAVST\" AS MEASURE_FROM, "
		  + "\"SLUTAVST\" AS MEASURE_TO, "
		  + "\"KLASS\"::integer AS functional_road_class, "
		  + "ST_AsText(ST_LineMerge (ST_Force2D(geom))) AS GEOM "
		  + "FROM nvdb.functional_road_class "
		  + "WHERE \"RLID\" IN ( "
		  	+ "SELECT \"REFLINK_OI\" AS REFLINK_OID "
		  	+ "FROM nvdb.ref_link_part "
		  	+ "WHERE ? between \"FROM_DATE\" AND \"TO_DATE\" "
		  	+ "AND county_id IN( "
	          	+ "(SELECT value "
	            + "FROM unnest(?::character varying[]) AS county_id(value))) "
		  	+ "GROUP BY REFLINK_OID ) "
	  	+ "AND ? between \"FRAN_DATUM\" AND \"TILL_DATUM\" "
	  	+ "ORDER BY REFLINK_OID ASC, MEASURE_FROM ASC";
	
	private static final String queryLanesLong = 
			"SELECT \"RLID\" AS REFLINK_OID, "
		  + "\"STARTAVST\" AS MEASURE_FROM, "
		  + "\"SLUTAVST\" AS MEASURE_TO, "
		  + "\"KOEFAETSAL\" as lanes, "
		  + "ST_AsText(ST_LineMerge (ST_Force2D(geom))) AS GEOM "
		  + "FROM nvdb.number_of_lanes "
		  + "WHERE \"RLID\" IN ( "
		  	+ "SELECT \"REFLINK_OI\" AS REFLINK_OID "
		  	+ "FROM nvdb.ref_link_part "
		  	+ "WHERE ? between \"FROM_DATE\" AND \"TO_DATE\" "
		  	+ "AND county_id IN( "
          		+ "(SELECT value "
          		+ "FROM unnest(?::character varying[]) AS county_id(value))) "
		  	+ "GROUP BY REFLINK_OID ) "
		  + "AND ? between \"FRAN_DATUM\" AND \"TILL_DATUM\" "
		  + "ORDER BY REFLINK_OID ASC, MEASURE_FROM ASC";
	
	private static final String queryForbiddenDriveDirLong = 
			"SELECT \"RLID\" AS REFLINK_OID, "
		  + "\"STARTAVST\" AS MEASURE_FROM, "
		  + "\"SLUTAVST\" AS MEASURE_TO, "
		  + "CASE WHEN \"RIKTNING\" = 'Med' THEN 1 "
		  + "	  WHEN \"RIKTNING\" = 'Mot' THEN 2 "
		  + "	  ELSE 3"
		  + "END AS forbidden_direction, "
		  + "ST_AsText(ST_LineMerge (ST_Force2D(geom))) AS GEOM "
		  + "FROM nvdb.prohibited_direction_of_travel "
		  + "WHERE \"RLID\" IN ( "
		  	+ "SELECT \"REFLINK_OI\" AS REFLINK_OID "
		  	+ "FROM nvdb.ref_link_part "
		  	+ "WHERE ? between \"FROM_DATE\" AND \"TO_DATE\" "
		  	+ "AND county_id IN( "
          		+ "(SELECT value "
          		+ "FROM unnest(?::character varying[]) AS county_id(value))) "
		  	+ "GROUP BY REFLINK_OID ) "
		  + "AND ? between \"FRAN_DATUM\" AND \"TILL_DATUM\" "
		  + "ORDER BY REFLINK_OID ASC, MEASURE_FROM ASC;";
	
	private static final String querySpeedLimLong = 
			"SELECT \"RLID\" AS REFLINK_OID, "
		  + "\"STARTAVST\" AS MEASURE_FROM, "
		  + "\"SLUTAVST\" AS MEASURE_TO, "
		  + "\"HTHAST\"::double precision AS speed, "
		  + "CASE WHEN \"RIKTNING\" = 'Med' THEN 1 "
		  + "	  WHEN \"RIKTNING\" = 'Mot' THEN 2 "
		  + "	  ELSE 3"
		  + "END AS speed_direction, "
		  + "ST_AsText(ST_LineMerge (ST_Force2D(geom))) AS GEOM "
		  + "FROM nvdb.speed_limit "
		  + "WHERE \"RLID\" IN ( "
		  	+ "SELECT \"REFLINK_OI\" AS REFLINK_OID "
		  	+ "FROM nvdb.ref_link_part "
		  	+ "WHERE ? between \"FROM_DATE\" AND \"TO_DATE\" "
		  	+ "AND county_id IN( "
	          	+ "(SELECT value "
	            + "FROM unnest(?::character varying[]) AS county_id(value))) "
		  	+ "GROUP BY REFLINK_OID ) "
		  + "AND ? between \"FRAN_DATUM\" AND \"TILL_DATUM\" "
		  + "ORDER BY REFLINK_OID ASC, MEASURE_FROM ASC;";
	
	private static final String queryNetwork = 
			"SELECT \"REFLINK_OI\" AS REFLINK_OID, "
		  + "\"FROM_MEASU\" AS MEASURE_FROM, "
		  + "\"TO_MEASURE\" AS MEASURE_TO, "
		  + "\"FROM_REFNO\" AS REFNODE_OID_FROM, "
		  + "\"TO_REFNODE\" AS REFNODE_OID_TO, "
		  + "ST_AsText(ST_LineMerge (ST_Force2D(links.geom))) AS GEOM, "
		  + "ST_Length(links.geom) AS GEOMETRIC_LENGTH, "
		  + "\"FROM_DATE\" "
		  + "FROM nvdb.ref_link_part AS links "
		  + "INNER JOIN nvdb.road_traffic_network AS net "
		  + "ON(\"RLID\" = \"REFLINK_OI\") "
		  + "WHERE county_id IN( "
          	+ "(SELECT value "
            + "FROM unnest(?::character varying[]) AS county_id(value))) "
		  + "AND ? between \"FROM_DATE\" AND \"TO_DATE\""
		  + "AND \"NÄTTYP\" = 1;";
	
	private static final String queryNetworkSodraLanken = 
			"SELECT \"REFLINK_OI\" AS REFLINK_OID, "
		  + "\"FROM_MEASU\" AS MEASURE_FROM, "
		  + "\"TO_MEASURE\" AS MEASURE_TO, "
		  + "\"FROM_REFNO\" AS REFNODE_OID_FROM, "
		  + "\"TO_REFNODE\" AS REFNODE_OID_TO, "
		  + "ST_AsText(ST_LineMerge (ST_Force2D(geom))) AS GEOM, "
		  + "ST_Length(geom) AS GEOMETRIC_LENGTH, "
		  + "\"FROM_DATE\" "
		  + "FROM nvdb.ref_link_part "
		  + "WHERE ? between \"FRAN_DATUM\" AND \"TILL_DATUM\" "
		  + "AND REFLINK_OID IN ('16499:1188', '16499:1188', '19191:2562', '19191:2562', '19192:1322864', '19192:1322864', '19192:1322876', '19192:1322876', '19192:1322880', '19192:1322880', '16499:2316', '16499:2316', '16499:2316', '16499:2322', '16499:2322', '16499:2322', '16499:2322', '16506:1862', '16506:1862', '16506:1876', '16506:1876', '16499:847', '16499:847', '16499:267', '16499:267', '16499:445', '16499:445', '16499:453', '16499:453', '16499:529', '16499:529', '16499:567', '16499:567', '16499:577', '16499:577', '16499:1209', '16499:1209', '16499:1209', '16499:1209', '16499:1209', '16499:1333', '16499:1333', '13465:30261', '13465:30261', '16507:4669', '16507:4669', '16507:4719', '16507:4719', '16507:4729', '16507:4729', '16507:4739', '16507:4739', '16507:4759', '16507:4759', '16507:4767', '16506:821', '16506:779', '16506:811', '16506:811', '16506:821', '16507:2569', '16507:3963', '16507:3963', '16507:4767', '16507:5403', '16507:6040', '16507:6047', '16507:6047', '16507:6105', '16507:6105', '16507:6120', '16507:6120', '16507:6231', '16507:6231', '16507:6397', '16507:6397', '16507:6494', '16507:6494', '16507:15068', '16507:2389', '16507:2389', '16507:3973', '16507:3973', '16507:4809', '16507:4809', '16507:4819', '16507:4835', '16507:4835', '16507:4923', '16507:5388', '16507:5388', '16507:5398', '16507:5398', '16507:5403', '16507:5403', '16507:5403', '16507:5648', '16507:5648', '16507:5648', '16507:5742', '16507:5742', '16507:5742', '16507:5815', '16507:5815', '16507:5815', '16507:5879', '16507:5881', '16507:5920', '16507:5920', '16507:5941', '16507:5941', '16507:6040', '16507:6307', '16507:6307', '16507:6323', '16507:6323', '16506:779', '16506:779', '16506:1147', '16506:1147', '16506:1147', '16506:1597', '16506:1750', '16506:1750', '16506:1750', '16506:2108', '16506:2108', '16507:1505', '16507:1505', '16507:1533', '16507:1533', '16507:1541', '16507:2366', '16507:2366', '16507:2379', '16507:2508', '16507:2508', '13636:3893', '13636:3921', '13636:4033', '13636:4033', '13636:4036', '13636:4036', '13637:1266', '16495:506', '16495:506', '16495:637', '16495:637', '16495:730', '16495:730', '16495:770', '16495:770', '16495:842', '16495:842', '16495:1015', '16495:1015', '16495:1411', '16495:1411', '16495:1481', '16495:1481', '16495:1575', '16495:1631', '16495:1631', '16495:1631', '16495:1749', '16495:1749', '16495:2066', '16499:419', '16499:429', '16499:429', '16499:627', '16499:639', '16499:673', '16499:986', '16499:1021', '16499:1269', '16499:1269', '16499:1269', '16499:1540', '16499:1540', '16499:1540', '16499:1540', '16499:1540', '16499:1738', '16499:1750', '16499:2055', '16499:2067', '16499:2083', '16499:2090', '16499:2090', '16499:2103', '16499:2114', '16499:2136', '16499:2200', '16499:2209', '16499:2283', '16506:293', '16506:293', '16506:311', '16506:311', '16506:683', '16506:683', '16506:683', '13471:30628', '13471:30628', '13471:30636', '13471:30642', '13471:30646', '13636:3739', '13636:3743', '13636:3755', '13636:3759', '13636:3763', '13636:3767', '13636:3783', '13636:3787', '13636:3797', '13636:3801', '13636:3803', '13636:3853', '13636:3855', '13636:3859', '13636:3873', '13636:3877', '13636:3885', '13636:3913', '13636:3917', '12740:2721', '12740:6185', '12740:6185', '12740:7389', '12740:7389', '10935:6279', '10935:6279', '10935:6279', '16507:4929', '19192:1322862', '19192:1322866', '19192:1322868', '19192:1322870', '19192:1322872', '19192:1322874', '19192:1322878', '19191:2977', '19191:3120', '19191:3122', '19191:3124', '16499:1586', '16499:1594', '16499:1013', '16506:935', '16506:943', '16506:951', '16506:957', '16506:961', '16506:967', '16506:973', '16506:973', '16506:979', '16506:985', '16506:991', '16506:999', '16506:1007', '16506:1017', '16506:1027', '16507:4917', '16507:4843', '16507:4851', '16507:4861', '16507:4871', '16507:4911', '16507:5848', '16507:5865', '16507:5869', '16507:5888', '16507:5896', '16507:5904', '16507:6052', '16507:6074', '16507:6074', '16507:6083', '16507:6151', '16507:6241', '16507:6301', '16507:6435', '16507:1465', '16507:1475', '16507:1483', '16507:1491', '16507:1497', '16507:1515', '16507:1523', '16507:2133', '16507:2133', '16507:2521', '16507:3857', '16507:5372', '16507:5382', '16507:5751', '16507:5759', '16507:5765', '16506:1768', '16506:1776', '16506:1936', '16506:1944', '16506:1998', '16506:2013', '16506:2097', '16506:2112', '16506:2146', '16506:623', '16506:629', '16506:675', '16506:689', '16506:803', '16506:839', '16506:851', '16506:887', '16506:1049', '16506:1430', '16506:1436', '16506:1467', '16506:1473', '16506:1604', '16506:1617', '16506:1623', '16506:1716', '16506:1721', '16506:1727', '16499:1715', '16499:1729', '16499:2311', '16499:2332', '16499:2369', '16499:2518', '16506:45', '16506:55', '16506:63', '16506:71', '16506:81', '16506:91', '16506:99', '16506:105', '16506:111', '16506:119', '16506:127', '16506:143', '16506:303', '16506:371', '16506:379', '16506:503', '16506:511', '16506:519', '16506:527', '16506:533', '16506:537', '16506:617', '16506:759', '16499:557', '16499:597', '16499:605', '16499:651', '16499:663', '16499:1278', '16499:1284', '16499:1288', '16499:1294', '16499:1302', '16499:1310', '16499:1316', '16499:1341', '16499:1371', '16499:1518', '16499:1576', '16499:1614', '16499:1646', '16499:1878', '16499:1928', '16499:2128', '16499:277', '16499:285', '16499:305', '16499:311', '16499:317', '13636:3845', '13636:3847', '13636:3851', '13636:3897', '13636:3901', '13636:3905', '13636:3909', '13471:30512', '13471:30526', '13471:30650', '13471:30654', '13471:30660', '13636:3791', '13636:3807', '13636:3811', '13636:3815', '13636:3819', '13636:3837', '13636:3841', '13465:29896', '13465:30251', '13465:30257', '13465:30267', '13465:30273', '12584:13846', '12584:14093', '12584:14112', '12584:14156', '19191:2267350', '19191:2267354', '19191:2267356', '16499:2444', '16528:6', '16499:619', '16499:619', '12735:48421', '12735:48428', '12735:48428', '1000:82027', '1000:82923', '1000:82923', '1000:82923', '1000:82923', '1000:82929', '1000:82929', '10935:6287', '10935:6289', '12740:2681', '12740:2715', '12740:2727', '12740:2735', '12740:8667', '12740:8677', '12740:9673', '12740:9673', '12740:10656', '12740:10658', '13471:27908', '13471:30400', '13471:31727', '13636:3747', '13636:3771', '13636:3775', '13636:3779', '13636:3863', '13636:3867', '13636:3869', '13636:3881', '13636:3962', '13636:3975', '16111:197', '16111:237', '16111:586', '16495:86', '16495:184', '16495:196', '16495:206', '16495:214', '16495:224', '16495:234', '16495:248', '16495:258', '16495:266', '16495:322', '16495:330', '16495:336', '16495:344', '16495:356', '16495:368', '16495:408', '16495:420', '16495:430', '16495:438', '16495:460', '16495:472', '16495:480', '16495:488', '16495:621', '16495:643', '16495:651', '16495:659', '16495:667', '16495:722', '16495:738', '16495:746', '16495:754', '16495:762', '16495:778', '16495:786', '16495:794', '16495:802', '16495:810', '16495:818', '16495:826', '16495:834', '16495:878', '16495:948', '16495:1009', '16495:1022', '16495:1030', '16495:1128', '16495:1136', '16495:1142', '16495:1147', '16495:1147', '16495:1147', '16495:1158', '16495:1158', '16495:1166', '16495:1166', '16495:1172', '16495:1184', '16495:1190', '16495:1281', '16495:1287', '16495:1305', '16495:1305', '16495:1334', '16495:1342', '16495:1364', '16495:1369', '16495:1375', '16495:1383', '16495:1391', '16495:1399', '16495:1405', '16495:1418', '16495:1436', '16495:1440', '16495:1444', '16495:1507', '16495:1530', '16495:1534', '16495:1538', '16495:1542', '16495:1548', '16495:1558', '16495:1564', '16495:1594', '16495:1602', '16495:1612', '16495:1646', '16495:1674', '16495:1674', '16495:1705', '16495:1705', '16495:1715', '16495:1715', '16495:1730', '16495:1755', '16495:1786', '16495:1786', '16495:1796', '16495:1796', '16495:1813', '16495:1813', '16495:1820', '16495:1825', '16495:1832', '16495:1834', '16495:1838', '16495:1882', '16495:1886', '16495:1890', '16495:1908', '16495:1910', '16495:1914', '16495:1918', '16495:1920', '16495:1926', '16495:1928', '16495:1932', '16495:1934', '16495:1948', '16495:1952', '16495:1956', '16495:1970', '16495:1989', '16495:2000', '16495:2055', '16507:3917', '16507:3927', '16507:3945', '16507:3955', '16507:3983', '16507:3989', '16507:3997', '16507:4009', '16507:4019', '16507:4027', '16507:4479', '16507:4489', '16507:4537', '16507:4607', '16507:4615', '16507:4623', '16507:4631', '16507:4645', '16507:4661', '16507:4675', '16507:4683', '16507:4699', '16507:4709', '16507:4747', '16507:4753', '16507:4795', '16507:4801', '16507:4827', '16507:4877', '16507:4933', '16507:5001', '16507:5394', '16507:5413', '16507:5432', '16507:5439', '16507:5447', '16507:5455', '16507:5461', '16507:5809', '16507:5949', '16507:5974', '16507:5983', '16507:5988', '16507:5996', '16507:6001', '16507:6009', '16507:6013', '16507:6018', '16507:6022', '16507:6030', '16507:6224', '16507:6265', '16507:6269', '16507:6277', '16507:6282', '16507:6379', '16507:6379', '16507:6420', '16507:6420', '16507:6447', '16507:6447', '16507:6483', '16507:6485', '16507:6489', '19191:2659', '19191:2661', '19191:2267358', '19191:2267358', '13636:3889', '16507:4373', '16507:6385', '19191:2267346', '19191:2267348', '16499:549', '16499:1522', '16499:1522', '16499:2224', '16499:2224', '16499:2224', '16499:2224', '16499:2224', '16499:2224', '12735:48421', '19191:2203950', '16495:1985', '16495:1985', '16495:1985', '1000:82031', '16506:877', '16506:1486', '16506:1486', '13465:31455', '13465:31455', '13465:31455', '16111:1052', '16111:1052', '16499:587', '16499:613', '12735:48366', '1000:82031', '1000:82031', '1000:82035', '1000:82035', '1000:82035', '1000:82049', '1000:82049', '1000:82049', '1000:82056', '1000:82056', '1000:82056', '16499:2326')";

	/**
	 * Default constructor, tries to make connection to DB and initializes all
	 * prepared statements.
	 * 
	 * Uses net.sourceforge.jtds.jdbc.Driver Connection URL is
	 * "jdbc:jtds:sqlserver://<b>host<b>;instance=<b>instance</b>;databaseName=
	 * <b>name</b> 
	 * 
	 * @param host
	 * @param instance
	 * @param name
	 * @param user
	 * @param password
	 * @param conCase
	 */
	public SQLDatabaseReader(String host, int port, String name, String user, String password, int conCase) {
		try {
			if (conCase == 1) {

				// import driver
				Class.forName("org.postgresql.Driver");
				// make connection to database
				this.connectionURL = "jdbc:postgresql://" + host + ":" 
									+ port + "/" + name;
				this.conn = DriverManager.getConnection(connectionURL, user, password);

				// setup prepared statements.
				this.psFunctionalRoadClass = this.conn.prepareStatement(queryFunctionalRoadClass);
				this.psFunctionalRoadClassLong = this.conn.prepareStatement(quryFunctionalRoadClassLong);
				this.psLanesLong = this.conn.prepareStatement(queryLanesLong);
				this.psForbiddenDriveDirLong = this.conn.prepareStatement(queryForbiddenDriveDirLong);
				this.psSpeedLong = this.conn.prepareStatement(querySpeedLimLong);
				this.psNetwork = this.conn.prepareStatement(queryNetwork);
				this.psNetworkSodraLanken = this.conn.prepareStatement(queryNetworkSodraLanken);

			} else {
				this.connectionURL = null;
				this.conn = null;
				System.out.println("core.SQLDatabaseReader.java: no such connection case (conCase): " + conCase);
				System.out.println("Shutting down.");
				System.exit(0);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(e.getLocalizedMessage());
			System.out.println(e.getSQLState());
		}
	}

	/**
	 * Passes values to prepared statement that reads reflinks for Södra Länken
	 * in Stockholm.
	 * 
	 * @return a <b>ResultSet</b> with columns REFLINK_OID (SQL-Varchar),
	 *         MEASURE_FROM(SQL-float), MEASURE_TO(SQL-float),
	 *         REFNODE_OID_FROM(SQL-Varchar), REFNODE_OID_TO(SQL-Varchar),
	 *         GEOM(SQL-Varchar [wkt]), GEOMETRIC_LENGTH(SQL-float),
	 *         VALID_FROM(int)
	 * @throws SQLException
	 *             if failed to clear parameters of prepared statement, or if
	 *             failed to set new parameters of prepared statement, or if
	 *             failed to execute query.
	 */
	public ResultSet getNetworkSodraLanken() throws SQLException {
		this.psNetworkSodraLanken.clearParameters();

		this.psNetworkSodraLanken.setInt(1, 20151117);
		this.psNetworkSodraLanken.setString(2, "AB");
		this.psNetworkSodraLanken.setInt(3, 20151117);

		return this.psNetworkSodraLanken.executeQuery();
	}

	/**
	 * Passes values to prepared statement that reads a road network for any
	 * Swedish region (län).
	 * 
	 * @param today
	 *            YYYYMMDD integer.
	 * @param regionArray
	 *            Stockholm = "AB".
	 * @return a <b>ResultSet</b> with columns REFLINK_OID (SQL-Varchar),
	 *         MEASURE_FROM(SQL-float), MEASURE_TO(SQL-float),
	 *         REFNODE_OID_FROM(SQL-Varchar), REFNODE_OID_TO(SQL-Varchar),
	 *         GEOM(SQL-Varchar [wkt]), GEOMETRIC_LENGTH(SQL-float),
	 *         VALID_FROM(int)
	 * @throws SQLException
	 *             if failed to clear parameters of prepared statement, or if
	 *             failed to set new parameters of prepared statement, or if
	 *             failed to execute query.
	 */
	public ResultSet getNetworkByRegion(int today, String[] regionArray) throws SQLException {
		this.psNetwork.clearParameters();

		Array regions = this.conn.createArrayOf("varchar", regionArray);
		this.psNetwork.setArray(1, regions);
		this.psNetwork.setInt(2, today);


		return this.psNetwork.executeQuery();
	}

	/**
	 * Passes values to prepared statement that reads the attribute (företeelse)
	 * functional_road_class (funktionell vägklass) for all reflinks in any
	 * Swedish region (län).
	 * 
	 * @param today
	 *            YYYYMMDD integer.
	 * @param regionArray
	 *            Stockholm = "AB".
	 * @return a <b>ResultSet</b> with columns REFLINK_OID (SQL-Varchar),
	 *         MEASURE_FROM(SQL-float), MEASURE_TO(SQL-float),
	 *         functional_road_class (int) GEOM(SQL-Varchar [wkt])
	 * @throws SQLException
	 *             if failed to clear parameters of prepared statement, or if
	 *             failed to set new parameters of prepared statement, or if
	 *             failed to execute query.
	 */
	public ResultSet getClassificationAll(int today, String[] regionArray) throws SQLException {
		this.psFunctionalRoadClassLong.clearParameters();

		this.psFunctionalRoadClassLong.setInt(1, today);
		
		Array regions = this.conn.createArrayOf("varchar", regionArray);
		this.psFunctionalRoadClassLong.setArray(2, regions);
		
		this.psFunctionalRoadClassLong.setInt(3, today);
		
		System.out.println(this.psFunctionalRoadClassLong.toString());
		return this.psFunctionalRoadClassLong.executeQuery();
	}

	/**
	 * Passes values to prepared statement that reads the attribute (företeelse)
	 * functional_road_class (funktionell vägklass) for any REFLINK_OID.
	 * 
	 * @param today
	 *            YYYYMMDD integer.
	 * @param region
	 *            Stockholm = "AB".
	 * @return a <b>ResultSet</b> with columns REFLINK_OID (SQL-Varchar),
	 *         MEASURE_FROM(SQL-float), MEASURE_TO(SQL-float),
	 *         functional_road_class (int) GEOM(SQL-Varchar [wkt])
	 * @throws SQLException
	 *             if failed to clear parameters of prepared statement, or if
	 *             failed to set new parameters of prepared statement, or if
	 *             failed to execute query.
	 */
	public ResultSet getClassification(int today, String refLinkOid) throws SQLException {
		this.psFunctionalRoadClass.clearParameters();

		this.psFunctionalRoadClass.setString(1, refLinkOid);
		this.psFunctionalRoadClass.setInt(2, today);
		
		return this.psFunctionalRoadClass.executeQuery();
	}

	/**
	 * Passes values to prepared statement that reads the attribute (företeelse)
	 * number of lanes for all reflinks in any Swedish region (län).
	 * 
	 * @param today
	 *            YYYYMMDD integer.
	 * @param region
	 *            Stockholm = "AB".
	 * @return a <b>ResultSet</b> with columns REFLINK_OID (SQL-Varchar),
	 *         MEASURE_FROM(SQL-float), MEASURE_TO(SQL-float), lanes (int)
	 *         GEOM(SQL-Varchar [wkt])
	 * @throws SQLException
	 *             if failed to clear parameters of prepared statement, or if
	 *             failed to set new parameters of prepared statement, or if
	 *             failed to execute query.
	 */
	public ResultSet getLanesAll(int today, String[] regionArray) throws SQLException {
		this.psLanesLong.clearParameters();

		this.psLanesLong.setInt(1, today);
		
		Array regions = this.conn.createArrayOf("varchar", regionArray);
		this.psLanesLong.setArray(2, regions);
		this.psLanesLong.setInt(3, today);

		return this.psLanesLong.executeQuery();
	}

	/**
	 * Passes values to prepared statement that reads the attribute (företeelse)
	 * forbidden_direction (Förbjuden Färdriktning) for all reflinks in any
	 * Swedish region (län).
	 * 
	 * @param today
	 *            YYYYMMDD integer.
	 * @param region
	 *            Stockholm = "AB".
	 * @return a <b>ResultSet</b> with columns REFLINK_OID (SQL-Varchar),
	 *         MEASURE_FROM(SQL-float), MEASURE_TO(SQL-float),
	 *         forbidden_direction (int) GEOM(SQL-Varchar [wkt])
	 * @throws SQLException
	 *             if failed to clear parameters of prepared statement, or if
	 *             failed to set new parameters of prepared statement, or if
	 *             failed to execute query.
	 */
	public ResultSet getForbiddedTravelDirectionAll(int today, String[] regionArray) throws SQLException {
		this.psForbiddenDriveDirLong.clearParameters();

		this.psForbiddenDriveDirLong.setInt(1, today);
		
		Array regions = this.conn.createArrayOf("varchar", regionArray);
		this.psForbiddenDriveDirLong.setArray(2, regions);
		this.psForbiddenDriveDirLong.setInt(3, today);

		return this.psForbiddenDriveDirLong.executeQuery();
	}

	/**
	 * Passes values to prepared statement that reads the attribute (företeelse)
	 * speed (Högsta tillåtna hastighet) for all reflinks in any Swedish region
	 * (län).
	 * 
	 * @param today
	 *            YYYYMMDD integer.
	 * @param region
	 *            Stockholm = "AB".
	 * @return a <b>ResultSet</b> with columns REFLINK_OID (SQL-Varchar),
	 *         MEASURE_FROM(SQL-float), MEASURE_TO(SQL-float), speed
	 *         (SQL-float), speed_direction (int) GEOM(SQL-Varchar [wkt])
	 * @throws SQLException
	 *             if failed to clear parameters of prepared statement, or if
	 *             failed to set new parameters of prepared statement, or if
	 *             failed to execute query.
	 */
	public ResultSet getSpeedLimitKmPHWithDirectionAll(int today, String[] regionArray) throws SQLException {
		this.psSpeedLong.clearParameters();

		this.psSpeedLong.setInt(1, today);
		
		Array regions = this.conn.createArrayOf("varchar", regionArray);
		this.psSpeedLong.setArray(2, regions);
		this.psSpeedLong.setInt(3, today);

		return this.psSpeedLong.executeQuery();
	}

	public ResultSet read(String query) throws SQLException {
		Statement state = conn.createStatement();
		ResultSet result = state.executeQuery(query);
		return result;
	}

	/**
	 * Closes all prepared statements and connection.
	 */
	public void closeConnection() {
		if (this.psSpeedLong != null) {
			try {
				this.psSpeedLong.close();
			} catch (SQLException e) {
				/* ignored */ }
		}
		if (this.psFunctionalRoadClass != null) {
			try {
				this.psFunctionalRoadClass.close();
			} catch (SQLException e) {
				/* ignored */ }
		}
		if (this.psFunctionalRoadClassLong != null) {
			try {
				this.psFunctionalRoadClassLong.close();
			} catch (SQLException e) {
				/* ignored */ }
		}
		if (this.psLanesLong != null) {
			try {
				this.psLanesLong.close();
			} catch (SQLException e) {
				/* ignored */ }
		}
		if (this.psForbiddenDriveDirLong != null) {
			try {
				this.psForbiddenDriveDirLong.close();
			} catch (SQLException e) {
				/* ignored */ }
		}
		if (this.psNetwork != null) {
			try {
				this.psNetwork.close();
			} catch (SQLException e) {
				/* ignored */ }
		}
		if (this.psNetworkSodraLanken != null) {
			try {
				this.psNetworkSodraLanken.close();
			} catch (SQLException e) {
				/* ignored */ }
		}
		try {
			this.conn.close();
		} catch (SQLException e) {
			/* ignored */ }
	}

	public static void main(String[] args) {
		// Example usage for select statement

		SQLDatabaseReader sqldbr = new SQLDatabaseReader("PCSTH11541", 5432, "netdb", "gruck", "gruck", 1);

		try {
			//
			ResultSet result = sqldbr.read("select top 100 * from netdb.road.NET_RefLinkPart");
			while (result.next()) {
				System.out.println(result.getString("GID") + "\t" + result.getString("REFLINK_OID"));
			}
			sqldbr.closeConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			sqldbr.closeConnection();
		}
	}
}
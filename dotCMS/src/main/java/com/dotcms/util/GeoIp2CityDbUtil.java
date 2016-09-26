package com.dotcms.util;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.com.maxmind.geoip2.DatabaseReader;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.repackage.com.maxmind.geoip2.model.CityResponse;
import com.dotcms.repackage.com.maxmind.geoip2.record.City;
import com.dotcms.repackage.com.maxmind.geoip2.record.Country;
import com.dotcms.repackage.com.maxmind.geoip2.record.Subdivision;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.rules.conditionlet.Location;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * Provides utility methods to interact with the GeoIP2 API City Database. This
 * library lets developers discover geographic information about a specific IP
 * address, which is ideal for displaying content to Website users based on
 * different criteria, such as, city, state/province, country, language, postal
 * code, etc.
 * <p>
 * By default, Tomcat returns IPv6 addresses via the {@link HttpServletRequest}
 * object. This can be changed by adding the following parameter to the startup
 * command line of your Tomcat server:
 * <p>
 * {@code -Djava.net.preferIPv4Stack=true}
 * </p>
 * <p>
 * The GeoIP2 API is compatible with both IPv4 and IPv6 addresses. This
 * implementation uses a free of charge local <a
 * href="http://dev.maxmind.com/geoip/geoip2/geolite2/">GeoLite2 database</a>,
 * which is a file containing the IP-based geo-location information.
 * Administrators can download the file from the MaxMind Website and update such
 * a file as required.
 * <p>
 * For more information, please visit the <a
 * href="http://maxmind.github.io/GeoIP2-java/index.html">GeoIP2 Java API</a>
 * page.
 * </p>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-15-2015
 * 
 */
public class GeoIp2CityDbUtil {

	private static DatabaseReader databaseReader = null;
	private static long lastModified = 0;
	private static String dbPath = null;

	/**
	 * Singleton holder based on the initialization-on-demand approach.
	 */
	private static class SingletonHolder {

		private static GeoIp2CityDbUtil INSTANCE = new GeoIp2CityDbUtil(
				Config.getStringProperty("GEOIP2_CITY_DATABASE_PATH_OVERRIDE", Config.CONTEXT.getRealPath("/WEB-INF/geoip2/GeoLite2-City.mmdb")));

	}

	/**
	 * Returns a unique instance of the {@link GeoIp2CityDbUtil} class.
	 * 
	 * @return The {@link GeoIp2CityDbUtil} instance.
	 */
	public static GeoIp2CityDbUtil getInstance() {
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Private constructor that will initialize the connection to the local
	 * GeoIP2 database. If the database file is updated, it will have to be
	 * re-loaded. Therefore, its path is kept in memory.
	 * 
	 * @param databasePath
	 *            - The path to the database file in the file system.
	 * @throws DotRuntimeException
	 *             If the connection to the GeoIP2 database file cannot be
	 *             established.
	 */
	private GeoIp2CityDbUtil(String databasePath) {
		dbPath = databasePath;
		File database = new File(databasePath);
		connectToDatabase(database);
	}

	/**
	 * Establishes the connection with the IP database. If a previous connection
	 * has already been created, it will be closed. Such a scenario would mean
	 * that the database file has been updated, so the database reader must be
	 * re-built to load the new information.
	 * 
	 * @param database
	 *            - The {@link File} reference to the database file.
	 * @throws DotRuntimeException
	 *             If the connection to the GeoIP2 database file cannot be
	 *             established.
	 */
	private static void connectToDatabase(File database) {
		try {
			if (databaseReader != null) {
				databaseReader.close();
			}
			databaseReader = new DatabaseReader.Builder(database).build();
			lastModified = database.lastModified();
		} catch (IOException e) {
			Logger.error(GeoIp2CityDbUtil.class,
					"Connection to the GeoIP2 database could not be established.");
			throw new DotRuntimeException(
					"Connection to the GeoIP2 database could not be established.",
					e);
		}
	}

	/**
	 * Returns the {@link DatabaseReader} object used to perform the queries to
	 * the IP database.
	 * <p>
	 * When the class is initially instantiated, the modification date of the
	 * database file is kept in memory. This method will read the last modified
	 * date of the requested database file in order to determine whether it must
	 * be re-loaded or not. If it has to, synchronization will be used to load
	 * the new database file.
	 * </p>
	 * 
	 * @return The {@link DatabaseReader} object with the latest content of the
	 *         database.
	 */
	private static DatabaseReader getDatabaseReader() {
		File database = new File(dbPath);
		long fileLastModified = database.lastModified();
		if (fileLastModified != lastModified) {
			synchronized (GeoIp2CityDbUtil.class) {
				if (fileLastModified != lastModified) {
					connectToDatabase(database);
				}
			}
		}
		return databaseReader;
	}

	/**
	 * Returns the ISO code of the state, province or region (referred to as
	 * "subdivision") the specified IP address belongs to. The ISO code is a one
	 * or two-character representation (depending on the country) of the name of
	 * the subdivision.
	 * 
	 * @param ipAddress
	 *            - The IP address to get information from.
	 * @return The ISO code representing the state, province, or region.
	 * @throws UnknownHostException
	 *             If the IP address of a host could not be determined.
	 * @throws IOException
	 *             If the connection to the GeoIP2 service could not be
	 *             established, or the result object could not be created.
	 * @throws GeoIp2Exception
	 *             If the IP address is not present in the service database.
	 */
	public String getSubdivisionIsoCode(String ipAddress)
			throws UnknownHostException, IOException, GeoIp2Exception {
		InetAddress inetAddress = InetAddress.getByName(ipAddress);
		CityResponse city = getDatabaseReader().city(inetAddress);
		Subdivision subdivision = city.getMostSpecificSubdivision();
		return subdivision.getIsoCode();
	}

	/**
	 * Returns the ISO code of the country the specified IP address belongs to.
	 * The ISO code is a two-character representation of the name of the
	 * country.
	 * 
	 * @param ipAddress
	 *            - The IP address to get information from.
	 * @return The ISO code representing the country.
	 * @throws UnknownHostException
	 *             If the IP address of a host could not be determined.
	 * @throws IOException
	 *             If the connection to the GeoIP2 service could not be
	 *             established, or the result object could not be created.
	 * @throws GeoIp2Exception
	 *             If the IP address is not present in the service database.
	 */
	public String getCountryIsoCode(String ipAddress)
			throws UnknownHostException, IOException, GeoIp2Exception {
		InetAddress inetAddress = InetAddress.getByName(ipAddress);
		CityResponse city = getDatabaseReader().city(inetAddress);
		Country country = city.getCountry();
		return country.getIsoCode();
	}

	/**
	 * returns an instance of {@code Location} from ip address
	 * @param ipAddress the ip address to represent
	 * @return the location
	 * @throws IOException if the connection to the GeoIP2 service could not be
	 *  established, or the result object could not be created.
	 * @throws GeoIp2Exception if the IP address is not present in the service database.
     */
	public Location getLocationByIp(String ipAddress)
			throws IOException, GeoIp2Exception {
		InetAddress inetAddress = InetAddress.getByName(ipAddress);
		CityResponse city = getDatabaseReader().city(inetAddress);
		com.dotcms.repackage.com.maxmind.geoip2.record.Location location = city.getLocation();
		return new Location(location.getLatitude(), location.getLongitude());
	}

	/**
	 * Returns the name of the city the specified IP address belongs to.
	 * 
	 * @param ipAddress
	 *            - The IP address to get information from.
	 * @return The city name.
	 * @throws UnknownHostException
	 *             If the IP address of a host could not be determined.
	 * @throws IOException
	 *             If the connection to the GeoIP2 service could not be
	 *             established, or the result object could not be created.
	 * @throws GeoIp2Exception
	 *             If the IP address is not present in the service database.
	 */
	public String getCityName(String ipAddress) throws UnknownHostException,
			IOException, GeoIp2Exception {
		InetAddress inetAddress = InetAddress.getByName(ipAddress);
		CityResponse cityResponse = getDatabaseReader().city(inetAddress);
		City city = cityResponse.getCity();
		return city.getName();
	}

	/**
	 * Returns the {@link TimeZone} associated with location, as specified by
	 * the <a href="http://www.iana.org/time-zones">IANA Time Zone Database</a>.
	 * For example: {@code "America/New_York"}.
	 * 
	 * @param ipAddress
	 *            - The IP address to get information from.
	 * @return The associated {@link TimeZone} object.
	 * @throws UnknownHostException
	 *             If the IP address of a host could not be determined.
	 * @throws IOException
	 *             If the connection to the GeoIP2 service could not be
	 *             established, or the result object could not be created.
	 * @throws GeoIp2Exception
	 *             If the IP address is not present in the service database.
	 */
	public TimeZone getTimeZone(String ipAddress) throws UnknownHostException,
			IOException, GeoIp2Exception {
		InetAddress inetAddress = InetAddress.getByName(ipAddress);
		CityResponse city = getDatabaseReader().city(inetAddress);
		String zone = city.getLocation().getTimeZone();
		return TimeZone.getTimeZone(zone);
	}

	/**
	 * Returns the {@link Date} when the client issued the request. This
	 * information is obtained by adjusting the server's current date/time with
	 * the time zone the user is in.
	 * 
	 * @param ipAddress
	 *            - The IP address to get information from.
	 * @return The client's current {@link Date}.
	 * @throws UnknownHostException
	 *             If the IP address of a host could not be determined.
	 * @throws IOException
	 *             If the connection to the GeoIP2 service could not be
	 *             established, or the result object could not be created.
	 * @throws GeoIp2Exception
	 *             If the IP address is not present in the service database.
	 */
	public Calendar getDateTime(String ipAddress) throws UnknownHostException,
			IOException, GeoIp2Exception {
		TimeZone timeZone = getTimeZone(ipAddress);
		Calendar calendar = Calendar.getInstance(timeZone);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		long clientDateTime = new GregorianCalendar(year, month, day, hour,
				minute, second).getTimeInMillis();
		calendar.setTimeInMillis(clientDateTime);
		return calendar;
	}

}

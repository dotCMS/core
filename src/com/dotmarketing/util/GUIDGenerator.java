/*
 * GUIDGenerator.java
 *
 * Created on 29 August 2001, 09:41
 */

package com.dotmarketing.util;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.security.SecureRandom;

/**
 * This class is from: http://www.activescript.co.uk/
 * <p>
 * This GUID generator can be safely pooled on a single machine and deployed in
 * a cluster to provide completely scalable performance.
 * </p>
 * <p>
 * The problem of generating unique IDs can essentially be broken down as
 * uniqueness over space and uniqueness over time which, when combined, produces
 * a globally unique sequence.
 * </p>
 * <p>
 * Taking the UUID and GUID Internet standards draft for the Network Working
 * Group by Paul J. Leach, Microsoft, and Rich Salz, Certco, as a starting point
 * we assume that the GUID be represented as a 36-digit alphanumeric (including
 * hyphens) of the format xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.
 * </p>
 * <p>
 * The first 20 characters represent the uniqueness over time (the low 32-bits
 * of the time stamp as the first 8, the next 4 as the mid 16 bits of the time
 * stamp, the next 4 as the high value of the time stamp multiplexed with the
 * version, and the next 4 a combination of the lock sequence high -multiplexed
 * with the variant field - and the low bits)
 * </p>
 * <p>
 * Note: The internet guidelines suggest the timestamp as a 60-bit value to a
 * precision of 100ns since 00:00:00.00, 15 October 1582 (the date of Gregorian
 * reform to the Christian calendar)
 * </p>
 * <p>
 * The last 12 characters are a 48-bit node identifier usually implemented as
 * the IEEE 802 address, which gives uniqueness over space.
 * </p>
 * <p>
 * These are combined to produce a unique sequence. <br>
 * Some of the main problems in an EJB implementation of this technique are: -
 * <ul>
 * <li>1) there is no access to the traditional IEEE 802 address.</li>
 * <li>2) more than one object can exist at the same time and so generate the
 * same time stamp at a granularity of a millisecond (Java's timestamp
 * granularity).</li>
 * <li> 3) a clock sequence or equivalent is used as a way of reading/writing a
 * value to storage (e.g. a database or system file) that can be used in case
 * the clock is set backwards and as a seed for the number sequence. This is
 * even more of a problem when a cluster of machines do not use the same
 * database.</li>
 * <li>4) Singletons are not portable and not recommended in the EJB specs.</li>
 * </ul>
 * 
 * 
 * <p>
 * The GUID is constucted by.
 * <ul>
 * <li> 1) (1-8 hex characters) use the low 32 bits of
 * System.currentTimeMillis(). Note: could use the recommended date format by
 * adding 12219292800000L to the system time long before grabbing the low 32
 * bits. <br>
 * This gives us a uniqueness of a millisecond - therefore any clashing object
 * will have to be generated within the same millisecond. </li>
 * <li> 2) (9-16 hex characters) the IP address of the machine as a hex
 * representation of the 32 bit integer underlying IP - gives us a spatial
 * uniqueness to a single machine - guarantees that these characters will be
 * different for machines in a cluster or on a LAN. Note: This is not
 * appropriate for a global addressing scheme to distinguish java objects in any
 * JVM on the Internet. </li>
 * <li> 3) (17-24 hex characters) the hex value of the Stateless Session bean
 * object's hashCode (a 32 bit int) - in the Java language spec - the hashcode
 * value of Object is defined as - <I>As much as is reasonably practical, the
 * hashCode method defined by class object does return distinct integers for
 * distinct objects. (This is typically implemented by converting the internal
 * address of the object into an integer, but this implementation technique is
 * not required by the Java programming language.)</I>** </li>
 * 
 * <li> 4) (25-32 hex characters) a random 32 bit integer generated for each
 * method invocation from the SecureRandom java class using
 * SecureRandom.nextInt(). This method produces a cryptographically strong
 * pseudo-random integer. The Java lang defines this as - <I>Returns a
 * pseudo-random, uniformly distributed int value drawn from this random number
 * generator's sequence. The general contract of nextInt is that one int value
 * in the specified range is pseudorandomly generated and returned. All n
 * possible int values are produced with (approximately) equal probability.</I>**
 * </li>
 * </ul>
 * <p>
 * This gives us a value that is a combination of
 * <ul>
 * <li> 1) is unique to the millisecond</li>
 * <li>2) is unique to the machine</li>
 * <li>3) is unique to the object creating it</li>
 * <li>4) is unique to the method call for the same object</li>
 * </ul>
 * <p>
 * Note: the potential theoretical conflicts are:
 * <ul>
 * <li>1) that two objects on the same machine are assigned the exact same
 * hashCode (I do not know of any implementations that do this but there may be
 * some out there) and at the same millisecond must also get the same integer
 * value from the SecureRandom implementation.</li>
 * <li>2) The same int value is returned from the SecureRandom object in
 * subsequent method calls for the same object in the same millisecond.</li>
 * <li>3) A reset clock (which would require a redeployment of the bean) will
 * produce an identical hashcode for the new deployment as a previous one AND
 * the random values will have to be the same in the same repeated millisecond
 * value as in a previous sequence. </li>
 * </ul>
 * 
 * @author Steve Woodcock
 * @version 1.1
 */
public class GUIDGenerator {

	/** Creates new GUIDGenerator */
	public GUIDGenerator() throws Exception {

		try {
			StringBuffer stringbuffer = new StringBuffer();
			StringBuffer stringbuffer1 = new StringBuffer();
			seeder = new SecureRandom();
			InetAddress inetaddress = InetAddress.getLocalHost();
			byte abyte0[] = inetaddress.getAddress();
			String s = hexFormat(getInt(abyte0), 8);
			String s1 = hexFormat(hashCode(), 8);
			stringbuffer.append("-");
			stringbuffer1.append(s.substring(0, 4));
			stringbuffer.append(s.substring(0, 4));
			stringbuffer.append("-");
			stringbuffer1.append(s.substring(4));
			stringbuffer.append(s.substring(4));
			stringbuffer.append("-");
			stringbuffer1.append(s1.substring(0, 4));
			stringbuffer.append(s1.substring(0, 4));
			stringbuffer.append("-");
			stringbuffer1.append(s1.substring(4));
			stringbuffer.append(s1.substring(4));
			midValue = stringbuffer.toString();
			midValueUnformated = stringbuffer1.toString();
			int i = seeder.nextInt();
		} catch (Exception exception) {
			throw new Exception("error - failure to instantiate GUIDGenerator" + exception);
		}
	}

	/**
	 * <p>
	 * The private method that actually does the work. The String passed into
	 * the method is either the formatted or unformatted mid value which is
	 * combined with the low 32 bits (obtained by a bit wise &) of the time and
	 * the next value in the secureRandom sequence.
	 * </p>
	 * 
	 * @param s
	 *            The string containing the mid value of the required format for
	 *            the UUID.
	 * @return A string containing the UUID in the desired format.
	 * 
	 */
	private String getVal(String s) {
		int i = (int) System.currentTimeMillis() & 0xffffffff;
		int j = seeder.nextInt();
		return hexFormat(i, 8) + s + hexFormat(j, 8);
	}

	/**
	 * <p>
	 * Used to provide a UUID that does not conform to the GUID RFC. The String
	 * returned does not have the xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx format
	 * instead it is xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx. This is to provide a
	 * shorter version of the UUID for easier database manipulation.
	 * </p>
	 * <p>
	 * However, it is recommended that th full format be used.
	 * </P>
	 * 
	 * @throws RemoteException
	 *             Required to be thrown by the EJB specification.
	 * @return A String representing a UUID in the format
	 *         xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx. Each character in the string is
	 *         a hexadecimal.
	 */
	public String getUnformatedUUID() {
		return getVal(midValueUnformated);
	}

	/**
	 * <p>
	 * Returns a UUID formated according to the draft internet standard. See the
	 * class level documentation for more details.
	 * </P>
	 * 
	 * @return A String representing a UUID in the format
	 *         xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.
	 */
	public String getUUID() {
		return getVal(midValue);
	}

	/**
	 * <p>
	 * A utility method to take a byte array of 4 bytes and produce an int
	 * value. This is used to convert the quad xxx.xxx.xxx.xxx value of the IP
	 * address to the underlying 32-bit int that the ip address represents.
	 * There is no way to obtain this value in Java so we need to convert it
	 * ourselves. </P
	 * 
	 * @param abyte0
	 *            Th byte array containg 4 bytes that represent an IP address.
	 * @return An int that is the actual value of the ip address.
	 */
	private int getInt(byte abyte0[]) {
		int i = 0;
		int j = 24;
		for (int k = 0; j >= 0; k++) {
			int l = abyte0[k] & 0xff;
			i += l << j;
			j -= 8;
		}

		return i;
	}

	/**
	 * <p>
	 * A utility method to produce a correctly formatted hex string string from
	 * an int value and and an int specifying the length the hex string that
	 * represents the int value should be.
	 * </p>
	 * <p>
	 * Utilises both the padHex and toHexString methods.
	 * </p>
	 * 
	 * @param i
	 *            The int value that is to be transformed to a hex string.
	 * @param j
	 *            An int specifying the length of the hex string to be returned.
	 * @return A string that contains the formatted hex string.
	 */
	private String hexFormat(int i, int j) {
		String s = Integer.toHexString(i);
		return padHex(s, j) + s;
	}

	/**
	 * <p>
	 * A utility method that takes in a string of hex characters and prepends a
	 * number characters to the string to make up a string of the required
	 * length as defined in the int value passed into the method. This is
	 * because the values for say the hashcode on a lower memory machine will
	 * only be 4 characters long and so to the correct formatting is produced 0
	 * characters must be prepended to the fornt of the string.
	 * <p>
	 * 
	 * @param s
	 *            The String containing the hex values.
	 * @param i
	 *            The int specifying the length that the string should be.
	 * @return A String of the correct length containing the original hex value
	 *         and a number of pad zeros at the front of the string.
	 */
	private String padHex(String s, int i) {
		StringBuffer stringbuffer = new StringBuffer();
		if (s.length() < i) {
			for (int j = 0; j < i - s.length(); j++)
				stringbuffer.append("0");

		}
		return stringbuffer.toString();
	}

	/**
	 * <p>
	 * The random seed used in the method call to provide the required
	 * randomized element. The normal random class is not used as the sequences
	 * produced are more uniform than this implementation and will produce a
	 * predictable sequence which could lead to a greater chance of number
	 * clashes.
	 * <p>
	 */
	private SecureRandom seeder;

	/**
	 * <p>
	 * The cached mid value of the UUID. This consists of the hexadecimal
	 * version of the IP address of the machine and the object's hashcode. These
	 * are stored as -xxxx-xxxx-xxxx-xxxx to speed up the method calls. This
	 * value does not change over the lifespan of the object and so is able to
	 * be cached in this manner.
	 * <p>
	 */
	private String midValue;

	/**
	 * <p>
	 * The unformatted cached mid value of the UUID. This consists of the
	 * hexadecimal version of the IP address of the machine and the object's
	 * hashcode. These are stored as xxxxxxxxxxxxxxxx to speed up the method
	 * calls. This value does not change over the lifespan of the object and so
	 * is able to be cached in this manner. This vlaue is used to supply the
	 * middle part of the UUID for the unformatted method.
	 * <p>
	 */
	private String midValueUnformated;
}

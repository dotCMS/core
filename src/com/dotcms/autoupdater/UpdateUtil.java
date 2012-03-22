package com.dotcms.autoupdater;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.dotcms.autoupdater.ActivityIndicator;
import com.dotcms.autoupdater.Messages;
import com.dotcms.autoupdater.UpdateAgent;
import com.dotcms.autoupdater.UpdateException;


public class UpdateUtil {
	public static boolean unzipFile(File zipFile, String fileName, File destFile)
			throws ZipException, IOException {
		ZipFile zip = new ZipFile(zipFile);
		ZipEntry entry = zip.getEntry(fileName);
		if (entry == null) {
			return false;
		}
		InputStream is = zip.getInputStream(entry);
		int BUFFER = 1024;
		int count;
		byte data[] = new byte[BUFFER];

		File parentFile = destFile.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		FileOutputStream fos = new FileOutputStream(destFile);
		BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
		while ((count = is.read(data, 0, BUFFER)) != -1) {
			dest.write(data, 0, count);
		}
		dest.flush();
		dest.close();

		return true;
	}

	public static boolean unzipDirectory(File zipFile, String directoryName,
			String home, boolean dryrun) throws ZipException, IOException {
		ActivityIndicator.startIndicator();
		FileInputStream fis = new FileInputStream(zipFile);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));

		ZipEntry entry;
		BufferedOutputStream dest = null;
		int BUFFER = 1024;
		while ((entry = zis.getNextEntry()) != null) {
			if (!entry.isDirectory()) {
				if (directoryName == null
						|| entry.getName().startsWith(directoryName)) {
					UpdateAgent.logger
							.debug(Messages
									.getString("FileUpdater.debug.extract.file") + entry ); //$NON-NLS-1$

					if (!dryrun) {

						int count;
						byte data[] = new byte[BUFFER];
						File destFile = new File(home + File.separator
								+ entry.getName());
						UpdateAgent.logger.debug(destFile.getAbsoluteFile());
						File parentFile = destFile.getParentFile();
						if (!parentFile.exists()) {
							parentFile.mkdirs();
						}
						FileOutputStream fos = new FileOutputStream(destFile);
						dest = new BufferedOutputStream(fos, BUFFER);
						while ((count = zis.read(data, 0, BUFFER)) != -1) {
							dest.write(data, 0, count);
						}
						dest.flush();
						dest.close();
					}

				}
			}
		}
		ActivityIndicator.endIndicator();
		return true;
	}

	public static Properties getInnerFileProps(File zipFile)
			throws ZipException, IOException, UpdateException {
		// Lets look for the dotCMS jar inside the zip file

		FileInputStream fis = new FileInputStream(zipFile);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
		ZipEntry entry;
		BufferedOutputStream dest = null;

		while ((entry = zis.getNextEntry()) != null) {
			String entryName1 = entry.getName();
			if (entryName1.startsWith("dotCMS/WEB-INF/lib/dotcms_") //$NON-NLS-1$
					&& !entryName1.startsWith("dotCMS/WEB-INF/lib/dotcms_ant")) { //$NON-NLS-1$
				// We found it
				ZipInputStream zis2 = new ZipInputStream(
						new BufferedInputStream(zis));
				ZipEntry entry2;
				while ((entry2 = zis2.getNextEntry()) != null) {
					String entryName2 = entry2.getName();
					// Let's look inside to see if we find it
					if (entryName2
							.equals("com/liferay/portal/util/build.properties")) { //$NON-NLS-1$
						Properties props = new Properties();
						props.load(zis2);
						return props;

					}
				}
			}
		}
		throw new UpdateException(Messages
				.getString("UpdateUtil.error.no.version"), //$NON-NLS-1$
				UpdateException.ERROR);
	}

	public static Integer getFileMinorVersion(File zipFile)
			throws ZipException, IOException, UpdateException {
		Properties props = getInnerFileProps(zipFile);
		String prop = props.getProperty("dotcms.release.build"); //$NON-NLS-1$

		if (prop != null && !prop.equals("")) { //$NON-NLS-1$
			Integer i = Integer.parseInt(prop);
			return i;
		}
		throw new UpdateException(Messages
				.getString("UpdateUtil.error.no.version"), //$NON-NLS-1$
				UpdateException.ERROR);

	}

	public static String getFileMayorVersion(File zipFile) throws ZipException,
			IOException, UpdateException {
		Properties props = getInnerFileProps(zipFile);
		String prop = props.getProperty("dotcms.release.version"); //$NON-NLS-1$
		return prop;
	}

	public static String getMD5(File f) throws IOException {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
			InputStream is = new FileInputStream(f);
			byte[] buffer = new byte[8192];
			int read = 0;
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}

		} catch (NoSuchAlgorithmException e) {
			UpdateAgent.logger.debug(
					"NoSuchAlgorithmException: " + e.getMessage(), e); //$NON-NLS-1$
		}
		byte[] md5sum = digest.digest();
		BigInteger bigInt = new BigInteger(1, md5sum);
		return bigInt.toString(16);
	
	}

}

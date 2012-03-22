package com.dotcms.autoupdater;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import com.dotcms.autoupdater.ActivityIndicator;
import com.dotcms.autoupdater.Messages;
import com.dotcms.autoupdater.UpdateAgent;
import com.dotcms.autoupdater.UpdateException;
import com.dotcms.autoupdater.UpdateUtil;


/**
 * This class backups and updates the file system.
 * 
 * @author andres
 * 
 */
public class FileUpdater {

	public static Logger logger;

	final int BUFFER = 1024;

	private File file;
	
	//We only delete files that are on one of these locations
	private String[] delLocations={"src/","dotCMS/html/","WEB-INF/lib/"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	public FileUpdater(File file, String home, String backupFile) {
		super();
		this.file = file;
		this.home = home;
		this.backupFile = backupFile;
		logger = UpdateAgent.logger;
	}

	private String home;
	private String backupFile;
	private List<String> delList;


	public boolean doUpdate(boolean dryrun) throws IOException, UpdateException {
		getDelList();
		verifyWritePermissions();
		doBackup(dryrun);
		processData(dryrun);

		return true;

	}
	
	private boolean verifyWritePermissions() throws UpdateException{
		List<String> missingPerms=new ArrayList<String>();
		for (String fileName : delList) {
			
			String fullFileName=home+File.separator+fileName;
			File file=new File(fullFileName);
			if (file.exists() && !file.canWrite()) {
				missingPerms.add(fileName);
			}
		}
		
		
		try {
			FileInputStream fis = new FileInputStream(file);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry;
			
			
			while ((entry = zis.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					
						File destFile=new File(home
								+ File.separator + entry.getName());
						File parentFile =destFile.getParentFile();
						if (destFile.exists() && !destFile.canWrite()) {
							missingPerms.add(entry.getName());
							logger.debug("Missing write permission on: " + entry.getName());
						} else {
							//Recurse to see if the parent has write permissions
							boolean done=false;
							while (!done) {
							if (parentFile.exists()) {
								done=true;
								if (!parentFile.canWrite()) {
									missingPerms.add(parentFile.getAbsolutePath());
									logger.debug("Missing write permission on: " + parentFile.getAbsolutePath());
								} 
							} else {
								parentFile=parentFile.getParentFile();
							}
						}
						}
				}
			}
		} catch (FileNotFoundException e) {
			logger.debug("FileNotFoundException: " + e.getMessage(),e); //$NON-NLS-1$
		} catch (IOException e) {
			logger.debug("IOException: " + e.getMessage(),e); //$NON-NLS-1$
		}
		
		if (missingPerms.size()==0) {
			return true;
		}
		StringBuffer sb=new StringBuffer();
		if (missingPerms.size()==1) {
			sb.append(Messages.getString("FileUpdater.error.file.permission")); //$NON-NLS-1$
		} else {
			sb.append(Messages.getString("FileUpdater.error.files.permissions")); //$NON-NLS-1$
		}
		
		if (UpdateAgent.isDebug) {
			throw new UpdateException( Messages.getString("FileUpdater.debug.file.permission"),UpdateException.ERROR);  //$NON-NLS-1$
		}
		
		if (missingPerms.size()<=10) {

			for (String fileName:missingPerms) {
				sb.append(fileName+"\n"); //$NON-NLS-1$
			}
		} else {
			if (missingPerms.size()==11) {
				for (String fileName:missingPerms) {
					sb.append(fileName+"\n"); //$NON-NLS-1$
				}	
			} else {
				for (int i=0;i<10;i++) {
					sb.append(missingPerms.get(i)+"\n");					 //$NON-NLS-1$
				}
				int left=missingPerms.size()-10;
				sb.append(Messages.getString("FileUpdater.text.other.files", left+"",UpdateAgent.logFile)); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
		}
		
		throw new UpdateException(sb.toString(),UpdateException.ERROR);
	}

	public void getDelList() throws IOException {
		logger.debug("Starting getDelList" );
		ZipFile zip=new ZipFile(file);
		ZipEntry entry=zip.getEntry("dellist"); //$NON-NLS-1$
		
		delList = new ArrayList<String>();
		if (entry!=null) {
			logger.debug("dellist found");
			InputStream is= zip.getInputStream(entry);
			InputStreamReader isr=new InputStreamReader(is);
			BufferedReader br=new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				delList.add(line);
			}	
		} else {
			logger.debug("No dellist found");
		}
		logger.debug("Finished getDelList" );
		

	}

	private void doBackup(boolean dryrun) throws IOException {
		logger.info(Messages.getString("FileUpdater.debug.start.backup")); //$NON-NLS-1$
		ActivityIndicator.startIndicator();
		ZipOutputStream out = null;
		FileInputStream fis = new FileInputStream(file);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
		ZipEntry entry;
		boolean hasEntry=false;
		if (!dryrun) {
			out = new ZipOutputStream(new FileOutputStream(backupFile));
		}

		while ((entry = zis.getNextEntry()) != null) {
			if (!entry.isDirectory()) {
				if (!entry.getName().equalsIgnoreCase("dellist")) { //$NON-NLS-1$
					File oldFile = new File(home + File.separator
							+ entry.getName());
					if (oldFile.exists() && !oldFile.isDirectory()) {
						logger.debug(Messages.getString("FileUpdater.debug.backup.file") + entry); //$NON-NLS-1$
						if (!dryrun) {
							backup(out, oldFile, entry.getName());
							
						}
						hasEntry=true;
					}
				}

			}
		}
		ActivityIndicator.endIndicator();
		if (hasEntry) {
			if (!dryrun) {
				out.close();
			}
		logger.info(Messages.getString("FileUpdater.debug.end.backup"));  //$NON-NLS-1$
		} else {
			logger.info(Messages.getString("FileUpdater.text.no.backup")); //$NON-NLS-1$
		}

	}

	public void processData(boolean dryrun) throws IOException {
		
		File jardir = new File(home+File.separator+"dotCMS"+File.separator+"WEB-INF/lib/");
		logger.debug("Looking for previous dotcms jars: " + jardir.getAbsolutePath()); //$NON-NLS-1$
		Pattern pat = Pattern.compile("dotcms_(\\d.\\d.*).jar");
		for(File jar:jardir.listFiles()){
			Matcher mat = pat.matcher(jar.getName());
			if(mat.matches()){
				logger.debug("Found file: " + jar.getName());
				if (!dryrun) {
					logger.debug("Deleting file: " + jar.getAbsolutePath());
					jar.delete();
				}
			}

		}
		
	
		if (delList!=null) {
			logger.info(Messages.getString("FileUpdater.debug.start.delete.files")); //$NON-NLS-1$
			ActivityIndicator.startIndicator();
			for (String fileName : delList) {
				if (isDeletable(fileName)) {
				String fullFileName=home+File.separator+fileName;
				logger.debug("Looking for file: " + fullFileName); //$NON-NLS-1$
				File file=new File(fullFileName);
				if (file.exists()) {		
					
					logger.debug(Messages.getString("FileUpdater.debug.delete.file") + fullFileName); //$NON-NLS-1$
					if (!dryrun) {
						file.delete();
					}
				}
				}
				
			}
			ActivityIndicator.endIndicator();
			logger.info(Messages.getString("FileUpdater.debug.end.delete.files")); //$NON-NLS-1$
		}
		
		logger.info(Messages.getString("FileUpdater.debug.start.replace.files")); //$NON-NLS-1$

		UpdateUtil.unzipDirectory(file, null, home, dryrun);
		logger.info(Messages.getString("FileUpdater.debug.end.replace.files")); //$NON-NLS-1$
	

	}

	private boolean isDeletable(String fileName) {
		for (String stub : delLocations ) {
			if (fileName.startsWith(stub)) {
				return true;
			}
		}
		return false;
	}

	private void backup(ZipOutputStream out, File f, String entry)
			throws IOException {
		FileInputStream in = new FileInputStream(f);

		// Add ZIP entry to output stream.
		out.putNextEntry(new ZipEntry(entry));
		byte[] buf = new byte[1024];

		// Transfer bytes from the file to the ZIP file
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}

		// Complete the entry
		out.closeEntry();
		in.close();

	}
	

}

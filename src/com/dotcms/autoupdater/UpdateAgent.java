package com.dotcms.autoupdater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import com.dotcms.autoupdater.ActivityIndicator;
import com.dotcms.autoupdater.AntInvoker;
import com.dotcms.autoupdater.DownloadProgress;
import com.dotcms.autoupdater.FileUpdater;
import com.dotcms.autoupdater.Messages;
import com.dotcms.autoupdater.PostProcess;
import com.dotcms.autoupdater.SSLFactory;
import com.dotcms.autoupdater.UpdateAgent;
import com.dotcms.autoupdater.UpdateException;
import com.dotcms.autoupdater.UpdateOptions;
import com.dotcms.autoupdater.UpdateUtil;



public class UpdateAgent {

	String[] homeCheckElements = { "build.xml", "dotCMS/WEB-INF/web.xml"}; //$NON-NLS-1$ //$NON-NLS-2$

	String preProcessHome = "build_temp"; //$NON-NLS-1$

	public static Logger logger;
	public static boolean isDebug=false;
	public static String logFile;

	private String url;
	private String home;
	private String version;
	private String minor;
	private String backupFile;
	private String proxy;
	private String proxyUser;
	private String proxyPass;
	private boolean allowTestingBuilds;

	private String confirmText = Messages.getString("UpdateAgent.text.confirm"); //$NON-NLS-1$
	private String helpText = Messages.getString("UpdateAgent.text.help"); //$NON-NLS-1$
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new UpdateAgent().run(args);
	}

	UpdateOptions updateOptions;

	public void run(String[] args) {
		updateOptions = new UpdateOptions();
		Options options = updateOptions.getOptions();
		// create the parser
		CommandLineParser parser = new GnuParser();
		CommandLine line=null;
		try {
			// parse the command line arguments
			line = parser.parse(options, args);

		} catch (MissingOptionException e) { 
			System.err.println(Messages.getString("UpdateAgent.error.command.missing.options")); //$NON-NLS-1$
			List<String> list=e.getMissingOptions();
			for (String item:list) {
				System.err.println(item);
			}
			return;
		} catch (ParseException e) {
			System.err.println(Messages.getString("UpdateAgent.error.command.parsing") + e.getMessage()); //$NON-NLS-1$
			
			return;
			
		} 
		try {

			configureLogger(line);

			if (line.hasOption(UpdateOptions.HELP) || args.length == 0) {
				printHelp(options);
				return;
			}
			allowTestingBuilds = Boolean.parseBoolean(line.getOptionValue(UpdateOptions.ALLOW_TESTING_BUILDS));
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyMMdd_HHmm"); //$NON-NLS-1$
			
			proxy = line.getOptionValue(UpdateOptions.PROXY);
			proxyUser = line.getOptionValue(UpdateOptions.PROXY_USER);
			proxyPass = line.getOptionValue(UpdateOptions.PROXY_PASS);
			url = line.getOptionValue(UpdateOptions.URL, updateOptions
					.getDefault("update.url", "")); //$NON-NLS-1$ //$NON-NLS-2$
			home = line.getOptionValue(UpdateOptions.HOME, System
					.getProperty("user.dir")); //$NON-NLS-1$
			logger.info(Messages.getString("UpdateAgent.text.dotcms.home") + home); //$NON-NLS-1$
			checkHome(home);
			checkRequisites(home);
			String newMinor=""; //$NON-NLS-1$
			version = getVersion();
			minor = getMinor();
			backupFile = line.getOptionValue(UpdateOptions.BACKUP,
					"update_backup_b"+minor+"_" + sdf.format(new Date()) + ".zip"); //$NON-NLS-1$ //$NON-NLS-2$
			if (!backupFile.endsWith(".zip")) { //$NON-NLS-1$
				backupFile += ".zip"; //$NON-NLS-1$
			}
			String agentVersion = getAgentVersion();
			logger.debug(Messages.getString("UpdateAgent.text.autoupdater.version") + agentVersion); //$NON-NLS-1$

			File f = null;
			
			if(line.hasOption(UpdateOptions.SPECIFIC_VERSION)){
				logger.info(Messages.getString("UpdateAgent.text.your.version") + version + " / " + minor + " Version " + line.getOptionValue(UpdateOptions.SPECIFIC_VERSION)); //$NON-NLS-1$ //$NON-NLS-2$
				Map<String, String> map = new HashMap<String, String>();
				map.put("version", version); //$NON-NLS-1$
				map.put("buildNumber", minor); //$NON-NLS-1$
				map.put("specificVersion", line.getOptionValue(UpdateOptions.SPECIFIC_VERSION)); //$NON-NLS-1$
				if(allowTestingBuilds){
					map.put("allowTestingBuilds", "true"); //$NON-NLS-1$
				}
				PostMethod method = doGet(url, map);
				int ret = method.getStatusCode();
				if (ret == 200) {
					// Get the version of the jar.
					try {
						newMinor = method
								.getResponseHeader("Minor-Version") //$NON-NLS-1$
								.getValue();
						if(newMinor.trim().length() > 0){
							String[] minorArr = newMinor.split("_");
							if(minorArr.length > 1){
								logger.info(Messages.getString("UpdateAgent.text.latest.version") + minorArr[0] + " / " + minorArr[1] );//$NON-NLS-1$
							}else{
								logger.info(Messages.getString("UpdateAgent.text.latest.version") + newMinor );//$NON-NLS-1$
							}
							logger.info(" ");//$NON-NLS-1$
						}else{
							throw new Exception();
						}
					} catch (Exception e) {
						logger.debug(
								Messages.getString("UpdateAgent.error.no.minor.version"), //$NON-NLS-1$
								e);
						throw new UpdateException(
								Messages.getString("UpdateAgent.error.no.minor.version"), //$NON-NLS-1$
								UpdateException.ERROR);
					}

					String fileName = "update_" + newMinor //$NON-NLS-1$ //$NON-NLS-2$
							+ ".zip"; //$NON-NLS-1$
					f = new File(home + File.separator + "updates" //$NON-NLS-1$
							+ File.separator + fileName);
					if (f.exists()) {
						//check md5 of file
						String MD5 = null;
						boolean hasMD5 = false;
						if (method.getResponseHeader("Content-MD5") != null && !method.getResponseHeader("Content-MD5").equals("") && !method.getResponseHeader("Content-MD5").equals("null")) { //$NON-NLS-1$
							MD5 = method.getResponseHeader("Content-MD5").getValue(); //$NON-NLS-1$
							if(!MD5.equals("")){
								hasMD5 = true;
							}
						}
						if(hasMD5){
							String dlMD5 = UpdateUtil.getMD5(f);
							logger.debug(Messages.getString("UpdateAgent.debug.server.md5") + MD5); //$NON-NLS-1$
							logger.debug(Messages.getString("UpdateAgent.debug.file.md5") + dlMD5); //$NON-NLS-1$

							if (MD5 == null || MD5.length() == 0 || !dlMD5.equals(MD5)) {
								logger
								.fatal(Messages.getString("UpdateAgent.error.md5.failed")); //$NON-NLS-1$
								throw new UpdateException(
										Messages.getString("UpdateAgent.error.file.exists") + fileName, //$NON-NLS-1$
										UpdateException.ERROR);	
							}
						} else {
							// file verified, let's use it
							logger.info(f.getName() + ": " + Messages.getString("UpdateAgent.text.md5.verified") ); //$NON-NLS-1$ //$NON-NLS-2$
						}

					} else {
						//Create the updates directory
						if (!f.getParentFile().exists()) {
							f.getParentFile().mkdirs();
						}
						// Download the content
						download(f, method, version);
					}

				} else {
					switch (ret) {
					case 204:
						throw new UpdateException(Messages.getString("UpdateAgent.text.dotcms.uptodate"), //$NON-NLS-1$
								UpdateException.SUCCESS);

					case 401:
						throw new UpdateException(
								Messages.getString("UpdateAgent.error.login.failed"), //$NON-NLS-1$
								UpdateException.ERROR);
					case 403:
						throw new UpdateException(
								Messages.getString("UpdateAgent.error.login.failed"), //$NON-NLS-1$
								UpdateException.ERROR);
					default:
						throw new UpdateException(
								Messages.getString("UpdateAgent.error.unexpected.http.code") //$NON-NLS-1$
										+ ret, UpdateException.ERROR);
					}
				}

				
			}else if (line.hasOption(UpdateOptions.FILE)) {
				logger.info(Messages.getString("UpdateAgent.text.your.version") + version + " / " + minor + " file " + line.getOptionValue(UpdateOptions.FILE)); //$NON-NLS-1$ //$NON-NLS-2$
				// Use user provided file
				f = new File(home + File.separator + "updates" //$NON-NLS-1$
						+ File.separator +line.getOptionValue(UpdateOptions.FILE));
				if (!f.exists()) {
					throw new UpdateException(Messages.getString("UpdateAgent.error.file.not.found"), //$NON-NLS-1$
							UpdateException.ERROR);
				}
				
				// Get the version locally
				String fileMajor = UpdateUtil.getFileMayorVersion(f);
				/*
				if (!fileMajor.equalsIgnoreCase(version)) {
					throw new UpdateException(Messages.getString("UpdateAgent.error.file.wrong.version") + fileMajor,UpdateException.ERROR); //$NON-NLS-1$
				}
				*/
				
				Integer fileMinor = UpdateUtil.getFileMinorVersion(f);
				
				logger.info(Messages.getString("UpdateAgent.text.file.version") + version + " / " //$NON-NLS-1$ //$NON-NLS-2$
						+ fileMinor);
				newMinor=fileMinor+""; //$NON-NLS-1$
						
				logger.info(" ");//$NON-NLS-1$
				
			} else {
				
				if (!line.hasOption(UpdateOptions.NO_UPDATE)) {
					// Check to see if new version of the updater exists
					File newAgent = downloadAgent();
					if (newAgent != null) {
						// Exit, we need the
						logger
								.info(Messages.getString("UpdateAgent.text.new.autoupdater")); //$NON-NLS-1$
						System.exit(0);
					}

				}
			
				
				// Download file
              
				logger.info(Messages.getString("UpdateAgent.text.your.version") + version + " / " + minor); //$NON-NLS-1$ //$NON-NLS-2$

				if (f == null) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("version", version); //$NON-NLS-1$
					map.put("buildNumber", minor); //$NON-NLS-1$
					if(allowTestingBuilds){
						map.put("allowTestingBuilds", "true"); //$NON-NLS-1$
					}
					PostMethod method = doGet(url, map);
					int ret = method.getStatusCode();
					if (ret == 200) {
						// Get the version of the jar.
						try {
							newMinor = method
									.getResponseHeader("Minor-Version") //$NON-NLS-1$
									.getValue();
							if(newMinor.trim().length() > 0){
								String[] minorArr = newMinor.split("_");
								if(minorArr.length > 1){
									logger.info(Messages.getString("UpdateAgent.text.latest.version") + minorArr[0] + " / " + minorArr[1] );//$NON-NLS-1$
								}else{
									logger.info(Messages.getString("UpdateAgent.text.latest.version") + newMinor );//$NON-NLS-1$
								}
								logger.info(" ");//$NON-NLS-1$
							}else{
								throw new Exception();
							}
						} catch (Exception e) {
							logger.debug(
									Messages.getString("UpdateAgent.error.no.minor.version"), //$NON-NLS-1$
									e);
							throw new UpdateException(
									Messages.getString("UpdateAgent.error.no.minor.version"), //$NON-NLS-1$
									UpdateException.ERROR);
						}

						String fileName = "update_" + newMinor //$NON-NLS-1$ //$NON-NLS-2$
								+ ".zip"; //$NON-NLS-1$
						f = new File(home + File.separator + "updates" //$NON-NLS-1$
								+ File.separator + fileName);
						if (f.exists()) {
							//check md5 of file
							String MD5 = null;
							boolean hasMD5 = false;
							if (method.getResponseHeader("Content-MD5") != null && !method.getResponseHeader("Content-MD5").equals("") && !method.getResponseHeader("Content-MD5").equals("null")) { //$NON-NLS-1$
								MD5 = method.getResponseHeader("Content-MD5").getValue(); //$NON-NLS-1$
								if(!MD5.equals("")){
									hasMD5 = true;
								}
							}
							if(hasMD5){
								String dlMD5 = UpdateUtil.getMD5(f);
								logger.debug(Messages.getString("UpdateAgent.debug.server.md5") + MD5); //$NON-NLS-1$
								logger.debug(Messages.getString("UpdateAgent.debug.file.md5") + dlMD5); //$NON-NLS-1$

								if (MD5 == null || MD5.length() == 0 || !dlMD5.equals(MD5)) {
									logger
									.fatal(Messages.getString("UpdateAgent.error.md5.failed")); //$NON-NLS-1$
									throw new UpdateException(
											Messages.getString("UpdateAgent.error.file.exists") + fileName, //$NON-NLS-1$
											UpdateException.ERROR);	
								}
							} else {
								// file verified, let's use it
								logger.info(f.getName() + ": " + Messages.getString("UpdateAgent.text.md5.verified") ); //$NON-NLS-1$ //$NON-NLS-2$
							}

						} else {
							//Create the updates directory
							if (!f.getParentFile().exists()) {
								f.getParentFile().mkdirs();
							}
							// Download the content
							download(f, method, version);
						}

					} else {
						switch (ret) {
						case 204:
							throw new UpdateException(Messages.getString("UpdateAgent.text.dotcms.uptodate"), //$NON-NLS-1$
									UpdateException.SUCCESS);

						case 401:
							throw new UpdateException(
									Messages.getString("UpdateAgent.error.login.failed"), //$NON-NLS-1$
									UpdateException.ERROR);
						case 403:
							throw new UpdateException(
									Messages.getString("UpdateAgent.error.login.failed"), //$NON-NLS-1$
									UpdateException.ERROR);
						default:
							throw new UpdateException(
									Messages.getString("UpdateAgent.error.unexpected.http.code") //$NON-NLS-1$
											+ ret, UpdateException.ERROR);
						}
					}

				} 
			}
				if (f != null && f.exists()) {
					// Preprocess
					preProcess(f, home);

					// Ask for confirmation
					confirm(line);

					// Extract files

					processData(f, home, backupFile, line
							.hasOption(UpdateOptions.DRY_RUN));

					postProcess(home, line.hasOption(UpdateOptions.DRY_RUN));
					throw new UpdateException(Messages.getString("UpdateAgent.text.dotcms.dotcms.updated"  ) + version + " / " //$NON-NLS-1$ //$NON-NLS-2$
							+ newMinor, UpdateException.SUCCESS);//$NON-NLS-1$
							

				}

			

		} catch (IOException e) {
			//Just in case it was left running
			ActivityIndicator.endIndicator();
			
			if (isDebug) {
				logger.debug("IOException: ", e);	 //$NON-NLS-1$
			} else {
				logger.error(Messages.getString("UpdateAgent.error.downloading") + e.getMessage()); //$NON-NLS-1$
			}
			
			logger.info(" ");//$NON-NLS-1$
			
		} catch (UpdateException e) {
			//Just in case it was left running
			ActivityIndicator.endIndicator();
			if (e.getType() != UpdateException.CANCEL) {
				if (e.getType() == UpdateException.ERROR) {
					if (isDebug) {
						logger.debug("UpdateException: ", e);	 //$NON-NLS-1$
					} else {
						logger.error(Messages.getString("UpdateAgent.error.updating") + e.getMessage()); //$NON-NLS-1$
					}
				} else {

					logger.info(e.getMessage());
				}
			}
			
			logger.info(" ");//$NON-NLS-1$
		}

	}

	private void confirm(CommandLine line) throws UpdateException {
		// Ask for confirmation
		boolean doUpgrade = false;
		if (line.hasOption(UpdateOptions.FORCE)
				|| line.hasOption(UpdateOptions.DRY_RUN)) {
			doUpgrade = true;
		} else {
			doUpgrade = confirmUI();
		}

		if (!doUpgrade) {
			throw new UpdateException(Messages.getString("UpdateAgent.cancel.user"), //$NON-NLS-1$
					UpdateException.CANCEL);
		}
	}

	private boolean processData(File f, String home, String backupFile,
			boolean dryrun) throws IOException, UpdateException {
		FileUpdater fileUpdater = new FileUpdater(f, home, backupFile);
		return fileUpdater.doUpdate(dryrun);
	}

	private boolean postProcess(String home, boolean dryrun) {
		if (dryrun) {
			logger.info(Messages.getString("UpdateAgent.text.dryrun")); //$NON-NLS-1$
			return true;
		} else {
			logger.info(Messages.getString("UpdateAgent.debug.start.post.process")); //$NON-NLS-1$			
			PostProcess pp = new PostProcess();
			pp.setHome(home);
			pp.postProcess(false);
			logger.info(Messages.getString("UpdateAgent.debug.end.post.process")); //$NON-NLS-1$
			return true;
		}
	}

	private boolean preProcess(File updateFile, String home)
			throws ZipException, IOException, UpdateException {
		logger.info(Messages.getString("UpdateAgent.debug.start.validation")); //$NON-NLS-1$

		//First if we don't have the ant jars, we extract them.  This is a pretty ugly hack, but there's no way to guarantee the user already has them
		File antLauncher = new File(home + File.separator + "bin" + File.separator + "ant" + File.separator + "ant-launcher.jar"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (!antLauncher.exists()) {
			logger.debug(Messages.getString("UpdateAgent.debug.extracting.ant")); //$NON-NLS-1$
			UpdateUtil.unzipDirectory(updateFile, "bin/ant", home, false); //$NON-NLS-1$
			
		}
		
		// Find if we have a build.xml in the update. If we do, we use that one.
		// Otherwise we use the current one
		File buildFile = new File(home + File.separator + "build_new.xml"); //$NON-NLS-1$
		boolean updateBuild = UpdateUtil.unzipFile(updateFile, "build.xml", //$NON-NLS-1$
				buildFile);

		// Create the temp file structrue
		AntInvoker invoker = new AntInvoker(home);
		boolean ret = false;
		if (updateBuild) {
			ret = invoker.runTask("prepare-temp", "build_new.xml"); //$NON-NLS-1$ //$NON-NLS-2$
			if (ret) {
				// Copy the right buld.xml to the temp directory.
				buildFile = new File(home + File.separator + preProcessHome
						+ File.separator + "build.xml"); //$NON-NLS-1$
				UpdateUtil.unzipFile(updateFile, "build.xml", buildFile); //$NON-NLS-1$
			}

		} else {
			ret = invoker.runTask("prepare-temp",null); //$NON-NLS-1$
		}

		if (!ret) {
			String error = Messages.getString("UpdateAgent.error.ant.prepare.temp"); //$NON-NLS-1$
			if (!isDebug) {
				error += Messages.getString("UpdateAgent.text.use.verbose",logFile); //$NON-NLS-1$
			}
			throw new UpdateException(error, UpdateException.ERROR);
		}
		

		FileUpdater fileUpdater = new FileUpdater(updateFile, home + File.separator + preProcessHome, null);
		fileUpdater.getDelList();
		fileUpdater.processData(false);
	

		// Execute the pre process.
		PostProcess pp = new PostProcess();
		pp.setHome(home + File.separator + preProcessHome);
		
		if (!pp.postProcess(true)) {
			String error = Messages.getString("UpdateAgent.error.plugin.incompatible"); //$NON-NLS-1$
			if (!isDebug) {
				error += Messages.getString("UpdateAgent.text.use.verbose",logFile); //$NON-NLS-1$
			}

			// Try to clean up. Do throw anything if we fail
			logger.debug("Starting to try clean temp directory after failure");
			if (updateBuild) {
				invoker.runTask("clean-temp", "build_new.xml"); //$NON-NLS-1$ //$NON-NLS-2$
				buildFile.delete();
			} else {
				invoker.runTask("clean-temp",null); //$NON-NLS-1$
			}
			logger.debug("Finished to try clean temp directory after failure");
			

			throw new UpdateException(error, UpdateException.ERROR);
		}

		// Clean up
		if (updateBuild) {
			ret = invoker.runTask("clean-temp", "build_new.xml"); //$NON-NLS-1$ //$NON-NLS-2$
			buildFile.delete();

		} else {
			ret = invoker.runTask("clean-temp",null); //$NON-NLS-1$
		}
		if (!ret) {
			String error = Messages.getString("UpdateAgent.error.ant.clean.temp"); //$NON-NLS-1$
			if (!isDebug) {
				error += Messages.getString("UpdateAgent.text.use.verbose",logFile); //$NON-NLS-1$
			}
			throw new UpdateException(error, UpdateException.ERROR);
		}

		logger.info(Messages.getString("UpdateAgent.debug.end.validation")); //$NON-NLS-1$
		return true;

	}

	private void configureLogger(CommandLine line) {
		
		Logger logRoot = Logger.getRootLogger();
		
		//File appender get all logs always
		//Console one get all on debug
		//	...gets errors on quiet
		//	...on normal it gets info from UpdateAgent only.
		ConsoleAppender app=new ConsoleAppender(new PatternLayout("%m%n")); //$NON-NLS-1$
		FileAppender logApp=null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyMMdd_HHmm"); //$NON-NLS-1$	
			logFile="update_" + sdf.format(new Date()) +".log"; //$NON-NLS-2$  //$NON-NLS-1$
			if (line.hasOption(UpdateOptions.LOG)) {
				logFile= line.getOptionValue(UpdateOptions.LOG);
			}
			logApp = new FileAppender(new PatternLayout("%d %m%n"),logFile); //$NON-NLS-1$  
			logRoot.addAppender(logApp);
			logApp.setThreshold(Level.DEBUG);
		} catch (IOException e) {
			System.err.println(Messages.getString("UpdateAgent.error.cant.create.log") + e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
			System.err.println(Messages.getString("UpdateAgent.error.cant.create.log.will.continue")); //$NON-NLS-1$
		}
	

		
		
		
		Logger l = Logger.getLogger(UpdateAgent.class);
		l.setLevel(Level.DEBUG);
		logRoot.setLevel(Level.INFO);
		
		if (line.hasOption(UpdateOptions.VERBOSE)) {
			app.setThreshold(Level.DEBUG);
			logRoot.addAppender(app);
			isDebug=true;
		} else {
			l.addAppender(app);
			if (line.hasOption(UpdateOptions.QUIET)) {
				app.setThreshold(Level.ERROR);
			} else {
				app.setThreshold(Level.INFO);
			}
		}
		logger = Logger.getLogger(UpdateAgent.class);

	}
	
	private File findJarFile() throws IOException, UpdateException{
		String[] libDirs=new String[]{
				File.separator + "common"+ File.separator + "lib",
				File.separator + "dotCMS"+File.separator +"WEB-INF"+ File.separator + "lib"
				};
		File libDir = null;
		for (String libDirName:libDirs) {
			File f=new File(home+File.separator+libDirName);
			if (f.exists() && f.isDirectory()) {
				libDir=f;
				break;
			}
		}
		if (libDir==null) {
			throw new UpdateException(
					Messages.getString("UpdateAgent.error.jar.not.found"), //$NON-NLS-1$
					UpdateException.ERROR);
		}
		
		File[] dotCMSjars = libDir.listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				String fileName = pathname.getName().toLowerCase();
				if (fileName.startsWith("dotcms_") && fileName.endsWith(".jar") //$NON-NLS-1$ //$NON-NLS-2$
						&& (!fileName.startsWith("dotcms_ant"))) { //$NON-NLS-1$
					return true;
				}
				return false;
			}

		});
		if (dotCMSjars.length > 1) {
			String jars = ""; //$NON-NLS-1$
			for (File jar : dotCMSjars) {
				jars += " " + jar.getName(); //$NON-NLS-1$
			}
			throw new UpdateException(
					Messages.getString("UpdateAgent.error.multiple.jars") //$NON-NLS-1$
							+ jars, UpdateException.ERROR);

		}

		if (dotCMSjars.length < 1) {
			throw new UpdateException(
					Messages.getString("UpdateAgent.error.jar.not.found"), //$NON-NLS-1$
					UpdateException.ERROR);
		}
		return dotCMSjars[0];
	}
	

	private Properties getJarProps() throws IOException, UpdateException {
		
		JarFile jar = new JarFile(findJarFile());
		JarEntry entry = jar
				.getJarEntry("com/liferay/portal/util/build.properties"); //$NON-NLS-1$
		Properties props = new Properties();
		InputStream in = jar.getInputStream(entry);
		props.load(in);
		return props;
	}

	private String getVersion() throws IOException, UpdateException {

		Properties props = getJarProps();
		return props.getProperty("dotcms.release.version"); //$NON-NLS-1$
	}

	private String getMinor() throws IOException, UpdateException {

		Properties props = getJarProps();
		return props.getProperty("dotcms.release.build"); //$NON-NLS-1$
	}

	private boolean checkRequisites(String home) throws UpdateException {
		PostProcess pp = new PostProcess();
		return pp.checkRequisites();
	}

	private boolean checkHome(String home) throws UpdateException {

		File f = null;
		for (String check : homeCheckElements) {
			f = new File(home + File.separator + check);
			if (!f.exists()) {
				throw new UpdateException(
						Messages.getString("UpdateAgent.error.home.not.valid"), //$NON-NLS-1$
						UpdateException.ERROR);
			}
		}
		return true;
	}

	private void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		String txt = Messages.getString("UpdateAgent.text.agent.version") + getAgentVersion() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		txt += helpText;
		formatter.printHelp("autoUpdater <options>", "", options, txt); //$NON-NLS-1$ //$NON-NLS-2$

	}

	private boolean confirmUI() {
		boolean done = false;
		boolean ret = false;
		while (!done) {
			System.out.println(confirmText);
			InputStreamReader isr = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isr);
			try {
				String sample = br.readLine();
				if (sample.equalsIgnoreCase(Messages.getString("UpdateAgent.text.yes"))) { //$NON-NLS-1$
					done = true;
					ret = true;
				}
				if (sample.equalsIgnoreCase(Messages.getString("UpdateAgent.text.y"))) { //$NON-NLS-1$
					System.out.println(Messages.getString("UpdateAgent.text.yes.or.no")); //$NON-NLS-1$

				}
				if (sample.equalsIgnoreCase(Messages.getString("UpdateAgent.text.no")) //$NON-NLS-1$
						|| sample.equalsIgnoreCase(Messages.getString("UpdateAgent.text.n"))) { //$NON-NLS-1$
					done = true;
				}

			} catch (IOException e) {

				e.printStackTrace();
			}
		}
		return ret;
	}

	private File downloadAgent() {
		File f = new File(home + File.separator + "bin" + File.separator + "autoupdater" + File.separator  //$NON-NLS-1$
				+ "autoUpdater.new"); //$NON-NLS-1$
		try {
			f.createNewFile();
			Map<String, String> map = new HashMap<String, String>();
			map.put("version", getReleaseVersion()); //$NON-NLS-1$
			map.put("agent_version", getAgentVersion()); //$NON-NLS-1$
			if(allowTestingBuilds){
				map.put("allowTestingBuilds", "true"); //$NON-NLS-1$
			}
			PostMethod method = doGet(url, map);
			int ret = download(f, method, null);
			if (ret == 200) {
				return f;
			}
			if (ret == 204) {
				logger.debug(Messages.getString("UpdateAgent.text.autoupdater.uptodate")); //$NON-NLS-1$
			}
		} catch (IOException e) {
			logger.error(Messages.getString("UpdateAgent.error.no.autoupdater.version") //$NON-NLS-1$
					+ e.getMessage());
			logger.debug("IOException: ", e); //$NON-NLS-1$
		}
		f.delete();
		return null;
	}

	private String getAgentVersion() {
		Class clazz = this.getClass();

		String className = clazz.getSimpleName();
		String classFileName = className + ".class"; //$NON-NLS-1$
		String pathToThisClass = clazz.getResource(classFileName).toString();

		int mark = pathToThisClass.indexOf("!"); //$NON-NLS-1$
		try {
			String pathToManifest = pathToThisClass.toString().substring(0,
					mark + 1);
			pathToManifest += "/META-INF/MANIFEST.MF"; //$NON-NLS-1$
			Manifest manifest = new Manifest(new URL(pathToManifest)
					.openStream());
			return manifest.getMainAttributes().getValue("Agent-Version"); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			logger.error(Messages.getString("UpdateAgent.error.get.autoupdater.jar.version") + e.getMessage()); //$NON-NLS-1$
			logger.debug("MalformedURLException: ", e); //$NON-NLS-1$
		} catch (IOException e) {
			logger.error(Messages.getString("UpdateAgent.error.get.autoupdater.jar.version") + e.getMessage()); //$NON-NLS-1$
			logger.debug("IOException: ", e); //$NON-NLS-1$
		}
		return Messages.getString("UpdateAgent.text.unknown"); //$NON-NLS-1$
	}
	
	private String getReleaseVersion() {
		Class clazz = this.getClass();

		String className = clazz.getSimpleName();
		String classFileName = className + ".class"; //$NON-NLS-1$
		String pathToThisClass = clazz.getResource(classFileName).toString();

		int mark = pathToThisClass.indexOf("!"); //$NON-NLS-1$
		try {
			String pathToManifest = pathToThisClass.toString().substring(0,
					mark + 1);
			pathToManifest += "/META-INF/MANIFEST.MF"; //$NON-NLS-1$
			Manifest manifest = new Manifest(new URL(pathToManifest)
					.openStream());
			return manifest.getMainAttributes().getValue("Release-Version"); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			logger.error(Messages.getString("UpdateAgent.error.get.autoupdater.jar.version") + e.getMessage()); //$NON-NLS-1$
			logger.debug("MalformedURLException: ", e); //$NON-NLS-1$
		} catch (IOException e) {
			logger.error(Messages.getString("UpdateAgent.error.get.autoupdater.jar.version") + e.getMessage()); //$NON-NLS-1$
			logger.debug("IOException: ", e); //$NON-NLS-1$
		}
		return Messages.getString("UpdateAgent.text.unknown"); //$NON-NLS-1$
	}

	private PostMethod doGet(String fileUrl, Map<String, String> pars)
			throws HttpException, IOException {
		HttpClient client = new HttpClient();
		//ClassLoader loader = ClassLoader.getSystemClassLoader();
		//URL url = loader.getResource("org/dotcms/update/jssecacerts"); //$NON-NLS-1$

		//Protocol authhttps = new Protocol("https", new SSLFactory(null, null, //$NON-NLS-1$	url, "changeit"), 443); //$NON-NLS-1$
		//Protocol.registerProtocol("https", authhttps); //$NON-NLS-1$

		int ret = 0;
		// Setup a proxy
		if (proxy != null && proxy.length() > 0) {
			String proxyHost = proxy.substring(0, proxy.indexOf(":")); //$NON-NLS-1$
			String proxyPort = proxy.substring(proxy.indexOf(":") + 1); //$NON-NLS-1$

			client.getHostConfiguration().setProxy(proxyHost,
					Integer.parseInt(proxyPort));
			if (proxyUser != null && proxyUser.length() > 0
					&& proxyPass != null && proxyPass.length() > 0) {
				// Authenticate with proxy

				client.getState().setProxyCredentials(null, null,
						new UsernamePasswordCredentials(proxyUser, proxyPass));

			}

		}
		PostMethod method = new PostMethod(fileUrl);
		Object[] keys = (Object[]) pars.keySet().toArray();
		NameValuePair[] data = new NameValuePair[keys.length];
		for (int i = 0; i < keys.length; i++) {
			String key = (String) keys[i];
			NameValuePair pair = new NameValuePair(key, pars.get(key));
			data[i] = pair;
		}

		method.setRequestBody(data);
		client.executeMethod(method);
		return method;
	}

	private int download(File f, PostMethod method, String major) throws URIException,
			MalformedURLException {

		int ret = 0;
		try {

			ret = method.getStatusCode();
			logger.debug(Messages.getString("UpdateAgent.debug.return.code") + ret); //$NON-NLS-1$
			if (ret == 200) {
				// Just in case something else fails
				ret = -1;
				InputStream is = method.getResponseBodyAsStream();

				// f = File.createTempFile("dotCMS_update", ".zip");
				// f.deleteOnExit();
				OutputStream out = new FileOutputStream(f);
				byte[] b = new byte[1024];
				int len;
				int count = 0;
				long length = 0;
				String newMinor = null;

				try {
					String lenghtString = method.getResponseHeader(
							"Content-Length").getValue(); //$NON-NLS-1$
					length = Long.parseLong(lenghtString);
				} catch (Exception e) {
				}

				try {
					newMinor = method.getResponseHeader("Minor-Version") //$NON-NLS-1$
							.getValue();
				} catch (Exception e) {
				}

				String dlMessage = Messages.getString("UpdateAgent.text.downloading"); //$NON-NLS-1$
				if (newMinor != null) {
					dlMessage += Messages.getString("UpdateAgent.text.new.minor");//$NON-NLS-1$
					/*
					if (major!=null) {
						dlMessage += major + " / ";//$NON-NLS-1$
					}
					*/
					dlMessage += newMinor;
				}
				if (length > 0) {
					dlMessage += " (" + length / 1024 + "kB)"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				logger.info(dlMessage);
				long startTime = System.currentTimeMillis();
				long refreshInterval = 500;
				DownloadProgress dp = new DownloadProgress(length);
				while ((len = is.read(b)) != -1) {
					for (int i = 0; i < len; i++) {
						out.write((char) b[i]);
						count++;
					}
					long currentTime = System.currentTimeMillis();

					if ((currentTime - startTime) > refreshInterval) {
						String message = dp.getProgressMessage(count,
								startTime, currentTime);
						startTime = currentTime;
						System.out.print("\r" + message); //$NON-NLS-1$
					}
				}
				String message = dp.getProgressMessage(count, startTime, System
						.currentTimeMillis());
				System.out.print("\r" + message); //$NON-NLS-1$
				System.out.println(""); //$NON-NLS-1$
				out.close();
				is.close();

				// verfiy md5
				String MD5 = null;
				boolean hasMD5 = false;
				if (method.getResponseHeader("Content-MD5") != null 
						&& !method.getResponseHeader("Content-MD5").equals("")) { //$NON-NLS-1$
					MD5 = method.getResponseHeader("Content-MD5").getValue(); //$NON-NLS-1$
					if(!MD5.equals("")){
					  hasMD5 = true;
					}
				}
				if(hasMD5){
					String dlMD5 =UpdateUtil.getMD5(f);
					logger.debug(Messages.getString("UpdateAgent.debug.server.md5") + MD5); //$NON-NLS-1$
					logger.debug(Messages.getString("UpdateAgent.debug.file.md5") + dlMD5); //$NON-NLS-1$

					if (MD5 == null || MD5.length() == 0 || !dlMD5.equals(MD5)) {
						logger
						.fatal(Messages.getString("UpdateAgent.error.md5.failed")); //$NON-NLS-1$
						f.delete();

					} else {
						// everything went ok, we return the right return code
						ret = 200;
						logger.info(Messages.getString("UpdateAgent.text.md5.verified")); //$NON-NLS-1$
					}
				}else{
					ret = 200;
					logger.info(Messages.getString("UpdateAgent.text.md5.verified")); //$NON-NLS-1$
				}

			}
			if (ret == 204) {
				logger.debug(Messages.getString("UpdateAgent.debug.no.content")); //$NON-NLS-1$
			}
		} catch (HttpException e) {
			logger.error(Messages.getString("UpdateAgent.error.downloading.file") + e.getMessage()); //$NON-NLS-1$
			logger.debug("HttpException: ", e); //$NON-NLS-1$
		} catch (IOException e) {
			logger.error(Messages.getString("UpdateAgent.error.downloading.file") + e.getMessage()); //$NON-NLS-1$
			logger.debug("IOException: ", e); //$NON-NLS-1$
		}
		if (method != null) {
			method.releaseConnection();
		}

		return ret;
	}


}

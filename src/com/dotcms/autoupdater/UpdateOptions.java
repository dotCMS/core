package com.dotcms.autoupdater;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import com.dotcms.autoupdater.Messages;
import com.dotcms.autoupdater.UpdateOptions;

public class UpdateOptions {
	
	public static String USER = "user"; //$NON-NLS-1$
	public static String PASSWORD = "password"; //$NON-NLS-1$
	public static String FILE = "file"; //$NON-NLS-1$
	public static String BACKUP = "backup"; //$NON-NLS-1$
	public static String URL = "url"; //$NON-NLS-1$
	public static String FORCE = "force"; //$NON-NLS-1$
	public static String NO_UPDATE = "noupdaterupdate"; //$NON-NLS-1$
	public static String DRY_RUN = "dryrun"; //$NON-NLS-1$
	public static String HELP = "help"; //$NON-NLS-1$
	public static String QUIET = "quiet"; //$NON-NLS-1$
	public static String VERBOSE = "verbose"; //$NON-NLS-1$
	public static String HOME = "home"; //$NON-NLS-1$
	public static String PROXY = "proxy"; //$NON-NLS-1$
	public static String PROXY_USER = "proxyuser"; //$NON-NLS-1$
	public static String PROXY_PASS = "proxypass"; //$NON-NLS-1$
	public static String LOG = "log"; //$NON-NLS-1$
	public static String ALLOW_TESTING_BUILDS = "allowtestingbuilds";
	public static String SPECIFIC_VERSION = "version";
	
	
	Properties props = new Properties();


	
	public UpdateOptions() {
		loadDefaults();
	}
	
	private void loadDefaults() {
		ClassLoader cl = this.getClass().getClassLoader();
		InputStream is = cl
				.getResourceAsStream("com/dotcms/autoupdater/update.properties"); //$NON-NLS-1$
		try {
			props.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public Options getOptions() {
		Options options = new Options();
		Option userOption = new Option(UpdateOptions.USER, true,
				Messages.getString("UpdateOptions.text.user")); //$NON-NLS-1$		
		Option passOption = new Option(UpdateOptions.PASSWORD, true,
				Messages.getString("UpdateOptions.text.password")); //$NON-NLS-1$
		Option fileOption = new Option(UpdateOptions.FILE, true,
				Messages.getString("UpdateOptions.text.file")); //$NON-NLS-1$
		Option backupOption = new Option(
				UpdateOptions.BACKUP,
				true,
				Messages.getString("UpdateOptions.text.backup")); //$NON-NLS-1$
		Option urlOption = new Option(UpdateOptions.URL, true,
				Messages.getString("UpdateOptions.text.url") //$NON-NLS-1$
						+ props.getProperty("update.url", "")); //$NON-NLS-1$ //$NON-NLS-2$
		Option forceOption = new Option(UpdateOptions.FORCE, false,
				Messages.getString("UpdateOptions.text.force")); //$NON-NLS-1$
		Option noUpdaterOption = new Option(UpdateOptions.NO_UPDATE, false,
		Messages.getString("UpdateOptions.text.no.autoupdater.updater")); //$NON-NLS-1$
		Option dryrunOption = new Option(UpdateOptions.DRY_RUN, false,
		Messages.getString("UpdateOptions.text.dryrun")); //$NON-NLS-1$
		Option helpOption = new Option(UpdateOptions.HELP, false, Messages.getString("UpdateOptions.text.help"));		 //$NON-NLS-1$
		Option quietOption = new Option(UpdateOptions.QUIET, false, Messages.getString("UpdateOptions.text.quiet")); //$NON-NLS-1$
		Option verboseOption = new Option(UpdateOptions.VERBOSE, false, Messages.getString("UpdateOptions.text.verbose")); //$NON-NLS-1$
		Option homeOption = new Option(
				UpdateOptions.HOME,
				true,
				Messages.getString("UpdateOptions.text.home",System.getProperty("user.dir"))); //$NON-NLS-1$ //$NON-NLS-2$						
		Option logOption = new Option(UpdateOptions.LOG,true,"Name of the log file to be created. Defaults to: update_{timestamp}.log");

		Option proxy = new Option(UpdateOptions.PROXY, true,
				Messages.getString("UpdateOptions.text.proxy")); //$NON-NLS-1$
		Option proxyUser = new Option(UpdateOptions.PROXY_USER, true,
				Messages.getString("UpdateOptions.text.proxy.user")); //$NON-NLS-1$
		Option proxyPass = new Option(UpdateOptions.PROXY_PASS, true,
				Messages.getString("UpdateOptions.text.proxy.pass")); //$NON-NLS-1$
		Option allowTestingBuildsOption = new Option(UpdateOptions.ALLOW_TESTING_BUILDS, true,
				Messages.getString("UpdateOptions.text.allowtestingbuilds")); //$NON-NLS-1$	
		Option specificVersionOption = new Option(UpdateOptions.SPECIFIC_VERSION, true,
				Messages.getString("UpdateOptions.text.specificversion")); //$NON-NLS-1$	

		OptionGroup originGroup=new OptionGroup();
		originGroup.addOption(urlOption);
		
		OptionGroup fileGroup=new OptionGroup();
		fileGroup.addOption(fileOption);
	
		
		OptionGroup verboseGroup=new OptionGroup();
		verboseGroup.addOption(verboseOption);
		verboseGroup.addOption(quietOption);
		
		options.addOptionGroup(originGroup);
		options.addOptionGroup(fileGroup);
		options.addOptionGroup(verboseGroup);
		
		//options.addOption(userOption);
		//options.addOption(passOption);
		options.addOption(forceOption);
		//options.addOption(noUpdaterOption);
		options.addOption(dryrunOption);
		options.addOption(helpOption);
		options.addOption(homeOption);
		options.addOption(backupOption);
		options.addOption(proxy);
		options.addOption(proxyUser);
		options.addOption(proxyPass);
		options.addOption(logOption);
		options.addOption(allowTestingBuildsOption);
		options.addOption(specificVersionOption);

		return options;
	}
	
	public String getDefault(String key, String defValue) {
		return props.getProperty(key,defValue);
	}

}

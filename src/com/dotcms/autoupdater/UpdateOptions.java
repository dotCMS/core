package com.dotcms.autoupdater;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class UpdateOptions {

    public static String USER = "user";
    public static String PASSWORD = "password";
    public static String FILE = "file";
    public static String BACKUP = "backup";
    public static String URL = "url";
    public static String FORCE = "force";
    public static String NO_UPDATE = "noupdaterupdate";
    public static String DRY_RUN = "dryrun";
    public static String HELP = "help";
    public static String QUIET = "quiet";
    public static String VERBOSE = "verbose";
    public static String HOME = "home";
    public static String PROXY = "proxy";
    public static String PROXY_USER = "proxyuser";
    public static String PROXY_PASS = "proxypass";
    public static String LOG = "log";
    public static String ALLOW_TESTING_BUILDS = "allowtestingbuilds";
    public static String SPECIFIC_VERSION = "version";

    private Properties props = new Properties();
    private static Options options;

    public UpdateOptions () {
        loadDefaults();
    }

    private void loadDefaults () {

        ClassLoader cl = this.getClass().getClassLoader();
        InputStream is = cl.getResourceAsStream( "com/dotcms/autoupdater/update.properties" );
        try {
            props.load( is );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public Options getOptions () {

        if ( options == null ) {
            options = new Options();
        } else {
            return options;
        }

        Option userOption = new Option( UpdateOptions.USER, true, Messages.getString( "UpdateOptions.text.user" ) );
        Option passOption = new Option( UpdateOptions.PASSWORD, true, Messages.getString( "UpdateOptions.text.password" ) );
        Option fileOption = new Option( UpdateOptions.FILE, true, Messages.getString( "UpdateOptions.text.file", UpdateAgent.FOLDER_HOME_UPDATER + File.separator + "updates" ) );
        //Option backupOption = new Option( UpdateOptions.BACKUP, true, Messages.getString( "UpdateOptions.text.backup" ) );
        Option urlOption = new Option( UpdateOptions.URL, true, Messages.getString( "UpdateOptions.text.url" ) + props.getProperty( "update.url", "" ) );
        Option forceOption = new Option( UpdateOptions.FORCE, false, Messages.getString( "UpdateOptions.text.force" ) );
        Option noUpdaterOption = new Option( UpdateOptions.NO_UPDATE, false, Messages.getString( "UpdateOptions.text.no.autoupdater.updater" ) );
        Option dryrunOption = new Option( UpdateOptions.DRY_RUN, false, Messages.getString( "UpdateOptions.text.dryrun" ) );
        Option helpOption = new Option( UpdateOptions.HELP, false, Messages.getString( "UpdateOptions.text.help" ) );
        Option quietOption = new Option( UpdateOptions.QUIET, false, Messages.getString( "UpdateOptions.text.quiet" ) );
        Option verboseOption = new Option( UpdateOptions.VERBOSE, false, Messages.getString( "UpdateOptions.text.verbose" ) );
        Option homeOption = new Option( UpdateOptions.HOME, true, Messages.getString( "UpdateOptions.text.home", System.getProperty( "user.dir" ) ) );
        Option logOption = new Option( UpdateOptions.LOG, true, "Name of the log file to be created. Defaults to: update_{timestamp}.log" );
        Option proxy = new Option( UpdateOptions.PROXY, true, Messages.getString( "UpdateOptions.text.proxy" ) );
        Option proxyUser = new Option( UpdateOptions.PROXY_USER, true, Messages.getString( "UpdateOptions.text.proxy.user" ) );
        Option proxyPass = new Option( UpdateOptions.PROXY_PASS, true, Messages.getString( "UpdateOptions.text.proxy.pass" ) );
        Option allowTestingBuildsOption = new Option( UpdateOptions.ALLOW_TESTING_BUILDS, true, Messages.getString( "UpdateOptions.text.allowtestingbuilds" ) );
        Option specificVersionOption = new Option( UpdateOptions.SPECIFIC_VERSION, true, Messages.getString( "UpdateOptions.text.specificversion" ) );

        OptionGroup originGroup = new OptionGroup();
        originGroup.addOption( urlOption );

        OptionGroup fileGroup = new OptionGroup();
        fileGroup.addOption( fileOption );

        OptionGroup verboseGroup = new OptionGroup();
        verboseGroup.addOption( verboseOption );
        verboseGroup.addOption( quietOption );

        options.addOptionGroup( originGroup );
        options.addOptionGroup( fileGroup );
        options.addOptionGroup( verboseGroup );

        //options.addOption(userOption);
        //options.addOption(passOption);
        options.addOption( forceOption );
        //options.addOption(noUpdaterOption);
        options.addOption( dryrunOption );
        options.addOption( helpOption );
        options.addOption( homeOption );
        //options.addOption( backupOption );
        options.addOption( proxy );
        options.addOption( proxyUser );
        options.addOption( proxyPass );
        options.addOption( logOption );
        options.addOption( allowTestingBuildsOption );
        options.addOption( specificVersionOption );

        return options;
    }

    public String getDefault ( String key, String defValue ) {
        return props.getProperty( key, defValue );
    }

    public void setHomeFolder ( String homeFolder ) {
        options.getOption( UpdateOptions.HOME ).setDescription( Messages.getString( "UpdateOptions.text.home", homeFolder ) );
    }

    public void setUpdateFilesFolder (String updateFilesFolder) {
        options.getOption( UpdateOptions.FILE ).setDescription( Messages.getString( "UpdateOptions.text.file", updateFilesFolder ) );
    }

}
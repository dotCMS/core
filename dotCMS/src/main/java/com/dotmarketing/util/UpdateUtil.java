package com.dotmarketing.util;

import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.util.ReleaseInfo;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpClient;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpException;
import com.dotcms.repackage.org.apache.commons.httpclient.NameValuePair;
import com.dotcms.repackage.org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class UpdateUtil {

    /**
     * @return the new version if found. Null if up to date.
     * @throws DotDataException if an error is encountered
     */
    public static String getNewVersion () throws DotDataException {

        //Loading the update url
        Properties props = loadUpdateProperties();
        String fileUrl = props.getProperty( Constants.PROPERTY_UPDATE_FILE_UPDATE_URL, "" );

        Map<String, String> pars = new HashMap<String, String>();
        pars.put( "version", ReleaseInfo.getVersion() );
        //pars.put("minor", ReleaseInfo.getBuildNumber() + "");
        pars.put( "check_version", "true" );
        pars.put( "level", Long.toString(LicenseUtil.getLevel()));
        if ( LicenseUtil.getSerial() != null ) {
            pars.put( "license", LicenseUtil.getSerial() );
        }

        HttpClient client = new HttpClient();

        PostMethod method = new PostMethod( fileUrl );
        Object[] keys = (Object[]) pars.keySet().toArray();
        NameValuePair[] data = new NameValuePair[keys.length];
        for ( int i = 0; i < keys.length; i++ ) {
            String key = (String) keys[i];
            NameValuePair pair = new NameValuePair( key, pars.get( key ) );
            data[i] = pair;
        }

        method.setRequestBody( data );
        String ret = null;

        try {
            client.executeMethod( method );
            int retCode = method.getStatusCode();
            if ( retCode == 204 ) {
                Logger.info( UpdateUtil.class, "No new updates found" );
            } else {
                if ( retCode == 200 ) {
                    String newMinor = method.getResponseHeader( "Minor-Version" )
                            .getValue();
                    String newPrettyName = null;
                    if ( method.getResponseHeader( "Pretty-Name" ) != null ) {
                        newPrettyName = method.getResponseHeader( "Pretty-Name" )
                                .getValue();
                    }

                    if ( newPrettyName == null ) {
                        Logger.info( UpdateUtil.class, "New Version: "
                                + newMinor );
                        ret = newMinor;
                    } else {
                        Logger.info( UpdateUtil.class, "New Version: "
                                + newPrettyName + "/" + newMinor );
                        ret = newPrettyName;
                    }

                } else {
                    throw new DotDataException( "Unknown return code: " + method.getStatusCode() + " (" + method.getStatusText() + ")" );
                }
            }
        } catch ( HttpException e ) {
            Logger.error( UpdateUtil.class, "HttpException: " + e.getMessage(),
                    e );
            throw new DotDataException( "HttpException: " + e.getMessage(), e );

        } catch ( IOException e ) {
            Logger.error( UpdateUtil.class, "IOException: " + e.getMessage(), e );
            throw new DotDataException( "IOException: " + e.getMessage(), e );
        }

        return ret;
    }

    /**
     * Loads the update.properties file
     *
     * @return the update.properties properties
     */
    private static Properties loadUpdateProperties () {

        Properties props = new Properties();

        ClassLoader cl = UpdateUtil.class.getClassLoader();
        InputStream is = cl.getResourceAsStream( Constants.PROPERTIES_UPDATE_FILE_LOCATION );
        try {
            props.load( is );
        } catch ( IOException e ) {
            Logger.debug( UpdateUtil.class, "IOException: " + e.getMessage(), e );
        }

        return props;
    }

}
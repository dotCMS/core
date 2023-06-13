package com.dotcms.enterprise.license;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class LicenseRepoDAO {

    
    @WrapInTransaction
    protected static Optional<DotLicenseRepoEntry> requestLicense() throws DotDataException, SQLException {

        Optional<DotLicenseRepoEntry> license =Optional.empty();
        final DotConnect dc = new DotConnect();

        // am I in there?
        dc.setSQL("SELECT id FROM sitelic WHERE serverid =?");
        dc.addParam(APILocator.getServerAPI().readServerId());
        List<Map<String,Object>> results = dc.loadObjectResults();
        if(!results.isEmpty()) {
            return forceLicenseFromRepo(String.valueOf(results.get(0).get("id")));
        }


        // get some other license
        dc.setSQL("SELECT id FROM sitelic WHERE serverid IS NULL OR lastping<? ORDER BY lastping ASC");
        Calendar cal=Calendar.getInstance();
        cal.add(Calendar.MINUTE, -Config.getIntProperty("LICENSE_REPO_LEASE_EXPIRE_MINUTES",10));
        dc.addParam(cal.getTime());
        results = dc.loadObjectResults(  );
        if(!results.isEmpty()) {
            return forceLicenseFromRepo(String.valueOf(results.get(0).get("id")));
        }

        return license;
    }


    @WrapInTransaction
    protected static Optional<DotLicenseRepoEntry> forceLicenseFromRepo(final String id) throws DotDataException, SQLException {
        Optional<DotLicenseRepoEntry> license =Optional.empty();

        if(!UtilMethods.isSet(id)){
            return license;
        }


        final DotConnect dc = new DotConnect();
        dc.setSQL("SELECT * FROM sitelic where id = ?")
        .addParam(id);
        final List<Map<String,Object>> results = dc.loadObjectResults(  );

        if(!results.isEmpty()) {
            final String sid=APILocator.getServerAPI().readServerId();

            license = toEntry(results.get(0));

            if(license.isPresent()  && license.get().dotLicense.expired){
                return Optional.empty();
            }

            dc.setSQL("update sitelic set serverid = null, lastping=? where serverid=? or id = ? ")
            .addParam(new Date(0))
            .addParam(sid)
            .addParam(id)
            .loadResult();

            dc.setSQL("update sitelic set serverid = ?, lastping=? where id = ? ")
            .addParam(sid)
            .addParam(new Date())
            .addParam(id)
            .loadResult();

        }

        return license;
    }
    
    protected static void updateLastPing() throws DotDataException {
        if(LicenseManager.getInstance().getLevel()>LicenseLevel.COMMUNITY.level) {
            transactionalUpdateLastPing();
        }
    }

    @WrapInTransaction
    private static void transactionalUpdateLastPing () throws DotDataException {
        final DotConnect dc=new DotConnect();
        dc.setSQL("UPDATE sitelic SET lastping=?, serverid=? WHERE id=? AND (serverid=? or serverid is null)");
        dc.addParam(Calendar.getInstance().getTime());
        dc.addParam(APILocator.getServerAPI().readServerId());
        dc.addParam(LicenseManager.getInstance().getSerial());
        dc.addParam(APILocator.getServerAPI().readServerId());
        dc.loadResult();
    }

    @WrapInTransaction
    protected static void setServerIdToCurrentLicense(String serial) throws DotDataException {
            final DotConnect dc=new DotConnect();
            dc.setSQL("UPDATE sitelic SET serverid=?, lastping=? WHERE id=?");
            dc.addParam(APILocator.getServerAPI().readServerId());
            dc.addParam(new Date());
            dc.addParam(serial);
            dc.loadResult();
            
            dc.setSQL("UPDATE sitelic SET serverid=null, lastping=? WHERE serverid=? and id<>?");
            dc.addParam(new Date(0));
            dc.addParam(APILocator.getServerAPI().readServerId());
    
            dc.addParam(serial);
            dc.loadResult();
    }


    @CloseDBIfOpened
    protected static boolean isLicenseOnRepo(final String serial) throws DotDataException {
        final  DotConnect dc=new DotConnect();
        dc.setSQL("SELECT lastping FROM sitelic WHERE id=? ");
        dc.addParam(serial);

        return dc.loadObjectResults().size()>0;
    }

    @WrapInTransaction
    protected static void upsertLicenseToRepo(final String serial,  final String licenseRaw) throws DotDataException, IOException {

        DotConnect dc=new DotConnect();
        if(!isLicenseOnRepo(serial)){
            dc.setSQL("INSERT INTO sitelic(id,license,lastping) VALUES(?,?,?)");
            dc.addParam(serial);
            dc.addParam(licenseRaw);
            dc.addParam(new Date(0));
            dc.loadResult();
        }
        else{
            dc.setSQL("UPDATE sitelic set license=? where id=?");
            dc.addParam(licenseRaw);
            dc.addParam(serial);
            dc.loadResult();
        }
    }
    

    protected static void insertAvailableLicensesFromZipFile(final InputStream zipfile) throws IOException, DotDataException {

        ZipInputStream in= null;
        ZipEntry entry = null;

        try {

            in = new ZipInputStream(zipfile);

            while((entry = in.getNextEntry())!=null) {
                try {
                    String license=IOUtils.toString(in);
                    String serial= new LicenseTransformer(license).dotLicense.serial;
                    Logger.info(LicenseUtil.class, "found license serial: " + serial);
                    upsertLicenseToRepo(serial, license);
                } finally {
                    in.closeEntry();
                }
            }
        } catch(Exception ex) {
            Logger.error(System.class, "Can't process license zip file", ex);
        } finally {
            CloseUtils.closeQuietly(in);
        }
    }

    @WrapInTransaction
    protected static int getLicenseRepoTotal() throws DotDataException {
        final DotConnect dc=new DotConnect();
        dc.setSQL("SELECT COUNT(*) AS cc FROM sitelic");
        return ((Number)dc.loadObjectResults().get(0).get("cc")).intValue();
    }

    @CloseDBIfOpened
    protected static int getLicenseRepoAvailableCount() throws DotDataException {
        final  DotConnect dc=new DotConnect();
        dc.setSQL("SELECT COUNT(*) AS cc FROM sitelic WHERE serverid IS NULL OR lastping<?");
        Calendar cal=Calendar.getInstance();
        cal.add(Calendar.MINUTE, -Config.getIntProperty("LICENSE_REPO_LEASE_EXPIRE_MINUTES",10));
        dc.addParam(cal.getTime());
        return ((Number)dc.loadObjectResults().get(0).get("cc")).intValue();
    }

    @CloseDBIfOpened
    protected static List<DotLicenseRepoEntry> getLicenseRepoList () throws DotDataException, IOException {

        final String currentServerId = APILocator.getServerAPI().readServerId();

        DotConnect dc = new DotConnect();
        dc.setSQL( "SELECT * FROM sitelic ORDER BY lastping DESC " );
        List<Map<String, Object>> list = dc.loadObjectResults();
        List<DotLicenseRepoEntry> results = new ArrayList<>();
        for ( Map<String, Object> m : list ) {
            Optional<DotLicenseRepoEntry> dlre = toEntry(m);
            if(dlre.isPresent()){
                results.add(dlre.get());
            }
        }
        return results;
    }
    
    private static Optional<DotLicenseRepoEntry> toEntry (Map<String, Object> map ){
        final String currentServerId = APILocator.getServerAPI().readServerId();
        String serverId = (String) map.get( "serverid" );
        String license = (String) map.get( "license" );
        Date lastPing = (Date) map.get( "lastping" );
        DotLicenseRepoEntry entry = new DotLicenseRepoEntry()
                        .withServerId(serverId)
                        .withLicense(license)
                        .withLastPing(lastPing);

        try{
            DotLicense dotLicense = new LicenseTransformer(String.valueOf(map.get( "license" ))).dotLicense;

            entry = entry.withDotLicense(dotLicense);

        }
        catch(Exception e){
            return Optional.empty();

        }


        boolean available = true;
        if ( (UtilMethods.isSet( serverId ) && UtilMethods.isSet( currentServerId )) && serverId.equals( currentServerId ) ) {
            available = false;
        } else if ( UtilMethods.isSet( serverId ) ) {
            available = false;
            if ( map.get( "lastping" ) != null ) {
                Calendar cal = Calendar.getInstance();
                cal.add( Calendar.MINUTE, -Config.getIntProperty( "LICENSE_REPO_LEASE_EXPIRE_MINUTES", 10 ) );
                available = cal.getTime().after( (java.util.Date) map.get( "lastping" ) );
            }
        }
        entry = entry.withAvailable(available);
        return Optional.of(entry);

    }
    

    @WrapInTransaction
    protected static void deleteLicense(String id) throws DotDataException {
        Calendar cal=Calendar.getInstance();
        cal.add(Calendar.MINUTE, -Config.getIntProperty("LICENSE_REPO_LEASE_EXPIRE_MINUTES",10));
        DotConnect dc=new DotConnect();
        dc.setSQL("DELETE FROM sitelic WHERE id=? ");
        dc.addParam(id);

        dc.loadResult();
    }

    public static void freeLicense() throws DotDataException {
        freeLicense(LicenseManager.getInstance().getSerial());
    }

    @WrapInTransaction
    public static void freeLicense(String id) throws DotDataException {
        DotConnect dc=new DotConnect();
        dc.setSQL("UPDATE sitelic SET serverid=NULL, lastping=? WHERE id=?");
        dc.addParam(new Date(0));
        dc.addParam(id);
        dc.loadResult();
    }
    
    @WrapInTransaction
    public static List<DotLicenseRepoEntry>  selectDeadLicenses() throws DotDataException {


    	int timeout  = Config.getIntProperty("HEARTBEAT_TIMEOUT", 180);
    	Calendar cal = Calendar.getInstance();
    	cal.add(Calendar.SECOND, -timeout);

        DotConnect dc=new DotConnect();
    	dc.setSQL("SELECT * FROM sitelic where lastping < ? and serverid is not null and serverid <> ? ORDER BY lastping DESC");
		dc.addParam(cal.getTime());
        dc.addParam(APILocator.getServerAPI().readServerId());
        
        List<Map<String, Object>> list = dc.loadObjectResults();
        List<DotLicenseRepoEntry> results = new ArrayList<>();
        for ( Map<String, Object> m : list ) {
            Optional<DotLicenseRepoEntry> dlre = toEntry(m);
            if(dlre.isPresent()){
                results.add(dlre.get());
            }
        }
        return results;
    }
    
}

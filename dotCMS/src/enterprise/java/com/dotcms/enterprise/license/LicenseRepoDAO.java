/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.license;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ServerFactoryImpl;
import org.apache.commons.io.IOUtils;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class LicenseRepoDAO {

    
    private static Date licenseTimeout(){

        return ServerFactoryImpl.getServerTimeout();
        
    }
    
    
    
    
    @WrapInTransaction
    protected static Optional<DotLicenseRepoEntry> requestLicense() throws DotDataException {
        final Optional<DotLicenseRepoEntry> license = Optional.empty();
        final DotConnect dc = new DotConnect();

        // am I in there?
        final String serverId = APILocator.getServerAPI().readServerId();
        final long startTime = APILocator.getServerAPI().getServerStartTime();
        dc.setSQL("SELECT id FROM sitelic WHERE serverid = ? AND startup_time = ?")
                .addParam(serverId)
                .addParam(startTime);
        List<Map<String,Object>> results = dc.loadObjectResults();
        if (!results.isEmpty()) {
            return forceLicenseFromRepo(String.valueOf(results.get(0).get("id")));
        }

        // get some other license
        dc.setSQL("SELECT id FROM sitelic WHERE serverid IS NULL OR lastping<? ORDER BY lastping ASC");
        dc.addParam(licenseTimeout());
        results = dc.loadObjectResults();
        if (!results.isEmpty()) {
            return assignRepoLicense(serverId, startTime, results);
        }

        return license;
    }

    /**
     * Checks if license provided by its id has been assigned to a server
     *
     * @param id license id
     * @param serverId server id
     * @return true if license was assigned correctly, otherwise false
     * @throws DotDataException if something goes wrong when executing the SQL
     */
    private static boolean verifyServerHasLicense(final String id,
                                                  final String serverId,
                                                  final long startTime) throws DotDataException {
        final DotConnect connect = new DotConnect();
        connect.setSQL("SELECT id FROM sitelic WHERE id = ? AND serverid = ? AND startup_time = ?")
                .addParam(id)
                .addParam(serverId)
                .addParam(startTime);

        if (connect.loadObjectResults().isEmpty()) {
            Logger.error(
                    LicenseRepoDAO.class,
                    String.format("License %s is no longer assigned to server %s", id, serverId));
            return false;
        } else {
            Logger.info(LicenseRepoDAO.class, String.format("License %s was applied to server %s", id, serverId));
            return true;
        }
    }

    @WrapInTransaction
    protected static Optional<DotLicenseRepoEntry> forceLicenseFromRepo(final String id) throws DotDataException {
        Optional<DotLicenseRepoEntry> license =Optional.empty();

        if(!UtilMethods.isSet(id)){
            return license;
        }

        final DotConnect dc = new DotConnect();
        dc.setSQL("SELECT * FROM sitelic where id = ?").addParam(id);
        final List<Map<String,Object>> results = dc.loadObjectResults(  );

        if (!results.isEmpty()) {
            final String sid=APILocator.getServerAPI().readServerId();

            license = toEntry(results.get(0));

            if(license.isPresent()  && license.get().dotLicense.expired){
                return Optional.empty();
            }

            dc.setSQL("update sitelic set serverid=null, startup_time=null, lastping=? where serverid=? or id=? ")
            .addParam(new Date(0))
            .addParam(sid)
            .addParam(id)
            .loadResult();

            dc.setSQL("update sitelic set serverid=?, startup_time=?, lastping=? where id=? ")
            .addParam(sid)
            .addParam(APILocator.getServerAPI().getServerStartTime())
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
        dc.addParam(licenseTimeout());
        return ((Number)dc.loadObjectResults().get(0).get("cc")).intValue();
    }

    @CloseDBIfOpened
    protected static List<DotLicenseRepoEntry> getLicenseRepoList () throws DotDataException, IOException {

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
        long startupTime = map.get( "startup_time" )!=null ?
                DbConnectionFactory.isOracle() ?
                        ((BigDecimal) map.get( "startup_time")).longValue() :
                        (Long) map.get( "startup_time" ): 0 ;
        DotLicenseRepoEntry entry = new DotLicenseRepoEntry()
                        .withServerId(serverId)
                        .withLicense(license)
                        .withStartupTime(startupTime)
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
                available = licenseTimeout().after( (java.util.Date) map.get( "lastping" ) );
            }
        }
        entry = entry.withAvailable(available);
        return Optional.of(entry);

    }
    

    @WrapInTransaction
    protected static void deleteLicense(String id) throws DotDataException {

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
        dc.setSQL("UPDATE sitelic SET serverid=NULL, startup_time=NULL, lastping=? WHERE id=?");
        dc.addParam(new Date(0));
        dc.addParam(id);
        dc.loadResult();
    }
    
    @WrapInTransaction
    public static List<DotLicenseRepoEntry>  selectDeadLicenses() throws DotDataException {



        DotConnect dc=new DotConnect();
    	dc.setSQL("SELECT * FROM sitelic where lastping < ? and serverid is not null and serverid <> ? ORDER BY lastping DESC");
		dc.addParam(licenseTimeout());
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

    /**
     * Assigns license by forcing it from repo and verifies right away if the server took it correctly
     *
     * @param serverId server id
     * @param startTime server start time
     * @param licenses sitelic table results
     * @return Optional holding {@link DotLicenseRepoEntry} instance
     */
    @WrapInTransaction
    private static Optional<DotLicenseRepoEntry> assignRepoLicense(final String serverId,
                                                                   final long startTime,
                                                                   final List<Map<String, Object>> licenses) {
        Optional<DotLicenseRepoEntry> repoEntry = Optional.empty();
        for (final Map<String, Object> rowMap : licenses) {
            final String id = String.valueOf(rowMap.get("id"));
            try {
                repoEntry = forceLicenseFromRepo(id);
                if (repoEntry.isPresent()) {
                    if (verifyServerHasLicense(id, serverId, startTime)) {
                        break;
                    } else {
                        Logger.warn(
                                LicenseRepoDAO.class,
                                String.format(
                                        "License %s could not be assigned to server %s since it was to other server",
                                        id,
                                        serverId));
                    }
                } else {
                    Logger.warn(LicenseRepoDAO.class, String.format("License %s could not be found", id));
                }
            } catch (DotDataException e) {
                Logger.error(
                        LicenseRepoDAO.class,
                        String.format(
                                "Could not extract contents for license id %s with server %s, ignoring it",
                                id,
                                serverId));
            }
        }

        if (!repoEntry.isPresent()) {
            Logger.warn(LicenseRepoDAO.class, String.format("License could not be assigned to server %s", serverId));
        }

        return repoEntry;
    }

    /**
     * Check for server duplicity by fetching sitelic table rows matching current server id and not matching the
     * server start time. 
     *
     * @param serverId server id
     * @param license license raw data
     * @param startTime server start time
     * @return true if more than zero records are found, otherwise false
     */
    @CloseDBIfOpened
    public static boolean isServerDuplicated(final String serverId,
                                             final String license,
                                             final long startTime) throws DotDataException {
        String sql = "SELECT id FROM sitelic WHERE (serverid = ? OR license = ?) AND startup_time != ?";

        //LOBs are not supported in comparison conditions.
        // However, you can use PL/SQL programs for comparisons on CLOB data.
        if(DbConnectionFactory.isOracle()) {
            sql = "SELECT id FROM sitelic WHERE (serverid = ? OR to_char(license) = ?) AND startup_time != ?";
        }
        return !new DotConnect()
                .setSQL(sql)
                .addParam(serverId)
                .addParam(license)
                .addParam(startTime)
                .loadObjectResults()
                .isEmpty();
    }

    /**
     * Meant to be executed at start up time to update the server startup time.
     *
     * @param serverId server id
     * @param license license raw data
     * @param startTime server start time
     */
    @WrapInTransaction
    public static void updateLicenseStartTime(final String serverId,
                                              final String license,
                                              final long startTime) throws DotDataException {
        String sql = "UPDATE sitelic SET startup_time = ? WHERE serverid = ? AND license = ?";

        //LOBs are not supported in comparison conditions.
        // However, you can use PL/SQL programs for comparisons on CLOB data.
        if(DbConnectionFactory.isOracle()) {
            sql = "UPDATE sitelic SET startup_time = ? WHERE serverid = ? AND to_char(license) = ?";
        }

        new DotConnect()
                .setSQL(sql)
                .addParam(startTime)
                .addParam(serverId)
                .addParam(license)
                .loadObjectResults();
    }

}

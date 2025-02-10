/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.cluster.ServerFactoryImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final public class LicenseRepoDAO {

    
    private static Date licenseTimeout(){

        return ServerFactoryImpl.getServerTimeout();
        
    }
    

    



    @WrapInTransaction
    protected static void updateLastPing() throws DotDataException {

        final String sql = "UPDATE sitelic SET lastping=? WHERE serverid=?";
        Object[] params = {
                Calendar.getInstance().getTime(),
                ServerAPI.SERVER_ID.get()
        };

        int updated = new DotConnect().executeUpdate(sql, params);
        if(updated==0){
            Logger.warn(LicenseRepoDAO.class, "No license found, applying a new one");
            DotLicense license = LicenseManager.getInstance().myLicense.get();
            insertDefaultLicense();
        }
    }



    @WrapInTransaction
    protected static void insertDefaultLicense() throws DotDataException {
        DotLicense license = new DotLicense();
        String serial = license.serial;
        String licenseRaw = license.raw;
        String serverId = ServerAPI.SERVER_ID.get();
        DotConnect dc=new DotConnect();
        dc.executeUpdate("DELETE from sitelic WHERE id=? or serverid=?", serial, serverId);
        dc.setSQL("INSERT INTO sitelic(id,license,lastping,startup_time, serverid) VALUES(?,?,now(),?, ?)");
        dc.addParam(serial);
        dc.addParam(licenseRaw);
        dc.addParam(ManagementFactory.getRuntimeMXBean().getStartTime());
        dc.addParam(serverId);
        dc.loadResult();

    }


    @WrapInTransaction
    protected static int getLicenseRepoTotal() throws DotDataException {
        final DotConnect dc=new DotConnect();
        dc.setSQL("SELECT COUNT(*) AS cc FROM sitelic");
        return dc.getInt("cc");
    }

    @CloseDBIfOpened
    protected static int getLicenseRepoAvailableCount() throws DotDataException {
        final  DotConnect dc=new DotConnect();
        dc.setSQL("SELECT COUNT(*) AS cc FROM sitelic WHERE serverid IS NULL OR lastping<?");
        dc.addParam(licenseTimeout());
        return dc.getInt("cc");
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

    /**
     * This deletes unused licenses that have not been pinged in the timeout peroid
     * @throws DotDataException
     */
    @WrapInTransaction
    public static void deleteOldLicenses() throws DotDataException {

        DotConnect dc=new DotConnect();
        dc.setSQL("DELETE FROM sitelic WHERE lastping < ?");
        dc.addParam(licenseTimeout());

        dc.loadResult();
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




}

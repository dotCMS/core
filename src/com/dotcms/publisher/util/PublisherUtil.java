package com.dotcms.publisher.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimetypesFileTypeMap;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import edu.emory.mathcs.backport.java.util.Arrays;
/**
 * This class manage all the operation we can do over a from/to a PublishQueue index (search, add and delete)
 * @author Oswaldo
 *
 */
public class PublisherUtil {

	/**
	 * Returns an object represent the single row of publishing_end_point table.
	 * We descrypt the auth_key in this case.
	 *
	 * Oct 30, 2012 - 11:21:23 AM
	 */
	public static PublishingEndPoint getObjectByMap(Map<String, Object> row){
		PublishingEndPoint pep = new PublishingEndPoint();
		pep.setId(row.get("id").toString());
		if(row.get("group_id") != null){
			pep.setGroupId(row.get("group_id").toString());
		}
		pep.setAddress(row.get("address").toString());
		pep.setPort(row.get("port").toString());
		pep.setProtocol(row.get("protocol").toString());
		pep.setServerName(new StringBuilder(row.get("server_name").toString()));
		pep.setAuthKey(new StringBuilder(row.get("auth_key").toString()));

		if(row.get("sending").toString().equals("1") || row.get("sending").toString().equalsIgnoreCase("true")){
			pep.setSending(true);
		}else{
			pep.setSending(false);
		}

		if(row.get("enabled").toString().equals("1") || row.get("enabled").toString().equalsIgnoreCase("true")){
			pep.setEnabled(true);
		}else{
			pep.setEnabled(false);
		}

		return pep;
	}


	public static Set<String> getPropertiesSet(List<Map<String, Object>> list, String property) {
		Set<String> properties = new HashSet<String>();

		for(Map<String, Object> row : list) {
			properties.add((String) row.get(property));
		}

		return properties;
	}


	public static Environment getEnvironmentByMap(Map<String, Object> row){
		Environment e = new Environment();
		e.setId(row.get("id").toString());
		e.setName(row.get("name").toString());
		e.setPushToAll(DbConnectionFactory.isDBTrue(row.get("push_to_all").toString()));
		return e;
	}

	public static Bundle getBundleByMap(Map<String, Object> row){
		Bundle b = new Bundle();
		b.setId(row.get("id").toString());
		b.setName(row.get("name").toString());
		b.setPublishDate((Date)row.get("publish_date"));
		b.setExpireDate((Date)row.get("expire_date"));
		b.setOwner(row.get("owner").toString());
		b.setForcePush(row.get("force_push") == null ? false :DbConnectionFactory.isDBTrue(row.get("force_push").toString()));
		return b;
	}

	public static PushedAsset getPushedAssetByMap(Map<String, Object> row){
		PushedAsset b = new PushedAsset();
		b.setBundleId(row.get("bundle_id").toString());
		b.setAssetId(row.get("asset_id").toString());
		b.setAssetType(row.get("asset_type").toString());
		b.setPushDate((Date)row.get("push_date"));
		b.setEnvironmentId(row.get("environment_id").toString());
		return b;
	}

    /**
     * Returns the identifiers for given lucene queries
     *
     * @param luceneQueries
     * @return
     */
    public static List<String> getContentIds ( List<String> luceneQueries ) {

        List<String> ret = new ArrayList<String>();
        List<ContentletSearch> cs = new ArrayList<ContentletSearch>();
        for ( String luceneQuery : luceneQueries ) {
            try {
                cs = APILocator.getContentletAPI().searchIndex( luceneQuery, 0, 0, "moddate", APILocator.getUserAPI().getSystemUser(), false );
            } catch ( Exception e ) {
                Logger.error( PublisherUtil.class, e.getMessage(), e );
            }
        }
        for ( ContentletSearch contentletSearch : cs ) {
            if ( !ret.contains( contentletSearch.getIdentifier() ) ) {
                ret.add( contentletSearch.getIdentifier() );
            }
        }
        return ret;
    }

    private static final String IDENTIFIER = "identifier:";
	private static final int _ASSET_LENGTH_LIMIT = 20;

    public static List<String> prepareQueries ( List<PublishQueueElement> bundle ) {

        StringBuilder assetBuffer = new StringBuilder();
        List<String> assets;
        assets = new ArrayList<String>();

        if ( bundle.size() == 1 && bundle.get( 0 ).getType().equals( "contentlet" ) ) {
            assetBuffer.append( "+" + IDENTIFIER ).append( bundle.get( 0 ).getAsset() );

            assets.add( assetBuffer.toString() + " +live:true" );
            assets.add( assetBuffer.toString() + " +working:true" );

        } else {
            int counter = 1;
            PublishQueueElement c;
            for ( int ii = 0; ii < bundle.size(); ii++ ) {
                c = bundle.get( ii );

                if ( !c.getType().equals( "contentlet" ) ) {
                    if ( (counter == _ASSET_LENGTH_LIMIT || (ii + 1 == bundle.size())) && !assetBuffer.toString().isEmpty() ) {
                        assets.add( "+(" + assetBuffer.toString() + ") +live:true" );
                        assets.add( "+(" + assetBuffer.toString() + ") +working:true" );
                    }
                    continue;
                }

                assetBuffer.append( IDENTIFIER ).append( c.getAsset() );
                assetBuffer.append( " " );

                if ( counter == _ASSET_LENGTH_LIMIT || (ii + 1 == bundle.size()) ) {
                    assets.add( "+(" + assetBuffer.toString() + ") +live:true" );
                    assets.add( "+(" + assetBuffer.toString() + ") +working:true" );

                    assetBuffer = new StringBuilder();
                    counter = 0;
                } else
                    counter++;
            }
        }
        return assets;
    }

}
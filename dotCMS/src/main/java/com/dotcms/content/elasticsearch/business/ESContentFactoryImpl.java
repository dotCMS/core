package com.dotcms.content.elasticsearch.business;

import java.io.Serializable;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.functionscore.random.RandomScoreFunctionBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.util.NumberUtils;

import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.net.sf.hibernate.ObjectNotFoundException;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.query.ComplexCriteria;
import com.dotmarketing.business.query.Criteria;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.SimpleCriteria;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkFlowFactory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.NumberUtil;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Implementation class for the {@link ContentletFactory} interface. This class
 * represents the data layer used to query contentlet data from the database.
 *
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class ESContentFactoryImpl extends ContentletFactory {

	private ContentletCache cc ;
	private ESClient client;
	private LanguageAPI langAPI;

	private static final Contentlet cache404Content= new Contentlet();
	public static final String CACHE_404_CONTENTLET="CACHE_404_CONTENTLET";

	/**
	 * Default factory constructor that initializes the connection with the
	 * Elastic index.
	 */
	public ESContentFactoryImpl() {
	    cc = CacheLocator.getContentletCache();
	    langAPI =  APILocator.getLanguageAPI();
		client = new ESClient();
		cache404Content.setInode(CACHE_404_CONTENTLET);
	}

	@Override
	protected Object loadField(String inode, String fieldContentlet) throws DotDataException {
	    String sql="SELECT "+fieldContentlet+" FROM contentlet WHERE inode=?";
	    DotConnect dc=new DotConnect();
	    dc.setSQL(sql);
	    dc.addParam(inode);
	    ArrayList results=dc.loadResults();
	    if(results.size()==0) return null;
	    Map m=(Map)results.get(0);
	    return m.get(fieldContentlet);
	}

	@Override
	protected void cleanField(String structureInode, Field field) throws DotDataException, DotStateException, DotSecurityException {
	    StringBuffer sql = new StringBuffer("update contentlet set " );
        if(field.getFieldContentlet().indexOf("float") != -1){
        	if(DbConnectionFactory.isMySql())
       		 	sql.append(field.getFieldContentlet() + " = ");
       	    else
       	    	sql.append("\""+field.getFieldContentlet()+"\"" + " = ");
        }else{
            sql.append(field.getFieldContentlet() + " = ");
        }
        if(field.getFieldContentlet().indexOf("bool") != -1){
            sql.append(DbConnectionFactory.getDBFalse());
        }else if(field.getFieldContentlet().indexOf("date") != -1){
            sql.append(DbConnectionFactory.getDBDateTimeFunction());
        }else if(field.getFieldContentlet().indexOf("float") != -1){
            sql.append(0.0);
        }else if(field.getFieldContentlet().indexOf("integer") != -1){
            sql.append(0);
        }else{
            sql.append("''");
        }

        sql.append(" where structure_inode = ?");
        DotConnect dc = new DotConnect();
        dc.setSQL(sql.toString());
        dc.addParam(structureInode);
        dc.loadResult();
        //we could do a select here to figure out exactly which guys to evict
        cc.clearCache();
	}

	@Override
	protected void cleanIdentifierHostField(String structureInode) throws DotDataException, DotMappingException, DotStateException, DotSecurityException {
	    StringBuffer sql = new StringBuffer("update identifier set parent_path='/', host_inode=? ");
        sql.append(" where id in (select identifier from contentlet where structure_inode = ?)");
        DotConnect dc = new DotConnect();
        dc.setSQL(sql.toString());
        dc.addParam(APILocator.getHostAPI().findSystemHost().getIdentifier());
        dc.addParam(structureInode);
        dc.loadResults();
        //we could do a select here to figure out exactly which guys to evict
        cc.clearCache();
        CacheLocator.getIdentifierCache().clearCache();
	}

	@Override
	protected long contentletCount() throws DotDataException {
	    DotConnect dc = new DotConnect();
        dc.setSQL("select count(*) as count from contentlet");
        List<Map<String,String>> results = dc.loadResults();
        long count = Long.parseLong(results.get(0).get("count"));
        return count;
	}

	@Override
	protected long contentletIdentifierCount() throws DotDataException {
	    DotConnect dc = new DotConnect();
        if(DbConnectionFactory.isOracle()){
            dc.setSQL("select count(*) as count from (select distinct identifier from contentlet)");
        }else{
            dc.setSQL("select count(*) as count from (select distinct identifier from contentlet) as t");
        }

        List<Map<String,String>> results = dc.loadResults();
        long count = Long.parseLong(results.get(0).get("count"));
        return count;
	}

	@Override
	public com.dotmarketing.portlets.contentlet.business.Contentlet convertContentletToFatContentlet(Contentlet cont,
			com.dotmarketing.portlets.contentlet.business.Contentlet fatty) throws DotDataException {
	    String name = "";
        try {
            // If the contentlet doesn't have the identifier is pointless to call ContentletAPI().getName().
            if (UtilMethods.isSet(cont) && UtilMethods.isSet(cont.getIdentifier())){
                name = APILocator.getContentletAPI().getName(
                        cont, APILocator.getUserAPI().getSystemUser(), true);
            }
        }catch (DotSecurityException e) {

        }
        List<Field> fields = FieldsCache.getFieldsByStructureInode(cont.getStructureInode());
        for (Field f : fields) {
            if (f.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                continue;
            }
            if (f.getFieldType().equals(Field.FieldType.BINARY.toString())) {
                continue;
            }

            if(!APILocator.getFieldAPI().valueSettable(f)){
                continue;
            }
            Object value;
            value = cont.get(f.getVelocityVarName());
            try{
                fatty.setField(f, value);
            }catch (DotRuntimeException re) {
                throw new DotDataException("Unable to set field value",re);
            }
        }
        fatty.setInode(cont.getInode());
        fatty.setIdentifier(UtilMethods.isSet(cont.getIdentifier())?cont.getIdentifier():null);
        fatty.setSortOrder(new Long(cont.getSortOrder()).intValue());
        fatty.setStructureInode(cont.getStructureInode());
        fatty.setLanguageId(cont.getLanguageId());
        fatty.setNextReview(cont.getNextReview());
        fatty.setLastReview(cont.getLastReview());
        fatty.setOwner(cont.getOwner());
        fatty.setModUser(cont.getModUser());
        fatty.setModDate(cont.getModDate());
        fatty.setReviewInterval(cont.getReviewInterval());
        fatty.setTitle(name);
        fatty.setFriendlyName(name);
        List<String> wysiwygFields = cont.getDisabledWysiwyg();
        if( wysiwygFields != null && wysiwygFields.size() > 0 ) {
            StringBuilder wysiwyg = new StringBuilder();
            int j = 0;
            for(String wysiwygField : wysiwygFields ) {
                wysiwyg.append(wysiwygField);
                j++;
                if( j < wysiwygFields.size() ) wysiwyg.append(",");
            }
            fatty.setDisabledWysiwyg(wysiwyg.toString());
        }
        return fatty;
	}

	@Override
	public Contentlet convertFatContentletToContentlet(com.dotmarketing.portlets.contentlet.business.Contentlet fatty)
			throws DotDataException, DotStateException, DotSecurityException {
	    Contentlet contentlet = new Contentlet();


        contentlet.setStructureInode(fatty.getStructureInode());
        Map<String, Object> contentletMap = fatty.getMap();

        try {
            APILocator.getContentletAPI().copyProperties(contentlet, contentletMap);
        } catch (Exception e) {
            Logger.error(this,"Unable to copy contentlet properties",e);
            throw new DotDataException("Unable to copy contentlet properties",e);
        }
        contentlet.setInode(fatty.getInode());
        contentlet.setStructureInode(fatty.getStructureInode());
        contentlet.setIdentifier(fatty.getIdentifier());
        contentlet.setSortOrder(fatty.getSortOrder());
        contentlet.setLanguageId(fatty.getLanguageId());
        contentlet.setNextReview(fatty.getNextReview());
        contentlet.setLastReview(fatty.getLastReview());
        contentlet.setOwner(fatty.getOwner());
        contentlet.setModUser(fatty.getModUser());
        contentlet.setModDate(fatty.getModDate());
        contentlet.setReviewInterval(fatty.getReviewInterval());

	     if(UtilMethods.isSet(fatty.getIdentifier())){
	        IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
	        Identifier identifier = identifierAPI.find(fatty.getIdentifier());
	        Folder folder = null;
	        if(identifier.getParentPath().length()>1){
	            folder = APILocator.getFolderAPI().findFolderByPath(identifier.getParentPath(), identifier.getHostId(), APILocator.getUserAPI().getSystemUser(),false);
	        }else{
	            folder = APILocator.getFolderAPI().findSystemFolder();
	        }
	        contentlet.setHost(identifier.getHostId());
	        contentlet.setFolder(folder.getInode());

	        // lets check if we have publish/expire fields to set
	        Structure st=contentlet.getStructure();
	        if(UtilMethods.isSet(st.getPublishDateVar()))
	            contentlet.setDateProperty(st.getPublishDateVar(), identifier.getSysPublishDate());
	        if(UtilMethods.isSet(st.getExpireDateVar()))
	            contentlet.setDateProperty(st.getExpireDateVar(), identifier.getSysExpireDate());
		} else {
	        if(!UtilMethods.isSet(contentlet.getStructureInode())) {
	            throw new DotDataException("Contentlet must have a structure type.");
	        }

            if (contentlet.isSystemHost()) {
                // When we are saving a systemHost we cannot call
                // APILocator.getHostAPI().findSystemHost() method, because this
                // method will create a system host if not exist which cause 
                // a infinite loop.
                contentlet.setHost(Host.SYSTEM_HOST);
            } else {
                contentlet.setHost(APILocator.getHostAPI().findSystemHost().getIdentifier());
            }
            contentlet.setFolder(APILocator.getFolderAPI().findSystemFolder().getInode());
        }
        String wysiwyg = fatty.getDisabledWysiwyg();
        if( UtilMethods.isSet(wysiwyg) ) {
            List<String> wysiwygFields = new ArrayList<String>();
            StringTokenizer st = new StringTokenizer(wysiwyg,",");
            while( st.hasMoreTokens() ) wysiwygFields.add(st.nextToken().trim());
            contentlet.setDisabledWysiwyg(wysiwygFields);
        }
        return contentlet;
	}

	@Override
	protected List<Map<String, Serializable>> DBSearch(Query query, List<Field> fields, String structureInode) throws ValidationException,
			DotDataException {
	    Map<String, Field> velVarfieldsMap = null;
        Map<String, Field> fieldsMap = null;
        try {
            fieldsMap = UtilMethods.convertListToHashMap(fields, "getFieldContentlet", String.class);
        } catch (Exception e) {
            Logger.error(ESContentFactoryImpl.class,e.getMessage(),e);
            throw new DotDataException(e.getMessage(), e);
        }
        try {
            velVarfieldsMap = UtilMethods.convertListToHashMap(fields, "getVelocityVarName", String.class);
        } catch (Exception e) {
            Logger.error(ESContentFactoryImpl.class,e.getMessage(),e);
            throw new DotDataException(e.getMessage(), e);
        }
        List<Map<String, Serializable>> res = new ArrayList<Map<String,Serializable>>();
        Criteria c = query.getCriteria();
        StringBuilder bob = new StringBuilder();
        List<Object> params = null;

        bob.append("SELECT ");
        if(UtilMethods.isSet(query.getSelectAttributes())){
            String title = "inode";
            for (Field f : fields) {
                if(f.isListed()){
                    title = f.getFieldContentlet();
                    break;
                }
            }
            boolean first = true;
            for (String att : query.getSelectAttributes()) {
                if(!first){
                    bob.append(",");
                }
                if(velVarfieldsMap.get(att) != null){
                    bob.append(velVarfieldsMap.get(att).getFieldContentlet());
                }else{
                    bob.append(att);
                }
                first = false;
            }
            bob.append("," + title);
        }else{
            bob.append("*");
        }
        bob.append(" FROM contentlet WHERE structure_inode = '" + structureInode + "'");
        if(c != null){
            params = new ArrayList<Object>();
            if(c instanceof SimpleCriteria){
                bob.append(" AND ");
                String att = velVarfieldsMap.get(((SimpleCriteria) c).getAttribute()) != null ? velVarfieldsMap.get(((SimpleCriteria) c).getAttribute()).getFieldContentlet() : ((SimpleCriteria) c).getAttribute();
                bob.append(att + " " + ((SimpleCriteria) c).getOperator() + " ?");
                params.add(((SimpleCriteria) c).getValue());
            }else if(c instanceof ComplexCriteria){
                bob.append(" AND ");
                List<Criteria> criteriaList = ((ComplexCriteria) c).getCriteria();
                boolean open = false;
                for (Criteria criteria : criteriaList) {
                    if(criteria instanceof SimpleCriteria){
                        if(((ComplexCriteria)c).getPreceedingOperator(criteria) != null){
                            bob.append(" " + ((ComplexCriteria)c).getPreceedingOperator(criteria) + " ");
                            bob.append("(structure_inode = '" + structureInode + "' AND ");
                            open = true;
                        }
                        String att = velVarfieldsMap.get(((SimpleCriteria) criteria).getAttribute()) != null ? velVarfieldsMap.get(((SimpleCriteria) criteria).getAttribute()).getFieldContentlet() : ((SimpleCriteria) criteria).getAttribute();
                        bob.append(att + " " + ((SimpleCriteria) criteria).getOperator() + " ?");
                        if(open){
                            bob.append(")");
                            open = false;
                        }
                        params.add(((SimpleCriteria) criteria).getValue());
                    }else if(criteria instanceof ComplexCriteria){
                        if(((ComplexCriteria)c).getPreceedingOperator(criteria) != null){
                            bob.append(" " + ((ComplexCriteria)c).getPreceedingOperator(criteria) + " ");
                        }
                        bob.append(" (structure_inode = '" + structureInode + "' AND ");
                        buildComplexCriteria(structureInode, velVarfieldsMap, (ComplexCriteria)criteria, bob, params);
                        bob.append(")");
                    }
                }
            }
        }
        bob.append(";");
        DotConnect dc = new DotConnect();
        dc.setSQL(bob.toString());
        if(params != null){
            for (Object value : params) {
                dc.addParam(value);
            }
        }
        if(query.getStart() > 0){
            dc.setStartRow(query.getStart());
        }
        if(query.getLimit() > 0){
            dc.setStartRow(query.getLimit());
        }
        List<Map<String, String>> dbrows = dc.loadResults();
        for (Map<String, String> row : dbrows) {
            Map<String, Serializable> m = new HashMap<String, Serializable>();
            for (String colkey : row.keySet()) {
                if(colkey.startsWith("bool")){
                    if(fieldsMap.get(colkey) != null){
                        m.put(fieldsMap.get(colkey).getVelocityVarName(), new Boolean(row.get(colkey)));
                    }
                }else if(colkey.startsWith("float")){
                    if(fieldsMap.get(colkey) != null){
                        m.put(fieldsMap.get(colkey).getVelocityVarName(), new Float(row.get(colkey)));
                    }
                }else if(colkey.startsWith("date")){
                    if(fieldsMap.get(colkey) != null){
                        m.put(fieldsMap.get(colkey).getVelocityVarName(), row.get(colkey));
                    }
                }else if(colkey.startsWith("integer")){
                    if(fieldsMap.get(colkey) != null){
                        m.put(fieldsMap.get(colkey).getVelocityVarName(), new Integer(row.get(colkey)));
                    }
                }else if(colkey.startsWith("text")){
                    if(fieldsMap.get(colkey) != null){
                        m.put(fieldsMap.get(colkey).getVelocityVarName(), row.get(colkey));
                    }
                }else if(colkey.equals("working")){
                    if(fieldsMap.get(colkey) != null){
                        m.put(fieldsMap.get(colkey).getVelocityVarName(), new Boolean(row.get(colkey)));
                    }
                }else if(colkey.startsWith("deleted")){
                    if(fieldsMap.get(colkey) != null){
                        m.put(fieldsMap.get(colkey).getVelocityVarName(), new Boolean(row.get(colkey)));
                    }
                }else{
                    m.put(colkey, row.get(colkey));
                }
            }
            if(m.get("title") == null || !UtilMethods.isSet(m.get("title").toString())){
                boolean found = false;
                for (Field f : fields) {
                    if(f.isListed()){
                        m.put("title", row.get(f.getFieldContentlet()));
                        found = true;
                        break;
                    }
                }
                if(!found){
                    m.put("title", row.get("inode"));
                }
            }
            res.add(m);
        }
        return res;
	}

	/**
	 *
	 * @param structureInode
	 * @param velVarfieldsMap
	 * @param criteriaToBuildOut
	 * @param bob
	 * @param params
	 */
	private void buildComplexCriteria(String structureInode, Map<String, Field> velVarfieldsMap, ComplexCriteria criteriaToBuildOut, StringBuilder bob, List<Object> params){
        List<Criteria> cs = criteriaToBuildOut.getCriteria();
        boolean first = true;
        boolean open = false;
        for (Criteria criteria : cs) {
            if(criteria instanceof SimpleCriteria){
                if(!first){
                    bob.append(" " + criteriaToBuildOut.getPreceedingOperator(criteria) + " ");
                    bob.append("(structure_inode = '" + structureInode + "' AND ");
                    open = true;
                }
                String att = velVarfieldsMap.get(((SimpleCriteria) criteria).getAttribute()) != null ?
                                    velVarfieldsMap.get(((SimpleCriteria) criteria).getAttribute()).getFieldContentlet() :
                                    ((SimpleCriteria) criteria).getAttribute();
                bob.append(att + " " + ((SimpleCriteria) criteria).getOperator() + " ?");
                if(open){
                    bob.append(")");
                    open = false;
                }
                params.add(((SimpleCriteria) criteria).getValue());
            }else if(criteria instanceof ComplexCriteria){
                if(!first){
                    bob.append(" " + criteriaToBuildOut.getPreceedingOperator(criteria) + " ");
                }
                bob.append(" (structure_inode = '" + structureInode + "' AND ");
                buildComplexCriteria(structureInode, velVarfieldsMap, (ComplexCriteria)criteria, bob, params);
                bob.append(") ");
            }
            first = false;
        }
    }

	@Override
	protected void delete(List<Contentlet> contentlets) throws DotDataException {
		delete(contentlets, true);
	}

	@Override
	protected void delete(List<Contentlet> contentlets, boolean deleteIdentifier) throws DotDataException {
        /*
         First thing to do is to clean up the trees for the given Contentles
         */
        final int maxRecords = 500;
        List<String> inodes = new ArrayList<String>();

        for ( Contentlet contentlet : contentlets ) {
            inodes.add("'" + contentlet.getInode() + "'");

            //Another group of 500 contentles ids is ready...
            if ( inodes.size() >= maxRecords ) {
                deleteTreesForInodes( inodes );
                inodes = new ArrayList<String>();
            }
        }

        //And if is something left..
        if ( inodes.size() > 0 ) {
            deleteTreesForInodes( inodes );
        }

        //Now workflows, and versions
        List<String> identsDeleted = new ArrayList<String>();
        for (Contentlet con : contentlets) {
            cc.remove(con.getInode());

            // delete workflow task for contentlet
            WorkFlowFactory wff = FactoryLocator.getWorkFlowFactory();
            WorkflowTask wft = wff.findTaskByContentlet(con);
            if ( InodeUtils.isSet(wft.getInode() ) ) {
                wff.deleteWorkflowTask(wft);
            }

            //Remove the tag references to this Contentlet
            APILocator.getTagAPI().deleteTagInodesByInode(con.getInode());

            if(InodeUtils.isSet(con.getInode())){
                APILocator.getPermissionAPI().removePermissions(con);

                ContentletVersionInfo verInfo=APILocator.getVersionableAPI().getContentletVersionInfo(con.getIdentifier(), con.getLanguageId());
                if(verInfo!=null && UtilMethods.isSet(verInfo.getIdentifier())) {
                    if(UtilMethods.isSet(verInfo.getLiveInode()) && verInfo.getLiveInode().equals(con.getInode()))
                        try {
                            APILocator.getVersionableAPI().removeLive(con);
                        } catch (Exception e) {
                            throw new DotDataException(e.getMessage(),e);
                        }
                    if(verInfo.getWorkingInode().equals(con.getInode()))
                        APILocator.getVersionableAPI().deleteContentletVersionInfo(con.getIdentifier(), con.getLanguageId());
                }

                try {
                    com.dotmarketing.portlets.contentlet.business.Contentlet c =
                            (com.dotmarketing.portlets.contentlet.business.Contentlet)HibernateUtil.load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, con.getInode());
                    if(c!=null && InodeUtils.isSet(c.getInode())) {
                        HibernateUtil.delete(c);
                    }
                }
                catch(Exception ex) {
                    Logger.warn(this, "error deleting contentlet inode "+con.getInode()+". Maybe were deleted already?");
                }

            }
        }
        if (deleteIdentifier) {
	        for (Contentlet c : contentlets) {
	            if(InodeUtils.isSet(c.getInode())){
	                Identifier ident = APILocator.getIdentifierAPI().find(c.getIdentifier());
	                String si = ident.getInode();
	                if(!identsDeleted.contains(si) && si!=null && si!="" ){
	                    APILocator.getIdentifierAPI().delete(ident);
	                    identsDeleted.add(si);
	                }
	            }
	        }
        }
	}

    /**
     * Deletes from the tree and multi_tree tables Contentles given a list of
     * inodes.
     *
     * @param inodes
     *            List of contentles inodes
     */
    private void deleteTreesForInodes(List<String> inodes) throws DotDataException {
        DotConnect db = new DotConnect();
        try {
            final String sInodeIds = StringUtils.join(inodes, ",");

            // workaround for dbs where we can't have more than one constraint
            // or triggers
            db.executeStatement("delete from tree where child in (" + sInodeIds
                    + ") or parent in (" + sInodeIds + ")");

            // workaround for dbs where we can't have more than one constraint
            // or triggers
            db.executeStatement("delete from multi_tree where child in (" + sInodeIds
                    + ") or parent1 in (" + sInodeIds + ") or parent2 in (" + sInodeIds + ")");
        } catch (SQLException e) {
            throw new DotDataException("Error deleting tree and multi-tree.", e);
        }
    }

	@Override
	protected int deleteOldContent(Date deleteFrom) throws DotDataException {
	    ContentletCache cc = CacheLocator.getContentletCache();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(deleteFrom);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date date = calendar.getTime();
        //Because of the way Oracle databases handle dates,
        //this string is converted to Uppercase.This does
        //not cause a problem with the other databases
        DotConnect dc = new DotConnect();

        String countSQL = ("select count(*) as count from contentlet");
        dc.setSQL(countSQL);
        List<Map<String, String>> result = dc.loadResults();
        int before = Integer.parseInt(result.get(0).get("count"));

        String deleteContentletSQL = "delete from contentlet where identifier<>'SYSTEM_HOST' and mod_date < ? " +
        "and not exists (select * from contentlet_version_info where working_inode=contentlet.inode or live_inode=contentlet.inode)";
        dc.setSQL(deleteContentletSQL);
        dc.addParam(date);
        dc.loadResult();

        String deleteOrphanInodes="delete from inode where type='contentlet' and idate < ? and inode not in (select inode from contentlet)";
        dc.setSQL(deleteOrphanInodes);
        dc.addParam(date);
        dc.loadResult();

        dc.setSQL(countSQL);
        result = dc.loadResults();
        int after = Integer.parseInt(result.get(0).get("count"));

        int deleted=before - after;

        // deleting orphan binary files
        java.io.File assets=new java.io.File(APILocator.getFileAssetAPI().getRealAssetsRootPath());
        for(java.io.File ff1 : assets.listFiles())
            if(ff1.isDirectory() && ff1.getName().length()==1 && ff1.getName().matches("^[a-f0-9]$"))
                for(java.io.File ff2 : ff1.listFiles())
                    if(ff2.isDirectory() && ff2.getName().length()==1 && ff2.getName().matches("^[a-f0-9]$"))
                        for(java.io.File ff3 : ff2.listFiles())
                            try {
                                if(ff3.isDirectory()) {
                                    Contentlet con=find(ff3.getName());
                                    if(con==null || !UtilMethods.isSet(con.getIdentifier()))
                                        if(!FileUtils.deleteQuietly(ff3))
                                            Logger.warn(this, "can't delete "+ff3.getAbsolutePath());
                                }
                            }
                            catch(Exception ex) {
                                Logger.warn(this, ex.getMessage());
                            }


        return deleted;
	}

	@Override
	protected void deleteVersion(Contentlet contentlet) throws DotDataException {
	    String conInode = contentlet.getInode();
        DotConnect db = new DotConnect();
        db.setSQL("delete from tree where child = ? or parent = ?");
        db.addParam( conInode );
        db.addParam( conInode );
        db.getResult();

        // workaround for dbs where we can't have more than one constraint
        // or triggers
        db.setSQL( "delete from multi_tree where child = ? or parent1 = ? or parent2 = ?" );
        db.addParam(conInode);
        db.addParam(conInode);
        db.addParam(conInode);
        db.getResult();

        cc.remove(conInode);
        com.dotmarketing.portlets.contentlet.business.Contentlet c =
                (com.dotmarketing.portlets.contentlet.business.Contentlet) InodeFactory.getInode(conInode, com.dotmarketing.portlets.contentlet.business.Contentlet.class);
        //Checking contentlet exists inode > 0
        if(InodeUtils.isSet(c.getInode())){
            HibernateUtil.delete(c);
            APILocator.getPermissionAPI().removePermissions(contentlet);
        }
	}

	@Override
	protected Contentlet find(String inode) throws ElasticsearchException, DotStateException, DotDataException, DotSecurityException {
		Contentlet con = cc.get(inode);
		if (con != null && InodeUtils.isSet(con.getInode())) {
			if(CACHE_404_CONTENTLET.equals(con.getInode())){
				return null;
			}
			return con;
		}
		com.dotmarketing.portlets.contentlet.business.Contentlet fatty = null;
        try{
            fatty = (com.dotmarketing.portlets.contentlet.business.Contentlet)HibernateUtil.load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, inode);
        } catch (DotHibernateException e) {
            if(!(e.getCause() instanceof ObjectNotFoundException))
                throw e;
        }
        if(fatty == null){
        	cc.add(inode, cache404Content);
            return null;
        }else{
            Contentlet c = convertFatContentletToContentlet(fatty);
            cc.add(c.getInode(), c);
            return c;
        }
	}

	@Override
	protected List<Contentlet> findAllCurrent() throws DotDataException {
		throw new DotDataException("findAllCurrent() will blow your stack off, use findAllCurrent(offset, limit)");
	}

    @Override
    protected List<Contentlet> findAllCurrent ( int offset, int limit ) throws ElasticsearchException {

        QueryBuilder builder = QueryBuilders.matchAllQuery();

        SearchResponse response = client.getClient().prepareSearch()
                .setQuery( builder ).addFields("inode","identifier")
                .setSize( limit ).setFrom( offset ).execute().actionGet();
        SearchHits hits = response.getHits();
        List<Contentlet> cons = new ArrayList<Contentlet>();

        for ( SearchHit hit : hits ) {
            try {
                cons.add( find( hit.field("inode").getValue().toString() ) );
            } catch ( Exception e ) {
                throw new ElasticsearchException( e.getMessage(), e );
            }
        }

        return cons;
    }

	@Override
	protected List<Contentlet> findAllUserVersions(Identifier identifier) throws DotDataException, DotStateException, DotSecurityException {
	    List<Contentlet> cons = new ArrayList<Contentlet>();
        if(!InodeUtils.isSet(identifier.getInode()))
            return cons;
        HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
        hu.setQuery("select inode from inode in class " + com.dotmarketing.portlets.contentlet.business.Contentlet.class.getName() +
                " , vi in class "+ContentletVersionInfo.class.getName()+" where vi.identifier=inode.identifier and " +
                " inode.inode<>vi.workingInode and "+
                " mod_user <> 'system' and inode.identifier = '" + identifier.getInode() + "'" +
                " and type='contentlet' order by mod_date desc");
        List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
        if(fatties == null)
            return cons;
        else{
            for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
                Contentlet content = convertFatContentletToContentlet(fatty);
                cc.add(String.valueOf(content.getInode()), content);
                cons.add(content);
            }
        }
        return cons;
	}

	@Override
	protected List<Contentlet> findAllVersions(Identifier identifier) throws DotDataException, DotStateException, DotSecurityException {
	    if(!InodeUtils.isSet(identifier.getInode()))
            return new ArrayList<Contentlet>();

        DotConnect dc = new DotConnect();
        dc.setSQL("SELECT inode FROM contentlet WHERE identifier=? order by mod_date desc");
        dc.addObject(identifier.getId());
        List<Map<String,Object>> list=dc.loadObjectResults();
        ArrayList<String> inodes=new ArrayList<String>(list.size());
        for(Map<String,Object> r : list)
            inodes.add(r.get("inode").toString());
        return findContentlets(inodes);
	}

	@Override
	protected List<Contentlet> findByStructure(String structureInode, int limit, int offset) throws DotDataException, DotStateException, DotSecurityException {
	    HibernateUtil hu = new HibernateUtil();
        hu.setQuery("select inode from inode in class " + com.dotmarketing.portlets.contentlet.business.Contentlet.class.getName() +
                ", contentletvi in class "+ContentletVersionInfo.class.getName()+
                " where type = 'contentlet' and structure_inode = '" + structureInode + "' " +
                " and contentletvi.identifier=inode.identifier and contentletvi.workingInode=inode.inode ");
        if(offset > 0)
            hu.setFirstResult(offset);
        if(limit > 0)
            hu.setMaxResults(limit);
        List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties =  hu.list();
        List<Contentlet> result = new ArrayList<Contentlet>();
        for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
            Contentlet content = convertFatContentletToContentlet(fatty);
            cc.add(String.valueOf(content.getInode()), content);
            result.add(convertFatContentletToContentlet(fatty));
        }
        return result;
	}

	@Override
	protected Contentlet findContentletByIdentifier(String identifier, Boolean live, Long languageId) throws DotDataException {
		try {
			Client client = new ESClient().getClient();

			StringWriter sw= new StringWriter();
			sw.append(" +identifier:" + identifier);
			sw.append(" +languageid:" + languageId);
			sw.append(" +deleted:false");

			SearchRequestBuilder request = createRequest(client, sw.toString());

			IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
			SearchResponse response = request.setIndices((live ? info.live : info.working))
			        .addFields("inode","identifier").execute().actionGet();
			SearchHits hits = response.getHits();
			Contentlet contentlet = find(hits.getAt(0).field("inode").getValue().toString());
			return contentlet;
		}
		// if we don't have the con in this language
		catch(ArrayIndexOutOfBoundsException aibex){
			return null;
		}
		catch (Exception e) {
			throw new ElasticsearchException(e.getMessage());

		}
	}

	@Override
	protected Contentlet findContentletForLanguage(long languageId, Identifier identifier) throws DotDataException {
		return findContentletByIdentifier(identifier.getId(), false, languageId);
	}

	@Override
	protected List<Contentlet> findContentlets(List<String> inodes) throws DotDataException, DotStateException, DotSecurityException {

	    ArrayList<Contentlet> result = new ArrayList<Contentlet>();
        ArrayList<String> inodesNotFound = new ArrayList<String>();
        for (String i : inodes) {
            Contentlet c = cc.get(i);
            if(c != null && InodeUtils.isSet(c.getInode())){
                result.add(c);
            } else {
                inodesNotFound.add(i);
            }
        }
        if(inodesNotFound.isEmpty()){
            return result;
        }

        final String hql = "select {contentlet.*} from contentlet join inode contentlet_1_ " +
                "on contentlet_1_.inode = contentlet.inode and contentlet_1_.type = 'contentlet' where  contentlet.inode in ('";

        for(int init=0; init < inodesNotFound.size(); init+=200) {
            int end = Math.min(init + 200, inodesNotFound.size());

            HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
            hu.setSQLQuery( hql + StringUtils.join(inodesNotFound.subList(init, end), "','") + "')");

            List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties =  hu.list();
            for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
                Contentlet con = convertFatContentletToContentlet(fatty);
                result.add(con);
                cc.add(con.getInode(), con);
            }
            HibernateUtil.getSession().clear();
        }

        return result;
	}

	/**
	 *
	 * @param hostId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DotDataException
	 */
	protected List<Contentlet> findContentletsByHost(String hostId, int limit, int offset) throws DotDataException {
		try {

			SearchResponse response = createRequest(client.getClient(), "+conhost:"+hostId).
			        setSize(limit).setFrom(offset).addFields("inode","identifier").execute()
					.actionGet();

			SearchHits hits = response.getHits();

			List<Contentlet> cons = new ArrayList<Contentlet>();
			for (int i = 0; i < hits.getHits().length; i++) {
				try {
					cons.add(find(hits.getAt(i).field("inode").getValue().toString()));
				} catch (Exception e) {
					throw new ElasticsearchException(e.getMessage(),e);
				}
			}
			return cons;
		} catch (Exception e) {
			throw new ElasticsearchException(e.getMessage());
		}
	}

	@Override
	protected List<Contentlet> findContentletsByIdentifier(String identifier, Boolean live, Long languageId) throws DotDataException, DotStateException, DotSecurityException {
	    List<Contentlet> cons = new ArrayList<Contentlet>();
        StringBuilder queryBuffer = new StringBuilder();
        queryBuffer.append("select {contentlet.*} ")
                   .append("from contentlet, inode contentlet_1_, contentlet_version_info contentvi ")
                   .append("where contentlet_1_.type = 'contentlet' and contentlet.inode = contentlet_1_.inode and ")
                   .append("contentvi.identifier=contentlet.identifier and ")
                   .append(((live!=null && live.booleanValue()) ?
                            "contentvi.live_inode":"contentvi.working_inode"))
                   .append(" = contentlet_1_.inode ");

        if(languageId!=null){
            queryBuffer.append(" and contentvi.lang = ? ");
        }

        queryBuffer.append(" and contentlet.identifier = ? ");

        HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
        hu.setSQLQuery(queryBuffer.toString());
        if(languageId!=null){
          hu.setParam(languageId.longValue());
        }
        hu.setParam(identifier);
        List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
        for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
            Contentlet con = convertFatContentletToContentlet(fatty);
            cc.add(String.valueOf(con.getInode()), con);
            cons.add(con);
        }
        return cons;
	}

	@Override
	protected List<Contentlet> findContentletsWithFieldValue(String structureInode, Field field) throws DotDataException {
	    List<Contentlet> result = new ArrayList<Contentlet>();

        try {
            Structure structure = CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
            if ((structure == null) || (!InodeUtils.isSet(structure.getInode())))
                return result;

            if ((field == null) || (!InodeUtils.isSet(field.getInode())))
                return result;

            DotConnect dc = new DotConnect();
            String countSQL = ("select count(*) as count from contentlet, contentlet_version_info contentletvi" +
                               " where contentlet.identifier=contentletvi.identifier " +
                               " and contentletvi.live_inode=contentlet.inode " +
                               " and structure_inode= '" + structure.getInode() + "' and " +
                               field.getFieldContentlet() + " is not null and " +
                               field.getFieldContentlet() + "<>''");
            dc.setSQL(countSQL);
            List<HashMap<String, String>> resultCount = dc.getResults();
            int count = Integer.parseInt(resultCount.get(0).get("count"));
            int limit = 500;

            HibernateUtil hu = new HibernateUtil();
            hu.setQuery("from inode in class com.dotmarketing.portlets.contentlet.business.Contentlet, " +
                        " contentletvi in class "+ContentletVersionInfo.class.getName() +
                        " where contentletvi.identifier=inode.identifier " +
                        " and contentletvi.live_inode=inode.inode " +
                        " and structure_inode= '" + structure.getInode() + "' " +
                        " and " + field.getFieldContentlet() + " is not null" +
                        " and " + field.getFieldContentlet() + "<>'' " +
                        " order by " + field.getFieldContentlet());
            hu.setMaxResults(limit);
            for (int offset = 0; offset < count; offset+=limit) {
                if (offset > 0)
                    hu.setFirstResult(offset);
                List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties =  hu.list();
                for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
                    result.add(convertFatContentletToContentlet(fatty));
                }
            }
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(),e);
        }

        return result;
	}

	@Override
	protected List<Contentlet> findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working,
			long languageId) throws DotDataException, DotStateException, DotSecurityException {
	    
	    
       if(Config.getBooleanProperty("FIND_PAGE_CONTENTLETS_FROM_CACHE", false)){
            return findPageContentletFromCache(HTMLPageIdentifier, containerIdentifier, orderby, working, languageId);
       }
	    
	    
	    
	    StringBuilder condition = new StringBuilder();
        if (working) {
            condition.append("contentletvi.working_inode=contentlet.inode")
                     .append(" and contentletvi.deleted = ")
                     .append(DbConnectionFactory.getDBFalse());
        }
        else {
            condition.append("contentletvi.live_inode=contentlet.inode")
                     .append(" and contentletvi.deleted = ")
                     .append(DbConnectionFactory.getDBFalse());
        }

        if (languageId == 0) {
            languageId = langAPI.getDefaultLanguage().getId();
            condition.append(" and contentletvi.lang = ").append(languageId);
        }else if(languageId == -1){
            Logger.debug(this, "LanguageId is -1 so we will not use a language to pull contentlets");
        }else{
            condition.append(" and contentletvi.lang = ").append(languageId);
        }

        HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);

        if (!UtilMethods.isSet(orderby) || orderby.equals("tree_order")) {
            orderby = "multi_tree.tree_order";
        }
        String query = "SELECT {contentlet.*} FROM contentlet JOIN inode contentlet_1_ ON (contentlet.inode=contentlet_1_.inode) "
        	+ " JOIN multi_tree ON (multi_tree.child = contentlet.identifier) "
            + " JOIN contentlet_version_info contentletvi ON (contentlet.identifier=contentletvi.identifier) "
            + " where multi_tree.parent1 = ? and multi_tree.parent2 = ? and " + condition.toString() + " order by "
            + orderby;

        hu.setSQLQuery(query);
        hu.setParam(HTMLPageIdentifier);
        hu.setParam(containerIdentifier);

        List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties =  hu.list();
        List<Contentlet> result = new ArrayList<Contentlet>();
        for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
            Contentlet content = convertFatContentletToContentlet(fatty);
            cc.add(content.getInode(), content);
            result.add(content);
        }
        return result;
	}


	protected List<Contentlet> findPageContentletFromCache(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working,
            long languageId) throws DotDataException, DotStateException, DotSecurityException {
        StringBuilder condition = new StringBuilder();
        
        if (!UtilMethods.isSet(orderby) || orderby.equals("tree_order")) {
            orderby = " multi_tree.tree_order ";
        }
        languageId = (languageId==0) ?  langAPI.getDefaultLanguage().getId() : languageId;
        

        condition
            .append("select contentlet_version_info.{0} as mynode from contentlet_version_info, multi_tree ")
            .append(" where ")
            .append(" contentlet_version_info.identifier =  multi_tree.child " )
            .append(" and contentlet_version_info.deleted = ? ")
            .append(" and multi_tree.parent1 = ? ")
            .append(" and multi_tree.parent2 = ? ");
        if (languageId > 0) {
            condition.append(" and contentlet_version_info.lang = ").append(languageId);
        }

        int marker = condition.indexOf("{0}");
        if(working){
            condition.replace(marker, marker+3,"working_inode");
        }else{
            condition.replace(marker, marker+3,"live_inode");
        }

        DotConnect db = new DotConnect();
        db.setSQL(condition.toString());
        db.addParam(false);
        db.addParam(HTMLPageIdentifier);
        db.addParam(containerIdentifier);
        
        List<Map<String,Object>> res = db.loadObjectResults();
        List<Contentlet> cons = new ArrayList<>();
        for(Map<String,Object> map :res ){
            Contentlet c = find((String) map.get("mynode"));
            if(c!=null && c.getInode()!=null){
                cons.add(c);
            }
        }
        return cons;
    }
	
	
	
	
	
	@Override
	protected List<Contentlet> getContentletsByIdentifier(String identifier) throws DotDataException, DotStateException, DotSecurityException {
	    return getContentletsByIdentifier(identifier, null);
	}

	@Override
	protected List<Contentlet> getContentletsByIdentifier(String identifier, Boolean live) throws DotDataException, DotStateException, DotSecurityException {
	    StringBuilder queryBuffer = new StringBuilder();
        queryBuffer.append("SELECT {contentlet.*} ")
                   .append(" FROM contentlet JOIN inode contentlet_1_ ON (contentlet.inode = contentlet_1_.inode) ")
        		   .append(" JOIN contentlet_version_info contentletvi ON (contentlet.identifier=contentletvi.identifier) ")
                   .append(" WHERE ")
                   .append((live!=null && live.booleanValue() ?
                           "contentletvi.live_inode" : "contentletvi.working_inode"))
                   .append(" = contentlet.inode and contentlet.identifier = ? ");
        HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
        hu.setSQLQuery(queryBuffer.toString());
        hu.setParam(identifier);
        List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties =  hu.list();
        List<Contentlet> result = new ArrayList<Contentlet>();
        for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
            Contentlet content = convertFatContentletToContentlet(fatty);
            cc.add(content.getInode(), content);
            result.add(content);
        }
        return result;
	}

	@Override
	protected Identifier getRelatedIdentifier(Contentlet contentlet, String relationshipType) throws DotDataException {
	    String tableName;
        try {
            tableName = "identifier";
        } catch (Exception e) {
            throw new DotDataException("Unable to instantiate identifier",e);
        }
        HibernateUtil dh = new HibernateUtil(Identifier.class);

        String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
        + tableName + "_1_ where tree.parent = ? and "+ tableName+"_1_.type ='"+tableName+"' and tree.child = " + tableName + ".id and " + tableName
        + "_1_.inode = " + tableName + ".id and tree.relation_type = ?";

        Logger.debug(this, "HibernateUtilSQL:getChildOfClassByRelationType\n " + sql + "\n");

        dh.setSQLQuery(sql);

        Logger.debug(this, "contentlet inode:  " + contentlet.getInode() + "\n");

        dh.setParam(contentlet.getInode());
        dh.setParam(relationshipType);

        return (Identifier)dh.load();
	}

	@Override
	protected List<Link> getRelatedLinks(Contentlet contentlet) throws DotDataException {
	    HibernateUtil dh = new HibernateUtil(Link.class);

        Link l = new Link();
        String tableName = l.getType();

        String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
        + tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
        + "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type ='"+tableName+"'";

        Logger.debug(this, "HibernateUtilSQL:getRelatedLinks\n " + sql);

        dh.setSQLQuery(sql);

        Logger.debug(this, "inode:  " + contentlet.getInode() + "\n");

        dh.setParam(contentlet.getInode());

        return dh.list();
	}

	@Override
	protected long indexCount(String query) {
	    String qq=findAndReplaceQueryDates(translateQuery(query, null).getQuery());

	    // we check the query to figure out wich indexes to hit
        String indexToHit;
        IndiciesInfo info;
        try {
            info=APILocator.getIndiciesAPI().loadIndicies();
        }
        catch(DotDataException ee) {
            Logger.fatal(this, "Can't get indicies information",ee);
            return 0;
        }
        if(query.contains("+live:true") && !query.contains("+deleted:true"))
            indexToHit=info.live;
        else
            indexToHit=info.working;

        Client client=new ESClient().getClient();
        QueryStringQueryBuilder qb = QueryBuilders.queryString(qq);
        CountRequestBuilder crb = client.prepareCount();
        crb.setQuery(qb);
        crb.setIndices(indexToHit);
        return crb.execute().actionGet().getCount();
	}

    /**
     * It will call createRequest with null as sortBy parameter
     *
     * @param client
     * @param query
     * @return
     */
    private SearchRequestBuilder createRequest(Client client, String query) {
		return createRequest(client, query, null);
	}

    /**
     *
     * @param client
     * @param query
     * @param sortBy i.e. "random" or null object.
     * @return
     */
    private SearchRequestBuilder createRequest(Client client, String query, String sortBy) {




        if(Config.getBooleanProperty("ELASTICSEARCH_USE_FILTERS_FOR_SEARCHING",false) && sortBy!=null && ! sortBy.toLowerCase().startsWith("score")) {

            if("random".equals(sortBy)){
                return client.prepareSearch()
                        .setQuery(QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery(), new RandomScoreFunctionBuilder()))
                        .setPostFilter(FilterBuilders.queryFilter(QueryBuilders.queryString(query)).cache(true));
            } else {
                return client.prepareSearch()
                        .setQuery(QueryBuilders.matchAllQuery())
                        .setPostFilter(FilterBuilders.queryFilter(QueryBuilders.queryString(query)).cache(true));
            }

        } else {
            return client.prepareSearch().setQuery(QueryBuilders.queryString(query));
        }
    }

	@Override
	protected SearchHits indexSearch(String query, int limit, int offset, String sortBy) {
	    String qq=findAndReplaceQueryDates(translateQuery(query, sortBy).getQuery());

	    // we check the query to figure out wich indexes to hit
	    String indexToHit;
	    IndiciesInfo info;
	    try {
	        info=APILocator.getIndiciesAPI().loadIndicies();
	    }
	    catch(DotDataException ee) {
	        Logger.fatal(this, "Can't get indicies information",ee);
	        return null;
	    }
	    if(query.contains("+live:true") && !query.contains("+deleted:true"))
	        indexToHit=info.live;
	    else
	        indexToHit=info.working;

	    Client client=new ESClient().getClient();
	    SearchResponse resp = null;
        try {

        	SearchRequestBuilder srb = createRequest(client, qq, sortBy);

        	srb.setIndices(indexToHit);
        	srb.addFields("inode","identifier");

            if(limit>0)
                srb.setSize(limit);
            if(offset>0)
                srb.setFrom(offset);

            if(UtilMethods.isSet(sortBy) ) {
            	sortBy = sortBy.toLowerCase();
            	if(sortBy.endsWith("-order")) {
            	    // related content ordering
            	    int ind0=sortBy.indexOf('-'); // relationships tipicaly have a format stname1-stname2
            	    int ind1=ind0>0 ? sortBy.indexOf('-',ind0+1) : -1;
            	    if(ind1>0) {
            	        String relName=sortBy.substring(0, ind1);
            	        if((ind1+1)<sortBy.length()) {
                	        String identifier=sortBy.substring(ind1+1, sortBy.length()-6);
                	        if(UtilMethods.isSet(identifier)) {
                	            srb.addSort(SortBuilders.scriptSort("related", "number")
                	                                    .lang("native")
                	                                    .param("relName", relName)
                	                                    .param("identifier", identifier)
                	                                    .order(SortOrder.ASC));
                	        }
            	        }
            	    }
            	}
            	else if(sortBy.startsWith("score")){
            		String[] test = sortBy.split("\\s+");
            		String defaultSecondarySort = "moddate";
            		SortOrder defaultSecondardOrder = SortOrder.DESC;

            		if(test.length>2){
            			if(test[2].equalsIgnoreCase("desc"))
            				defaultSecondardOrder = SortOrder.DESC;
            			else
            				defaultSecondardOrder = SortOrder.ASC;
            		}
            		if(test.length>1){
            			defaultSecondarySort= test[1];
            		}

            		srb.addSort("_score", SortOrder.DESC);
            		srb.addSort(defaultSecondarySort, defaultSecondardOrder);
            	}
            	else if(!sortBy.startsWith("undefined") && !sortBy.startsWith("undefined_dotraw") && !sortBy.equals("random")) {
            		String[] sortbyArr=sortBy.split(",");
	            	for (String sort : sortbyArr) {
	            		String[] x=sort.trim().split(" ");
	            		srb.addSort(SortBuilders.fieldSort(x[0].toLowerCase() + "_dotraw").order(x.length>1 && x[1].equalsIgnoreCase("desc") ?
	                                SortOrder.DESC : SortOrder.ASC));

					}
            	}
            }



            try{
            	resp = srb.execute().actionGet();
            }catch (SearchPhaseExecutionException e) {
				if(e.getMessage().contains("dotraw] in order to sort on")){
					return new InternalSearchHits(InternalSearchHits.EMPTY,0,0);
				}else{
					throw e;
				}
			}
        } catch (Exception e) {
            Logger.debug(this, e.getMessage(), e); 
            throw new RuntimeException(e);
        }
	    return resp.getHits();
	}

	@Override
	protected void removeUserReferences(String userId) throws DotDataException, DotStateException, ElasticsearchException, DotSecurityException {
	   User systemUser =  APILocator.getUserAPI().getSystemUser();
       User userToReplace = APILocator.getUserAPI().loadUserById(userId);
	   updateUserReferences(userToReplace,systemUser.getUserId(), systemUser );
	}

        /**
         * Method will replace user references of the given User in Contentlets
         * with the replacement user id
         * @param userToReplace the user to replace
         * @param replacementUserId Replacement User Id
         * @param user the user requesting the operation
         * @exception DotDataException There is a data inconsistency
         * @throws DotSecurityException
         */
	protected void updateUserReferences(User userToReplace, String replacementUserId, User user) throws DotDataException, DotStateException, ElasticsearchException, DotSecurityException {
        DotConnect dc = new DotConnect();
        try {

            String tempKeyword = DbConnectionFactory.getTempKeyword();
            // CTU content-to-update table
            final String tableName = (DbConnectionFactory.isMsSql()?"#":"") + "CTU_"
                + UtilMethods.getRandomNumber(10000000);

            StringBuilder createTempTable = new StringBuilder();

            if (DbConnectionFactory.isMsSql()) {
                createTempTable.append("SELECT inode INTO ");
                createTempTable.append(tableName);
                createTempTable.append(" FROM contentlet WHERE mod_user = '");
                createTempTable.append(userToReplace.getUserId());
                createTempTable.append("'");
            } else {
                createTempTable.append("CREATE ");
                createTempTable.append(tempKeyword);
                createTempTable.append(" TABLE ");
                createTempTable.append(tableName);
                createTempTable.append(DbConnectionFactory.isOracle() ? " ON COMMIT PRESERVE ROWS " : " ");
                createTempTable.append("as select inode from contentlet ");
                createTempTable.append("where mod_user = '");
                createTempTable.append(userToReplace.getUserId());
                createTempTable.append("'");
            }

            dc.executeStatement(createTempTable.toString());

            dc.setSQL("UPDATE contentlet set mod_user = ? where mod_user = ? ");
            dc.addParam(replacementUserId);
            dc.addParam(userToReplace.getUserId());
            dc.loadResult();

            dc.setSQL("update contentlet_version_info set locked_by=? where locked_by  = ?");
            dc.addParam(replacementUserId);
            dc.addParam(userToReplace.getUserId());
            dc.loadResult();

            FlushCacheRunnable reindexContent = new FlushCacheRunnable() {
                @Override
                public void run() {

                    NotificationAPI notAPI = APILocator.getNotificationAPI();

                    try {
                        ESContentletIndexAPI indexAPI = new ESContentletIndexAPI();

                        DotConnect dc = new DotConnect();
                        dc.setSQL("select count(*) as count from " + tableName);
                        List<Map<String,String>> results = dc.loadResults();
                        long totalCount = Long.parseLong(results.get(0).get("count"));

                        Connection conn = DbConnectionFactory.getConnection();
                        try(PreparedStatement ps = conn.prepareStatement("select inode from " + tableName)) {

                            List<Contentlet> contentToIndex = new ArrayList<>();
                            int batchSize = 100;
                            int completed = 0;

                            try (ResultSet rs = ps.executeQuery()) {
                                for (int i = 1; rs.next(); i++) {
                                    String inode = rs.getString("inode");
                                    cc.remove(inode);
                                    Contentlet content = find(inode);
                                    contentToIndex.add(content);
                                    contentToIndex.addAll(indexAPI.loadDeps(content));

                                    if (i % batchSize == 0) {
                                        indexAPI.indexContentList(contentToIndex, null, false);
                                        completed += batchSize;
                                        contentToIndex = new ArrayList<>();
                                        HibernateUtil.getSession().clear();
                                        Logger.info(this,
                                            String.format("Reindexing related content after deletion of user %s. "
                                                + "Completed: " + completed + " out of " + totalCount,
                                            userToReplace.getUserId() + "/" + userToReplace.getFullName()));
                                    }
                                }

                                // index remaining records if any
                                if(!contentToIndex.isEmpty()) {
                                    indexAPI.indexContentList(contentToIndex, null, false);
                                }
                            }
                        }

                        dc.setSQL("DROP TABLE " + tableName);
                        dc.loadResult();

                        Logger.info(this, String.format("Reindex of updated related content after deleting user %s "
                                + " has finished successfully.",
                            userToReplace.getUserId() + "/" + userToReplace.getFullName()));

                    } catch (Exception e) {
                        Logger.error(this.getClass(),e.getMessage(),e);
                        notAPI.error(String.format("Unable to Reindex updated related content for deleted user '%s'. "
                            + "Please run a full Reindex.",
                            userToReplace.getUserId() + "/" + userToReplace.getFullName()), user.getUserId());
                    }
                }
            };

            HibernateUtil.addCommitListener(reindexContent);


        } catch (DotDataException | SQLException e) {
            Logger.error(this.getClass(),e.getMessage(),e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

	@Override
    protected Contentlet save(Contentlet contentlet) throws DotDataException, DotStateException, DotSecurityException {
	    return save(contentlet,null);
	}

	@Override
	protected Contentlet save(Contentlet contentlet, String existingInode) throws DotDataException, DotStateException, DotSecurityException {
	    com.dotmarketing.portlets.contentlet.business.Contentlet fatty = new com.dotmarketing.portlets.contentlet.business.Contentlet();
        if(InodeUtils.isSet(contentlet.getInode())){
            fatty = (com.dotmarketing.portlets.contentlet.business.Contentlet)HibernateUtil.load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, contentlet.getInode());
        }
        fatty = convertContentletToFatContentlet(contentlet, fatty);

        if(UtilMethods.isSet(existingInode))
            HibernateUtil.saveWithPrimaryKey(fatty, existingInode);
        else
            HibernateUtil.saveOrUpdate(fatty);

        final Contentlet content = convertFatContentletToContentlet(fatty);

        if (InodeUtils.isSet(contentlet.getHost())) {
            content.setHost(contentlet.getHost());
        }

        if (InodeUtils.isSet(contentlet.getFolder())) {
            content.setFolder(contentlet.getFolder());
        }

        cc.remove(content.getInode());
        cc.add(content.getInode(), content);
        HibernateUtil.evict(content);

        return content;
	}

	/**
	 *
	 * @param contentlets
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	protected void save(List<Contentlet> contentlets) throws DotDataException, DotStateException, DotSecurityException {
		for(Contentlet con : contentlets)
		    save(con);
	}

	@Override
	protected List<Contentlet> search(String query, int limit, int offset, String sortBy) throws DotDataException, DotStateException, DotSecurityException {
	    SearchHits hits = indexSearch(query, limit, offset, sortBy);
	    List<String> inodes=new ArrayList<String>();
	    for(SearchHit h : hits)
	        inodes.add(h.field("inode").getValue().toString());
	    return findContentlets(inodes);
	}

	@Override
	protected void UpdateContentWithSystemHost(String hostIdentifier) throws DotDataException {
		Host systemHost = APILocator.getHostAPI().findSystemHost();
		for (int i = 0; i < 10000; i++) {
			int offset = i * 1000;
			List<Contentlet> cons = findContentletsByHost(hostIdentifier, 1000, offset);
			List<String> ids = new ArrayList<String>();
			for (Contentlet con : cons)
				con.setHost(systemHost.getIdentifier());
		}
	}

	@Override
	protected void removeFolderReferences(Folder folder) throws DotDataException, DotStateException, ElasticsearchException, DotSecurityException {
	    Identifier folderId = null;
        try{
            folderId = APILocator.getIdentifierAPI().find(folder.getIdentifier());
        }catch(Exception e){
            Logger.debug(this, "Unable to get parent folder for folder = " + folder.getInode(), e);
        }
        DotConnect dc = new DotConnect();
        dc.setSQL("select identifier,inode from identifier,contentlet where identifier.id = contentlet.identifier and parent_path = ? and host_inode=?");
        dc.addParam(folderId.getPath());
        dc.addParam(folder.getHostId());
        List<HashMap<String, String>> contentInodes = dc.loadResults();
        dc.setSQL("update identifier set parent_path = ? where asset_type='contentlet' and parent_path = ? and host_inode=?");
        dc.addParam("/");
        dc.addParam(folderId.getPath());
        dc.addParam(folder.getHostId());
        dc.loadResult();
        for(HashMap<String, String> ident:contentInodes){
             String inode = ident.get("inode");
             cc.remove(inode);
             Contentlet content = find(inode);
             new ESContentletIndexAPI().addContentToIndex(content);
        }
	}

	///////////////////////////////////////////////////////
	////////// imported from old LuceneUtils //////////////
	///////////////////////////////////////////////////////

	   public static class TranslatedQuery implements Serializable {

	        private static final long serialVersionUID = 1L;
	        private String query;
	        private String sortBy;

	        /**
	         * @return the query
	         */
	        public String getQuery() {
	            return query;
	        }
	        /**
	         * @param query the query to set
	         */
	        public void setQuery(String query) {
	            this.query = query;
	        }
	        /**
	         * @return the sortBy
	         */
	        public String getSortBy() {
	            return sortBy;
	        }
	        /**
	         * @param sortBy the sortBy to set
	         */
	        public void setSortBy(String sortBy) {
	            this.sortBy = sortBy;
	        }
	    }

	/**
	 * Takes a dotCMS-generated query and translates it into valid terms and
	 * keywords for accessing the Elastic index.
	 *
	 * @param query
	 *            - The Lucene query.
	 * @param sortBy
	 *            - The parameter used to order the results.
	 * @return The translated query used to search content in our Elastic index.
	 */
	    public static TranslatedQuery translateQuery(String query, String sortBy) {

	        TranslatedQuery result = CacheLocator.getContentletCache().getTranslatedQuery(query + " --- " + sortBy);
	        if(result != null)
	            return result;

	        result = new TranslatedQuery();

	        String originalQuery = query;
	        Structure st;
	        String stInodestr = "structureInode";
	        String stInodeStrLowered = "structureinode";
	        String stNameStrLowered = "structurename";
	        String contentTypeInodeStr = "contentTypeInode";

	        if (query.contains(stNameStrLowered))
	            query = query.replace(stNameStrLowered,"structureName");

	        if (query.contains(stInodeStrLowered))
	            query = query.replace(stInodeStrLowered,stInodestr);

            if (query.toLowerCase().contains(contentTypeInodeStr.toLowerCase()))
                query = query.replace(contentTypeInodeStr,stInodestr);

	        if (query.contains(stInodestr)) {
	            // get structure information
	            int index = query.indexOf(stInodestr) + stInodestr.length() + 1;
	            String inode = null;
	            try {
	                inode = query.substring(index, query.indexOf(" ", index));
	            } catch (StringIndexOutOfBoundsException e) {
	                Logger.debug(ESContentFactoryImpl.class, e.toString());
	                inode = query.substring(index);
	            }
	            st = CacheLocator.getContentTypeCache().getStructureByInode(inode);
	            if (!InodeUtils.isSet(st.getInode()) || !UtilMethods.isSet(st.getVelocityVarName())) {
	                Logger.error(ESContentFactoryImpl.class,
	                        "Unable to find Structure or Structure Velocity Variable Name from passed in structureInode Query : "
	                                + query);

	                result.setQuery(query);
	                result.setSortBy(sortBy);

	                return result;
	            }

	            // replace structureInode
	            query = query.replace("structureInode:"+inode, "structureName:" + st.getVelocityVarName());

	            // handle the field translation
	            List<Field> fields = FieldsCache.getFieldsByStructureVariableName(st.getVelocityVarName());
	            Map<String, Field> fieldsMap;
	            try {
	                fieldsMap = UtilMethods.convertListToHashMap(fields, "getFieldContentlet", String.class);
	            } catch (Exception e) {
	                Logger.error(ESContentFactoryImpl.class, e.getMessage(), e);
	                result.setQuery(query);
	                result.setSortBy(sortBy);
	                return result;
	            }
	            String[] matcher = { "date", "text", "text_area", "integer", "float", "bool" };
	            for (String match : matcher) {
	                if (query.contains(match)) {
	                    List<RegExMatch> mathes = RegEX.find(query, match + "([1-9][1-5]?):");
	                    for (RegExMatch regExMatch : mathes) {
	                        String oldField = regExMatch.getMatch().substring(0, regExMatch.getMatch().indexOf(":"));
	                        query = query.replace(oldField, st.getVelocityVarName() + "."
	                                + fieldsMap.get(oldField).getVelocityVarName());
	                    }
	                }
	            }

	            // handle categories
	            String catRegExpr = "((c(([a-f0-9]{8,8})\\-([a-f0-9]{4,4})\\-([a-f0-9]{4,4})\\-([a-f0-9]{4,4})\\-([a-f0-9]{12,12}))c:on)|(c[0-9]*c:on))";//DOTCMS-4564
	            if (RegEX.contains(query, catRegExpr)) {
	                List<RegExMatch> mathes = RegEX.find(query, catRegExpr);
	                for (RegExMatch regExMatch : mathes) {
	                    try {
	                        String catInode = regExMatch.getGroups().get(0).getMatch().substring(1, regExMatch.getGroups().get(0).getMatch().indexOf("c:on"));
	                        query = query.replace(regExMatch.getMatch(), "categories:"
	                                + APILocator.getCategoryAPI().find(catInode,
	                                        APILocator.getUserAPI().getSystemUser(), true).getCategoryVelocityVarName());
	                    } catch (Exception e) {
	                        Logger.error(ESContentFactoryImpl.class, e.getMessage() + " : Error loading category", e);
	                        result.setQuery(query);
	                        result.setSortBy(sortBy);
	                        return result;
	                    }
	                }
	            }

	            result.setSortBy(translateQuerySortBy(sortBy, originalQuery));
	        }

	        //Pad Numbers
	        List<RegExMatch> numberMatches = RegEX.find(query, "(\\w+)\\.(\\w+):([0-9]+\\.?[0-9]+ |\\.?[0-9]+ |[0-9]+\\.?[0-9]+$|\\.?[0-9]+$)");
	        if(numberMatches != null && numberMatches.size() > 0){
	            for (RegExMatch numberMatch : numberMatches) {
	                List<Field> fields = FieldsCache.getFieldsByStructureVariableName(numberMatch.getGroups().get(0).getMatch());
	                for (Field field : fields) {
	                    if(field.getVelocityVarName().equalsIgnoreCase(numberMatch.getGroups().get(1).getMatch())){
	                        if (field.getFieldContentlet().startsWith("float")) {
	                            query = query.replace(numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + numberMatch.getGroups().get(2).getMatch(),
	                                    numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + NumberUtil.pad(NumberUtils.parseNumber((numberMatch.getGroups().get(2).getMatch()),Float.class)) + " ");
	                        }else if(field.getFieldContentlet().startsWith("integer")) {
	                            query = query.replace(numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + numberMatch.getGroups().get(2).getMatch(),
	                                    numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + NumberUtil.pad(NumberUtils.parseNumber((numberMatch.getGroups().get(2).getMatch()),Long.class)) + " ");
	                        }else if(field.getFieldContentlet().startsWith("bool")) {
	                            String oldSubQuery = numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + numberMatch.getGroups().get(2).getMatch();
	                            String oldFieldBooleanValue = oldSubQuery.substring(oldSubQuery.indexOf(":")+1,oldSubQuery.indexOf(":") + 2);
	                            String newFieldBooleanValue="";
	                            if(oldFieldBooleanValue.equals("1") || oldFieldBooleanValue.equals("true"))
	                                newFieldBooleanValue = "true";
	                            else if(oldFieldBooleanValue.equals("0") || oldFieldBooleanValue.equals("false"))
	                                newFieldBooleanValue = "false";
	                            query = query.replace(numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + numberMatch.getGroups().get(2).getMatch(),
	                                    numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + newFieldBooleanValue + " ");
	                        }
	                    }
	                }
	            }
	        }

	        if (UtilMethods.isSet(sortBy))
	            result.setSortBy(translateQuerySortBy(sortBy, query));

	        // DOTCMS-6247
	        query = query.toLowerCase();
	        //Pad NumericalRange Numbers
	        List<RegExMatch> numberRangeMatches = RegEX.find(query, "(\\w+)\\.(\\w+):\\[(([0-9]+\\.?[0-9]+ |\\.?[0-9]+ |[0-9]+\\.?[0-9]+|\\.?[0-9]+) to ([0-9]+\\.?[0-9]+ |\\.?[0-9]+ |[0-9]+\\.?[0-9]+|\\.?[0-9]+))\\]");
	        if(numberRangeMatches != null && numberRangeMatches.size() > 0){
	            for (RegExMatch numberMatch : numberRangeMatches) {
	                List<Field> fields = FieldsCache.getFieldsByStructureVariableName(numberMatch.getGroups().get(0).getMatch());
	                for (Field field : fields) {
	                    if(field.getVelocityVarName().equalsIgnoreCase(numberMatch.getGroups().get(1).getMatch())){
	                        if (field.getFieldContentlet().startsWith("float")) {
	                            query = query.replace(numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":[" + numberMatch.getGroups().get(3).getMatch() + " to " + numberMatch.getGroups().get(4).getMatch() +"]",
	                                    numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":[" + NumberUtil.pad(NumberUtils.parseNumber((numberMatch.getGroups().get(3).getMatch()),Float.class)) + " TO " + NumberUtil.pad(NumberUtils.parseNumber((numberMatch.getGroups().get(4).getMatch()),Float.class)) + "]");
	                        }else if(field.getFieldContentlet().startsWith("integer")) {
	                            query = query.replace(numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":[" + numberMatch.getGroups().get(3).getMatch() + " to " + numberMatch.getGroups().get(4).getMatch() +"]",
	                                    numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":[" + NumberUtil.pad(NumberUtils.parseNumber((numberMatch.getGroups().get(3).getMatch()),Long.class)) + " TO " + NumberUtil.pad(NumberUtils.parseNumber((numberMatch.getGroups().get(4).getMatch()),Long.class)) + "]");
	                        }
	                    }
	                }
	            }
	        }
	        result.setQuery(query.trim());

	        CacheLocator.getContentletCache().addTranslatedQuery(originalQuery + " --- " + sortBy, result);

	        return result;
	    }

	    /**
	     *
	     * @param sortBy
	     * @param originalQuery
	     * @return
	     */
	    private static String translateQuerySortBy(String sortBy, String originalQuery) {

	        if(sortBy == null)
	            return null;

	        List<RegExMatch> matches = RegEX.find(originalQuery,  "structureName:([^\\s)]+)");
	        List<Field> fields = null;
	        Structure structure = null;
	        if(matches.size() > 0) {
	            String structureName = matches.get(0).getGroups().get(0).getMatch();
	            fields = FieldsCache.getFieldsByStructureVariableName(structureName);
	            structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(structureName);
	        } else {
	            matches = RegEX.find(originalQuery, "structureInode:([^\\s)]+)");
	            if(matches.size() > 0) {
	                String structureInode = matches.get(0).getGroups().get(0).getMatch();
	                fields = FieldsCache.getFieldsByStructureInode(structureInode);
	                structure = CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
	            }
	        }

	        if(fields == null)
	            return sortBy;

	        Map<String, Field> fieldsMap;
	        try {
	            fieldsMap = UtilMethods.convertListToHashMap(fields, "getFieldContentlet", String.class);
	        } catch (Exception e) {
	            Logger.error(ESContentFactoryImpl.class, e.getMessage(), e);
	            return sortBy;
	        }

	        String[] matcher = { "date", "text", "text_area", "integer", "float", "bool" };
	        List<RegExMatch> mathes;
	        String oldField, oldFieldTrim, newField;
	        for (String match : matcher) {
	            if (sortBy.contains(match)) {
	                mathes = RegEX.find(sortBy, match + "([1-9][1-5]?)");
	                for (RegExMatch regExMatch : mathes) {
	                    oldField = regExMatch.getMatch();
	                    oldFieldTrim = oldField.replaceAll("[,\\s]", "");
	                    if(fieldsMap.get(oldFieldTrim) != null) {
	                        newField = oldField.replace(oldFieldTrim, structure.getVelocityVarName() + "." + fieldsMap.get(oldFieldTrim).getVelocityVarName());
	                        sortBy = sortBy.replace(oldField, newField);
	                    }
	                }
	            }
	        }

	        return sortBy;
	    }

	    /**
	     *
	     * @param query
	     * @return
	     */
        private static String findAndReplaceQueryDates(String query) {
            query = RegEX.replaceAll(query, " ", "\\s{2,}");

            List<RegExMatch> matches = RegEX.find(query, "[\\+\\-\\!\\(]?" + "structureName" + ":(\\S+)\\)?");
            String structureVarName = null;
            if ((matches != null) && (0 < matches.size()))
                structureVarName = matches.get(0).getGroups().get(0).getMatch();

            if (!UtilMethods.isSet(structureVarName)) {
                matches = RegEX.find(query, "[\\+\\-\\!\\(]?" + "structureName".toLowerCase() + ":(\\S+)\\)?");
                if ((matches != null) && (0 < matches.size()))
                    structureVarName = matches.get(0).getGroups().get(0).getMatch();
            }

            if (!UtilMethods.isSet(structureVarName)) {
                Logger.debug(ESContentFactoryImpl.class, "Structure Variable Name not found");
            }
            if(structureVarName!=null){
	            Structure selectedStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(structureVarName);
	            if ((selectedStructure == null) || !InodeUtils.isSet(selectedStructure.getInode())) {
	                Logger.debug(ESContentFactoryImpl.class, "Structure not found");
	            }
            }

            //delete additional blank spaces on date range
            if(UtilMethods.contains(query, "[ ")) {
                query = query.replace("[ ", "[");
            }

            if(UtilMethods.contains(query, " ]")) {
                query = query.replace(" ]", "]");
            }

            String clausesStr = RegEX.replaceAll(query, "", "[\\+\\-\\(\\)]*");
            String[] tokens = clausesStr.split(" ");
            String token;
            List<String> clauses = new ArrayList<String>();
            for (int pos = 0; pos < tokens.length; ++pos) {
                token = tokens[pos];
                if (token.matches("\\S+\\.\\S+:\\S*")) {
                    clauses.add(token);
                } else if (token.matches("\\d+:\\S*")) {
                    clauses.set(clauses.size() - 1, clauses.get(clauses.size() - 1) + " " + token);
                } else if (token.matches("\\[\\S*")) {
                    clauses.set(clauses.size() - 1, clauses.get(clauses.size() - 1) + token);
                } else if (token.matches("to")) {
                    clauses.set(clauses.size() - 1, clauses.get(clauses.size() - 1) + " " + token);
                } else if (token.matches("\\S*\\]")) {
                    clauses.set(clauses.size() - 1, clauses.get(clauses.size() - 1) + " " + token);
                } else if (token.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                    clauses.set(clauses.size() - 1, clauses.get(clauses.size() - 1) + " " + token);
                } else {
                    clauses.add(token);
                }
            }

            //DOTCMS - 4127
            List<Field> dateFields = new ArrayList<Field>();
            String tempStructureVarName;
            Structure tempStructure;

            for (String clause: clauses) {

                // getting structure names from query
                if(clause.indexOf('.') >= 0 && (clause.indexOf('.') < clause.indexOf(':'))){

                    tempStructureVarName = clause.substring(0, clause.indexOf('.'));
                    tempStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(tempStructureVarName);

                    List<Field> tempStructureFields = new ArrayList<>(FieldsCache.getFieldsByStructureVariableName(tempStructure.getVelocityVarName()));

                    for (int pos = 0; pos < tempStructureFields.size();) {

                        if (tempStructureFields.get(pos).getFieldType().equals(Field.FieldType.DATE_TIME.toString()) ||
                                tempStructureFields.get(pos).getFieldType().equals(Field.FieldType.DATE.toString()) ||
                                tempStructureFields.get(pos).getFieldType().equals(Field.FieldType.TIME.toString())) {
                            ++pos;
                        } else {
                            tempStructureFields.remove(pos);
                        }

                    }

                    dateFields.addAll(tempStructureFields);
                }
            }

            String replace;
            for (String clause: clauses) {
                for (Field field: dateFields) {

                    structureVarName = CacheLocator.getContentTypeCache().getStructureByInode(field.getStructureInode()).getVelocityVarName().toLowerCase();

                    if (clause.startsWith(structureVarName + "." + field.getVelocityVarName().toLowerCase() + ":") || clause.startsWith("moddate:")) {
                        replace = new String(clause);
                        if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()) || clause.startsWith("moddate:")) {
                            matches = RegEX.find(replace, "\\[(\\d{1,2}/\\d{1,2}/\\d{4}) to ");
                            for (RegExMatch regExMatch : matches) {
                                replace = replace.replace("[" + regExMatch.getGroups().get(0).getMatch() + " to ", "["
                                        + regExMatch.getGroups().get(0).getMatch() + " 00:00:00 to ");
                            }

                            matches = RegEX.find(replace, " to (\\d{1,2}/\\d{1,2}/\\d{4})\\]");
                            for (RegExMatch regExMatch : matches) {
                                replace = replace.replace(" to " + regExMatch.getGroups().get(0).getMatch() + "]", " to "
                                        + regExMatch.getGroups().get(0).getMatch() + " 23:59:59]");
                            }
                        }

                        // Format MM/dd/yyyy hh:mm:ssa
                        replace = replaceDateTimeWithFormat(replace, "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?", "MM/dd/yyyy hh:mm:ssa");

                        // Format MM/dd/yyyy hh:mm:ss a
                        replace = replaceDateTimeWithFormat(replace, "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?", "MM/dd/yyyy hh:mm:ss a");

                        // Format MM/dd/yyyy hh:mm a
                        replace = replaceDateTimeWithFormat(replace, "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?", "MM/dd/yyyy hh:mm a");

                        // Format MM/dd/yyyy hh:mma
                        replace = replaceDateTimeWithFormat(replace, "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?", "MM/dd/yyyy hh:mma");

                        // Format MM/dd/yyyy HH:mm:ss
                        replace = replaceDateTimeWithFormat(replace, "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2})\\\"?", null);

                        // Format MM/dd/yyyy HH:mm
                        replace = replaceDateTimeWithFormat(replace, "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2})\\\"?", null);

                        // Format MM/dd/yyyy
                        replace = replaceDateWithFormat(replace, "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4})\\\"?");

                        // Format hh:mm:ssa
                        replace = replaceTimeWithFormat(replace, "\\\"?(\\d{1,2}:\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?", "hh:mm:ssa");

                        // Format hh:mm:ss a
                        replace = replaceTimeWithFormat(replace, "\\\"?(\\d{1,2}:\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?", "hh:mm:ss a");

                        // Format HH:mm:ss
                        replace = replaceTimeWithFormat(replace, "\\\"?(\\d{1,2}:\\d{1,2}:\\d{1,2})\\\"?", "HH:mm:ss");

                        // Format hh:mma
                        replace = replaceTimeWithFormat(replace, "\\\"?(\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?", "hh:mma");

                        // Format hh:mm a
                        replace = replaceTimeWithFormat(replace, "\\\"?(\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?", "hh:mm a");

                        // Format HH:mm
                        replace = replaceTimeWithFormat(replace, "\\\"?(\\d{1,2}:\\d{1,2})\\\"?", "HH:mm");

                        query = query.replace(clause, replace);

                        break;
                    }
                }
            }

            matches = RegEX.find(query, "\\[([0-9]*)(\\*+) to ");
            for (RegExMatch regExMatch : matches) {
                query = query.replace("[" + regExMatch.getGroups().get(0).getMatch() + regExMatch.getGroups().get(1).getMatch() + " to ", "["
                        + regExMatch.getGroups().get(0).getMatch() + " to ");
            }

            matches = RegEX.find(query, " to ([0-9]*)(\\*+)\\]");
            for (RegExMatch regExMatch : matches) {
                query = query.replace(" to " + regExMatch.getGroups().get(0).getMatch() + regExMatch.getGroups().get(1).getMatch() + "]", " to "
                        + regExMatch.getGroups().get(0).getMatch() + "]");
            }

            matches = RegEX.find(query, "\\[([0-9]*) (to) ([0-9]*)\\]");
            if(matches.isEmpty()){
            	matches = RegEX.find(query, "\\[([a-z0-9]*) (to) ([a-z0-9]*)\\]");
            }
            for (RegExMatch regExMatch : matches) {
                query = query.replace("[" + regExMatch.getGroups().get(0).getMatch() + " to "
                        + regExMatch.getGroups().get(2).getMatch() + "]", "["
                        + regExMatch.getGroups().get(0).getMatch() + " TO " + regExMatch.getGroups().get(2).getMatch()
                        + "]");
            }

            //https://github.com/elasticsearch/elasticsearch/issues/2980
            if (query.contains( "/" )) {
                query = query.replaceAll( "/", "\\\\/" );
            }

            return query;
        }

        /**
         *
         * @param query
         * @param regExp
         * @param dateFormat
         * @return
         */
        private static String replaceDateTimeWithFormat(String query, String regExp, String dateFormat) {
            List<RegExMatch> matches = RegEX.find(query, regExp);
            String originalDate;
            String luceneDate;
            StringBuilder newQuery;
            int begin;
            if ((matches != null) && (0 < matches.size())) {
                newQuery = new StringBuilder(query.length() * 2);
                begin = 0;
                for (RegExMatch regExMatch : matches) {
                    originalDate = regExMatch.getMatch();

                    if (UtilMethods.isSet(dateFormat))
                        luceneDate = toLuceneDateWithFormat(originalDate, dateFormat);
                    else
                        luceneDate = toLuceneDateTime(originalDate);

                    newQuery.append(query.substring(begin, regExMatch.getBegin()) + luceneDate);
                    begin = regExMatch.getEnd();
                }

                return newQuery.append(query.substring(begin)).toString();
            }

            return query;
        }

        private final static String ERROR_DATE = "error date";

        /**
         *
         * @param dateString
         * @param format
         * @return
         */
        private static String toLuceneDateWithFormat(String dateString, String format) {
            try {
                if (!UtilMethods.isSet(dateString))
                    return "";

                SimpleDateFormat sdf = new SimpleDateFormat(format);
                Date date = sdf.parse(dateString);
                String returnValue = toLuceneDate(date);

                return returnValue;
            } catch (Exception ex) {
                Logger.error(ESContentFactoryImpl.class, ex.toString());
                return ERROR_DATE;
            }
        }

        /**
         *
         * @param date
         * @return
         */
        private static String toLuceneDate(Date date) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                String returnValue = df.format(date);
                return returnValue;
            } catch (Exception ex) {
                Logger.error(ESContentFactoryImpl.class, ex.toString());
                return ERROR_DATE;
            }
        }

        /**
         *
         * @param dateString
         * @return
         */
        private static String toLuceneDateTime(String dateString) {
            String format = "MM/dd/yyyy HH:mm:ss";
            String result = toLuceneDateWithFormat(dateString, format);
            if (result.equals(ERROR_DATE)) {
                format = "MM/dd/yyyy HH:mm";
                result = toLuceneDateWithFormat(dateString, format);
            }
            return result;
        }

        /**
         *
         * @param query
         * @param regExp
         * @return
         */
        private static String replaceDateWithFormat(String query, String regExp) {
            List<RegExMatch> matches = RegEX.find(query, regExp);
            String originalDate;
            String luceneDate;
            StringBuilder newQuery;
            int begin;
            if ((matches != null) && (0 < matches.size())) {
                newQuery = new StringBuilder(query.length() * 2);
                begin = 0;
                for (RegExMatch regExMatch : matches) {
                    originalDate = regExMatch.getMatch();

                    luceneDate = toLuceneDate(originalDate);
                    luceneDate = luceneDate.substring(0, 8) + "*";

                    newQuery.append(query.substring(begin, regExMatch.getBegin()) + luceneDate);
                    begin = regExMatch.getEnd();
                }

                return newQuery.append(query.substring(begin)).toString();
            }

            return query;
        }

        /**
         *
         * @param query
         * @param regExp
         * @param timeFormat
         * @return
         */
        private static String replaceTimeWithFormat(String query, String regExp, String timeFormat) {
            List<RegExMatch> matches = RegEX.find(query, regExp);
            String originalDate;
            String luceneDate;
            StringBuilder newQuery;
            int begin;
            if ((matches != null) && (0 < matches.size())) {
                newQuery = new StringBuilder(query.length() * 2);
                begin = 0;
                for (RegExMatch regExMatch : matches) {
                    originalDate = regExMatch.getMatch();

                    luceneDate = toLuceneTimeWithFormat(originalDate, timeFormat);

                    newQuery.append(query.substring(begin, regExMatch.getBegin()) + luceneDate);
                    begin = regExMatch.getEnd();
                }

                return newQuery.append(query.substring(begin)).toString();
            }

            return query;
        }

        /**
         *
         * @param dateString
         * @param format
         * @return
         */
        private static String toLuceneTimeWithFormat(String dateString, String format) {
            try {
                if (!UtilMethods.isSet(dateString))
                    return "";

                SimpleDateFormat sdf = new SimpleDateFormat(format);
                Date time = sdf.parse(dateString);
                return toLuceneTime(time);
            } catch (Exception ex) {
                Logger.error(ESContentFactoryImpl.class, ex.toString());
                return ERROR_DATE;
            }
        }

        /**
         *
         * @param dateString
         * @return
         */
        private static String toLuceneDate(String dateString) {
            String format = "MM/dd/yyyy";
            return toLuceneDateWithFormat(dateString, format);
        }

        /**
         *
         * @param time
         * @return
         */
        private static String toLuceneTime(Date time) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("HHmmss");
                String returnValue = df.format(time);
                return returnValue;
            } catch (Exception ex) {
                Logger.error(ESContentFactoryImpl.class, ex.toString());
                return ERROR_DATE;
            }
        }

        /**
         *
         */
		public List<Map<String, String>> getMostViewedContent(String structureInode, Date startDate, Date endDate, User user) throws DotDataException {

			List<Map<String, String>> result = new ArrayList<Map<String, String>>();

			String sql = " select content_ident, sum(num_views) " +
					" from " +
					" ( select clickstream_request.associated_identifier as content_ident, count(clickstream_request.associated_identifier) as num_views " +
					" from contentlet, clickstream_request, contentlet_version_info " +
					" where contentlet.structure_inode = ? " +
					" and contentlet.inode=contentlet_version_info.live_inode " +
					" and contentlet_version_info.deleted = " + DbConnectionFactory.getDBFalse() +
					" and clickstream_request.associated_identifier = contentlet.identifier " +
					" and clickstream_request.timestampper between ? and ? " + // startDate and endDate
					" group by clickstream_request.associated_identifier " +
					" UNION ALL " +
					" select analytic_summary_content.inode as content_ident, sum(analytic_summary_content.hits) as num_views " +
					" from analytic_summary_content, analytic_summary , analytic_summary_period, contentlet,  contentlet_version_info " +
					" where analytic_summary_content.summary_id = analytic_summary.id " +
					" and analytic_summary.summary_period_id = analytic_summary_period.id " +
					" and analytic_summary_period.full_date between ? and ? " + // startDate and endDate
					" and contentlet.structure_inode = ? " +
					" and contentlet.inode= contentlet_version_info.live_inode " +
					" and contentlet_version_info.deleted = " + DbConnectionFactory.getDBFalse() +
					" and analytic_summary_content.inode = contentlet.identifier " +
					" group by content_ident  ) consolidated_tab " +
					" group by content_ident order by sum desc; ";

			DotConnect dc = new DotConnect();
	        dc.setSQL(sql);
	        dc.addParam(structureInode);
	        dc.addParam(startDate);
	        dc.addParam(endDate);
	        dc.addParam(startDate);
	        dc.addParam(endDate);
	        dc.addParam(structureInode);

	        List<Map<String, String>> contentIdentifiers = dc.loadResults();

	        PermissionAPI perAPI = APILocator.getPermissionAPI();
	        IdentifierAPI identAPI = APILocator.getIdentifierAPI();

	        for(Map<String, String> ident:contentIdentifiers){
	        	Identifier identifier = identAPI.find(ident.get("content_ident"));
	        	if(perAPI.doesUserHavePermission(identifier, PermissionAPI.PERMISSION_READ, user)){
	        		Map<String, String> h = new HashMap<String, String>();
	        		h.put("identifier", ident.get("content_ident"));
	        		h.put("numberOfViews", ident.get("numberOfViews"));
	        		result.add(h);
	        	}
	        }

			return result;
		}

	/**
	 * Finds every content in the system associated to the specified Content
	 * Type Inode and field and removes it completely.
	 *
	 * @param structureInode
	 *            - The Inode of the Content Type whose field will be deleted.
	 * @param field
	 *            - The {@link Field} that will be removed.
	 * @throws DotDataException
	 *             An error occurred when updating the contents.
	 */
    protected void clearField(String structureInode, Field field) throws DotDataException {
        // we are not a db field;
        if(field.getFieldContentlet() == null  || ! (field.getFieldContentlet().matches("^.*\\d+$"))){
          return;
        }
        Queries queries = getQueries(field);
        List<String> inodesToFlush = new ArrayList<>();

        Connection conn = DbConnectionFactory.getConnection();
        
        try(PreparedStatement ps = conn.prepareStatement(queries.getSelect())) {
            ps.setObject(1, structureInode);
            final int BATCH_SIZE = 200;

            try(ResultSet rs = ps.executeQuery();)
            {
            	PreparedStatement ps2 = conn.prepareStatement(queries.getUpdate());
                for (int i = 1; rs.next(); i++) {
                    String contentInode = rs.getString("inode");
                    inodesToFlush.add(contentInode);
                    ps2.setString(1, contentInode);
                    ps2.addBatch();

                    if (i % BATCH_SIZE == 0) {
                        ps2.executeBatch();
                    }
                }

                ps2.executeBatch(); // insert remaining records
            }

        } catch (SQLException e) {
            throw new DotDataException(String.format("Error clearing field '%s' for Content Type with ID: %s",
                    field.getVelocityVarName(), structureInode), e);

        }

        for (String inodeToFlush : inodesToFlush) {
            cc.remove(inodeToFlush);
        }
    }

    /**
     *
     * @param field
     * @return
     */
    public Queries getQueries(Field field) {

        StringBuilder select = new StringBuilder("SELECT inode FROM contentlet ");
        StringBuilder update = new StringBuilder("UPDATE contentlet SET ");
        StringBuilder whereField = new StringBuilder();
        boolean isFloatField = field.getFieldContentlet().contains("float");

        if (isFloatField) {
            if (DbConnectionFactory.isMySql()) {
                whereField.append("`").append(field.getFieldContentlet()).append("` IS NOT NULL AND `")
                        .append(field.getFieldContentlet()).append("` != ");
            } else if (DbConnectionFactory.isH2()) {
                whereField.append("\"").append(field.getFieldContentlet()).append("\" IS NOT NULL AND \"")
                        .append(field.getFieldContentlet()).append("\" != ");
            } else if ( DbConnectionFactory.isOracle() ) {
                whereField.append("'").append(field.getFieldContentlet()).append("' IS NOT NULL AND '")
                        .append(field.getFieldContentlet()).append("' != ");
            } else {
                whereField.append(field.getFieldContentlet()).append(" IS NOT NULL AND ").append(field.getFieldContentlet())
                        .append(" != ");
            }
        //https://github.com/dotCMS/core/issues/10245
        }else {
            whereField.append(field.getFieldContentlet()).append(" IS NOT NULL AND ");
            if ( field.getFieldContentlet().contains("text_area") ) {
                if ( DbConnectionFactory.isMsSql() ) {
                    whereField.append(" DATALENGTH (").append(field.getFieldContentlet()).append(")");
                } else if ( DbConnectionFactory.isOracle() ) {
                	whereField.append("TO_CHAR(").append(field.getFieldContentlet()).append(") != ");
                } else {
                    whereField.append(field.getFieldContentlet()).append(" != ");
                }
            } else {
                whereField.append(field.getFieldContentlet()).append(" != ");
            }
        }

        if (DbConnectionFactory.isMySql()) {
            update.append("`").append(field.getFieldContentlet()).append("`").append(" = ");
        } else if ( (DbConnectionFactory.isH2() || DbConnectionFactory.isOracle()) && isFloatField ) {
            update.append("\"").append(field.getFieldContentlet()).append("\"").append(" = ");
        }else{
            update.append(field.getFieldContentlet()).append(" = ");
        }

        if (field.getFieldContentlet().contains("bool")) {
            update.append(DbConnectionFactory.getDBFalse());
            whereField.append(DbConnectionFactory.getDBFalse());
        } else if (field.getFieldContentlet().contains("date")) {
            update.append(DbConnectionFactory.getDBDateTimeFunction());
            whereField.append(DbConnectionFactory.getDBDateTimeFunction());
        } else if (field.getFieldContentlet().contains("float")) {
            if ( DbConnectionFactory.isOracle() ) {
                update.append("'0.0'");// Oracle implicitly converts the character value to a NUMBER value,  implicitly converts '200' to 200:
                whereField.append("'0.0'");
            } else {
                update.append(0.0);
                whereField.append(0.0);
            }
        } else if (field.getFieldContentlet().contains("integer")) {
            update.append(0);
            whereField.append(0);
        } else {
            if (DbConnectionFactory.isMsSql() && field.getFieldContentlet().contains("text_area")){
                update.append("''");
                whereField.append(" > 0");
            }else {
                update.append("''");
                whereField.append("''");
            }
        }

        select.append(" WHERE structure_inode = ?").append(" AND (").append(whereField).append(")");
        update.append(" WHERE inode = ?");

        return new Queries().setSelect(select.toString()).setUpdate(update.toString());

    }

    public final class Queries {
        private String select;
        private String update;

        private Queries setSelect(String select) {
            this.select = select;
            return this;
        }

        private Queries setUpdate(String update) {
            this.update = update;
            return this;
        }

        public String getSelect() {
            return select;
        }

        public String getUpdate() {
            return update;
        }
    }

}

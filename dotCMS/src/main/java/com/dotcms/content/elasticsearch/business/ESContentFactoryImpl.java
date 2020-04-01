package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static com.dotmarketing.util.StringUtils.lowercaseStringExceptMatchingTokens;
import java.io.Serializable;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.NumberUtils;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.ESQueryCache;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.net.sf.hibernate.ObjectNotFoundException;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.query.ComplexCriteria;
import com.dotmarketing.business.query.Criteria;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.SimpleCriteria;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkFlowFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.NumberUtil;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

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
    private static final String[] ES_FIELDS = {"inode", "identifier"};
    private final ContentletCache contentletCache;
	private final LanguageAPI languageAPI;
	private final IndiciesAPI indiciesAPI;
	private final ESQueryCache queryCache;
    @VisibleForTesting
    public static final String CACHE_404_CONTENTLET = "CACHE_404_CONTENTLET";

    private static final Contentlet cache404Content = new Contentlet() {
        public String getInode() {
            return CACHE_404_CONTENTLET;
        }
    };

	@VisibleForTesting
	public static final String LUCENE_RESERVED_KEYWORDS_REGEX = "OR|AND|NOT|TO";

    /**
	 * Default factory constructor that initializes the connection with the
	 * Elastic index.
	 */
	public ESContentFactoryImpl() {
        this.contentletCache = CacheLocator.getContentletCache();
        this.languageAPI     =  APILocator.getLanguageAPI();
        this.indiciesAPI     = APILocator.getIndiciesAPI();
        this.queryCache = CacheLocator.getESQueryCache();
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
        contentletCache.clearCache();
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
        contentletCache.clearCache();
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
	        Identifier identifier = identifierAPI.loadFromDb(fatty.getIdentifier());

	        if(identifier==null) {
	            throw new DotStateException("Fatty's identifier not found in db. Fatty's inode: " + fatty.getInode()
                    + ". Fatty's identifier: " + fatty.getIdentifier());
            }

	        Folder folder = null;
	        if(!"/".equals(identifier.getParentPath())){
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
        List<String> inodes = new ArrayList<>();

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
            contentletCache.remove(con.getInode());

            // delete workflow task for contentlet
            final WorkFlowFactory workFlowFactory = FactoryLocator.getWorkFlowFactory();
            workFlowFactory.deleteWorkflowTaskByContentletIdAndLanguage(
                    con.getIdentifier(), con.getLanguageId());

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
                    com.dotmarketing.portlets.contentlet.business.Contentlet contentlet =
                            (com.dotmarketing.portlets.contentlet.business.Contentlet)HibernateUtil.load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, con.getInode());
                    if(contentlet!=null && InodeUtils.isSet(contentlet.getInode())) {

                        HibernateUtil.delete(contentlet);
                    }
                }
                catch(Exception ex) {
                    Logger.warn(this, "Error deleting contentlet inode "+con.getInode()+". Probably it was already deleted?");
                    Logger.debug(this, "Error deleting contentlet inode "+con.getInode()+". Probably it was already deleted?", ex);
                    this.checkOrphanInode (con.getInode());
                }

            }
            //Removes content from index
            APILocator.getContentletIndexAPI().removeContentFromIndex(con);
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
     * If the orphan exists will be deleted.
     * @param inode String
     */
    private void checkOrphanInode(final String inode) throws DotDataException {
         new DotConnect().setSQL("delete from inode where inode = ?").addParam(inode).loadResult();
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
            APILocator.getMultiTreeAPI().deleteMultiTreesForIdentifiers(inodes);
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
        APILocator.getMultiTreeAPI().deleteMultiTreesRelatedToIdentifier(conInode);

        contentletCache.remove(conInode);
        com.dotmarketing.portlets.contentlet.business.Contentlet c =
                (com.dotmarketing.portlets.contentlet.business.Contentlet) InodeFactory.getInode(conInode, com.dotmarketing.portlets.contentlet.business.Contentlet.class);
        //Checking contentlet exists inode > 0
        if(InodeUtils.isSet(c.getInode())){
            HibernateUtil.delete(c);
            Contentlet anyVersionContentlet = null;
            try {
                anyVersionContentlet = findContentletByIdentifierAnyLanguage(
                        contentlet.getIdentifier());
            } catch(DotSecurityException e) {
                Logger.debug(this,"Unable to find content in any version", e);
            }

            if(!UtilMethods.isSet(anyVersionContentlet) ||
                    !UtilMethods.isSet(anyVersionContentlet.getIdentifier())) {
                APILocator.getPermissionAPI().removePermissions(contentlet);
            }
        }
	}


    @Override
    public Optional<Contentlet> findInDb(final String inode) {
        try {
            if (inode != null) {
                com.dotmarketing.portlets.contentlet.business.Contentlet fatty = (com.dotmarketing.portlets.contentlet.business.Contentlet) HibernateUtil
                        .load(com.dotmarketing.portlets.contentlet.business.Contentlet.class,
                                inode);
                return Optional.ofNullable(convertFatContentletToContentlet(fatty));
            }
        } catch (DotDataException | DotSecurityException e) {
            if (!(e.getCause() instanceof ObjectNotFoundException)) {
                throw new DotRuntimeException(e);
            }
        }

        return Optional.empty();

    }
	
	
	
    @Override
    protected Contentlet find(final String inode) throws ElasticsearchException, DotStateException, DotDataException, DotSecurityException {
        Contentlet con = contentletCache.get(inode);
        if (con != null && InodeUtils.isSet(con.getInode())) {
            if (CACHE_404_CONTENTLET.equals(con.getInode())) {
                return null;
            }
            return con;
        }
        final Optional<Contentlet> dbContentlet = this.findInDb(inode);
        if (dbContentlet.isPresent()) {
            con = dbContentlet.get();
            contentletCache.add(con.getInode(), con);
            return con;
        } else {
            contentletCache.add(inode, cache404Content);
            return null;
        }

    }

	@Override
	protected List<Contentlet> findAllCurrent() throws DotDataException {
		throw new DotDataException("findAllCurrent() will blow your stack off, use findAllCurrent(offset, limit)");
	}

    @Override
    protected List<Contentlet> findAllCurrent ( int offset, int limit ) throws ElasticsearchException {

        String indexToHit;

        try {
            indexToHit = APILocator.getIndiciesAPI().loadIndicies().getWorking();
        }
        catch(DotDataException ee) {
            Logger.fatal(this, "Can't get indicies information",ee);
            return null;
        }

        SearchRequest searchRequest = new SearchRequest(indexToHit);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(limit);
        searchSourceBuilder.from(offset);
        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchSourceBuilder.fetchSource(new String[] {"inode"}, null);
        searchRequest.source(searchSourceBuilder);

        SearchHits  hits = cachedIndexSearch(searchRequest);
        
        List<Contentlet> cons = new ArrayList<>();

        for ( SearchHit hit : hits ) {
            try {
                Map<String, Object> sourceMap = hit.getSourceAsMap();
                cons.add( find( sourceMap.get("inode").toString()) );
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
                contentletCache.add(String.valueOf(content.getInode()), content);
                cons.add(content);
            }
        }
        return cons;
	}

    @Override
    protected List<Contentlet> findAllVersions(Identifier identifier) throws DotDataException, DotStateException, DotSecurityException {
        return findAllVersions(identifier, true);
    }

	@Override
	protected List<Contentlet> findAllVersions(Identifier identifier, boolean bringOldVersions) throws DotDataException, DotStateException, DotSecurityException {
	    if(!InodeUtils.isSet(identifier.getInode()))
            return new ArrayList<Contentlet>();

        DotConnect dc = new DotConnect();
        StringBuffer query = new StringBuffer();

        if(bringOldVersions) {
            query.append("SELECT inode FROM contentlet WHERE identifier=? order by mod_date desc");

        } else {
            query.append("SELECT inode FROM contentlet c INNER JOIN contentlet_version_info cvi "
                    + "ON (c.inode = cvi.working_inode OR c.inode = cvi.live_inode) "
                    + "WHERE c.identifier=? order by c.mod_date desc ");
        }

        dc.setSQL(query.toString());
        dc.addObject(identifier.getId());
        List<Map<String,Object>> list=dc.loadObjectResults();
        ArrayList<String> inodes=new ArrayList<String>(list.size());
        for(Map<String,Object> r : list)
            inodes.add(r.get("inode").toString());
        return findContentlets(inodes);
	}

    @Override
    protected List<Contentlet> findByStructure(String structureInode, int limit, int offset)
            throws DotDataException, DotStateException, DotSecurityException {
        return findByStructure(structureInode, null, limit, offset);
    }

    @Override
    protected List<Contentlet> findByStructure(String structureInode, Date maxDate, int limit,
            int offset) throws DotDataException, DotStateException, DotSecurityException {
        final HibernateUtil hu = new HibernateUtil();
        final StringBuilder select = new StringBuilder();
        select.append("select inode from inode in class ")
                .append(com.dotmarketing.portlets.contentlet.business.Contentlet.class.getName())
                .append(", contentletvi in class ").append(ContentletVersionInfo.class.getName())
                .append(" where type = 'contentlet' and structure_inode = '")
                .append(structureInode).append("' " );

        if (maxDate != null){
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (DbConnectionFactory.isOracle()) {
                select.append(" AND mod_date<=to_date('");
                select.append(format.format(maxDate));
                select.append("', 'YYYY-MM-DD HH24:MI:SS')");
            } else if (DbConnectionFactory.isMsSql()){
                    select.append(" AND mod_date <= CAST('");
                    select.append(format.format(maxDate));
                    select.append("' AS DATETIME)");
            } else {
                select.append(" AND mod_date<='");
                select.append(format.format(maxDate));
                select.append("'");
            }
        }

        select.append(" and contentletvi.identifier=inode.identifier and contentletvi.workingInode=inode.inode ");
        hu.setQuery(select.toString());

        if (offset > 0) {
            hu.setFirstResult(offset);
        }
        if (limit > 0) {
            hu.setMaxResults(limit);
        }
        List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
        List<Contentlet> result = new ArrayList<Contentlet>();
        for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
            Contentlet content = convertFatContentletToContentlet(fatty);
            contentletCache.add(String.valueOf(content.getInode()), content);
            result.add(convertFatContentletToContentlet(fatty));
        }
        return result;
    }

	@Override
	protected Contentlet findContentletByIdentifier(String identifier, Boolean live, Long languageId) throws DotDataException {
        final ContentletVersionInfo cvi = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, languageId);
        if(cvi == null  || UtilMethods.isEmpty(cvi.getIdentifier()) || (live && UtilMethods.isEmpty(cvi.getLiveInode()))) {
            return null;
        }
        return Try.of(()->find((live?cvi.getLiveInode():cvi.getWorkingInode()))).getOrElseThrow(e->new DotRuntimeException(e));
        
	}

	@Override
    protected Contentlet findContentletByIdentifierAnyLanguage(final String identifier) throws DotDataException, DotSecurityException {
	    
	    // Looking content up this way can avoid any DB hits as these calls are all cached.
	    final List<Language> langs = APILocator.getLanguageAPI().getLanguages();
	    for(final Language language : langs) {
	        final ContentletVersionInfo cvi = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, language.getId());
	        if(cvi != null  && UtilMethods.isSet(cvi.getIdentifier()) && !cvi.isDeleted()) {
	            return find(cvi.getWorkingInode());
	        }
	    }
	    return null;

    }

	@Override
	protected Contentlet findContentletForLanguage(long languageId, Identifier identifier) throws DotDataException {
		return findContentletByIdentifier(identifier.getId(), false, languageId);
	}

  @Override
  protected List<Contentlet> findContentlets(final List<String> inodes) throws DotDataException, DotStateException, DotSecurityException {

    final HashMap<String, Contentlet> conMap = new HashMap<>();
    for (String i : inodes) {
      Contentlet c = contentletCache.get(i);
      if (c != null && InodeUtils.isSet(c.getInode())) {
        conMap.put(c.getInode(), c);
      }
    }
    
    if (conMap.size() != inodes.size()) {
      List<String> missingCons = new ArrayList<>(CollectionUtils.subtract(inodes, conMap.keySet()));

      final String contentletBase = "select {contentlet.*} from contentlet join inode contentlet_1_ "
          + "on contentlet_1_.inode = contentlet.inode and contentlet_1_.type = 'contentlet' where  contentlet.inode in ('";

      for (int init = 0; init < missingCons.size(); init += 200) {
        int end = Math.min(init + 200, missingCons.size());

        HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
        final StringBuilder hql = new StringBuilder().append(contentletBase).append(StringUtils.join(missingCons.subList(init, end), "','"))
            .append("') order by contentlet.mod_date DESC");

        hu.setSQLQuery(hql.toString());

        List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
        for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
          Contentlet con = convertFatContentletToContentlet(fatty);
          conMap.put(con.getInode(), con);
          contentletCache.add(con.getInode(), con);
        }
        HibernateUtil.getSession().clear();
      }
    }
    
    return inodes.stream().map(inode -> conMap.get(inode)).filter(Objects::nonNull).collect(Collectors.toList());
  }

	/**
	 *
	 * @param hostId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DotDataException
	 */
	protected List<Contentlet> findContentletsByHost(final String hostId, final int limit,
            final int offset) {
		try {

		    final SearchRequest searchRequest = new SearchRequest();
            final SearchSourceBuilder searchSourceBuilder = createSearchSourceBuilder("+conhost:"
                    +hostId).size(limit).from(offset);
            searchRequest.source(searchSourceBuilder);
            return getContentletsFromSearchResponse(searchRequest);
		} catch (Exception e) {
			throw new ElasticsearchException(e.getMessage(), e);
		}
	}

    @NotNull
    private List<Contentlet> getContentletsFromSearchResponse(SearchRequest searchRequest) {
        

        SearchHits hits = cachedIndexSearch(searchRequest);

        List<Contentlet> cons = new ArrayList<>();
        for (int i = 0; i < hits.getHits().length; i++) {
            try {
                cons.add(find(hits.getAt(i).getSourceAsMap().get("inode").toString()));
            } catch (Exception e) {
                throw new ElasticsearchException(e.getMessage(),e);
            }
        }
        return cons;
    }

    @Override
	protected List<Contentlet> findContentletsByIdentifier(String identifier, Boolean live, Long languageId) throws DotDataException, DotStateException, DotSecurityException {
	    List<Contentlet> cons = new ArrayList<>();
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
            contentletCache.add(String.valueOf(con.getInode()), con);
            cons.add(con);
        }
        return cons;
	}

	@Override
	protected List<Contentlet> findContentletsWithFieldValue(String structureInode, Field field) throws DotDataException {
	    List<Contentlet> result = new ArrayList<>();

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
            languageId = languageAPI.getDefaultLanguage().getId();
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
            contentletCache.add(content.getInode(), content);
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
        languageId = (languageId==0) ?  languageAPI.getDefaultLanguage().getId() : languageId;
        

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
            contentletCache.add(content.getInode(), content);
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
	protected long indexCount(final String query) {
	    final String qq = LuceneQueryDateTimeFormatter
                .findAndReplaceQueryDates(translateQuery(query, null).getQuery());

	    // we check the query to figure out wich indexes to hit
        String indexToHit;
        IndiciesInfo info;
        try {
            info = APILocator.getIndiciesAPI().loadIndicies();
        }
        catch(DotDataException ee) {
            Logger.fatal(this, "Can't get indicies information",ee);
            return 0;
        }
        if(query.contains("+live:true") && !query.contains("+deleted:true")) {
            indexToHit = info.getLive();
        } else {
            indexToHit = info.getWorking();
        }

        SearchRequest searchRequest = getCountSearchRequest(qq);
        searchRequest.indices(indexToHit);

        final SearchHits hits = cachedIndexSearch(searchRequest);
       return hits.getTotalHits().value;
	}

    @Override
    protected long indexCount(final String query,
                        final long timeoutMillis) {

        final String queryStringQuery =
                LuceneQueryDateTimeFormatter.findAndReplaceQueryDates(translateQuery(query, null).getQuery());

        // we check the query to figure out which indexes to hit
        IndiciesInfo info;

        try {

            info = this.indiciesAPI.loadIndicies();
        } catch(DotDataException ee) {
            Logger.fatal(this, "Can't get indicies information",ee);
            return 0;
        }

        SearchRequest searchRequest = getCountSearchRequest(queryStringQuery);
        searchRequest.indices(query.contains("+live:true") && !query.contains("+deleted:true")?
                info.getLive(): info.getWorking());

        final SearchHits hits = cachedIndexSearch(searchRequest);
        return hits.getTotalHits().value;
    }

    @Override
    protected void indexCount(final String query,
                              final long timeoutMillis,
                              final Consumer<Long> indexCountSuccess,
                              final Consumer<Exception> indexCountFailure) {

        final String queryStringQuery =
                LuceneQueryDateTimeFormatter.findAndReplaceQueryDates(translateQuery(query, null).getQuery());

        // we check the query to figure out wich indexes to hit
        IndiciesInfo info;

        try {

            info=APILocator.getIndiciesAPI().loadIndicies();
        } catch(DotDataException ee) {
            Logger.fatal(this, "Can't get indicies information",ee);
            if (null != indexCountFailure) {

                indexCountFailure.accept(ee);
            }
            return;
        }

        SearchRequest searchRequest = getCountSearchRequest(queryStringQuery);
        searchRequest.indices(query.contains("+live:true") && !query.contains("+deleted:true")?
                info.getLive(): info.getWorking());

        RestHighLevelClientProvider.getInstance().getClient().searchAsync(searchRequest, RequestOptions.DEFAULT,
                        new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {

                indexCountSuccess.accept(searchResponse.getHits().getTotalHits().value);
            }

            @Override
            public void onFailure(Exception e) {

                if (null != indexCountFailure) {

                    indexCountFailure.accept(e);
                }
            }
        });
    }

    @NotNull
    private SearchRequest getCountSearchRequest(final String queryString) {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
        searchSourceBuilder.size(0);
        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

    /**
     * It will call createRequest with null as sortBy parameter
     *
     * @param query
     * @return
     */

    private SearchSourceBuilder createSearchSourceBuilder(final String query) {
        return createSearchSourceBuilder(query, null);
    }

    /**
     *
     * @param query
     * @param sortBy i.e. "random" or null object.
     * @return
     */

    private SearchSourceBuilder createSearchSourceBuilder(final String query, final String sortBy) {

        final SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.searchSource();

        QueryBuilder queryBuilder;
        QueryBuilder postFilter = null;

        searchSourceBuilder.fetchSource(ES_FIELDS, null);

        if(Config.getBooleanProperty("ELASTICSEARCH_USE_FILTERS_FOR_SEARCHING",false)
                && sortBy!=null && ! sortBy.toLowerCase().startsWith("score")) {

            if("random".equals(sortBy)){
                queryBuilder = QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery()
                        , new RandomScoreFunctionBuilder());
            } else {
                queryBuilder = QueryBuilders.matchAllQuery();
            }

            postFilter = QueryBuilders.queryStringQuery(query);

        } else {
            queryBuilder = QueryBuilders.queryStringQuery(query);
        }

        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));

        if(UtilMethods.isSet(postFilter)) {
            searchSourceBuilder.postFilter(postFilter);
        }

        return searchSourceBuilder;
    }

    


    

    SearchHits cachedIndexSearch(final SearchRequest searchRequest) {
        

        final Optional<SearchHits> optionalHits = queryCache.get(searchRequest);
        if(optionalHits.isPresent()) {
            return optionalHits.get();
        }
        try {
            SearchResponse response = RestHighLevelClientProvider.getInstance().getClient().search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits  = response.getHits();
            queryCache.put(searchRequest, hits);
            return hits;
        } catch (final ElasticsearchStatusException | IndexNotFoundException | SearchPhaseExecutionException e) {
            final String exceptionMsg = (null != e.getCause() ? e.getCause().getMessage() : e.getMessage());
            Logger.warn(this.getClass(), "----------------------------------------------");
            Logger.warn(this.getClass(), String.format("Elasticsearch error in index '%s'", (searchRequest.indices()!=null) ? String.join(",", searchRequest.indices()): "unknown"));
            Logger.warn(this.getClass(), String.format("ES Query: %s", String.valueOf(searchRequest.source()) ));
            Logger.warn(this.getClass(), String.format("Class %s: %s", e.getClass().getName(), exceptionMsg));
            Logger.warn(this.getClass(), "----------------------------------------------");
            return new SearchHits(new SearchHit[] {}, new TotalHits(0, Relation.EQUAL_TO), 0);
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when executing the Lucene Query [ %s ] : %s",
                            searchRequest.source().toString(), e.getMessage());
            Logger.warnAndDebug(ESContentFactoryImpl.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
            
        
        
    }
    

    
    
    @Override
    protected SearchHits indexSearch(final String query, final int limit, final int offset, String sortBy) {

        final String formattedQuery = LuceneQueryDateTimeFormatter
                .findAndReplaceQueryDates(translateQuery(query, sortBy).getQuery());

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
        if(query.contains("+live:true") && !query.contains("+deleted:true")) {
            indexToHit = info.getLive();
        } else {
            indexToHit = info.getWorking();
        }

        final SearchRequest searchRequest = new SearchRequest();
        SearchResponse response;
        final SearchSourceBuilder searchSourceBuilder = createSearchSourceBuilder(formattedQuery, sortBy);
        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchRequest.indices(indexToHit);

        if(limit>0) {
            searchSourceBuilder.size(limit);
        }
        if(offset>0) {
            searchSourceBuilder.from(offset);
        }
        if(UtilMethods.isSet(sortBy) ) {
            sortBy = sortBy.toLowerCase();

            if(sortBy.startsWith("score")){
                String[] sortByCriteria = sortBy.split("[,|\\s+]");
                String defaultSecondarySort = "moddate";
                SortOrder defaultSecondardOrder = SortOrder.DESC;

                if(sortByCriteria.length>2){
                    if(sortByCriteria[2].equalsIgnoreCase("desc")) {
                        defaultSecondardOrder = SortOrder.DESC;
                    } else {
                        defaultSecondardOrder = SortOrder.ASC;
                    }
                }
                if(sortByCriteria.length>1){
                    defaultSecondarySort= sortByCriteria[1];
                }

                searchSourceBuilder.sort("_score", SortOrder.DESC);
                searchSourceBuilder.sort(defaultSecondarySort, defaultSecondardOrder);
            } else if(!sortBy.startsWith("undefined") && !sortBy.startsWith("undefined_dotraw") && !sortBy.equals("random")) {
                addBuilderSort(sortBy, searchSourceBuilder);
            }
        }else{
            searchSourceBuilder.sort("moddate", SortOrder.DESC);
        }

        searchRequest.source(searchSourceBuilder);
        return cachedIndexSearch(searchRequest);


    }

    public static void addBuilderSort(String sortBy, SearchSourceBuilder srb) {
        String[] sortbyArr = sortBy.split(",");
        for (String sort : sortbyArr) {
            String[] x = sort.trim().split(" ");
            srb.sort(SortBuilders.fieldSort(x[0].toLowerCase() + "_dotraw")
                    .order(x.length > 1 && x[1].equalsIgnoreCase("desc") ?
                            SortOrder.DESC : SortOrder.ASC));

        }
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
	protected void updateUserReferences(final User userToReplace, final String replacementUserId, final User user) throws DotDataException, DotStateException, ElasticsearchException, DotSecurityException {
        final DotConnect dc = new DotConnect();
        try {
            dc.setSQL("UPDATE contentlet SET mod_user = ? WHERE mod_user = ?");
            dc.addParam(replacementUserId);
            dc.addParam(userToReplace.getUserId());
            dc.loadResult();

            dc.setSQL("UPDATE contentlet_version_info SET locked_by = ? WHERE locked_by = ?");
            dc.addParam(replacementUserId);
            dc.addParam(userToReplace.getUserId());
            dc.loadResult();

            HibernateUtil.addCommitListener(() -> {

                reindexReplacedUserContent(userToReplace, user);

            });
        } catch (DotDataException e) {
            Logger.error(this.getClass(),e.getMessage(),e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

    /**
     * Performs a re-indexation of contents whose user references have been updated with a new
     * user ID. This change requires contents to be re-indexed for them to have the correct
     * information. The Inodes of such contentlets are stored in a temporary table.
     *
     * @param userToReplace The user whose references will be removed.
     * @param user          The user performing this operation.
     */
    private void reindexReplacedUserContent(final User userToReplace, final User user) {
        final NotificationAPI notificationAPI = APILocator.getNotificationAPI();

        try {
            final StringBuilder luceneQuery = new StringBuilder();
            luceneQuery.append("+working:true +modUser:").append(userToReplace.getUserId());
            final int limit = 0;
            final int offset = -1;
            final List<ContentletSearch> contentlets = APILocator.getContentletAPI().searchIndex
                    (luceneQuery.toString(), limit, offset, null, user, false);
            long totalCount;

            if (UtilMethods.isSet(contentlets)) {
                final ContentletIndexAPIImpl indexAPI = new ContentletIndexAPIImpl();
                List<Contentlet> contentToIndex = new ArrayList<>();
                totalCount = contentlets.size();
                final int batchSize = 100;
                int completed = 0;
                int counter = 1;
                for (final ContentletSearch indexedContentlet : contentlets) {
                    // IMPORTANT: Remove contentlet from cache first
                    contentletCache.remove(indexedContentlet.getInode());

                    final Contentlet content = find(indexedContentlet.getInode());

                    IdentifierCache identifierCache = CacheLocator.getIdentifierCache();
                    identifierCache.removeContentletVersionInfoToCache(content.getIdentifier(), content.getLanguageId());
                    identifierCache.removeFromCacheByIdentifier(content.getIdentifier());

                    contentToIndex.add(content);
                    if (counter % batchSize == 0) {
                        indexAPI.addContentToIndex(contentToIndex);
                        completed += batchSize;
                        contentToIndex = new ArrayList<>();
                        Logger.info(this, String.format("Reindexing related content after " +
                                        "deletion of user '%s'. " + "Completed: " + completed + " out of " +
                                        totalCount,
                                userToReplace.getUserId() + "/" + userToReplace.getFullName()));
                        counter++;
                    }
                }
                // index remaining records if any
                if (!contentToIndex.isEmpty()) {
                    indexAPI.addContentToIndex(contentToIndex);
                }

                Logger.info(this, String.format("Reindexing of updated related content after " +
                    "deleting user '%s' has finished successfully.", userToReplace.getUserId()
                    + "/" + userToReplace.getFullName()));

                notificationAPI.generateNotification(
                    new I18NMessage("notification.contentapi.update.user.references"),
                    new I18NMessage(
                        "notification.contentapi.reindex.related.content.success", null,
                        userToReplace.getUserId()
                            + "/" + userToReplace.getFullName()),
                    null, // no actions
                    NotificationLevel.INFO,
                    NotificationType.GENERIC,
                    user.getUserId(),
                    user.getLocale()
                );


            }
        } catch (Exception e) {
            Logger.error(this.getClass(), String.format("Unable to reindex updated related content for " +
                "deleted " + "user '%s'. " + "Please run a full Reindex.", userToReplace.getUserId()
                + "/" + userToReplace.getFullName()), e);

            try {
                notificationAPI.generateNotification(
                    new I18NMessage("notification.contentapi.update.user.references"),
                    new I18NMessage(
                        "notification.contentapi.reindex.related.content.error", null,
                        userToReplace.getUserId()
                            + "/" + userToReplace.getFullName()),
                    null, // no actions
                    NotificationLevel.ERROR,
                    NotificationType.GENERIC,
                    user.getUserId(),
                    user.getLocale()
                );
            } catch (DotDataException e1) {
                Logger.error(this, "Unable to send error Notification", e);
            }
        }
    }

	@Override
    public Contentlet save(Contentlet contentlet) throws DotDataException, DotStateException, DotSecurityException {
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

        contentletCache.remove(content.getInode());
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
             contentletCache.remove(inode);
             Contentlet content = find(inode);
             new ContentletIndexAPIImpl().addContentToIndex(content);
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

	        TranslatedQuery result = CacheLocator.getContentletCache()
                    .getTranslatedQuery(query + " --- " + sortBy);

	        if(result != null) {
                result.setQuery(lowercaseStringExceptMatchingTokens(result.getQuery(),
                        LUCENE_RESERVED_KEYWORDS_REGEX));
                return result;
            }

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
	        query = lowercaseStringExceptMatchingTokens(query, LUCENE_RESERVED_KEYWORDS_REGEX);

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

	        CacheLocator.getContentletCache().addTranslatedQuery(
                    originalQuery + " --- " + sortBy, result);

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
     * @param maxDate
     *            - Date used to filter contents whose mod_date is less than or equals to
	 * @throws DotDataException
	 *             An error occurred when updating the contents.
	 */
    protected void clearField(String structureInode, Date maxDate, Field field) throws DotDataException {
        // we are not a db field;
        if(field.getFieldContentlet() == null  || ! (field.getFieldContentlet().matches("^.*\\d+$"))){
          return;
        }
        Queries queries = getQueries(field, maxDate);
        List<String> inodesToFlush = new ArrayList<>();

        Connection conn = DbConnectionFactory.getConnection();
        
        try(PreparedStatement ps = conn.prepareStatement(queries.getSelect())) {
            ps.setObject(1, structureInode);
            final int BATCH_SIZE = 200;

            try(ResultSet rs = ps.executeQuery())
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
            } catch (SQLException e) {
                Logger.error(this, String.format("Error clearing field '%s' for Content Type with ID: %s",
                        field.getVelocityVarName(), structureInode), e);
                throw new DotDataException(String.format("Error clearing field '%s' for Content Type with ID: %s",
                        field.getVelocityVarName(), structureInode), e);
            }

        } catch (SQLException e) {
            throw new DotDataException(String.format("Error clearing field '%s' for Content Type with ID: %s",
                    field.getVelocityVarName(), structureInode), e);

        }

        for (String inodeToFlush : inodesToFlush) {
            contentletCache.remove(inodeToFlush);
        }
    }

    protected void clearField(String structureInode, Field field) throws DotDataException {
        clearField(structureInode, null, field);
    }

    /**
     * @deprecated Use {@link ESContentFactoryImpl#getQueries(Field, Date)} instead
     * @param field
     * @return
     */
    @Deprecated
    public Queries getQueries(Field field) {
        return getQueries(field, null);
    }
    /**
     *
     * @param field
     * @return
     */
    public Queries getQueries(final Field field, final Date maxDate) {

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
                	whereField.append("LENGTH(").append(field.getFieldContentlet()).append(")");
                } else {
                    whereField.append(field.getFieldContentlet()).append(" != ");
                }
            } else {
                if (field.getFieldContentlet().contains("text") && DbConnectionFactory.isOracle()){
                    whereField.append(" TRIM(").append(field.getFieldContentlet()).append(") IS NOT NULL ");
                } else{
                    whereField.append(field.getFieldContentlet()).append(" != ");
                }
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
            if ((DbConnectionFactory.isMsSql() || DbConnectionFactory.isOracle()) && field.getFieldContentlet().contains("text_area")){
                update.append("''");
                whereField.append(" > 0");
            }else {
                update.append("''");
                if (!(field.getFieldContentlet().contains("text") && DbConnectionFactory.isOracle())){
                    whereField.append("''");
                }
            }
        }

        select.append(" WHERE structure_inode = ?").append(" AND (").append(whereField).append(")");

        if (maxDate != null) {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (DbConnectionFactory.isOracle()) {
                select.append(" AND mod_date<=to_date('");
                select.append(format.format(maxDate));
                select.append("', 'YYYY-MM-DD HH24:MI:SS')");
            } else if (DbConnectionFactory.isMsSql()){
                select.append(" AND mod_date <= CAST('");
                select.append(format.format(maxDate));
                select.append("' AS DATETIME)");
            } else {
                select.append(" AND mod_date<='");
                select.append(format.format(maxDate));
                select.append("'");
            }
        }

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


    /**
     * Basically this method updates the mod_date on a piece of content, given the respective inodes
     * @param inodes
     * @param user
     * @return
     * @throws DotDataException
     */
    @WrapInTransaction
    @Override
    public int updateModDate(final Set<String> inodes, final User user) throws DotDataException {
        if (inodes.isEmpty()) {
            return 0;
        }
        final String SQL_STATEMENT = "UPDATE contentlet SET mod_date = ?, mod_user = ? WHERE inode = ?";
        final Date now = DbConnectionFactory.now();
        final List<Params> updateParams = new ArrayList<>(inodes.size());
        for (final String inode : inodes) {
            updateParams.add(new Params(now, user.getUserId() ,inode));
        }
        final List<Integer> batchResult =
                Ints.asList(
                        new DotConnect().executeBatch(SQL_STATEMENT,
                                updateParams,
                                (preparedStatement, params) -> {
                                    final Date date = Date.class.cast(params.get(0));
                                    if (date instanceof java.sql.Date) {
                                        preparedStatement
                                                .setDate(1,
                                                        java.sql.Date.class.cast(date));
                                    } else if (date instanceof java.sql.Timestamp) {
                                        preparedStatement.setTimestamp(1,
                                                java.sql.Timestamp.class.cast(date));
                                    } else {
                                        Logger.error(getClass(),"Un-recognized SQL Date instance. "+date);
                                    }

                                    preparedStatement.setString(2,
                                            String.class.cast(params.get(1))
                                    );

                                    preparedStatement.setString(3,
                                            String.class.cast(params.get(2))
                                    );
                                })
                );

        final int count = batchResult.stream().reduce(0, Integer::sum);

        for(final String inode :inodes){
            contentletCache.remove(inode);
        }

        return count;
    }

}

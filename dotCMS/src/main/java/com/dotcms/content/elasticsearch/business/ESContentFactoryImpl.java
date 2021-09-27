package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.MAX_LIMIT;
import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.AUTO_ASSIGN_WORKFLOW;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITLE_IMAGE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_ACTION_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_ASSIGN_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_BULK_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_COMMENTS_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKFLOW_IN_PROGRESS;
import static com.dotmarketing.util.StringUtils.lowercaseStringExceptMatchingTokens;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.ESQueryCache;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.net.sf.hibernate.ObjectNotFoundException;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.system.SimpleMapAppContext;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.I18NMessage;
import com.dotcms.util.transform.TransformerLocator;
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
import com.dotmarketing.db.commands.DatabaseCommand.QueryReplacements;
import com.dotmarketing.db.commands.UpsertCommand;
import com.dotmarketing.db.commands.UpsertCommandFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
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
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.NumberUtils;

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
    public static final int ES_TRACK_TOTAL_HITS_DEFAULT = 10000000;
    public static final String ES_TRACK_TOTAL_HITS = "ES_TRACK_TOTAL_HITS";
    private static final String[] UPSERT_INODE_EXTRA_COLUMNS = {"owner", "idate", "type"};
    private static final String[] UPSERT_EXTRA_COLUMNS = {"show_on_menu", "title", "mod_date", "mod_user",
            "sort_order", "friendly_name", "structure_inode", "disabled_wysiwyg", "identifier",
            "language_id", "date1", "date2", "date3", "date4", "date5", "date6", "date7", "date8",
            "date9", "date10", "date11", "date12", "date13", "date14", "date15", "date16", "date17",
            "date18", "date19", "date20", "date21", "date22", "date23", "date24", "date25", "text1",
            "text2", "text3", "text4", "text5", "text6", "text7", "text8", "text9", "text10",
            "text11", "text12", "text13", "text14", "text15", "text16", "text17", "text18",
            "text19", "text20", "text21", "text22", "text23", "text24", "text25", "text_area1",
            "text_area2", "text_area3", "text_area4", "text_area5", "text_area6", "text_area7",
            "text_area8", "text_area9", "text_area10", "text_area11", "text_area12", "text_area13",
            "text_area14", "text_area15", "text_area16", "text_area17", "text_area18",
            "text_area19", "text_area20", "text_area21", "text_area22", "text_area23",
            "text_area24", "text_area25", "integer1", "integer2", "integer3", "integer4",
            "integer5", "integer6", "integer7", "integer8", "integer9", "integer10", "integer11",
            "integer12", "integer13", "integer14", "integer15", "integer16", "integer17",
            "integer18", "integer19", "integer20", "integer21", "integer22", "integer23",
            "integer24", "integer25", "float1", "float2", "float3", "float4", "float5", "float6",
            "float7", "float8", "float9", "float10", "float11", "float12", "float13", "float14",
            "float15", "float16", "float17", "float18", "float19", "float20", "float21", "float22",
            "float23", "float24", "float25", "bool1", "bool2", "bool3", "bool4", "bool5", "bool6",
            "bool7", "bool8", "bool9", "bool10", "bool11", "bool12", "bool13", "bool14", "bool15",
            "bool16", "bool17", "bool18", "bool19", "bool20", "bool21", "bool22", "bool23",
            "bool24", "bool25"};

    private static final String[] UPSERT_EXTRA_COLUMNS_ORACLE = {"show_on_menu", "title", "mod_date", "mod_user",
            "sort_order", "friendly_name", "structure_inode", "disabled_wysiwyg", "identifier",
            "language_id", "date1", "date2", "date3", "date4", "date5", "date6", "date7", "date8",
            "date9", "date10", "date11", "date12", "date13", "date14", "date15", "date16", "date17",
            "date18", "date19", "date20", "date21", "date22", "date23", "date24", "date25", "text1",
            "text2", "text3", "text4", "text5", "text6", "text7", "text8", "text9", "text10",
            "text11", "text12", "text13", "text14", "text15", "text16", "text17", "text18",
            "text19", "text20", "text21", "text22", "text23", "text24", "text25", "text_area1",
            "text_area2", "text_area3", "text_area4", "text_area5", "text_area6", "text_area7",
            "text_area8", "text_area9", "text_area10", "text_area11", "text_area12", "text_area13",
            "text_area14", "text_area15", "text_area16", "text_area17", "text_area18",
            "text_area19", "text_area20", "text_area21", "text_area22", "text_area23",
            "text_area24", "text_area25", "integer1", "integer2", "integer3", "integer4",
            "integer5", "integer6", "integer7", "integer8", "integer9", "integer10", "integer11",
            "integer12", "integer13", "integer14", "integer15", "integer16", "integer17",
            "integer18", "integer19", "integer20", "integer21", "integer22", "integer23",
            "integer24", "integer25", "\"float1\"", "\"float2\"", "\"float3\"", "\"float4\"",
            "\"float5\"", "\"float6\"", "\"float7\"", "\"float8\"", "\"float9\"", "\"float10\"",
            "\"float11\"", "\"float12\"", "\"float13\"", "\"float14\"",
            "\"float15\"", "\"float16\"", "\"float17\"", "\"float18\"", "\"float19\"", "\"float20\"",
            "\"float21\"", "\"float22\"", "\"float23\"", "\"float24\"", "\"float25\"", "bool1",
            "bool2", "bool3", "bool4", "bool5", "bool6", "bool7", "bool8", "bool9", "bool10",
            "bool11", "bool12", "bool13", "bool14", "bool15", "bool16", "bool17", "bool18", "bool19",
            "bool20", "bool21", "bool22", "bool23", "bool24", "bool25"};

    private static final String[] UPSERT_EXTRA_COLUMNS_MYSQL = {"show_on_menu", "title", "mod_date", "mod_user",
            "sort_order", "friendly_name", "structure_inode", "disabled_wysiwyg", "identifier",
            "language_id", "date1", "date2", "date3", "date4", "date5", "date6", "date7", "date8",
            "date9", "date10", "date11", "date12", "date13", "date14", "date15", "date16", "date17",
            "date18", "date19", "date20", "date21", "date22", "date23", "date24", "date25", "text1",
            "text2", "text3", "text4", "text5", "text6", "text7", "text8", "text9", "text10",
            "text11", "text12", "text13", "text14", "text15", "text16", "text17", "text18",
            "text19", "text20", "text21", "text22", "text23", "text24", "text25", "text_area1",
            "text_area2", "text_area3", "text_area4", "text_area5", "text_area6", "text_area7",
            "text_area8", "text_area9", "text_area10", "text_area11", "text_area12", "text_area13",
            "text_area14", "text_area15", "text_area16", "text_area17", "text_area18",
            "text_area19", "text_area20", "text_area21", "text_area22", "text_area23",
            "text_area24", "text_area25", "integer1", "integer2", "integer3", "integer4",
            "integer5", "integer6", "integer7", "integer8", "integer9", "integer10", "integer11",
            "integer12", "integer13", "integer14", "integer15", "integer16", "integer17",
            "integer18", "integer19", "integer20", "integer21", "integer22", "integer23",
            "integer24", "integer25", "float1", "float2", "float3", "`float4`", "float5", "float6",
            "float7", "`float8`", "float9", "float10", "float11", "float12", "float13", "float14",
            "float15", "float16", "float17", "float18", "float19", "float20", "float21", "float22",
            "float23", "float24", "float25", "bool1", "bool2", "bool3", "bool4", "bool5", "bool6",
            "bool7", "bool8", "bool9", "bool10", "bool11", "bool12", "bool13", "bool14", "bool15",
            "bool16", "bool17", "bool18", "bool19", "bool20", "bool21", "bool22", "bool23",
            "bool24", "bool25"};
    private static final int MAX_FIELDS_ALLOWED = 25;

    private final ContentletCache contentletCache;
	private final LanguageAPI languageAPI;
	private final ESQueryCache queryCache;
    private static final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
            .getDefaultObjectMapper();

    @VisibleForTesting
    public static final String CACHE_404_CONTENTLET = "CACHE_404_CONTENTLET";

    private static final Contentlet cache404Content = new Contentlet() {
        public String getInode() {
            return CACHE_404_CONTENTLET;
        }
    };

	@VisibleForTesting
	public static final String LUCENE_RESERVED_KEYWORDS_REGEX = "OR|AND|NOT|TO";
    private static final Set<String> REMOVABLE_KEY_SET = CollectionsUtils.set(WORKFLOW_ACTION_KEY,
            WORKFLOW_ASSIGN_KEY, WORKFLOW_COMMENTS_KEY, WORKFLOW_BULK_KEY,
            WORKFLOW_IN_PROGRESS, AUTO_ASSIGN_WORKFLOW, TITLE_IMAGE_KEY, "_use_mod_date");

    /**
	 * Default factory constructor that initializes the connection with the
	 * Elastic index.
	 */
	public ESContentFactoryImpl() {
        this.contentletCache = CacheLocator.getContentletCache();
        this.languageAPI     =  APILocator.getLanguageAPI();
        this.queryCache      = CacheLocator.getESQueryCache();
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
        Set<String> identsDeleted = new HashSet<>();
        for (final Contentlet contentlet : contentlets) {
            contentletCache.remove(contentlet.getInode());

            // delete workflow task for contentlet
            final WorkFlowFactory workFlowFactory = FactoryLocator.getWorkFlowFactory();
            workFlowFactory.deleteWorkflowTaskByContentletIdAndLanguage(
                    contentlet.getIdentifier(), contentlet.getLanguageId());

            //Remove the tag references to this Contentlet
            APILocator.getTagAPI().deleteTagInodesByInode(contentlet.getInode());

            if(InodeUtils.isSet(contentlet.getInode())){
                APILocator.getPermissionAPI().removePermissions(contentlet);

                Optional<ContentletVersionInfo> verInfo=APILocator.getVersionableAPI().getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());

                if(verInfo.isPresent()) {
                    if(UtilMethods.isSet(verInfo.get().getLiveInode()) && verInfo.get().getLiveInode().equals(contentlet.getInode()))
                        try {
                            APILocator.getVersionableAPI().removeLive(contentlet);
                        } catch (Exception e) {
                            throw new DotDataException(e.getMessage(),e);
                        }
                    if(verInfo.get().getWorkingInode().equals(contentlet.getInode()))
                        APILocator.getVersionableAPI().deleteContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());
                }
                delete(contentlet.getInode());
            }
            //Removes content from index
            APILocator.getContentletIndexAPI().removeContentFromIndex(contentlet);
        }
        if (deleteIdentifier) {
	        for (Contentlet c : contentlets) {
	            if(InodeUtils.isSet(c.getInode())){
	                Identifier ident = APILocator.getIdentifierAPI().find(c.getIdentifier());
	                if(ident==null || UtilMethods.isEmpty(ident.getId())) {
	                    continue;
	                }
	                String si = ident.getId();
	                if(!identsDeleted.contains(si)){
	                    APILocator.getIdentifierAPI().delete(ident);
	                    identsDeleted.add(si);
	                }
	            }
	        }
        }
	}

    private void delete(final String inode) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("delete from contentlet where inode=?");
        dotConnect.addParam(inode);
        dotConnect.loadResult();
        checkOrphanInode (inode);
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
        final DotConnect dotConnect = new DotConnect();
        try {
            final String sInodeIds = StringUtils.join(inodes, ",");

            // workaround for dbs where we can't have more than one constraint
            // or triggers
            dotConnect.executeStatement("delete from tree where child in (" + sInodeIds
                    + ") or parent in (" + sInodeIds + ")");

            // workaround for dbs where we can't have more than one constraint
            // or triggers
            APILocator.getMultiTreeAPI().deleteMultiTreesForIdentifiers(inodes);
        } catch (SQLException e) {
            throw new DotDataException("Error deleting tree and multi-tree.", e);
        }
    }

    /**
     * Deletes all the Contentlet versions that are older than the specified date.
     *
     * @param deleteFrom The date as of which all contents older than that will be deleted.
     *
     * @return The number of records deleted by this operation.
     *
     * @throws DotDataException An error occurred when interacting with the data source.
     */
    @Override
    protected int deleteOldContent(final Date deleteFrom) throws DotDataException {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(deleteFrom);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        final Date date = calendar.getTime();
        DotConnect dc = new DotConnect();
        final String countSQL = "select count(*) as count from contentlet";
        dc.setSQL(countSQL);
        List<Map<String, String>> result = dc.loadResults();
        final int before = Integer.parseInt(result.get(0).get("count"));
        dc = new DotConnect();
        final String query = new StringBuilder("SELECT DISTINCT inode FROM contentlet WHERE identifier <> 'SYSTEM_HOST' AND mod_date < ? AND ")
                .append(" inode NOT IN (SELECT working_inode FROM contentlet_version_info WHERE working_inode = contentlet.inode)")
                .append(" AND ")
                .append(" inode NOT IN (SELECT live_inode FROM contentlet_version_info WHERE live_inode = contentlet.inode)")
                .toString();
        dc.setSQL(query);
        dc.addParam(date);
        result = dc.loadResults();
        int oldInodesCount = result.size();
        if (oldInodesCount > 0) {
            final List<String> inodeList = result.stream().map(row -> row.get("inode")).collect(Collectors.toList());
            deleteContentData(inodeList);
        }
        dc = new DotConnect();
        dc.setSQL(countSQL);
        result = dc.loadResults();
        final int after = Integer.parseInt(result.get(0).get("count"));
        final int deleted = before - after;
        if (deleted > 0) {
            deleteOrphanedBinaryFiles();
        }
        return deleted;
    }

    /**
     * Deletes the content data associated to the specified list of Inodes. Based on such a list, the {@code contentlet}
     * will be cleaned up as well.
     *
     * @param inodeList The list of Inodes that will be deleted.
     *
     * @throws DotDataException An error occurred when interacting with the data source.
     */
    private void deleteContentData(final List<String> inodeList) throws DotDataException {
        final int splitAt = 100;
        // Split all records into lists of size 'truncateAt'
        final List<List<String>> inodesToDelete = Lists.partition(inodeList, splitAt);
        final List<String> queries = Lists.newArrayList("DELETE FROM contentlet WHERE inode IN (?)",
                "DELETE FROM inode WHERE inode IN (?)");
        for (final String query : queries) {
            for (final List<String> inodes : inodesToDelete) {
                final DotConnect dc = new DotConnect();
                // Generate the "(?,?,?...)" string depending of the number of inodes
                final String parameterPlaceholders = DotConnect.createParametersPlaceholder(inodes.size());
                dc.setSQL(query.replace("?", parameterPlaceholders));
                for (final String inode : inodes) {
                    dc.addParam(inode);
                }
                dc.loadResult();
            }
        }
    }

    /**
     * Deletes binary files in the {@code /assets/} folder that don't belong to a valid Inode. This cleanup routine
     * helps dotCMS keep things in order when deleting old versions of contentlets.
     */
    private void deleteOrphanedBinaryFiles() {
        // Deleting orphaned binary files
        final java.io.File assets = new java.io.File(APILocator.getFileAssetAPI().getRealAssetsRootPath());
        for (final java.io.File firstLevelFolder : assets.listFiles()) {
            if (firstLevelFolder.isDirectory() && firstLevelFolder.getName().length() == 1 && firstLevelFolder
                    .getName().matches("^[a-f0-9]$")) {
                for (final java.io.File secondLevelFolder : firstLevelFolder.listFiles()) {
                    if (secondLevelFolder.isDirectory() && secondLevelFolder.getName().length() == 1 &&
                            secondLevelFolder.getName().matches("^[a-f0-9]$")) {
                        for (final java.io.File asset : secondLevelFolder.listFiles()) {
                            try {
                                if (asset.isDirectory()) {
                                    final Contentlet contentlet = find(asset.getName());
                                    if (null == contentlet || !UtilMethods.isSet(contentlet.getIdentifier())) {
                                        if (!FileUtils.deleteQuietly(asset)) {
                                            Logger.warn(this, "Asset '" + asset.getAbsolutePath() + "' could " +
                                                    "not be deleted.");
                                        }
                                    }
                                }
                            } catch (final Exception ex) {
                                Logger.warn(this, String.format("An error occurred when deleting asset '%s': %s",
                                        asset.getAbsolutePath(), ex.getMessage()));
                            }
                        }
                    }
                }
            }
        }
    }

	@Override
	protected void deleteVersion(final Contentlet contentlet) throws DotDataException {
	    final String conInode = contentlet.getInode();
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("delete from tree where child = ? or parent = ?");
        dotConnect.addParam( conInode );
        dotConnect.addParam( conInode );
        dotConnect.loadResult();

        // workaround for dbs where we can't have more than one constraint
        // or triggers
        APILocator.getMultiTreeAPI().deleteMultiTreesRelatedToIdentifier(conInode);

        contentletCache.remove(conInode);

        delete(conInode);

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


    @Override
    public Optional<Contentlet> findInDb(final String inode) {
        try {
            if (inode != null) {
                final DotConnect dotConnect = new DotConnect();
                dotConnect.setSQL("select contentlet.*, inode.owner from contentlet, inode where contentlet.inode=? and contentlet.inode=inode.inode");
                dotConnect.addParam(inode);

                final List<Map<String, Object>> result = dotConnect.loadObjectResults();

                if (UtilMethods.isSet(result)) {
                    return Optional.ofNullable(
                            TransformerLocator.createContentletTransformer(result).asList().get(0));
                }
            }
        } catch (DotDataException e) {
            if (!(e.getCause() instanceof ObjectNotFoundException)) {
                throw new DotRuntimeException(e);
            }
        }

        return Optional.empty();

    }
	
	
	
    @Override
    protected Contentlet find(final String inode) throws ElasticsearchException, DotStateException, DotDataException, DotSecurityException {
        Contentlet contentlet = contentletCache.get(inode);
        if (contentlet != null && InodeUtils.isSet(contentlet.getInode())) {
            if (CACHE_404_CONTENTLET.equals(contentlet.getInode())) {
                return null;
            }
            return contentlet;
        }

        final Optional<Contentlet> dbContentlet = this.findInDb(inode);
        if (dbContentlet.isPresent()) {
            contentlet = dbContentlet.get();
            contentletCache.add(contentlet.getInode(), contentlet);
            return contentlet;
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
    protected List<Contentlet> findAllCurrent (final int offset, final int limit ) throws ElasticsearchException {

        final String indexToHit;

        try {
            indexToHit = APILocator.getIndiciesAPI().loadIndicies().getWorking();
        }
        catch(DotDataException ee) {
            Logger.fatal(this, "Can't get indicies information",ee);
            return null;
        }

        final SearchRequest searchRequest = new SearchRequest(indexToHit);
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(limit);
        searchSourceBuilder.from(offset);
        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchSourceBuilder.fetchSource(new String[] {"inode"}, null);
        searchRequest.source(searchSourceBuilder);

        final SearchHits  hits = cachedIndexSearch(searchRequest);
        
        final List<Contentlet> contentlets = new ArrayList<>();

        for (final SearchHit hit : hits ) {
            try {
                final Map<String, Object> sourceMap = hit.getSourceAsMap();
                contentlets.add( find( sourceMap.get("inode").toString()) );
            } catch ( Exception e ) {
                throw new ElasticsearchException( e.getMessage(), e );
            }
        }

        return contentlets;
    }

	@Override
	protected List<Contentlet> findAllUserVersions(final Identifier identifier) throws DotDataException, DotStateException, DotSecurityException {
        if(!InodeUtils.isSet(identifier.getId())) {
            return Collections.emptyList();
        }

        final DotConnect dotConnect = new DotConnect();
        final StringBuilder query = new StringBuilder();
        query.append("select inode, owner from inode, contentlet_version_info vi ")
                .append("where vi.identifier=inode.identifier and ")
                .append("inode.inode<>vi.workingInode and ")
                .append("mod_user <> 'system' and inode.identifier = ? ")
                .append("and type='contentlet' order by mod_date desc");

        dotConnect.setSQL(query.toString());
        dotConnect.addParam(identifier.getId());

        List results = dotConnect.loadObjectResults();

        return results == null ? Collections.emptyList()
                : TransformerLocator.createContentletTransformer(results).asList();

	}

    @Override
    protected List<Contentlet> findAllVersions(final Identifier identifier) throws DotDataException, DotStateException, DotSecurityException {
        return findAllVersions(identifier, true);
    }

    @Override
    protected List<Contentlet> findAllVersions(final Identifier identifier, final boolean bringOldVersions) throws DotDataException, DotStateException, DotSecurityException {
        return findAllVersions(identifier, bringOldVersions, null);
    }

	@Override
    public List<Contentlet> findAllVersions(final Identifier identifier,
            final boolean bringOldVersions, final Integer maxResults)
            throws DotDataException, DotStateException, DotSecurityException {

	    if(!InodeUtils.isSet(identifier.getId())) {
            return new ArrayList<>();
        }

        final DotConnect dc = new DotConnect();
        final StringBuffer query = new StringBuffer();

        if(bringOldVersions) {
            query.append("SELECT inode FROM contentlet WHERE identifier=? order by mod_date desc");

        } else {
            query.append("SELECT inode FROM contentlet c INNER JOIN contentlet_version_info cvi "
                    + "ON (c.inode = cvi.working_inode OR c.inode = cvi.live_inode) "
                    + "WHERE c.identifier=? order by c.mod_date desc ");
        }

        dc.setSQL(query.toString());
        dc.addObject(identifier.getId());

        if (maxResults != null){
            dc.setMaxRows(maxResults);
        }
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
        final DotConnect dotConnect = new DotConnect();
        final StringBuilder select = new StringBuilder();
        select.append("select contentlet.*, inode.owner ").append(
                "from contentlet, contentlet_version_info, inode ")
                .append("where structure_inode = '")
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

        select.append(" and contentlet_version_info.identifier=contentlet.identifier ")
                .append(" and contentlet_version_info.working_inode=contentlet.inode ")
                .append(" and contentlet.inode = inode.inode");
        dotConnect.setSQL(select.toString());

        if (offset > 0) {
            dotConnect.setStartRow(offset);
        }
        if (limit > 0) {
            dotConnect.setMaxRows(limit);
        }

        List<Contentlet> contentlets = TransformerLocator.createContentletTransformer
                (dotConnect.loadObjectResults()).asList();

        contentlets.forEach(contentlet ->contentletCache
                .add(String.valueOf(contentlet.getInode()), contentlet));

        return contentlets;
    }

	@Override
	protected Contentlet findContentletByIdentifier(String identifier, Boolean live, Long languageId) throws DotDataException {
        final Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, languageId);
        if(!cvi.isPresent() || UtilMethods.isEmpty(cvi.get().getIdentifier())
                || (live && UtilMethods.isEmpty(cvi.get().getLiveInode()))) {
            return null;
        }
        return Try.of(()->find((live?cvi.get().getLiveInode():cvi.get().getWorkingInode())))
                .getOrElseThrow(DotRuntimeException::new);
	}

	@Override
    protected Contentlet findContentletByIdentifierAnyLanguage(final String identifier) throws DotDataException, DotSecurityException {
	    
	    // Looking content up this way can avoid any DB hits as these calls are all cached.
	    return findContentletByIdentifierAnyLanguage(identifier, false);

    }

    @Override
    protected Contentlet findContentletByIdentifierAnyLanguage(final String identifier, final boolean includeDeleted) throws DotDataException, DotSecurityException {
        // Looking content up this way can avoid any DB hits as these calls are all cached.
        final List<Language> langs = this.languageAPI.getLanguages();
        for(final Language language : langs) {
            final Optional<ContentletVersionInfo> contentVersion = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, language.getId());
            if (contentVersion.isPresent() && UtilMethods.isSet(contentVersion.get().getIdentifier())
                    && (includeDeleted || !contentVersion.get().isDeleted())) {
                return find(contentVersion.get().getWorkingInode());
            }
            if (contentVersion.isPresent() && contentVersion.get().isDeleted() && !includeDeleted) {
                Logger.warn(this, String.format("Contentlet with ID '%s' exists, but is marked as 'Archived'.",
                        identifier));
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
      final Contentlet contentlet = contentletCache.get(i);
      if (contentlet != null && InodeUtils.isSet(contentlet.getInode())) {
        conMap.put(contentlet.getInode(), contentlet);
      }
    }
    
    if (conMap.size() != inodes.size()) {
        final List<String> missingCons = new ArrayList<>(
                CollectionUtils.subtract(inodes, conMap.keySet()));

        final String contentletBase =
                "select contentlet.*, contentlet_1_.owner  from contentlet join inode contentlet_1_ "
                        + " on contentlet_1_.inode = contentlet.inode and contentlet_1_.type = 'contentlet' where  contentlet.inode in ('";

        for (int init = 0; init < missingCons.size(); init += 200) {
            int end = Math.min(init + 200, missingCons.size());
            final DotConnect dotConnect = new DotConnect();
            final StringBuilder query = new StringBuilder().append(contentletBase)
                    .append(StringUtils.join(missingCons.subList(init, end), "','"))
                    .append("') order by contentlet.mod_date DESC");

            dotConnect.setSQL(query.toString());

            final List<Contentlet> result = TransformerLocator
                    .createContentletTransformer(dotConnect.loadObjectResults()).asList();

            result.forEach(contentlet -> {
                conMap.put(contentlet.getInode(), contentlet);
                contentletCache.add(contentlet.getInode(), contentlet);
            });
        }
    }

      return inodes.stream().map(inode -> conMap.get(inode)).filter(Objects::nonNull)
              .collect(Collectors.toList());
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
    private List<Contentlet> getContentletsFromSearchResponse(final SearchRequest searchRequest) {

        final SearchHits hits = cachedIndexSearch(searchRequest);

        final List<Contentlet> contentlets = new ArrayList<>();
        for (int i = 0; i < hits.getHits().length; i++) {
            try {
                contentlets.add(find(hits.getAt(i).getSourceAsMap().get("inode").toString()));
            } catch (Exception e) {
                throw new ElasticsearchException(e.getMessage(),e);
            }
        }
        return contentlets;
    }

    @Override
	protected List<Contentlet> findContentletsByIdentifier(String identifier, Boolean live, Long languageId) throws DotDataException, DotStateException, DotSecurityException {
	    final List<Contentlet> contentlets = new ArrayList<>();
        final StringBuilder queryBuffer = new StringBuilder();
        final DotConnect dotConnect = new DotConnect();
        queryBuffer.append("select contentlet.*, contentlet_1_.owner ")
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

        dotConnect.setSQL(queryBuffer.toString());
        if(languageId!=null){
            dotConnect.addParam(languageId.longValue());
        }
        dotConnect.addParam(identifier);
        final List<Contentlet> result = TransformerLocator
                .createContentletTransformer(dotConnect.loadObjectResults()).asList();

        result.forEach(contentlet -> contentletCache.add(String.valueOf(contentlet.getInode()), contentlet));

        return result;
	}

	@Override
	protected List<Contentlet> findContentletsWithFieldValue(final String structureInode, final Field field) throws DotDataException {
	    final List<Contentlet> contentlets = new ArrayList<>();

        try {

            if ((field == null) || (!InodeUtils.isSet(field.getInode())))
                return contentlets;

            DotConnect dotConnect = new DotConnect();
            final StringBuilder query = new StringBuilder();
            query.append("from contentlet, inode, contentlet_version_info contentletvi")
                    .append(" where contentlet.identifier=contentletvi.identifier ")
                    .append(" and contentletvi.live_inode=contentlet.inode ")
                    .append(" and inode.inode=contentlet.inode ")
                    .append(" and structure_inode= '")
                    .append(structureInode)
                    .append("' and ")
                    .append(field.getFieldContentlet())
                    .append(" is not null and ")
                    .append(field.getFieldContentlet())
                    .append("<>''");

            dotConnect.setSQL("select count(*) as count " + query);
            final List<Map<String, String>> resultCount = dotConnect.loadResults();
            final int count = Integer.parseInt(resultCount.get(0).get("count"));
            final int limit = 500;

            dotConnect = new DotConnect();
            dotConnect.setSQL("select * " + query + " order by " + field.getFieldContentlet());
            dotConnect.setMaxRows(limit);
            for (int offset = 0; offset < count; offset+=limit) {
                if (offset > 0)
                    dotConnect.setStartRow(offset);

                contentlets.addAll(TransformerLocator
                        .createContentletTransformer(dotConnect.loadObjectResults()).asList());
            }
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(),e);
        }

        return contentlets;
	}

	@Override
	protected List<Contentlet> findPageContentlets(final String HTMLPageIdentifier,
            final String containerIdentifier, String orderby, final boolean working,
            long languageId) throws DotDataException, DotStateException, DotSecurityException {

        if(Config.getBooleanProperty("FIND_PAGE_CONTENTLETS_FROM_CACHE", false)){
            return findPageContentletFromCache(HTMLPageIdentifier, containerIdentifier, orderby, working, languageId);
        }

        final StringBuilder condition = new StringBuilder();
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

        if (!UtilMethods.isSet(orderby) || orderby.equals("tree_order")) {
            orderby = "multi_tree.tree_order";
        }
        final StringBuilder query = new StringBuilder();
        query.append("SELECT contentlet.*, contentlet_1_.owner FROM contentlet JOIN inode contentlet_1_ ON (contentlet.inode=contentlet_1_.inode) ")
                .append(" JOIN multi_tree ON (multi_tree.child = contentlet.identifier) ")
                .append(" JOIN contentlet_version_info contentletvi ON (contentlet.identifier=contentletvi.identifier) ")
                .append(" where multi_tree.parent1 = ? and multi_tree.parent2 = ? and ")
                .append(condition.toString())
                .append(" order by ")
                .append(orderby);

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(query.toString());
        dotConnect.addParam(HTMLPageIdentifier);
        dotConnect.addParam(containerIdentifier);

        final List<Contentlet> result = TransformerLocator
                .createContentletTransformer(dotConnect.loadObjectResults()).asList();
        result.forEach(contentlet -> contentletCache.add(contentlet.getInode(), contentlet));

        return result;
	}


	protected List<Contentlet> findPageContentletFromCache(final String HTMLPageIdentifier,
            final String containerIdentifier, String orderby, final boolean working,
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

        final int marker = condition.indexOf("{0}");
        if(working){
            condition.replace(marker, marker+3,"working_inode");
        }else{
            condition.replace(marker, marker+3,"live_inode");
        }

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(condition.toString());
        dotConnect.addParam(false);
        dotConnect.addParam(HTMLPageIdentifier);
        dotConnect.addParam(containerIdentifier);
        
        final List<Map<String,Object>> results = dotConnect.loadObjectResults();
        final List<Contentlet> contentlets = new ArrayList<>();
        for(final Map<String,Object> resultMap:results){
            final Contentlet contentlet = find((String) resultMap.get("mynode"));
            if(contentlet!=null && contentlet.getInode()!=null){
                contentlets.add(contentlet);
            }
        }
        return contentlets;
    }
	
	
	
	
	
	@Override
	protected List<Contentlet> getContentletsByIdentifier(String identifier) throws DotDataException, DotStateException, DotSecurityException {
	    return getContentletsByIdentifier(identifier, null);
	}

	@Override
	protected List<Contentlet> getContentletsByIdentifier(final String identifier, final Boolean live)
            throws DotDataException, DotStateException, DotSecurityException {

	    final StringBuilder queryBuffer = new StringBuilder();
        queryBuffer.append("SELECT contentlet.*, contentlet_1_.owner ")
                   .append(" FROM contentlet JOIN inode contentlet_1_ ON (contentlet.inode = contentlet_1_.inode) ")
        		   .append(" JOIN contentlet_version_info contentletvi ON (contentlet.identifier=contentletvi.identifier) ")
                   .append(" WHERE ")
                   .append((live!=null && live.booleanValue() ?
                           "contentletvi.live_inode" : "contentletvi.working_inode"))
                   .append(" = contentlet.inode and contentlet.identifier = ? ");

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(queryBuffer.toString());
        dotConnect.addParam(identifier);

        final List<Contentlet> result = TransformerLocator
                .createContentletTransformer(dotConnect.loadObjectResults()).asList();
        result.forEach(contentlet -> contentletCache.add(contentlet.getInode(), contentlet));

        return result;
	}

	@Override
	protected Identifier getRelatedIdentifier(final Contentlet contentlet, final String relationshipType) throws DotDataException {

        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT identifier.* from identifier identifier, tree tree, inode inode ")
                .append("where tree.parent = ? and inode.type ='identifier' and ")
                .append("tree.child = identifier.id and inode.inode = identifier.id ")
                .append(" and tree.relation_type = ?");

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(sql.toString());
        dotConnect.addParam(contentlet.getInode());
        dotConnect.addParam(relationshipType);

        return TransformerLocator
                .createIdentifierTransformer(dotConnect.loadObjectResults()).asList().get(0);
	}

	@Override
	protected List<Link> getRelatedLinks(Contentlet contentlet) throws DotDataException {

        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT links.* from links links, tree tree, inode inode ")
                .append("where tree.parent = ? and tree.child = links.inode and ")
                .append("inode.inode = links.inode and inode.type ='links'");

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(sql.toString());

        dotConnect.addParam(contentlet.getInode());

        return TransformerLocator
                .createLinkTransformer(dotConnect.loadObjectResults()).asList();
	}

	@Override
	protected long indexCount(final String query) {
	    final String qq = LuceneQueryDateTimeFormatter
                .findAndReplaceQueryDates(translateQuery(query, null).getQuery());
        final CountRequest countRequest = getCountRequest(qq);
        return cachedIndexCount(countRequest);
    }

    @NotNull
    private CountRequest getCountRequest(final String queryString) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.queryStringQuery(queryString));
        final CountRequest countRequest = new CountRequest(inferIndexToHit(queryString));
        countRequest.source(sourceBuilder);
        return countRequest;
    }

   private String inferIndexToHit(final String query)  {
       // we check the query to figure out which indexes to hit

       final IndiciesInfo info;
       try {
           info = APILocator.getIndiciesAPI().loadIndicies();
       } catch (DotDataException e) {
           throw new DotRuntimeException(e);
       }

       final String indexToHit;
       if(query.contains("+live:true") && !query.contains("+deleted:true")) {
           indexToHit = info.getLive();
       } else {
           indexToHit = info.getWorking();
       }
       return indexToHit;
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

    private boolean useQueryCache=false;
    private boolean shouldQueryCache() {
        if(!useQueryCache) {
            useQueryCache = LicenseManager.getInstance().isEnterprise() && Config.getBooleanProperty("ES_CACHE_SEARCH_QUERIES", true);
        }
        return useQueryCache;
    }

    /**
     * The track_total_hits parameter allows you to control how the total number of hits should be tracked.
     * The default is set to 10K. This means that requests will count the total hit accurately up to 10,000 hits.
     * If the param is absent from the properties it still default to 10000000. The param can also be set to a true|false
     * if set to true it'll track as many items as there are. if set to false no tracking will be performed at all.
     * So it's better if it isn't set to false ever.
     * @param searchSourceBuilder
     */
     @VisibleForTesting
     void setTrackHits(final SearchSourceBuilder searchSourceBuilder){
        final int trackTotalHits = Config.getIntProperty(ES_TRACK_TOTAL_HITS, ES_TRACK_TOTAL_HITS_DEFAULT);
        searchSourceBuilder.trackTotalHitsUpTo(trackTotalHits);
    }
     /**
      * We return total hits of -1 when an error occurs
      */
     private final static SearchHits ERROR_HIT = new SearchHits(new SearchHit[] {}, new TotalHits(0, Relation.EQUAL_TO), 0);
     
     final static SearchHits EMPTY_HIT = new SearchHits(new SearchHit[] {}, new TotalHits(0, Relation.EQUAL_TO), 0);

    /**
     * if enabled SearchRequests are executed and then cached
     * @param searchRequest
     * @return
     */
    SearchHits cachedIndexSearch(final SearchRequest searchRequest) {
        
        final Optional<SearchHits> optionalHits = shouldQueryCache() ? queryCache.get(searchRequest) : Optional.empty();
        if(optionalHits.isPresent()) {
            return optionalHits.get();
        }
        try {
            SearchResponse response = RestHighLevelClientProvider.getInstance().getClient().search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits  = response.getHits();
            if(shouldQueryCache()) {
                queryCache.put(searchRequest, hits);
            }
            return hits;
        } catch (final ElasticsearchStatusException | IndexNotFoundException | SearchPhaseExecutionException e) {
            final String exceptionMsg = (null != e.getCause() ? e.getCause().getMessage() : e.getMessage());
            Logger.warn(this.getClass(), "----------------------------------------------");
            Logger.warn(this.getClass(), String.format("Elasticsearch SEARCH error in index '%s'", (searchRequest.indices()!=null) ? String.join(",", searchRequest.indices()): "unknown"));
            Logger.warn(this.getClass(), String.format("Thread: %s", Thread.currentThread().getName() ));
            Logger.warn(this.getClass(), String.format("ES Query: %s", String.valueOf(searchRequest.source()) ));
            Logger.warn(this.getClass(), String.format("Class %s: %s", e.getClass().getName(), exceptionMsg));
            Logger.warn(this.getClass(), "----------------------------------------------");
            if(shouldQueryCache(exceptionMsg)) {
                queryCache.put(searchRequest, ERROR_HIT);
            }
            return ERROR_HIT;
        } catch(final IllegalStateException e) {
            rebuildRestHighLevelClientIfNeeded(e);
            Logger.warnAndDebug(ESContentFactoryImpl.class, e);
            throw new DotRuntimeException(e);
        } catch (final Exception e) {
            if(ExceptionUtil.causedBy(e, IllegalStateException.class)) {
                rebuildRestHighLevelClientIfNeeded(e);
            }
            final String errorMsg = String.format("An error occurred when executing the Lucene Query [ %s ] : %s",
                            searchRequest.source().toString(), e.getMessage());
            Logger.warnAndDebug(ESContentFactoryImpl.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }
            
        
    private boolean shouldQueryCache(final String exceptionMsg) {
        if(!shouldQueryCache() || null == exceptionMsg) {
            return false;
        }
        final String exception = exceptionMsg.toLowerCase();
        return exception.contains("parse_exception") || 
               exception.contains("search_phase_execution_exception");
        
    }


    /**
     * if enabled CountRequest are executed and then cached
     * @param countRequest
     * @return
     */
    Long cachedIndexCount(final CountRequest countRequest) {

        final Optional<Long> optionalCount = shouldQueryCache() ? queryCache.get(countRequest) : Optional.empty();
        if(optionalCount.isPresent()) {
            return optionalCount.get();
        }
        try {
            final CountResponse response = RestHighLevelClientProvider.getInstance().getClient().count(countRequest, RequestOptions.DEFAULT);
            final long count = response.getCount();
            if(shouldQueryCache()) {
                queryCache.put(countRequest, count);
            }
            return count;
        } catch (final ElasticsearchStatusException | IndexNotFoundException | SearchPhaseExecutionException e) {
            final String exceptionMsg = (null != e.getCause() ? e.getCause().getMessage() : e.getMessage());
            Logger.warn(this.getClass(), "----------------------------------------------");
            Logger.warn(this.getClass(), String.format("Elasticsearch error in index '%s'", (countRequest.indices()!=null) ? String.join(",", countRequest.indices()): "unknown"));
            Logger.warn(this.getClass(), String.format("ES Query: %s", String.valueOf(countRequest.source()) ));
            Logger.warn(this.getClass(), String.format("Class %s: %s", e.getClass().getName(), exceptionMsg));
            Logger.warn(this.getClass(), "----------------------------------------------");
            if(shouldQueryCache(exceptionMsg)) {
                queryCache.put(countRequest, -1L);
            }
            return -1L;
        } catch(final IllegalStateException e) {
            rebuildRestHighLevelClientIfNeeded(e);
            Logger.warnAndDebug(ESContentFactoryImpl.class, e);
            throw new DotRuntimeException(e);
        } catch (final Exception e) {
            if(ExceptionUtil.causedBy(e, IllegalStateException.class)) {
                rebuildRestHighLevelClientIfNeeded(e);
            }
            final String errorMsg = String.format("An error occurred when executing the Lucene Query [ %s ] : %s",
                    countRequest.source().toString(), e.getMessage());
            Logger.warnAndDebug(ESContentFactoryImpl.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    
    
    @Override
    protected SearchHits indexSearch(final String query, final int limit, final int offset, String sortBy) {

        final String formattedQuery = LuceneQueryDateTimeFormatter
                .findAndReplaceQueryDates(translateQuery(query, sortBy).getQuery());

        // we check the query to figure out which indexes to hit
        final String indexToHit;
        try {
            indexToHit = inferIndexToHit(query);
        } catch (Exception e) {
            Logger.fatal(this, "Can't get indices information.", e);
            return null;
        }

        final SearchRequest searchRequest = new SearchRequest();
        final SearchSourceBuilder searchSourceBuilder = createSearchSourceBuilder(formattedQuery, sortBy);
        setTrackHits(searchSourceBuilder);

        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchRequest.indices(indexToHit);

        if(limit>0) {
            searchSourceBuilder.size(limit);
        }
        if(offset>0) {
            searchSourceBuilder.from(offset);
        }
        if(UtilMethods.isSet(sortBy)) {
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
            } else if(!sortBy.startsWith("undefined") && !sortBy.startsWith("undefined_dotraw") && !sortBy.equals("random")  && !sortBy.equals(SortOrder.ASC.toString())  && !sortBy.equals(SortOrder.DESC.toString())) {
                addBuilderSort(sortBy, searchSourceBuilder);
            }
        }else{
            searchSourceBuilder.sort("moddate", SortOrder.DESC);
        }
        searchRequest.source(searchSourceBuilder);
        return cachedIndexSearch(searchRequest);


    }

    PaginatedArrayList<ContentletSearch> indexSearchScroll(final String query, String sortBy) {

        final String formattedQuery = LuceneQueryDateTimeFormatter
                .findAndReplaceQueryDates(translateQuery(query, sortBy).getQuery());

        // we check the query to figure out which indexes to hit
        final String indexToHit;
        try {
            indexToHit = inferIndexToHit(query);
        } catch (Exception e) {
            Logger.fatal(this, "Can't get indices information.", e);
            return null;
        }

        final SearchRequest searchRequest = new SearchRequest();
        final SearchSourceBuilder searchSourceBuilder = createSearchSourceBuilder(formattedQuery, sortBy);
        searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchRequest.indices(indexToHit);

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

        searchSourceBuilder.size(MAX_LIMIT);
        searchRequest.source(searchSourceBuilder);
        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
        searchRequest.scroll(scroll);

        PaginatedArrayList<ContentletSearch> contentletSearchList = new PaginatedArrayList<>();

        try {
            SearchResponse searchResponse = RestHighLevelClientProvider.getInstance().getClient()
                    .search(searchRequest, RequestOptions.DEFAULT);
            String scrollId = searchResponse.getScrollId();
            SearchHits searchHits = searchResponse.getHits();

            contentletSearchList.addAll(getContentletSearchFromSearchHits(searchHits));
            contentletSearchList.setTotalResults(searchHits.getTotalHits().value);

            while (searchHits.getHits() != null && searchHits.getHits().length > 0) {

                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(scroll);
                searchResponse = RestHighLevelClientProvider.getInstance().getClient()
                        .scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = searchResponse.getScrollId();
                searchHits = searchResponse.getHits();

                contentletSearchList.addAll(getContentletSearchFromSearchHits(searchHits));
            }

            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = RestHighLevelClientProvider.getInstance()
                    .getClient().clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            boolean succeeded = clearScrollResponse.isSucceeded();

        } catch (final ElasticsearchStatusException | IndexNotFoundException | SearchPhaseExecutionException e) {
            final String exceptionMsg = (null != e.getCause() ? e.getCause().getMessage() : e.getMessage());
            Logger.warn(this.getClass(), "----------------------------------------------");
            Logger.warn(this.getClass(), String.format("Elasticsearch error in index '%s'", (searchRequest.indices()!=null) ? String.join(",", searchRequest.indices()): "unknown"));
            Logger.warn(this.getClass(), String.format("ES Query: %s", String.valueOf(searchRequest.source()) ));
            Logger.warn(this.getClass(), String.format("Class %s: %s", e.getClass().getName(), exceptionMsg));
            Logger.warn(this.getClass(), "----------------------------------------------");
            return new PaginatedArrayList<>();
        } catch(final IllegalStateException e) {
            rebuildRestHighLevelClientIfNeeded(e);
            Logger.warnAndDebug(ESContentFactoryImpl.class, e);
            throw new DotRuntimeException(e);
        } catch (final Exception e) {
            if(ExceptionUtil.causedBy(e, IllegalStateException.class)) {
                rebuildRestHighLevelClientIfNeeded(e);
            }
            final String errorMsg = String.format("An error occurred when executing the Lucene Query [ %s ] : %s",
                    searchRequest.source().toString(), e.getMessage());
            Logger.warnAndDebug(ESContentFactoryImpl.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }

        return contentletSearchList;


    }

    private List<ContentletSearch> getContentletSearchFromSearchHits(final SearchHits searchHits) {
        PaginatedArrayList<ContentletSearch> list=new PaginatedArrayList<>();
        list.setTotalResults(searchHits.getTotalHits().value);

        for (SearchHit sh : searchHits.getHits()) {
            try{
                Map<String, Object> sourceMap = sh.getSourceAsMap();
                ContentletSearch conwrapper= new ContentletSearch();
                conwrapper.setId(sh.getId());
                conwrapper.setIndex(sh.getIndex());
                conwrapper.setIdentifier(sourceMap.get("identifier").toString());
                conwrapper.setInode(sourceMap.get("inode").toString());
                conwrapper.setScore(sh.getScore());

                list.add(conwrapper);
            }
            catch(Exception e){
                Logger.error(this,e.getMessage(),e);
                throw e;
            }

        }
        return list;
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
                reindexReplacedUserContent(userToReplace, user, true);
                reindexReplacedUserContent(userToReplace, user, false);
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
     * @param working
     */
    private void reindexReplacedUserContent(final User userToReplace, final User user, final boolean working) {
        final NotificationAPI notificationAPI = APILocator.getNotificationAPI();

        try {
            final StringBuilder luceneQuery = new StringBuilder();
            luceneQuery.append(working?" +working:true":" +live:true")
                    .append(" +modUser:").append(userToReplace.getUserId());
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
    protected Contentlet save(Contentlet contentlet, String existingInode)
            throws DotDataException, DotStateException, DotSecurityException {

        final String inode = getInode(existingInode, contentlet);
        upsertContentlet(contentlet, inode);
        contentlet.setInode(inode);
        final Contentlet toReturn = findInDb(inode).orElseThrow(()->
                new DotStateException(String.format("Contentlet with inode '%s' not found in DB", inode)));

        if(UtilMethods.isNotSet(contentlet.getIdentifier())) {
            toReturn.setFolder(contentlet.getFolder());
            toReturn.setHost(contentlet.getHost());
        }

        REMOVABLE_KEY_SET.forEach(key -> toReturn.getMap().remove(key));

        contentletCache.remove(inode);
        return toReturn;
    }

    private void upsertContentlet(final Contentlet contentlet, final String inode) throws DotDataException {
        final UpsertCommand upsertContentletCommand = UpsertCommandFactory.getUpsertCommand();
        final SimpleMapAppContext replacements = new SimpleMapAppContext();

        replacements.setAttribute(QueryReplacements.TABLE, "contentlet");
        replacements.setAttribute(QueryReplacements.CONDITIONAL_COLUMN, "inode");
        replacements.setAttribute(QueryReplacements.CONDITIONAL_VALUE, inode);
        replacements.setAttribute(QueryReplacements.EXTRA_COLUMNS,
                DbConnectionFactory.isMySql() ? UPSERT_EXTRA_COLUMNS_MYSQL
                        : DbConnectionFactory.isOracle() ? UPSERT_EXTRA_COLUMNS_ORACLE
                                : UPSERT_EXTRA_COLUMNS);


        List<Object> parameters = new ArrayList<>();
        parameters.add(inode);
        parameters.addAll(getParamsToSaveUpdateContent(contentlet));
        upsertContentletCommand.execute(new DotConnect(), replacements, parameters.toArray());
    }

    private String getInode(final String existingInode, final Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        final String inode;
        if(UtilMethods.isSet(existingInode)){
            inode = existingInode;
        } else if (UtilMethods.isSet(contentlet.getInode())){
            inode = contentlet.getInode();
        } else {
            inode = UUIDGenerator.generateUuid();
        }

        if (!findInDb(inode).isPresent()) {
            final UpsertCommand upsertInodeCommand = UpsertCommandFactory.getUpsertCommand();
            final SimpleMapAppContext replacements = new SimpleMapAppContext();

            replacements.setAttribute(QueryReplacements.TABLE, "inode");
            replacements.setAttribute(QueryReplacements.CONDITIONAL_COLUMN, "inode");
            replacements.setAttribute(QueryReplacements.CONDITIONAL_VALUE, inode);
            replacements.setAttribute(QueryReplacements.EXTRA_COLUMNS, UPSERT_INODE_EXTRA_COLUMNS);
            replacements.setAttribute(QueryReplacements.DO_NOTHING_ON_CONFLICT, true);

            upsertInodeCommand
                    .execute(new DotConnect(), replacements, inode, contentlet.getOwner(),
                            new Timestamp(new Date().getTime()),
                            "contentlet");
        }

        return inode;
    }

	private List<Object> getParamsToSaveUpdateContent(final Contentlet contentlet)
            throws DotDataException {

        final List<Object> upsertValues = new ArrayList<>();

        upsertValues.add(contentlet.getStringProperty("showOnMenu") != null && contentlet
                .getStringProperty("showOnMenu").contains("true") ? Boolean.TRUE
                : Boolean.FALSE);

        // if the title was not intentionally set to null.
        final boolean allowTitle =
                null == contentlet.getNullProperties() || !contentlet.getNullProperties()
                        .contains(Contentlet.TITTLE_KEY);

        String name = (String) contentlet.getMap().get(Contentlet.TITTLE_KEY);

        //insert title
        upsertValues.add(name);
        upsertValues.add(new Timestamp(contentlet.getModDate().getTime()));
        upsertValues.add(contentlet.getModUser());
        upsertValues.add(new Long(contentlet.getSortOrder()).intValue());

        //insert friendly name
        upsertValues.add(name);


        upsertValues.add(contentlet.getContentTypeId());

        addWysiwygParam(contentlet, upsertValues);

        upsertValues.add(UtilMethods.isSet(contentlet.getIdentifier())?contentlet.getIdentifier():null);
        upsertValues.add(contentlet.getLanguageId());

        final Map<String, Object> fieldsMap = getFieldsMap(contentlet);

        try {
            addDynamicFields(upsertValues, fieldsMap,"date");
            addDynamicFields(upsertValues, fieldsMap,"text");
            addDynamicFields(upsertValues, fieldsMap,"text_area");
            addDynamicFields(upsertValues, fieldsMap,"integer");
            addDynamicFields(upsertValues, fieldsMap,"float");
            addDynamicFields(upsertValues, fieldsMap,"bool");
        } catch (JsonProcessingException e) {
            throw new DotDataException(e);
        }

        return upsertValues;
    }

    private void addWysiwygParam(final Contentlet contentlet, final List<Object> upsertValues) {
        final List<String> wysiwygFields = contentlet.getDisabledWysiwyg();
        if( wysiwygFields != null && wysiwygFields.size() > 0 ) {
            final StringBuilder wysiwyg = new StringBuilder();
            int j = 0;
            for(final String wysiwygField : wysiwygFields ) {
                wysiwyg.append(wysiwygField);
                j++;
                if( j < wysiwygFields.size() ) wysiwyg.append(",");
            }
            upsertValues.add(wysiwyg.toString());
        } else{
            upsertValues.add(null);
        }
    }

    /**
     * Add dates, text, text_area and integer fields from the contentlet's fields map to the {@link DotConnect} object
     * @param upsertValues
     * @param fieldsMap
     * @param prefix
     */
    private void addDynamicFields(final List<Object> upsertValues, final Map<String, Object> fieldsMap, final String prefix)
            throws JsonProcessingException {
        Object defaultValue = null;

        if (prefix.equals("integer") || prefix.equals("float")){
            defaultValue = 0;
        } else if (prefix.equals("bool")){
            defaultValue = Boolean.FALSE;
        }

        for (int i = 1; i <= MAX_FIELDS_ALLOWED; i++) {
            if (fieldsMap.containsKey(prefix + i)) {

                if (prefix.equals("date") && UtilMethods.isSet(fieldsMap.get(prefix + i))){
                    upsertValues.add(new Timestamp(((Date) fieldsMap.get(prefix + i)).getTime()));
                } else{
                    if (prefix.startsWith("text") && fieldsMap.get(prefix + i) instanceof Map){
                        upsertValues.add(mapper.writeValueAsString(fieldsMap.get(prefix + i)));
                    }else{
                        upsertValues.add(fieldsMap.get(prefix + i));
                    }
                }

            } else {
                upsertValues.add(defaultValue);
            }
        }
    }

    private Map<String, Object> getFieldsMap(final Contentlet contentlet) throws DotDataException {
        final Map<String, Object> fieldsMap = new HashMap<>();
        final List<Field> fields = FieldsCache.getFieldsByStructureInode(contentlet.getContentTypeId());
        for (Field field : fields) {
            if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                continue;
            }
            if (field.getFieldType().equals(Field.FieldType.BINARY.toString())) {
                continue;
            }

            if(!APILocator.getFieldAPI().valueSettable(field)){
                continue;
            }
            Object value;
            value = contentlet.get(field.getVelocityVarName());

            try {
                if(value != null && value instanceof Timestamp){
                    value = new Date(((Timestamp)value).getTime());
                }

                fieldsMap.put(field.getFieldContentlet(), value);
            } catch (final IllegalArgumentException e) {
                throw new DotDataException("Unable to set field value",e);
            }
        }

        return Collections.unmodifiableMap(fieldsMap);
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
	protected void UpdateContentWithSystemHost(final String hostIdentifier) throws DotDataException {
		final Host systemHost = APILocator.getHostAPI().findSystemHost();
		for (int i = 0; i < 10000; i++) {
			final int offset = i * 1000;
			final List<Contentlet> contentlets = findContentletsByHost(hostIdentifier, 1000, offset);
			for (final Contentlet contentlet : contentlets)
				contentlet.setHost(systemHost.getIdentifier());
		}
	}

	@Override
	protected void removeFolderReferences(final Folder folder) throws DotDataException, DotStateException, ElasticsearchException, DotSecurityException {
	    Identifier folderId = null;
        try{
            folderId = APILocator.getIdentifierAPI().find(folder.getIdentifier());
        }catch(Exception e){
            Logger.debug(this, "Unable to get parent folder for folder = " + folder.getInode(), e);
        }
        final DotConnect dc = new DotConnect();
        dc.setSQL("select identifier,inode from identifier,contentlet where identifier.id = contentlet.identifier and parent_path = ? and host_inode=?");
        dc.addParam(folderId.getPath());
        dc.addParam(folder.getHostId());
        final List<HashMap<String, String>> contentInodes = dc.loadResults();
        dc.setSQL("update identifier set parent_path = ? where asset_type='contentlet' and parent_path = ? and host_inode=?");
        dc.addParam("/");
        dc.addParam(folderId.getPath());
        dc.addParam(folder.getHostId());
        dc.loadResult();
        for(final HashMap<String, String> ident:contentInodes){
             final String inode = ident.get("inode");
             contentletCache.remove(inode);
             final Contentlet content = find(inode);
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
     * This method filters out some content types depending on the license level
     * @param query String with the lucene query where the filter will be applied
     * @return String Query excluding non allowed content types
     */
    private static String getQueryWithValidContentTypes(final String query){
        if (!LicenseManager.getInstance().isEnterprise()) {
            final StringBuilder queryBuilder = new StringBuilder(query);
            queryBuilder.append(" -baseType:" + BaseContentType.PERSONA.getType() + " ");
            queryBuilder.append(" -basetype:" + BaseContentType.FORM.getType() + " ");
            return queryBuilder.toString();
        }

        return query;
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

            query = getQueryWithValidContentTypes(query);

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
        } else if ( ( DbConnectionFactory.isOracle()) && isFloatField ) {
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

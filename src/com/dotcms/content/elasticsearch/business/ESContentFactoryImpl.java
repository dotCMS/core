package com.dotcms.content.elasticsearch.business;

import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.hibernate.ObjectNotFoundException;

import org.apache.commons.collections.map.LRUMap;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.CustomScoreQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.util.NumberUtils;

import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.query.ComplexCriteria;
import com.dotmarketing.business.query.Criteria;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.SimpleCriteria;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
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
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkFlowFactory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.NumberUtil;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ESContentFactoryImpl extends ContentletFactory {
	private ContentletCache cc = CacheLocator.getContentletCache();
	private ESClient client = null;
	private ESMappingAPIImpl mapping = new ESMappingAPIImpl();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

	public ESContentFactoryImpl() {
		client = new ESClient();

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
		/*Client client = new ESClient().getClient();

		QueryBuilder query = QueryBuilders.termQuery("stInode", structureInode);
		SearchResponse searchResponse = client.prepareSearch().setSearchType(SearchType.SCAN).setQuery(query).setSize(10)
				.setScroll(TimeValue.timeValueMinutes(30)).execute().actionGet();
		while (true) {
			searchResponse = client.prepareSearchScroll(searchResponse.scrollId()).setScroll(TimeValue.timeValueMinutes(30)).execute()
					.actionGet();
			for (SearchHit hit : searchResponse.hits()) {
				try {
					Contentlet con = loadInode(hit);

					con.getMap().remove(field.getVelocityVarName());

					save(con);
				} catch (DotMappingException e) {
					throw new DotDataException(e.getMessage());
				}

			}
			if (searchResponse.hits().totalHits() == 0) {
				break;
			}
		}*/
	    StringBuffer sql = new StringBuffer("update contentlet set " );
        if(field.getFieldContentlet().indexOf("float") != -1){
            sql.append("\""+field.getFieldContentlet()+"\"" + " = ");
        }else{
            sql.append(field.getFieldContentlet() + " = ");
        }
        if(field.getFieldContentlet().indexOf("bool") != -1){
            sql.append(DbConnectionFactory.getDBFalse());
        }else if(field.getFieldContentlet().indexOf("date") != -1){
            if(DbConnectionFactory.isOracle())
                sql.append("CURRENT_DATE");
            else if(DbConnectionFactory.isMsSql())
                sql.append("GETDATE()");
            else
                sql.append("NOW()");
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

	/*@Override
	protected void cleanHostField(String structureInode) throws DotDataException {
		Client client = new ESClient().getClient();

		QueryBuilder query = QueryBuilders.termQuery("stInode", structureInode);
		SearchResponse searchResponse = client.prepareSearch().setSearchType(SearchType.SCAN).setQuery(query).setSize(10)
				.setScroll(TimeValue.timeValueMinutes(30)).execute().actionGet();
		while (true) {
			searchResponse = client.prepareSearchScroll(searchResponse.scrollId()).setScroll(TimeValue.timeValueMinutes(30)).execute()
					.actionGet();
			for (SearchHit hit : searchResponse.hits()) {
				try {
					Contentlet con = new ESMappingAPIImpl().toContentlet(hit.getSource());
					con.setFolder(FolderAPI.SYSTEM_FOLDER);
					save(con);
				} catch (DotMappingException e) {
					throw new DotDataException(e.getMessage());
				}

			}
			if (searchResponse.hits().totalHits() == 0) {
				break;
			}
		}

	}*/

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
        if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)){
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
            name = APILocator.getContentletAPI().getName(cont, APILocator.getUserAPI().getSystemUser(), true);
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
        fatty.setIdentifier(cont.getIdentifier());
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
        //fatty.setFolder(cont.getFolder());
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
	    Contentlet con = new Contentlet();
        Identifier identifier = new Identifier();
        String folderInode = "";
        con.setStructureInode(fatty.getStructureInode());
        Map<String, Object> contentletMap = fatty.getMap();

        try {
            APILocator.getContentletAPI().copyProperties(con, contentletMap);
        } catch (Exception e) {
            Logger.error(this,"Unable to copy contentlet properties",e);
            throw new DotDataException("Unable to copy contentlet properties",e);
        }
        con.setInode(fatty.getInode());
        con.setStructureInode(fatty.getStructureInode());
        con.setIdentifier(fatty.getIdentifier());
        con.setSortOrder(fatty.getSortOrder());
        con.setLanguageId(fatty.getLanguageId());
        con.setNextReview(fatty.getNextReview());
        con.setLastReview(fatty.getLastReview());
        con.setOwner(fatty.getOwner());
        con.setModUser(fatty.getModUser());
        con.setModDate(fatty.getModDate());
        con.setReviewInterval(fatty.getReviewInterval());

        List<Field> fields = FieldsCache.getFieldsByStructureInode(fatty.getStructureInode());
        Field hostField = null;
        for (Field field: fields) {
            if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString()))
                hostField = field;
        }
        if (InodeUtils.isSet(fatty.getIdentifier())) {
            IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
            identifier = identifierAPI.find(fatty.getIdentifier());
            Folder folder = null;
            if(identifier.getParentPath().length()>1)
                folder = APILocator.getFolderAPI().findFolderByPath(identifier.getParentPath(), identifier.getHostId(), APILocator.getUserAPI().getSystemUser(),false);
            else
                folder = APILocator.getFolderAPI().findSystemFolder();

            folderInode = folder.getInode();
        }
        if (hostField != null) {
            String hostId = con.getStringProperty(hostField.getVelocityVarName());
            if (!InodeUtils.isSet(hostId)) {
                if (InodeUtils.isSet(fatty.getIdentifier())) {
                    con.setHost(identifier.getHostId());
                } else {
                    Host systemHost = APILocator.getHostAPI().findSystemHost();
                    con.setHost(systemHost.getIdentifier());
                }
            }else {
                con.setHost(hostId);
            }
        }
        con.setFolder(folderInode);
        String wysiwyg = fatty.getDisabledWysiwyg();
        if( UtilMethods.isSet(wysiwyg) ) {
            List<String> wysiwygFields = new ArrayList<String>();
            StringTokenizer st = new StringTokenizer(wysiwyg,",");
            while( st.hasMoreTokens() ) wysiwygFields.add(st.nextToken().trim());
            con.setDisabledWysiwyg(wysiwygFields);
        }
        return con;
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
	    StringBuffer buffy = new StringBuffer();
        StringBuffer idsbuffy = new StringBuffer();

        for (Contentlet contentlet : contentlets) {
            if(buffy.length() > 0){
                buffy.append(",'" + contentlet.getInode()+"'");
                idsbuffy.append(",'" + contentlet.getIdentifier()+"'");
            }else{
                buffy.append("'"+contentlet.getInode()+"'");
                idsbuffy.append("'"+contentlet.getIdentifier()+"'");
            }
        }
        // workaround for dbs where we can't have more than one constraint
        // or triggers
        DotConnect db = new DotConnect();
        db.setSQL("delete from tree where child in (" + buffy.toString() + ") or parent in (" + buffy.toString() + ")");
        db.getResult();

        // workaround for dbs where we can't have more than one constraint
        // or triggers
        db.setSQL("delete from multi_tree where child in (" + buffy.toString() + ") or parent1 in (" + buffy.toString() + ") or parent2 in (" + buffy.toString() + ")");
        db.getResult();

        List<String> identsDeleted = new ArrayList<String>();
        for (Contentlet con : contentlets) {
            cc.remove(con.getInode());

            // delete workflow task for contentlet
            WorkFlowFactory wff = FactoryLocator.getWorkFlowFactory();
            WorkflowTask wft = wff.findTaskByContentlet(con);
            wff.deleteWorkflowTask(wft);

            com.dotmarketing.portlets.contentlet.business.Contentlet c =
                (com.dotmarketing.portlets.contentlet.business.Contentlet) InodeFactory.getInode(con.getInode(), com.dotmarketing.portlets.contentlet.business.Contentlet.class);
            //Checking contentlet exists inode > 0
            if(InodeUtils.isSet(c.getInode())){
                APILocator.getPermissionAPI().removePermissions(c);

                ContentletVersionInfo verInfo=APILocator.getVersionableAPI().getContentletVersionInfo(c.getIdentifier(), c.getLanguageId());
                if(verInfo!=null && UtilMethods.isSet(verInfo.getIdentifier())) {
                    if(UtilMethods.isSet(verInfo.getLiveInode()) && verInfo.getLiveInode().equals(c.getInode()))
                        try {
                            APILocator.getVersionableAPI().removeLive(c.getIdentifier(), c.getLanguageId());
                        } catch (Exception e) {
                            throw new DotDataException(e.getMessage(),e);
                        }
                    if(verInfo.getWorkingInode().equals(c.getInode()))
                        APILocator.getVersionableAPI().deleteContentletVersionInfo(c.getIdentifier(), c.getLanguageId());
                }

                HibernateUtil.delete(c);
                //db.setSQL("delete from inode where identifier like '6050'");
                //try{
                //  db.loadResult();
                //}catch (Exception e) {
                //  Logger.error(this, e.getMessage(), e);
                //  throw new DotDataException(e.getMessage(), e);
                //}
                //InodeFactory.deleteInode(c);



            }
        }
        for (Contentlet c : contentlets) {
            if(InodeUtils.isSet(c.getInode())){
                //Identifier ident = (Identifier)InodeFactory.getInode(c.getIdentifier(), Identifier.class);
                //Identifier ident = InodeFactory.getInodeOfClassByCondition(Identifier.class,"inode= '"+c.getIdentifier()+"'");
                Identifier ident = APILocator.getIdentifierAPI().find(c.getIdentifier());
                String si = ident.getInode();
                if(!identsDeleted.contains(si) && si!=null && si!="" ){
                    APILocator.getIdentifierAPI().delete(ident);
                    //DotHibernate.delete(ident);
                    identsDeleted.add(si);
                }
            }
        }
	}

	@Override
	protected int deleteOldContent(Date deleteFrom, int offset) throws DotDataException {
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

        MaintenanceUtil.cleanMultiTreeTable();

        dc.setSQL(countSQL);
        result = dc.loadResults();
        int after = Integer.parseInt(result.get(0).get("count"));

        int deleted=before - after;

        if(deleted>0)
            cc.clearCache();

        return deleted;
	}

	@Override
	protected void deleteVersion(Contentlet contentlet) throws DotDataException {
	    String conInode = contentlet.getInode();
        DotConnect db = new DotConnect();
        db.setSQL("delete from tree where child = ? or parent = ?");
        db.addParam(conInode);
        db.addParam(conInode);
        db.getResult();

        // workaround for dbs where we can't have more than one constraint
        // or triggers
        db.setSQL("delete from multi_tree where child = ? or parent1 = ? or parent2 = ?");
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
	protected Contentlet find(String inode) throws ElasticSearchException, DotStateException, DotDataException, DotSecurityException {
		Contentlet con = cc.get(inode);
		if (con != null && InodeUtils.isSet(con.getInode())) {
			return con;
		}

		/*try {

			Client client = new ESClient().getClient();
			QueryBuilder builder = QueryBuilders.boolQuery().must(QueryBuilders.fieldQuery("inode", inode));

			SearchResponse response = client.prepareSearch().setQuery(builder).execute().actionGet();
			SearchHits hits = response.hits();
			Contentlet contentlet = loadInode(hits.getAt(0));

			return contentlet;
		} catch (Exception e) {
			throw new ElasticSearchException(e.getMessage());
		}*/
		com.dotmarketing.portlets.contentlet.business.Contentlet fatty = null;
        try{
            fatty = (com.dotmarketing.portlets.contentlet.business.Contentlet)HibernateUtil.load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, inode);
        } catch (DotHibernateException e) {
            if(!(e.getCause() instanceof ObjectNotFoundException))
                throw e;
        }
        if(fatty == null){
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
	protected List<Contentlet> findAllCurrent(int offset, int limit) throws ElasticSearchException {
		QueryBuilder builder = QueryBuilders.matchAllQuery();

		SearchResponse response = client.getClient().prepareSearch().setQuery(builder).setSize(limit).setFrom(offset).execute().actionGet();
		SearchHits hits = response.hits();
		List<Contentlet> cons = new ArrayList<Contentlet>();
		for (int i = 0; i < hits.getTotalHits(); i++) {
			try {
				cons.add(find(hits.getAt(i).field("inode").value().toString()));
			} catch (Exception e) {
				throw new ElasticSearchException(e.getMessage(),e);
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
//          result.add(content);
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

			QueryBuilder builder = QueryBuilders.queryString(sw.toString());

			//QueryBuilder builder = QueryBuilders.termQuery("identifier", identifier);

			IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
			SearchResponse response = client.prepareSearch((live ? info.live : info.working)).setQuery(builder)
			        .execute().actionGet();
			SearchHits hits = response.hits();
			Contentlet contentlet = find(hits.getAt(0).getSource().get("inode").toString());
			return contentlet;
		}
		// if we don't have the con in this language
		catch(java.lang.ArrayIndexOutOfBoundsException aibex){
			return null;
		}
		catch (Exception e) {
			throw new ElasticSearchException(e.getMessage());

		}
	}

	@Override
	protected Contentlet findContentletForLanguage(long languageId, Identifier identifier) throws DotDataException {
		return findContentletByIdentifier(identifier.getId(), false, languageId);
	}

	@Override
	protected List<Contentlet> findContentlets(List<String> inodes) throws DotDataException, DotStateException, DotSecurityException {
		/*try {
			Client client = new ESClient().getClient();

			BoolQueryBuilder builder = QueryBuilders.boolQuery();
			for (String inode : inodes) {
				builder.should(QueryBuilders.fieldQuery("inode", inode));
			}

			SearchResponse response = client.prepareSearch().setQuery(builder).addSort("modDate", SortOrder.DESC).execute().actionGet();
			SearchHits hits = response.hits();
			List<Contentlet> cons = new ArrayList<Contentlet>();
			for (int i = 0; i < hits.getTotalHits(); i++) {
				try {
					cons.add(find(hits.getAt(i).field("inode").value().toString()));
				} catch (Exception e) {
					throw new ElasticSearchException(e.getMessage());
				}

			}
			return cons;
		} catch (Exception e) {
			throw new ElasticSearchException(e.getMessage());
		}*/
	    List<Contentlet> result = new ArrayList<Contentlet>();
        List<String> inodesNotFound = new ArrayList<String>();
        for (String i : inodes) {
            Contentlet c = cc.get(i);
            if(c != null && InodeUtils.isSet(c.getInode())){
                result.add(c);
            }else{
                inodesNotFound.add(i);
            }
        }
        if(!(inodesNotFound.size()>0)){
            return result;
        }
        StringBuilder buffy = new StringBuilder();
        //http://jira.dotmarketing.net/browse/DOTCMS-5898
        StringBuilder hql = new StringBuilder("select {contentlet.*} from contentlet join inode contentlet_1_ " +
               "on contentlet_1_.inode = contentlet.inode and contentlet_1_.type = 'contentlet' where  ");
        List<String> clauses = new ArrayList<String>();
        int clauseCount =0;
        boolean isNewClause = false;

        if(inodesNotFound.size()>1000){
            for (String inode : inodesNotFound) {
                if(!(buffy.length()>0)){
                    buffy.append("'"+ inode + "'");
                }else{
                    buffy.append(",'" + inode + "'");
                }
                clauseCount+=1;
                if(clauseCount%1000==0){
                    String clause = " contentlet.inode in (" + buffy.toString() + ")";
                    buffy = new StringBuilder();
                    clauses.add(clause);
                    isNewClause = true;
                }else{
                    isNewClause = false;
                }

            }
            if(clauseCount>1000 && !isNewClause){
                String finalClause = " contentlet.inode in (" + buffy.toString() + ")";
                clauses.add(finalClause);
            }
            int inClauseCount=0;
            for(String clause:clauses){
                if(inClauseCount==0 || inClauseCount==clauses.size()){
                    hql.append(" " + clause);
                }else{
                    hql.append(" or " + clause);
                }
                inClauseCount+=1;
            }
        }else{
            for (String inode : inodesNotFound) {
                if(!(buffy.length()>0)){
                    buffy.append("'"+ inode + "'");
                }else{
                    buffy.append(",'" + inode + "'");
                }
            }
            hql.append(" contentlet.inode in (" + buffy.toString() + ")");
        }
        hql.append(" order by contentlet.inode");

        int offSet = 0;
        while(offSet<=inodesNotFound.size()){
            HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
            hu.setSQLQuery(hql.toString());
            hu.setMaxResults(500);
            hu.setFirstResult(offSet);
            List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties =  hu.list();
            for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
                Contentlet con = convertFatContentletToContentlet(fatty);
                result.add(con);
                cc.add(con.getInode(), con);
            }
            offSet+=500;
            HibernateUtil.flush();
        }
        return result;
	}

	protected List<Contentlet> findContentletsByHost(String hostId, int limit, int offset) throws DotDataException {
		try {

			BoolQueryBuilder builder = QueryBuilders.boolQuery().must(QueryBuilders.fieldQuery("conhost", hostId));

			SearchResponse response = client.getClient().prepareSearch().setQuery(builder).
			        setSize(limit).setFrom(offset).execute()
					.actionGet();

			SearchHits hits = response.hits();

			List<Contentlet> cons = new ArrayList<Contentlet>();
			for (int i = 0; i < hits.getHits().length; i++) {
				try {
					cons.add(find(hits.getAt(i).getSource().get("inode").toString()));
				} catch (Exception e) {
					throw new ElasticSearchException(e.getMessage(),e);
				}
			}
			return cons;
		} catch (Exception e) {
			throw new ElasticSearchException(e.getMessage());
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
            Structure structure = StructureCache.getStructureByInode(structureInode);
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
            HibernateUtil.closeSession();
        } catch (Exception e) {
            Logger.debug(this, e.toString());
            HibernateUtil.closeSession();
        }

        return result;
	}

	@Override
	protected List<Contentlet> findPageContentlets(String HTMLPageIdentifier, String containerIdentifier, String orderby, boolean working,
			long languageId) throws DotDataException, DotStateException, DotSecurityException {
	    StringBuilder condition = new StringBuilder();
        if (working) {
            condition.append("contentletvi.working_inode=contentlet.inode")
                     .append(" and contentletvi.deleted = ")
                     .append(com.dotmarketing.db.DbConnectionFactory.getDBFalse());
        }
        else {
            condition.append("contentletvi.live_inode=contentlet.inode")
                     .append(" and contentletvi.deleted = ")
                     .append(com.dotmarketing.db.DbConnectionFactory.getDBFalse());
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
	protected List<File> getRelatedFiles(Contentlet contentlet) throws DotDataException {
	    HibernateUtil dh = new HibernateUtil(File.class);

        File f = new File();
        String tableName = f.getType();

        String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
        + tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
        + "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type ='"+tableName+"'";

        Logger.debug(this, "HibernateUtilSQL:getRelatedFiles\n " + sql);

        dh.setSQLQuery(sql);

        Logger.debug(this, "inode:  " + contentlet.getInode() + "\n");

        dh.setParam(contentlet.getInode());

        return dh.list();
	}

	@Override
	protected Identifier getRelatedIdentifier(Contentlet contentlet, String relationshipType) throws DotDataException {
	    String tableName;
        try {
            //tableName = ((Inode) Identifier.class.newInstance()).getType();
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
        	QueryStringQueryBuilder qb = QueryBuilders.queryString(qq);
        	SearchRequestBuilder srb = client.prepareSearch();

        	if(UtilMethods.isSet(sortBy) && sortBy.equals("random")) {
        		CustomScoreQueryBuilder cs = new CustomScoreQueryBuilder(qb);
        		cs.script("random()");
        		srb.setQuery(cs);
        	} else {
        		srb.setQuery(qb);
        	}

        	srb.setIndices(indexToHit);

            if(limit>0)
                srb.setSize(limit);
            if(offset>0)
                srb.setFrom(offset);

            if(UtilMethods.isSet(sortBy) && !sortBy.startsWith("undefined") && !sortBy.startsWith("undefined_dotraw") && !sortBy.equals("random")) {
            	String[] sortbyArr=sortBy.split(",");
            	for (String sort : sortbyArr) {
            		String[] x=sort.trim().split(" ");
//            		srb.addSort(SortBuilders.fieldSort(x[0].toLowerCase()).order(x.length>1 && x[1].equalsIgnoreCase("desc") ?
//                            SortOrder.DESC : SortOrder.ASC));
//            		srb.addSort(SortBuilders.fieldSort(x[0].toLowerCase() + ".org").order(x.length>1 && x[1].equalsIgnoreCase("desc") ?
//                          SortOrder.DESC : SortOrder.ASC));
            		srb.addSort(SortBuilders.fieldSort(x[0].toLowerCase() + "_dotraw").order(x.length>1 && x[1].equalsIgnoreCase("desc") ?
                                SortOrder.DESC : SortOrder.ASC));
//            		srb.addSort(x[0].toLowerCase(),x.length>1 && x[1].equalsIgnoreCase("desc") ?
//                            SortOrder.DESC : SortOrder.ASC);
				}
            }
            try{
            	resp = srb.execute().actionGet();
            }catch (SearchPhaseExecutionException e) {
				if(e.getMessage().contains("-order_dotraw] in order to sort on")){
					return new InternalSearchHits(InternalSearchHits.EMPTY,0,0);
				}else{
					throw e;
				}
			}
        } catch (Exception e) {
            Logger.error(ESContentFactoryImpl.class, e.getMessage(), e);
            throw new RuntimeException(e);
        }
	    return resp.getHits();
	}


	@Override
	protected void removeUserReferences(String userId) throws DotDataException, DotStateException, ElasticSearchException, DotSecurityException {
	    DotConnect dc = new DotConnect();
        User systemUser = null;
        try {
           systemUser = APILocator.getUserAPI().getSystemUser();
           dc.setSQL("Select * from contentlet where mod_user = ?");
           dc.addParam(userId);
           List<HashMap<String, String>> contentInodes = dc.loadResults();
           dc.setSQL("UPDATE contentlet set mod_user = ? where mod_user = ? ");
           dc.addParam(systemUser.getUserId());
           dc.addParam(userId);
           dc.loadResult();
           for(HashMap<String, String> ident:contentInodes){
             String inode = ident.get("inode");
             cc.remove(inode);
             Contentlet content = find(inode);
             new ESIndexAPI().addContentToIndex(content);
          }
        } catch (DotDataException e) {
            Logger.error(this.getClass(),e.getMessage(),e);
            throw new DotDataException(e.getMessage(), e);
        }
	}

	@Override
	protected Contentlet save(Contentlet contentlet) throws DotDataException, DotStateException, DotSecurityException {
	    com.dotmarketing.portlets.contentlet.business.Contentlet fatty = new com.dotmarketing.portlets.contentlet.business.Contentlet();
        if(InodeUtils.isSet(contentlet.getInode())){
            fatty = (com.dotmarketing.portlets.contentlet.business.Contentlet)HibernateUtil.load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, contentlet.getInode());
        }
        fatty = convertContentletToFatContentlet(contentlet, fatty);
        HibernateUtil.saveOrUpdate(fatty);
        final Contentlet content = convertFatContentletToContentlet(fatty);

        if (InodeUtils.isSet(contentlet.getHost())) {
            content.setHost(contentlet.getHost());
        }

        cc.remove(content.getInode());
        cc.add(content.getInode(), content);
        HibernateUtil.evict(content);

        return content;
	}

	protected void save(List<Contentlet> contentlets) throws DotDataException, DotStateException, DotSecurityException {
		for(Contentlet con : contentlets)
		    save(con);
	}

	@Override
	protected List<Contentlet> search(String query, int limit, int offset, String sortBy) throws DotDataException, DotStateException, DotSecurityException {
	    SearchHits hits = indexSearch(query, limit, offset, sortBy);
	    List<String> inodes=new ArrayList<String>();
	    for(SearchHit h : hits)
	        inodes.add(h.getSource().get("inode").toString());
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
	protected void removeFolderReferences(Folder folder) throws DotDataException, DotStateException, ElasticSearchException, DotSecurityException {
	    //Folder parentFolder = null;
	    Identifier folderId = null;
        try{
            //parentFolder = APILocator.getFolderAPI().findParentFolder(folder, APILocator.getUserAPI().getSystemUser(), false);
            folderId = APILocator.getIdentifierAPI().find(folder.getIdentifier());
        }catch(Exception e){
            Logger.debug(this, "Unable to get parent folder for folder = " + folder.getInode(), e);
        }
        //String parentFolderId = parentFolder!=null?parentFolder.getInode():FolderAPI.SYSTEM_FOLDER;
        DotConnect dc = new DotConnect();
        dc.setSQL("select identifier,inode from identifier,contentlet where identifier.id = contentlet.identifier and parent_path = ? ");
        dc.addParam(folderId.getPath());
        List<HashMap<String, String>> contentInodes = dc.loadResults();
        dc.setSQL("update identifier set parent_path = ? where asset_type='contentlet' and parent_path = ?");
        dc.addParam("/");
        dc.addParam(folderId.getPath());
        dc.loadResult();
        for(HashMap<String, String> ident:contentInodes){
             String inode = ident.get("inode");
             cc.remove(inode);
             Contentlet content = find(inode);
             new ESIndexAPI().addContentToIndex(content);
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

	    protected static LRUMap translatedQueryCache = new LRUMap(5000);
	    public static TranslatedQuery translateQuery(String query, String sortBy) {

	        TranslatedQuery result = (TranslatedQuery) translatedQueryCache.get(query + " --- " + sortBy);
	        if(result != null)
	            return result;

	        result = new TranslatedQuery();

	        String originalQuery = query;
	        Structure st = null;
	        String stInodestr = "structureInode";
	        String stInodeStrLowered = "structureinode";
	        String stNameStrLowered = "structurename";

	        if (query.contains(stNameStrLowered))
	            query = query.replace(stNameStrLowered,"structureName");

	        if (query.contains(stInodeStrLowered))
	            query = query.replace(stInodeStrLowered,stInodestr);

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
	            st = StructureCache.getStructureByInode(inode);
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
	            List<com.dotmarketing.portlets.structure.model.Field> fields = FieldsCache.getFieldsByStructureVariableName(st.getVelocityVarName());
	            Map<String, com.dotmarketing.portlets.structure.model.Field> fieldsMap;
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
	                List<com.dotmarketing.portlets.structure.model.Field> fields = FieldsCache.getFieldsByStructureVariableName(numberMatch.getGroups().get(0).getMatch());
	                for (com.dotmarketing.portlets.structure.model.Field field : fields) {
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
	                List<com.dotmarketing.portlets.structure.model.Field> fields = FieldsCache.getFieldsByStructureVariableName(numberMatch.getGroups().get(0).getMatch());
	                for (com.dotmarketing.portlets.structure.model.Field field : fields) {
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

	        synchronized (translatedQueryCache) {
	            translatedQueryCache.put(originalQuery + " --- " + sortBy, result);
	        }

	        return result;
	    }

	    private static String translateQuerySortBy(String sortBy, String originalQuery) {

	        if(sortBy == null)
	            return null;

	        List<RegExMatch> matches = RegEX.find(originalQuery,  "structureName:([^\\s)]+)");
	        List<com.dotmarketing.portlets.structure.model.Field> fields = null;
	        Structure structure = null;
	        if(matches.size() > 0) {
	            String structureName = matches.get(0).getGroups().get(0).getMatch();
	            fields = FieldsCache.getFieldsByStructureVariableName(structureName);
	            structure = StructureCache.getStructureByVelocityVarName(structureName);
	        } else {
	            matches = RegEX.find(originalQuery, "structureInode:([^\\s)]+)");
	            if(matches.size() > 0) {
	                String structureInode = matches.get(0).getGroups().get(0).getMatch();
	                fields = FieldsCache.getFieldsByStructureInode(structureInode);
	                structure = StructureCache.getStructureByInode(structureInode);
	            }
	        }

	        if(fields == null)
	            return sortBy;

	        Map<String, com.dotmarketing.portlets.structure.model.Field> fieldsMap;
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
                //return query;
            }

            Structure selectedStructure = StructureCache.getStructureByVelocityVarName(structureVarName);

            if ((selectedStructure == null) || !InodeUtils.isSet(selectedStructure.getInode())) {
                Logger.debug(ESContentFactoryImpl.class, "Structure not found");
                //return query;
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
            List<com.dotmarketing.portlets.structure.model.Field> dateFields = new ArrayList<com.dotmarketing.portlets.structure.model.Field>();
            String tempStructureVarName;
            Structure tempStructure;

            for (String clause: clauses) {

                // getting structure names from query
                if(clause.indexOf('.') >= 0 && (clause.indexOf('.') < clause.indexOf(':'))){

                    tempStructureVarName = clause.substring(0, clause.indexOf('.'));
                    tempStructure = StructureCache.getStructureByVelocityVarName(tempStructureVarName);

                    List<com.dotmarketing.portlets.structure.model.Field> tempStructureFields = FieldsCache.getFieldsByStructureVariableName(tempStructure.getVelocityVarName());

                    for (int pos = 0; pos < tempStructureFields.size();) {

                        if (tempStructureFields.get(pos).getFieldType().equals(com.dotmarketing.portlets.structure.model.Field.FieldType.DATE_TIME.toString()) ||
                                tempStructureFields.get(pos).getFieldType().equals(com.dotmarketing.portlets.structure.model.Field.FieldType.DATE.toString()) ||
                                tempStructureFields.get(pos).getFieldType().equals(com.dotmarketing.portlets.structure.model.Field.FieldType.TIME.toString())) {
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
                for (com.dotmarketing.portlets.structure.model.Field field: dateFields) {

                    structureVarName = StructureCache.getStructureByInode(field.getStructureInode()).getVelocityVarName().toLowerCase();

                    if (clause.startsWith(structureVarName + "." + field.getVelocityVarName().toLowerCase() + ":") || clause.startsWith("moddate:")) {
                        replace = new String(clause);
                        if (field.getFieldType().equals(com.dotmarketing.portlets.structure.model.Field.FieldType.DATE_TIME.toString()) || clause.startsWith("moddate:")) {
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
            for (RegExMatch regExMatch : matches) {
                query = query.replace("[" + regExMatch.getGroups().get(0).getMatch() + " to "
                        + regExMatch.getGroups().get(2).getMatch() + "]", "["
                        + regExMatch.getGroups().get(0).getMatch() + " TO " + regExMatch.getGroups().get(2).getMatch()
                        + "]");
            }

            return query;
        }

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

        private static String toLuceneDateTime(String dateString) {
            String format = "MM/dd/yyyy HH:mm:ss";
            String result = toLuceneDateWithFormat(dateString, format);
            if (result.equals(ERROR_DATE)) {
                format = "MM/dd/yyyy HH:mm";
                result = toLuceneDateWithFormat(dateString, format);
            }
            return result;
        }

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

        private static String toLuceneDate(String dateString) {
            String format = "MM/dd/yyyy";
            return toLuceneDateWithFormat(dateString, format);
        }

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
}
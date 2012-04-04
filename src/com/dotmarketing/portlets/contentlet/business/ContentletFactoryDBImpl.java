/**
 * 
 */
package com.dotmarketing.portlets.contentlet.business;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.hibernate.ObjectNotFoundException;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.query.ComplexCriteria;
import com.dotmarketing.business.query.Criteria;
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
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletLangVersionInfo;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.LuceneHits;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 * @since 1.6
 */
/*
public class ContentletFactoryDBImpl extends ContentletFactory {

	private ContentletCache cc = CacheLocator.getContentletCache();
	
	public ContentletFactoryDBImpl() {}
	
	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	
	@Override
	protected List<Contentlet> findAllCurrent() throws DotDataException {
		return findAllCurrent(0, 0);
	}
	
	@Override
	protected List<Contentlet> findAllCurrent(int offset, int limit) throws DotDataException {
		HibernateUtil hu = new HibernateUtil();
		hu.setQuery("from inode in class com.dotmarketing.portlets.contentlet.business.Contentlet where type='contentlet' order by inode");
		if(offset > 0)
			hu.setFirstResult(offset);
		if(limit > 0)
			hu.setMaxResults(limit);
		List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties =  hu.list();
		List<Contentlet> result = new ArrayList<Contentlet>();
		return result;
	}
	
	protected Contentlet find(String inode) throws DotDataException, DotSecurityException{
		Contentlet con = cc.get(inode);
		if(con != null && InodeUtils.isSet(con.getInode())){
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
			return null;
		}else{
			Contentlet c = convertFatContentletToContentlet(fatty);
			cc.add(c.getInode(), c);
			return c;
		}
	}
	
	protected Contentlet findContentletForLanguage(long languageId, Identifier contentletId) throws DotDataException, DotSecurityException{
			StringBuilder queryBuffer = new StringBuilder();
			queryBuffer.append("select {contentlet.*} ");
			queryBuffer.append(" from contentlet, inode contentlet_1_, contentlet_lang_version_info contentletvi ");
			queryBuffer.append(" where contentlet_1_.type = 'contentlet' and contentlet.identifier = ?");
			queryBuffer.append(" and contentletvi.identifier=contentlet.identifier ");
			queryBuffer.append(" and contentlet.inode = contentlet_1_.inode ");
			queryBuffer.append(" and contentletvi.lang=? ");
			
			HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
			hu.setSQLQuery(queryBuffer.toString());
			hu.setParam(contentletId.getId());
			hu.setParam(languageId);
			com.dotmarketing.portlets.contentlet.business.Contentlet fatty = (com.dotmarketing.portlets.contentlet.business.Contentlet)hu.load();
			if(fatty == null || !InodeUtils.isSet(fatty.getInode())){
				return null;
			}
			Contentlet c = convertFatContentletToContentlet(fatty);
			cc.add(c.getInode(), c);
			return c;
	}
	
	@Override
	protected Contentlet findContentletByIdentifier(String identifier, Boolean live, Long languageId)throws DotDataException, DotSecurityException {
		StringBuilder queryBuffer = new StringBuilder();
		queryBuffer.append("select {contentlet.*} ");
		queryBuffer.append("from contentlet, inode contentlet_1_, contentlet_lang_version_info ");
		queryBuffer.append("where contentlet_1_.type = 'contentlet' and ");
		if(live!=null || languageId != null){

			if(live!=null){
				queryBuffer.append((live.booleanValue() ? "contentlet_lang_version_info.live_inode":"contentlet_lang_version_info.working_inode") + " =  contentlet_1_.inode ");
			}
			if(live!=null && languageId!=null){
				queryBuffer.append("and ");
			}
			if(languageId!=null){
				queryBuffer.append("contentlet_lang_version_info.lang = ? ");
			}
			if(live!=null || languageId!=null){
				queryBuffer.append("and ");
			}
		}	
		queryBuffer.append("contentlet.identifier = ? and contentlet.inode = contentlet_1_.inode");
		queryBuffer.append(" and contentlet_lang_version_info.identifier = contentlet.identifier");
		
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
		hu.setSQLQuery(queryBuffer.toString());
		if(languageId!=null){
		  hu.setParam(languageId.longValue());
		}
		hu.setParam(identifier);
		com.dotmarketing.portlets.contentlet.business.Contentlet fatty = (com.dotmarketing.portlets.contentlet.business.Contentlet)hu.load();
		if(fatty != null && InodeUtils.isSet(fatty.getInode())){
			Contentlet content = convertFatContentletToContentlet(fatty);
			cc.add(content.getInode(), content);
			return content;
		}else{
			return null;
		}
	}
	
	@Override
	protected List<Contentlet> findContentletsByIdentifier(String identifier, Boolean live, Long languageId)throws DotDataException, DotSecurityException {
		List<Contentlet> cons = new ArrayList<Contentlet>();
		StringBuilder queryBuffer = new StringBuilder();
		queryBuffer.append("select {contentlet.*} ");
		queryBuffer.append("from contentlet, inode contentlet_1_, contentlet_lang_version_info contentvi ");
		queryBuffer.append("where contentlet_1_.type = 'contentlet' and contentlet.inode = contentlet_1_.inode and contentvi.identifier=contentlet.identifier and ");
		
		queryBuffer.append(((live!=null && live.booleanValue()) ? "contentvi.live_inode":"contentvi.working_inode") + " = contentlet_1_.inode ");
		
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
	protected List<Contentlet> findContentlets(List<String> inodes) throws DotDataException, DotSecurityException{
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
	
	protected Contentlet save(Contentlet contentlet) throws DotDataException, DotSecurityException{
		com.dotmarketing.portlets.contentlet.business.Contentlet fatty = new com.dotmarketing.portlets.contentlet.business.Contentlet();
		if(InodeUtils.isSet(contentlet.getInode())){
			fatty = (com.dotmarketing.portlets.contentlet.business.Contentlet)HibernateUtil.load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, contentlet.getInode());
		}
		fatty = convertContentletToFatContentlet(contentlet, fatty);	
		HibernateUtil.saveOrUpdate(fatty);
		Contentlet content = convertFatContentletToContentlet(fatty);
		
		if (InodeUtils.isSet(contentlet.getHost())) {
			content.setHost(contentlet.getHost());
		}
		
		cc.remove(content.getInode());
		cc.add(content.getInode(), content);
		HibernateUtil.evict(content);
		return content;
	}
	
	@Override
	protected void lock(String contentletInode, User user) throws DotDataException, DotStateException, DotSecurityException {
	    Identifier ident=APILocator.getIdentifierAPI().findFromInode(contentletInode);
	    APILocator.getVersionableAPI().setLocked(ident.getId(), true, user);
	}
	
	@Override
	protected void unlock(String contentletInode,User user) throws DotDataException, DotStateException, DotSecurityException {
	    Identifier ident=APILocator.getIdentifierAPI().findFromInode(contentletInode);
        APILocator.getVersionableAPI().setLocked(ident.getId(), false, user);
	}
	
	@Override
	protected List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy) throws ParseException, DotDataException, DotSecurityException{
		ArrayList<Contentlet> contents = new ArrayList<Contentlet>();
		ArrayList<String> inodes = new ArrayList<String>();
		LuceneHits hits = new LuceneHits();
		hits = LuceneUtils.searchInCurrentIndex(luceneQuery, offset, limit, sortBy);
		for (int i =0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			inodes.add(doc.get("inode"));
		}
		List<Contentlet> contentlets = findContentlets(inodes);
		Map<String, Contentlet> map = new HashMap<String, Contentlet>(contentlets.size());  
		for (Contentlet contentlet : contentlets) {
			map.put(contentlet.getInode(), contentlet);
		}
		for (String inode : inodes) {
			if(map.get(inode) != null)
				contents.add(map.get(inode));
		}
		return contents;
	}
	
	@Override
	protected LuceneHits indexSearch(String luceneQuery, int limit, int offset,	String sortBy) throws ParseException {
		return LuceneUtils.searchInCurrentIndex(luceneQuery, offset, limit, sortBy);
	}
	
	@Override
	protected List<Contentlet> findByStructure(String structureInode, int limit, int offset) throws DotDataException, DotSecurityException {
		HibernateUtil hu = new HibernateUtil();
		hu.setQuery("select inode from inode in class " + com.dotmarketing.portlets.contentlet.business.Contentlet.class.getName() +
		        ", contentletvi in class "+ContentletLangVersionInfo.class.getName()+
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
//			result.add(content);
			result.add(convertFatContentletToContentlet(fatty));
		}
		return result;
	}
	
	@Override
	protected List<Contentlet> findPageContentlets(String HTMLPageIdentifier,String containerIdentifier, String orderby, boolean working,long languageId) throws DotDataException, DotSecurityException {
		StringBuilder condition = new StringBuilder();
		if (working) {
			condition.append("contentletvi_2_.working_inode=contentlet.inode")
		             .append(" and contentletvi_1_.deleted = ")
		             .append(com.dotmarketing.db.DbConnectionFactory.getDBFalse());
		}
		else {
			condition.append("contentletvi_2_.live_inode=contentlet.inode")
			         .append(" and contentletvi_1_.deleted = ")
			         .append(com.dotmarketing.db.DbConnectionFactory.getDBFalse());
		}
		if (languageId == 0) {
			languageId = langAPI.getDefaultLanguage().getId();
			condition.append(" and contentletvi_2_.lang = ").append(languageId);
		}else if(languageId == -1){
			Logger.debug(this, "LanguageId is -1 so we will not use a language to pull contentlets");
		}else{
			condition.append(" and contentletvi_2_.lang = ").append(languageId);
		}
		
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);

		if (!UtilMethods.isSet(orderby) || orderby.equals("tree_order")) {
			orderby = "multi_tree.tree_order";
		}
		String query = "select {contentlet.*} from contentlet, inode contentlet_1_, identifier, multi_tree, " 
		    + "contentlet_version_info contentletvi_1_, contentlet_lang_version_info contentletvi_2_ "
			+ "where contentlet.inode = contentlet_1_.inode and contentlet_1_.type='contentlet' and contentlet.identifier = identifier.id "
			+ " and identifier.id=contentletvi_1_.identifier and identifier.id=contentletvi_2_.identifier "
			+ " and multi_tree.child = identifier.id and multi_tree.parent1 = ? and multi_tree.parent2 = ? and " + condition.toString() + " order by "
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
	protected List<Contentlet> getContentletsByIdentifier(String identifier, Boolean live) throws DotDataException, DotSecurityException {
		StringBuilder queryBuffer = new StringBuilder();
		queryBuffer.append("select {contentlet.*} ");
		queryBuffer.append("from contentlet, inode contentlet_1_, contentlet_lang_version_info contentletvi ");
		queryBuffer.append("where ");
		if(live!=null){
			queryBuffer.append((live.booleanValue() ? "contentletvi.live_inode":"contentletvi.working_inode"))
			     .append(" = contentlet.inode and ");
		}
		queryBuffer.append("contentlet.identifier = ? and contentlet.inode = contentlet_1_.inode and contentlet_1_.type='contentlet'");
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
		hu.setSQLQuery(queryBuffer.toString());
		if(live!=null){
		  hu.setParam(true);
		}
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
	protected List<Contentlet> getContentletsByIdentifier(String identifier)throws DotDataException, DotSecurityException {
		 return getContentletsByIdentifier(identifier, null);
	}

	@Override
	protected Identifier getRelatedIdentifier(Contentlet contentlet, String relationshipType)throws DotDataException {
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
	protected List<File> getRelatedFiles(Contentlet contentlet)	throws DotDataException {
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
	protected List<Link> getRelatedLinks(Contentlet contentlet)	throws DotDataException {
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
			com.dotmarketing.portlets.contentlet.business.Contentlet c = 
				(com.dotmarketing.portlets.contentlet.business.Contentlet) InodeFactory.getInode(con.getInode(), com.dotmarketing.portlets.contentlet.business.Contentlet.class);
			//Checking contentlet exists inode > 0
			if(InodeUtils.isSet(c.getInode())){
				APILocator.getPermissionAPI().removePermissions(c);
				HibernateUtil.delete(c);
				//db.setSQL("delete from inode where identifier like '6050'");
				//try{
				//	db.loadResult();
				//}catch (Exception e) {
				//	Logger.error(this, e.getMessage(), e);
				//	throw new DotDataException(e.getMessage(), e);
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
	protected List<Contentlet> findAllUserVersions(Identifier identifier) throws DotDataException, DotSecurityException {
		List<Contentlet> cons = new ArrayList<Contentlet>();
		if(!InodeUtils.isSet(identifier.getInode()))
			return cons;
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
		hu.setQuery("select inode from inode in class " + com.dotmarketing.portlets.contentlet.business.Contentlet.class.getName() + 
		        " , vi in class "+ContentletLangVersionInfo.class.getName()+" where vi.identifier=inode.identifier and " +
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
	protected List<Contentlet> findAllVersions(Identifier identifier)throws DotDataException, DotSecurityException {
		List<Contentlet> cons = new ArrayList<Contentlet>();
		if(!InodeUtils.isSet(identifier.getInode()))
			return cons;
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
		hu.setQuery("select inode from inode in class " + com.dotmarketing.portlets.contentlet.business.Contentlet.class.getName() + 
		        " , vi in class "+ContentletLangVersionInfo.class.getName()+" where vi.identifier=inode.identifier and " +
		        " inode.identifier = '" + identifier.getInode() + "' " +
		        		"and type='contentlet'  order by mod_date desc");
		List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
		if(fatties == null)
			return cons;
		else{
			for (com.dotmarketing.portlets.contentlet.business.Contentlet fatty : fatties) {
				Contentlet content = convertFatContentletToContentlet(fatty);
				cc.add(content.getInode(), content);
				cons.add(content);
			}
		}
		return cons;
	}
	
	public Contentlet convertFatContentletToContentlet (com.dotmarketing.portlets.contentlet.business.Contentlet fatty) throws DotDataException,DotSecurityException {
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
			identifier = identifierAPI.findFromInode(fatty.getIdentifier());
			Folder folder = new Folder(); 
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
	
	public com.dotmarketing.portlets.contentlet.business.Contentlet convertContentletToFatContentlet (Contentlet cont, com.dotmarketing.portlets.contentlet.business.Contentlet fatty) throws DotDataException {
		String name = "";
		try {
			name = APILocator.getContentletAPI().getName(cont, APILocator.getUserAPI().getSystemUser(), true);
		}catch (DotSecurityException e) {
			
		}
		Map<String, Object> map = cont.getMap();
		Structure structure = cont.getStructure();
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
			value = map.get(f.getVelocityVarName());
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
	protected void cleanIdentifierHostField(String structureInode) throws DotDataException {
	    StringBuffer sql = new StringBuffer("update identifier set parent_path='/', host_inode=? ");
        sql.append(" where id in (select identifier from contentlet where structure_inode = ?)");
        DotConnect dc = new DotConnect();
        dc.setSQL(sql.toString());
        dc.addParam(APILocator.getHostAPI().findSystemHost().getIdentifier());
        dc.addParam(structureInode);
        dc.loadResults();
        //we could do a select here to figure out exactly which guys to evict
        CacheLocator.getContentletCache().clearCache();
        CacheLocator.getIdentifierCache().clearCache();
	}
	
	@Override
	protected void cleanField(String structureInode, Field field) throws DotDataException {
		StringBuffer sql = new StringBuffer("update contentlet set " );
		if(field.getFieldContentlet().indexOf("float") != -1){
			sql.append("\""+field.getFieldContentlet()+"\"" + " = ");
		}else{
			sql.append(field.getFieldContentlet() + " = ");
		}
		if(field.getFieldContentlet().indexOf("bool") != -1){
			sql.append(DbConnectionFactory.getDBFalse());
		}else if(field.getFieldContentlet().indexOf("date") != -1){
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL) || 
					DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");     	
				sql.append("'" + sdf.format(new Date()) + "'");
			}else{
				sql.append("'" + new Date() + "'");
			}
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
		dc.getResult();
		//we could do a select here to figure out exactly which guys to evict
		ContentletCache cc = CacheLocator.getContentletCache();
		cc.clearCache();
	}

	@SuppressWarnings("unchecked")
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
		
		//String getInodesSQL =
		//            "select inode from contentlet where mod_date < ? and inode not in " +
        //            " ((select working_inode from contentlet_lang_version_info) " +
        //            "  union (select live_inode from contentlet_lang_version_info)) ";
		//dc.setSQL(getInodesSQL);
		//dc.addParam(date);
	    //List<Map<String, Object>> results = dc.loadResults();
	    //int lenght = results.size();
	    //boolean first = true;
		String deleteContentletSQL = "delete from contentlet where mod_date < ? and inode not in " +
				                     " ((select working_inode from contentlet_lang_version_info) " +
				                     "  union (select live_inode from contentlet_lang_version_info)) ";
		dc.setSQL(deleteContentletSQL);
		dc.addParam(date);
		dc.loadResult();
	    
		MaintenanceUtil.cleanMultiTreeTable();
		
		dc.setSQL(countSQL);
		result = dc.loadResults();
		int after = Integer.parseInt(result.get(0).get("count"));
		return before - after;
	}
	
	
	protected List<Contentlet> findContentletsWithFieldValue(String structureInode, Field field) throws DotDataException {
		List<Contentlet> result = new ArrayList<Contentlet>();
		
		try {
			Structure structure = StructureCache.getStructureByInode(structureInode);
			if ((structure == null) || (!InodeUtils.isSet(structure.getInode())))
				return result;
			
			if ((field == null) || (!InodeUtils.isSet(field.getInode())))
				return result;
			
			DotConnect dc = new DotConnect();
			String countSQL = ("select count(*) as count from contentlet, contentlet_lang_version_info contentletvi" +
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
					    " contentletvi in class "+ContentletLangVersionInfo.class.getName() +
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
			//dc.setSQL("select count(*) as count from (select distinct identifier from inode where type like 'contentlet' and inode in (select inode from contentlet))");
			dc.setSQL("select count(*) as count from (select distinct identifier from contentlet)");
		}else{
			//dc.setSQL("select count(*) as count from (select distinct identifier from inode where type like 'contentlet' and inode in (select inode from contentlet)) as t");
			dc.setSQL("select count(*) as count from (select distinct identifier from contentlet) as t");
		}
		
		List<Map<String,String>> results = dc.loadResults();
		long count = Long.parseLong(results.get(0).get("count"));
		return count;
	}

	@Override
	protected List<Map<String, Serializable>> DBSearch(com.dotmarketing.business.query.GenericQueryFactory.Query query, List<Field> fields, String structureInode) throws ValidationException, DotDataException {
		Map<String, Field> velVarfieldsMap = null;
		Map<String, Field> fieldsMap = null;
		try {
			fieldsMap = UtilMethods.convertListToHashMap(fields, "getFieldContentlet", String.class);
		} catch (Exception e) {
			Logger.error(ContentletFactoryDBImpl.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage(), e);
		}
		try {
			velVarfieldsMap = UtilMethods.convertListToHashMap(fields, "getVelocityVarName", String.class);
		} catch (Exception e) {
			Logger.error(ContentletFactoryDBImpl.class,e.getMessage(),e);
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
	protected void UpdateContentWithSystemHost(String hostIdentifier) throws DotDataException, DotSecurityException {
		 DotConnect dc = new DotConnect();
		 List<HashMap<String, String>> contentIdents = null;
		 List<String> identsDeleted = new ArrayList<String>();
		 try {
			 dc.setSQL("select inode.inode,contentlet.identifier,host_inode,asset_type " 
					 + "from identifier,inode,contentlet where "
					 + "contentlet.inode = inode.inode and identifier.id = contentlet.identifier "
					 + "and asset_type like 'contentlet' and host_inode = ?");
			 dc.addParam(hostIdentifier);
			 contentIdents = dc.getResults();
			 
			 dc.setSQL("Update Identifier set host_inode = ? where asset_type like 'contentlet' and host_inode=?");
			 Host systemHost = APILocator.getHostAPI().findSystemHost();
			 dc.addParam(systemHost.getIdentifier());
			 dc.addParam(hostIdentifier);
			 dc.loadResult();

			 for(HashMap<String, String> ident:contentIdents){
			  String identifier = ident.get("identifier");
			  String inode = ident.get("inode");
			   if(!identsDeleted.contains(identifier) && identifier!=null && identifier!=""){
				 CacheLocator.getIdentifierCache().removeFromCacheByVersionable(cc.get(inode));
			     //Identifier iden = (Identifier)InodeFactory.getInodeOfClassByCondition
				//	                                (Identifier.class,"inode= '"+identifier+"'");
				 Identifier iden = APILocator.getIdentifierAPI().find(identifier);
				 identsDeleted.add(identifier);
			   }
			   if(InodeUtils.isSet(inode)){
				 cc.remove(inode);
				 Contentlet content = find(inode);
				 APILocator.getDistributedJournalAPI().addContentIndexEntry(content);
			   }
			 }
		} catch (DotDataException e) {
			Logger.error(ContentletFactoryDBImpl.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage(), e);		
		}
	}


	@Override
	protected void removeUserReferences(String userId) throws DotDataException, DotSecurityException {
		DotConnect dc = new DotConnect();
		User systemUser = null;
		try {
		   systemUser = APILocator.getUserAPI().getSystemUser();
		   dc.setSQL("Select * from contentlet where mod_user = ?");
		   dc.addParam(userId);
		   List<HashMap<String, String>> contentInodes = dc.getResults();
		   dc.setSQL("UPDATE contentlet set mod_user = ? where mod_user = ? ");
		   dc.addParam(systemUser.getUserId());
		   dc.addParam(userId);
		   dc.loadResult();
		   for(HashMap<String, String> ident:contentInodes){
			 String inode = ident.get("inode");
			 cc.remove(inode);
			 Contentlet content = find(inode);
			 APILocator.getDistributedJournalAPI().addContentIndexEntry(content);
		  }
		} catch (DotDataException e) {
			Logger.error(ContentletFactoryDBImpl.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage(), e);	
		}
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
	protected void unpublishAllVersions(Contentlet contentlet, User user) throws DotDataException, DotStateException, DotSecurityException {
		List<Contentlet> conVersions = new ArrayList<Contentlet>();
		if(InodeUtils.isSet(contentlet.getIdentifier())){
			conVersions = findContentletsByIdentifier(contentlet.getIdentifier(), null, contentlet.getLanguageId());
		}

		if(!conVersions.isEmpty()) {
			DotConnect db = new DotConnect();
			db.setSQL("update contentlet set mod_date=?, mod_user=? where identifier = ?");
			db.addParam(new java.util.Date());
			db.addParam(user.getUserId());
			db.addParam(contentlet.getIdentifier());
			db.loadResult();
			db.setSQL("update contentlet_lang_version_info set live_inode=null where identifier=?");
			db.addParam(contentlet.getIdentifier());
			db.loadResult();

			for(Contentlet con: conVersions){
				cc.remove(con.getInode());
			}
		}
	}
	
	
	protected void removeFolderReferences(Folder folder) throws DotDataException, DotSecurityException{
		Folder parentFolder = null;
		try{
			parentFolder = APILocator.getFolderAPI().findParentFolder(folder, APILocator.getUserAPI().getSystemUser(), false);
		}catch(Exception e){
			Logger.debug(this, "Unable to get parent folder for folder = " + folder.getInode(), e);
		}
		String parentFolderId = parentFolder!=null?parentFolder.getInode():FolderAPI.SYSTEM_FOLDER;
		DotConnect dc = new DotConnect();
		dc.setSQL("SELECT inode FROM contentlet WHERE folder = ?");
		dc.addParam(folder.getInode());
		List<HashMap<String, String>> contentInodes = dc.loadResults();
		dc.setSQL("update contentlet set folder=? where folder=?");
		dc.addParam(parentFolderId);
		dc.addParam(folder.getInode());
		dc.loadResult();
		for(HashMap<String, String> ident:contentInodes){
			 String inode = ident.get("inode");
			 cc.remove(inode);
			 Contentlet content = find(inode);
			 APILocator.getDistributedJournalAPI().addContentIndexEntry(content);
		}
	}
}*/
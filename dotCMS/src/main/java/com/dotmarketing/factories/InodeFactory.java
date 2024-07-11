package com.dotmarketing.factories;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Parameter;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.net.sf.hibernate.ObjectNotFoundException;

/**
 * 
 * @author will
 * @deprecated
 */
public class InodeFactory {

	public static java.util.List getChildrenClassByConditionAndOrderBy(String p, Class<? extends Inode> c, String condition,
			String orderby, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		if(c.equals(Structure.class)){
			throw new DotStateException("Structure mapping was deleted from hibernate files");
		}
		if(c.equals(Relationship.class)){
			throw new DotStateException("Relationship mapping was deleted from hibernate files");
		}
		if(c.equals(Template.class)){
			throw new DotStateException("Template mapping was deleted from hibernate files");
		}
		if(c.equals(Category.class)){
			throw new DotStateException("Category mapping was deleted from hibernate files");
		}
		if(c.equals(Folder.class)){
			throw new DotStateException("Folder mapping was deleted from hibernate files");
		}

		try {
			final String tableName = c.getDeclaredConstructor().newInstance().getType();
			final HibernateUtil dh = new HibernateUtil(c);

			String sql = "SELECT {" + tableName + ".*} from " + tableName + " " + tableName + ", tree tree, inode "
					+ tableName + "_1_ where tree.parent = ? and tree.child = " + tableName + ".inode and " + tableName
					+ "_1_.inode = " + tableName + ".inode and "+tableName+"_1_.type = '"+tableName+"' and " + condition + " order by " + orderby;

			if(!UtilMethods.isSet(orderby))
				sql +=  tableName + ".inode desc";

			//sql += (sql.toLowerCase().indexOf("limit") == -1 ? ", " + tableName + ".inode desc" : "");

			Logger.debug(InodeFactory.class, "hibernateUtilSQL:getChildrenClassByConditionAndOrderBy\n " + sql + "\n");

			dh.setSQLQuery(sql);

			Logger.debug(InodeFactory.class, "inode:  " + p + "\n");

			dh.setParam(p);

			if (limit != 0) {
				dh.setFirstResult(offset);
				dh.setMaxResults(limit);
			}

			return dh.list();
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "getChildrenClassByConditionAndOrderBy failed:" + e, e);

			// throw new DotRuntimeException(e.toString());
		}

		return new java.util.ArrayList();
	}

	public static java.util.List getChildrenClassByConditionAndOrderBy(Inode p, Class c, String condition,
			String orderby) {

		return getChildrenClassByConditionAndOrderBy(p.getInode(), c, condition, orderby, 0, 0);
	}

	public static Inode getInode(String x, Class c) {

		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}else if( c.equals(Folder.class)){
			throw new DotStateException("You should use the FolderAPI to get folder information");
		}else if(c.equals(Inode.class)){
			Logger.debug(InodeFactory.class, "You should not send Inode.class to getInode.  Send the extending class instead (inode:" + x + ")" );
			//Thread.dumpStack();
			DotConnect dc = new DotConnect();
			dc.setSQL("select type from inode where inode = ?");
			dc.addParam(x);
			try {
				if(dc.loadResults().size()>0){
				    final String type = dc.getString("type");
				    if(type == null) {
				        return new Inode();
				    }

					c = InodeUtils.getClassByDBType(type);
				}
				else{
					Logger.debug(InodeFactory.class,  x + " is not an Inode " );
					return new Inode();
				}
			} catch (DotDataException e) {
				// this is not an INODE
				Logger.debug(InodeFactory.class,  x + " is not an Inode", e );
				return new Inode();
			}
		}
		if(c.equals(Structure.class)){
			throw new DotStateException("Structure mapping was deleted from hibernate files");
		}
		if(c.equals(Relationship.class)){
			throw new DotStateException("Relationship mapping was deleted from hibernate files");
		}
		if(c.equals(Template.class)){
			throw new DotStateException("Template mapping was deleted from hibernate files");
		}
		if(c.equals(Folder.class)){
			throw new DotStateException("Folder mapping was deleted from hibernate files");
		}
		if (c.equals(Category.class)){
			throw new DotStateException("Category mapping was deleted from hibernate files");
		}
		
		
		
		Inode inode = null;
		try {
			inode = (Inode) new HibernateUtil(c).load(x);
		} catch (Exception e) {
			//return (Inode) new HibernateUtil(c).load(x);
		}
		return inode;
	}

	public static void deleteInode(Object o) throws DotHibernateException {
		if(Identifier.class.equals(o.getClass())){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		Inode inode = (Inode) o;
		if(inode ==null || !UtilMethods.isSet(inode.getInode())){

			Logger.error(Inode.class, "Empty Inode Passed in");
			return;
		}


		if(o instanceof Permissionable){
			try {
				APILocator.getPermissionAPI().removePermissions((Permissionable)o);
			} catch (DotDataException e) {
				Logger.error(InodeFactory.class,"Cannot delete object because permissions not deleted : " + e.getMessage(),e);
				return;
			}
		}


		// workaround for dbs where we can't have more than one constraint
		// or triggers
		DotConnect db = new DotConnect();
		db.setSQL("delete from tree where child = ? or parent =?");
		db.addParam(inode.getInode());
		db.addParam(inode.getInode());
		db.getResult();

		// workaround for dbs where we can't have more than one constraint
		// or triggers
		db.setSQL("delete from multi_tree where child = ? or parent1 =? or parent2 = ?");
		db.addParam(inode.getInode());
		db.addParam(inode.getInode());
		db.addParam(inode.getInode());
		db.getResult();

		if(Template.class.equals(o.getClass())){
			try {
				FactoryLocator.getTemplateFactory().deleteTemplateByInode(Template.class.cast(o).getInode());
			} catch (DotDataException e) {
				Logger.warnAndDebug(Template.class,e.getMessage(),e);
			}
		} else {
			HibernateUtil.delete(o);
			db.setSQL("delete from inode where inode = ?");
			db.addParam(inode.getInode());
			db.getResult();
		}
	}

	public static int countChildrenOfClass(Inode i, Class c) {
		return countChildrenOfClass(i, c, 0, 5);
	}

	public static int countChildrenOfClass(Inode i, Class c, int limit, int offset) {
		if( c.equals(Identifier.class)){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {

			String tableName = ((Inode) c.getDeclaredConstructor().newInstance()).getType();
			DotConnect db = new DotConnect();
			db
			.setSQL("select count(*) as test from inode, tree where inode.inode = tree.child and tree.parent = ?  and inode.type = '"
					+ tableName + "' ");
			db.addParam(i.getInode());
			// db.addParam(tableName);
			return db.getInt("test");
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "countChildrenOfClass failed:" + e, e);
			throw new DotRuntimeException(e.toString());
			// return 0;
		}
	}

	public static Map<String,Integer> countChildrenOfClass(Inode i, Class<? extends Inode>[] inodeClasses) {
		if(Identifier.class.equals(i.getClass())){
			throw new DotStateException("Identifiers are no longer Inodes!");
		}
		try {
			Map<String,Integer> statistics = new HashMap<>();

			if( inodeClasses != null && inodeClasses.length > 0 ) {

				String[] types = new String[inodeClasses.length];
				StringBuffer arrayParams = new StringBuffer();
				String params = null;
				int n = 0;

				for(Class<? extends Inode> anInodeClass: inodeClasses) {
					types[n++] = anInodeClass.getDeclaredConstructor().newInstance().getType();
					arrayParams.append("?,");
				}
				
				if( arrayParams.length() > 0 ) {
					params = arrayParams.toString().substring(0, arrayParams.length() - 1);
				}
	
				DotConnect db = new DotConnect();
				db
				.setSQL("select count(*) as test, inode.type from inode, tree where inode.inode = tree.child and tree.parent = ?  and inode.type IN (" + params + ") group by inode.type");
				db.addParam(i.getInode());
				
				for(String type: types) {
					db.addParam(type);
				}

				ArrayList<Map<String, Object>> results = db.getResults();
				int length = results.size();
				for(n = 0; n < length; n++)
				{
					Map<String, Object> hash = (Map<String, Object>) results.get(n);
					String type = (String) hash.get("type");
					Integer number = Integer.parseInt(((String) hash.get("test")));
					statistics.put(type, number);
				}
			}

			return statistics;
		} catch (Exception e) {
			Logger.warn(InodeFactory.class, "countChildrenOfClass(Inode,Class<Inode>[]) failed:" + e, e);
			throw new DotRuntimeException(e.toString());
			// return 0;
		}
	}

	//http://jira.dotmarketing.net/browse/DOTCMS-3232
    //To Check whether given inode exists in DB or not
	@CloseDBIfOpened
	public static boolean isInode(String inode){
		DotConnect dc = new DotConnect();
		String InodeQuery = "Select count(*) as count from inode where inode = ?";
		dc.setSQL(InodeQuery);
		dc.addParam(inode);
		ArrayList<Map<String, String>> results = new ArrayList<>();
		try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(InodeFactory.class,"isInode method failed:"+ e, e);
		}
		int count = Parameter.getInt(results.get(0).get("count"),0);
		if(count > 0){
			return true;
		}
		return false;
	}		
	public static Inode find(String id) throws DotDataException {
		Inode inode = null;
		try {
			inode = (Inode) HibernateUtil.load(Inode.class, id);
		} catch (DotHibernateException e) { 
			if(!(e.getCause() instanceof ObjectNotFoundException))
				throw e; 
		}
		return inode;
	}
	
	/**
	 * Method will change user references of the given userId in Inodes 
	 * with the replacement user Id 
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	@WrapInTransaction
	public static void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException{
        DotConnect dc = new DotConnect();
        

        
        
        try {
            List<String> contentTypes = dc.setSQL("select inode.inode as strucId from inode, structure where structure.inode=inode.inode and inode.owner=?")
                            .addParam(userId)
                            .loadObjectResults()
                            .stream()
                            .map(r->String.valueOf(r.get("strucId")))
                            .collect(Collectors.toList());
            
            
            
            
           dc.setSQL("update inode set owner = ? where owner = ?");
           dc.addParam(replacementUserId);
           dc.addParam(userId);
           dc.loadResult();

           
           // invalidate the content type cache
           for(String id : contentTypes) {
               try {
                   ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(id);
                   CacheLocator.getContentTypeCache2().remove(type);
               }
               catch(Exception e) {
                   Logger.warnAndDebug(InodeFactory.class, e);
               }
           }
           
           
           
        } catch (DotDataException e) {
            Logger.error(InodeFactory.class,e.getMessage(),e);
            throw new DotDataException(e.getMessage(), e);
        }
	}

}

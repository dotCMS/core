package com.dotmarketing.business.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.query.GenericQueryFactory.BuilderType;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * 
 * @author Jason Tesser
 *
 */

public class QueryUtil {

	/**
	 * Use this method to help with building the WHERE clause from ComplexCriteria
	 * @param criteriaToBuildOut
	 * @param queryToAppendTo
	 * @param params
	 */
	private static void buildComplexCriteria(Map<String, String> dbColToObjectAttribute, ComplexCriteria criteriaToBuildOut, StringBuilder queryToAppendTo, String conditionToAppend,List<Object> params){
		List<Criteria> cs = criteriaToBuildOut.getCriteria();
		boolean first = true;
		boolean open = false;
		for (Criteria criteria : cs) {
			if(criteria instanceof SimpleCriteria){
				if(!first){
					queryToAppendTo.append(" " + criteriaToBuildOut.getPreceedingOperator(criteria) + " ");
					queryToAppendTo.append("(" + (conditionToAppend != null ? conditionToAppend + " AND ": ""));
					open = true;
				}
				String att = dbColToObjectAttribute.get(((SimpleCriteria) criteria).getAttribute()) != null ? dbColToObjectAttribute.get(((SimpleCriteria) criteria).getAttribute()) : ((SimpleCriteria) criteria).getAttribute();
				queryToAppendTo.append(att + " " + ((SimpleCriteria) criteria).getOperator() + " ?");
				if(open){
					queryToAppendTo.append(")");
					open = false;
				}
				params.add(((SimpleCriteria) criteria).getValue());
			}else if(criteria instanceof ComplexCriteria){
				if(!first){
					queryToAppendTo.append(" " + criteriaToBuildOut.getPreceedingOperator(criteria) + " ");
				}
				queryToAppendTo.append(" (" + (conditionToAppend != null ? conditionToAppend + " AND ": ""));
				buildComplexCriteria(dbColToObjectAttribute, (ComplexCriteria)criteria, queryToAppendTo, conditionToAppend, params);
				queryToAppendTo.append(") ");
			}
			first = false;
		}
	}

	/**
	 * A generic method intended to
	 * @param query - Will handle * in the selectAttributes of the query and alias so you could pass in {*,text1 as title} which are 2 elements within the array.
	 * @param dbColToObjectAttribute - If you need here you can specify a title if the DB doesn't have one. 
	 * @param conditionToAppend
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws ValidationException
	 * @throws DotDataException
	 */
	public static List<Map<String, Serializable>> DBSearch(Query query, Map<String, String> dbColToObjectAttribute, String conditionToAppend, User user,boolean isPermissionsByIdentifier,boolean respectFrontendRoles) throws ValidationException,DotDataException{
		Map<String, String> objectAttributeTodbCol = new HashMap<String, String>();
	    PermissionAPI perAPI = APILocator.getPermissionAPI();
		for (String key : dbColToObjectAttribute.keySet()) {
			objectAttributeTodbCol.put(dbColToObjectAttribute.get(key), key);
		}
		List<Map<String, Serializable>> res = new ArrayList<Map<String,Serializable>>();
		List<Map<String, Serializable>> filteredResults = new ArrayList<Map<String,Serializable>>();
		Criteria c = query.getCriteria();
		StringBuilder bob = new StringBuilder();
		List<Object> params = null;
		List <PermissionableProxy> permDummys= new  ArrayList <PermissionableProxy> ();
		bob.append("SELECT ");		
		if(UtilMethods.isSet(query.getSelectAttributes())){
			boolean first = true;
			for (String att : query.getSelectAttributes()) {
				if(!first){
					bob.append(",");
				}
				if(objectAttributeTodbCol.get(att) != null){
					bob.append(query.getBuilderType()+"."+objectAttributeTodbCol.get(att));
				}else{
					bob.append(query.getBuilderType()+"."+att);
				}
				first = false;
			}
		}else{
			bob.append(query.getBuilderType()+".*");
		}
		String queryBuilderType = query.getBuilderType().toString();
		if(queryBuilderType.equalsIgnoreCase(BuilderType.CONTENTLET.toString())
				|| queryBuilderType.equalsIgnoreCase(BuilderType.FILE_ASSET.toString())
				|| queryBuilderType.equalsIgnoreCase(BuilderType.HTMLPAGE.toString())
				|| queryBuilderType.equalsIgnoreCase(BuilderType.MENU_LINK.toString())){		
			bob.append(","+queryBuilderType+".identifier,inode.owner");
		}else{
			bob.append(",inode.owner");
		}
		bob.append(" FROM " + query.getBuilderType()+", inode");
		if(UtilMethods.isSet(conditionToAppend)){
			 bob.append(" WHERE " + conditionToAppend);
		}
		if(c != null){
			params = new ArrayList<Object>();
			if(!UtilMethods.isSet(conditionToAppend)){
				bob.append(" WHERE ");
			}else{
				bob.append(" AND ");
			}
			if(c instanceof SimpleCriteria){	
				String att = objectAttributeTodbCol.get(((SimpleCriteria) c).getAttribute()) != null ? objectAttributeTodbCol.get(((SimpleCriteria) c).getAttribute()) : ((SimpleCriteria) c).getAttribute();
				bob.append(att + " " + ((SimpleCriteria) c).getOperator() + " ?");
				params.add(((SimpleCriteria) c).getValue());
			}else if(c instanceof ComplexCriteria){
				List<Criteria> criteriaList = ((ComplexCriteria) c).getCriteria();
				boolean open = false;
				for (Criteria criteria : criteriaList) {
					if(criteria instanceof SimpleCriteria){
						if(((ComplexCriteria)c).getPreceedingOperator(criteria) != null){
							bob.append(" " + ((ComplexCriteria)c).getPreceedingOperator(criteria) + " ");
							bob.append("(" + (conditionToAppend != null ? conditionToAppend + " AND ": ""));
							open = true;
						}
						String att = objectAttributeTodbCol.get(((SimpleCriteria) criteria).getAttribute()) != null ? objectAttributeTodbCol.get(((SimpleCriteria) criteria).getAttribute()) : ((SimpleCriteria) criteria).getAttribute();
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
						bob.append(" (" + (conditionToAppend != null ? conditionToAppend + " AND ": ""));
						buildComplexCriteria(objectAttributeTodbCol, (ComplexCriteria)criteria, bob, conditionToAppend, params);
						bob.append(")");
					}
				}
			}
		}
		if(!UtilMethods.isSet(conditionToAppend)&& c== null){
			bob.append(" WHERE "+ query.getBuilderType()+".inode=inode.inode");
		}
		else {
			bob.append(" AND "+ query.getBuilderType()+".inode=inode.inode");
		}
		DotConnect dc = new DotConnect();
		dc.setSQL(bob.toString().toLowerCase());
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
		String blank= "";
		List<Map<String, Object>> dbrows = dc.loadObjectResults();
		for (Map<String, Object> row : dbrows) {
			Map<String, Serializable> m = new HashMap<String, Serializable>();
			 PermissionableProxy permDummy= new PermissionableProxy();
			 permDummy.setPermissionByIdentifier(isPermissionsByIdentifier);
			 permDummy.setInode(row.get("inode").toString());
			if(isPermissionsByIdentifier && UtilMethods.isSet(row.get("identifier"))){//DOTCMS-3739
				 permDummy.setIdentifier(row.get("identifier").toString());
			}
			permDummys.add(permDummy);
			for (String colkey : row.keySet()) {
				if(colkey.startsWith("bool")|| colkey.startsWith("float")|| colkey.startsWith("integer")|| colkey.startsWith("text")|| colkey.equals("working")|| colkey.equals("deleted")){
					if(dbColToObjectAttribute.get(colkey) != null){
						if(row.get(colkey) instanceof Serializable){
							m.put(dbColToObjectAttribute.get(colkey), (Serializable) row.get(colkey));
						}else{
							m.put(dbColToObjectAttribute.get(colkey), row.get(colkey) == null ? "" : row.get(colkey).toString());
						}
					}
				}else if(colkey.startsWith("date") ){
					if(dbColToObjectAttribute.get(colkey) != null){
						if(row.get(colkey)==null){
							m.put(dbColToObjectAttribute.get(colkey), (Serializable)blank );
							}
						else{
							String date=row.get(colkey).toString();
							m.put(dbColToObjectAttribute.get(colkey), (Serializable)date);
							}
					}
				}else if(row.get(colkey) instanceof Date){ 
					if(row.get(colkey)==null)
						m.put(colkey, (Serializable)blank );
					else{
						String date=row.get(colkey).toString();
						m.put(colkey, (Serializable)date);
						}
				}
				else{
					m.put(colkey, (Serializable) row.get(colkey));
				}
			}
			
			try {
				permDummys=perAPI.filterCollection(permDummys, PermissionAPI.PERMISSION_READ, false, user );
			} catch (DotSecurityException e1) {

			}

			res.add(m);	
		}
		
		if (permDummys.size() > 0) {
			for (Map<String, Serializable> r : res) {
				for (PermissionableProxy pDummy : permDummys) {
					if (r.get("inode").equals(pDummy.getInode().toString())) {
						filteredResults.add(r);
					}
				}
			}
		}

		return filteredResults; 
	}
	
}

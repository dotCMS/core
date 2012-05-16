package com.dotmarketing.portlets.usermanager.factories;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.portlet.ActionRequest;
import javax.servlet.http.HttpSession;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.usermanager.struts.UserManagerListSearchForm;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;

/**
 * @author David Torres
 * @author Oswaldo Gallango
 *
 */

public class UserManagerListBuilderFactory {

	public static List<Map<String, Object>> doSearch(UserManagerListSearchForm form) {
		boolean isCount = false;
		return doSearch( form, isCount);
	}

	/**
	 * Return the list of user ids or the count of users from the given search form
	 * @param form
	 * @param isCount
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> doSearch(UserManagerListSearchForm form, boolean isCount) {
		StringBuffer querySelectPortion = new StringBuffer();
		StringBuffer queryWherePortion = new StringBuffer();

		DotConnect dc = new DotConnect();
		String select = "select distinct user_.userid as userid, user_.createdate from user_";
		if(isCount)
		{
			select = "select count(distinct user_.userid) as total from user_";
		}

		querySelectPortion.append(select);
		if(UtilMethods.isSet(form.getTagName())) {
			querySelectPortion.append(", tag_inode, tag");
		}

		queryWherePortion.append(" where user_.companyid ='");
		queryWherePortion.append(PublicCompanyFactory.getDefaultCompany().getCompanyId());
		queryWherePortion.append("'");

		String[] arrayUserIds = form.getArrayUserIds();
		List<Map<String, Object>> results = new ArrayList<Map<String,Object>>();
		boolean runQuery = true;
		if (arrayUserIds!=null && arrayUserIds.length > 0) {
			runQuery = false;
			boolean first = true;
			StringBuilder queryUserArrayPortion = new StringBuilder();
			for (int i=0; i<arrayUserIds.length; i++) {
				if(first){
					queryUserArrayPortion.append(" and user_.userid in ('" + arrayUserIds[i] + "'");
				}else{
					queryUserArrayPortion.append(",'"+arrayUserIds[i]+"'");
				}
				first = false;

				if((i % 500) == 0 && i != 0){
					queryUserArrayPortion.append(")");
					String query = querySelectPortion.toString() + queryWherePortion.toString() + queryUserArrayPortion.toString();
					Logger.debug(UserManagerListBuilderFactory.class, "query:" + query);
					dc.setSQL(query);
					try {
						results.addAll(dc.getResults());
					} catch (DotDataException e) {
						Logger.error(UserManagerListBuilderFactory.class,e.getMessage(),e);
					}
					queryUserArrayPortion.delete(0, queryUserArrayPortion.length());
					first = true;
				}
			}
		}else{
			String userIdSearch = (UtilMethods.isSet(form.getUserIdSearch()) ? form.getUserIdSearch().trim().toLowerCase() : null);
			String firstName =    (UtilMethods.isSet(form.getFirstName()) ? form.getFirstName().trim().toLowerCase() : null);
			String middleName =   (UtilMethods.isSet(form.getMiddleName()) ? form.getMiddleName().trim().toLowerCase()  : null);
			String lastName =     (UtilMethods.isSet(form.getLastName()) ? form.getLastName().trim().toLowerCase()  : null);
			String emailAddress = (UtilMethods.isSet(form.getEmailAddress()) ? form.getEmailAddress().trim().toLowerCase()  : null);
			String dateOfBirthTypeSearch =    (UtilMethods.isSet(form.getDateOfBirthTypeSearch()) ? form.getDateOfBirthTypeSearch() : null);
			Date dateOfBirthFromDate =        (UtilMethods.isSet(form.getDateOfBirthFromDate()) ? form.getDateOfBirthFromDate() : null);
			Date dateOfBirthToDate =          (UtilMethods.isSet(form.getDateOfBirthToDate()) ? form.getDateOfBirthToDate() : null);
			Date dateOfBirthSinceDate =       (UtilMethods.isSet(form.getDateOfBirthSinceDate()) ? form.getDateOfBirthSinceDate() : null);
			String lastLoginTypeSearch =      (UtilMethods.isSet(form.getLastLoginTypeSearch()) ? form.getLastLoginTypeSearch() : null);
			Date lastLoginFromDate =          (UtilMethods.isSet(form.getLastLoginDateFromDate()) ? form.getLastLoginDateFromDate() : null);
			Date lastLoginToDate =            (UtilMethods.isSet(form.getLastLoginDateToDate()) ? form.getLastLoginDateToDate() : null);
			String lastLoginSince =           (UtilMethods.isSet(form.getLastLoginSince()) ? form.getLastLoginSince() : null);
			String createdTypeSearch =        (UtilMethods.isSet(form.getCreatedTypeSearch()) ? form.getCreatedTypeSearch() : null);
			Date createdDateFromDate =        (UtilMethods.isSet(form.getCreatedDateFromDate()) ? form.getCreatedDateFromDate() : null);
			Date createdDateToDate =          (UtilMethods.isSet(form.getCreatedDateToDate()) ? form.getCreatedDateToDate() : null);
			String createdSince =             (UtilMethods.isSet(form.getCreatedSince()) ? form.getCreatedSince() : null);
			String lastVisitTypeSearch =      (UtilMethods.isSet(form.getLastVisitTypeSearch()) ? form.getLastVisitTypeSearch() : null);
			Date lastVisitDateFromDate =      (UtilMethods.isSet(form.getLastVisitDateFromDate()) ? form.getLastVisitDateFromDate() : null);
			Date lastVisitDateToDate =        (UtilMethods.isSet(form.getLastVisitDateToDate()) ? form.getLastVisitDateToDate() : null);
			String lastVisitSince =           (UtilMethods.isSet(form.getLastVisitSince()) ? form.getLastVisitSince() : null);
			String active =       (UtilMethods.isSet(form.getActive()) ? form.getActive() : null);
			String tagName =      (UtilMethods.isSet(form.getTagName()) ? form.getTagName() : null);


			// User fields filters
			if (UtilMethods.isSet(firstName))
			{
				firstName = "%" + firstName + "%";
				queryWherePortion.append(" and lower(user_.firstName) like ? ");
			}
			if (UtilMethods.isSet(middleName))
			{
				middleName = "%" + middleName + "%";
				queryWherePortion.append(" and lower(user_.middleName) like ? ");
			}
			if (UtilMethods.isSet(lastName))
			{
				lastName = "%" + lastName + "%";
				queryWherePortion.append(" and lower(user_.lastName) like ? ");
			}
			if (UtilMethods.isSet(emailAddress))
			{
				emailAddress = "%" + emailAddress + "%";
				queryWherePortion.append(" and lower(user_.emailAddress) like ? ");
			}
			if (UtilMethods.isSet(dateOfBirthTypeSearch)) {
				if (dateOfBirthTypeSearch.equalsIgnoreCase("DateRange")) {
					if (UtilMethods.isSet(dateOfBirthFromDate)) {
						queryWherePortion.append(" and user_.birthday >= ? ");
					}
					if (UtilMethods.isSet(dateOfBirthToDate))	{
						queryWherePortion.append(" and user_.birthday <= ? ");
					}
				}
				else if (dateOfBirthTypeSearch.equalsIgnoreCase("Since")) {
					if (UtilMethods.isSet(dateOfBirthSinceDate)) {
						queryWherePortion.append(" and user_.birthday like '");
						queryWherePortion.append(UtilMethods.dateToShortJDBC(dateOfBirthSinceDate));
						queryWherePortion.append("%'");
					}
				}
			}
			if (UtilMethods.isSet(lastLoginTypeSearch)) {
				if (lastLoginTypeSearch.equalsIgnoreCase("DateRange")) {
					if (UtilMethods.isSet(lastLoginFromDate)) {
						queryWherePortion.append(" and user_.logindate >= ? ");
					}
					if (UtilMethods.isSet(lastLoginToDate))	{
						queryWherePortion.append(" and user_.logindate <= ? ");
					}
				}
				else if (lastLoginTypeSearch.equalsIgnoreCase("Since")) {
					if (UtilMethods.isSet(lastLoginSince)) {
						queryWherePortion.append(" and user_.logindate >= ? ");
					}
				}
			}
			if (UtilMethods.isSet(createdTypeSearch)) {
				if (createdTypeSearch.equalsIgnoreCase("DateRange")) {
					if (UtilMethods.isSet(createdDateFromDate)) {
						queryWherePortion.append(" and user_.createdate >= ? ");
					}
					if (UtilMethods.isSet(createdDateToDate))	{
						queryWherePortion.append(" and user_.createdate <= ? ");
					}
				}
				else if (createdTypeSearch.equalsIgnoreCase("Since")) {
					if (UtilMethods.isSet(createdSince)) {
						queryWherePortion.append(" and user_.createdate >= ? ");
					}
				}
			}
			boolean usingClickStreamTable = false;
			if (UtilMethods.isSet(lastVisitTypeSearch)) {

				if (lastVisitTypeSearch.equalsIgnoreCase("DateRange")) {
					boolean userVerification = false;
					if (UtilMethods.isSet(lastVisitDateFromDate)) {
						if(!usingClickStreamTable) {
							querySelectPortion.append(", clickstream");
							usingClickStreamTable = true;
						}
						if (!userVerification) {
							queryWherePortion.append(" and clickstream.user_id = user_.userid ");
							userVerification = true;
						}
						queryWherePortion.append(" and clickstream.start_date >= ? ");
					}
					if (UtilMethods.isSet(lastVisitDateToDate))	{
						if(!usingClickStreamTable) {
							querySelectPortion.append(", clickstream");
							usingClickStreamTable = true;
						}
						if (!userVerification) {
							queryWherePortion.append(" and clickstream.user_id = user_.userid ");
							userVerification = true;
						}
						queryWherePortion.append(" and clickstream.start_date <= ? ");
					}
				}
				else if (lastVisitTypeSearch.equalsIgnoreCase("Since")) {
					if (UtilMethods.isSet(lastVisitSince)) {
						if(!usingClickStreamTable) {
							querySelectPortion.append(", clickstream");
							usingClickStreamTable = true;
						}
						queryWherePortion.append(" and clickstream.user_id = user_.userid ");
						queryWherePortion.append(" and clickstream.start_date >= ? ");
					}
				}
			}
			if (UtilMethods.isSet(active)) {
				if (active.equalsIgnoreCase("true")) {
					queryWherePortion.append(" and user_.active_ = " + DbConnectionFactory.getDBTrue() + " ");
				}
				else if (active.equalsIgnoreCase("false")) {
					queryWherePortion.append(" and user_.active_ = " + DbConnectionFactory.getDBFalse() + " ");
				}
			}

			// User Proxy fields filters
			boolean addingUserProxyWhere = false;
			if (UtilMethods.isSet(tagName))
			{
				StringTokenizer tagNameToken = new StringTokenizer(tagName, ",");
				StringBuffer tagNameParam = new StringBuffer("");
				if (tagNameToken.hasMoreTokens()) {
					for (; tagNameToken.hasMoreTokens();) {
						String token = tagNameToken.nextToken();
						tagNameParam.append("'"+token.trim().replace("'", "''")+"'");
						if (tagNameToken.hasMoreTokens()) {
							tagNameParam.append(",");
						}
					}
				}

				querySelectPortion.append(", user_proxy");
				queryWherePortion.append(" and tag.tag_id = tag_inode.tag_id and tag.tagname in ("+tagNameParam+") ");
				queryWherePortion.append(" and tag_inode.inode = user_proxy.inode ");
				queryWherePortion.append(" and user_proxy.user_Id = user_.userId ");
				addingUserProxyWhere = true;
			}

			// Address fields filters
			if (UtilMethods.isSet(form.getCity()) || UtilMethods.isSet(form.getCountry())
					|| UtilMethods.isSet(form.getState()) || UtilMethods.isSet(form.getZipStr())
					|| UtilMethods.isSet(form.getPhone()) || UtilMethods.isSet(form.getFax())
					|| UtilMethods.isSet(form.getCellPhone()))
			{
				querySelectPortion.append(", address");
				queryWherePortion.append(" and address.userId = user_.userId");
			}

			String city =      (UtilMethods.isSet(form.getCity()) ? form.getCity().trim().toLowerCase() : null);
			String state =     (UtilMethods.isSet(form.getState()) ? form.getState().trim().toLowerCase() : null);
			String country =   (UtilMethods.isSet(form.getCountry()) ? form.getCountry().trim().toLowerCase() : null);
			String zip =       (UtilMethods.isSet(form.getZipStr()) ? form.getZipStr().trim().toLowerCase() : null);
			String phone =     (UtilMethods.isSet(form.getPhone()) ? form.getPhone().trim().toLowerCase() : null);
			String fax =       (UtilMethods.isSet(form.getFax()) ? form.getFax().trim().toLowerCase() : null);
			String cellPhone = (UtilMethods.isSet(form.getCellPhone()) ? form.getCellPhone().trim().toLowerCase() : null);
			String referer = (UtilMethods.isSet(form.getUserReferer()) ? form.getUserReferer() : null);

			if (UtilMethods.isSet(city))
			{
				city = "%" + city + "%";
				queryWherePortion.append(" and lower(address.city) like ? ");
			}

			if (UtilMethods.isSet(state))
			{
				state = "%" + state + "%";
				queryWherePortion.append(" and lower(address.state) like ? ");
			}

			if (UtilMethods.isSet(country))
			{
				country = "%" + country + "%";
				queryWherePortion.append(" and lower(address.country) like ? ");
			}

			if (UtilMethods.isSet(zip))
			{
				zip = "%" + zip + "%";
				queryWherePortion.append(" and lower(address.zip) like ? ");
			}

			if (UtilMethods.isSet(phone))
			{
				phone = "%" + phone + "%";
				queryWherePortion.append(" and lower(address.phone) like ? ");
			}

			if (UtilMethods.isSet(fax))
			{
				fax = "%" + fax + "%";
				queryWherePortion.append(" and lower(address.fax) like ? ");
			}

			if (UtilMethods.isSet(cellPhone))
			{
				cellPhone = "%" + cellPhone + "%";
				queryWherePortion.append(" and lower(address.cell) like ? ");
			}

			if(UtilMethods.isSet(form.getUserReferer())){
				if(!usingClickStreamTable) {
					querySelectPortion.append(", clickstream, clickstream_request");
					usingClickStreamTable = true;
				} else {
					querySelectPortion.append(", clickstream_request");
				}

				referer = "%" + referer + "%";
				queryWherePortion.append(" and user_.userid=clickstream.user_id and clickstream_request.clickstream_id = clickstream.clickstream_id ");
				queryWherePortion.append(" and (clickstream_request.request_uri like ? or clickstream.referer like ?)");
			}

			//User Id Search
			if (UtilMethods.isSet(userIdSearch))
			{
				userIdSearch = "%" + userIdSearch + "%";
				queryWherePortion.append(" and lower(user_.userid) like ? ");
			}

			if (form.isSetVar()) {
				if (!addingUserProxyWhere) {
					querySelectPortion.append(", user_proxy");
					queryWherePortion.append(" and user_proxy.user_Id = user_.userId");
					addingUserProxyWhere = true;
				}

				int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");
				for (int i=1; i<=numberGenericVariables; i++) {
					if (UtilMethods.isSet(form.getVar(i)))
						queryWherePortion.append(" and lower(user_proxy.var"+i+") = '" + form.getVar(i).trim().toLowerCase() + "'");
				}
			}

			String[] categoriesList = form.getCategories();
			if((categoriesList != null) && (categoriesList.length > 0))
			{
				String categories="";
				int counter = 0;
				for(String cat : categoriesList){
					if(counter == 0){
						categories = categories + "tree.child = " + cat ;
					}else{
						categories = categories + " or tree.child = " + cat ;
					}
					counter+=1;
				}

				if (!addingUserProxyWhere) {
					querySelectPortion.append(", user_proxy, tree");
					queryWherePortion.append(" and user_.userid = user_proxy.user_id ");
					addingUserProxyWhere = true;
				}
				else {
					querySelectPortion.append(", tree");
				}
				queryWherePortion.append(" and tree.parent = user_proxy.inode and tree.parent in (select parent from tree where ("+categories+")" +
						" group by parent having count(parent) > "+(categoriesList.length - 1)+" ) ");
				if(!isCount){
					queryWherePortion.append(" group by user_proxy.user_id, user_.userid, user_.createdate ");
				}
			}

			String query = querySelectPortion.toString() + queryWherePortion.toString();
			if(!isCount)
			{
				query = query + " order by user_.createdate desc ";
			}
			Logger.debug(UserManagerListBuilderFactory.class, "query:" + query);
			dc.setSQL(query);
			if(UtilMethods.isSet(firstName))
			{
				dc.addParam(firstName);
			}
			if(UtilMethods.isSet(middleName))
			{
				dc.addParam(middleName);
			}
			if(UtilMethods.isSet(lastName))
			{
				dc.addParam(lastName);
			}
			if(UtilMethods.isSet(emailAddress))
			{
				dc.addParam(emailAddress);
			}
			if (UtilMethods.isSet(dateOfBirthTypeSearch)) {
				if (dateOfBirthTypeSearch.equalsIgnoreCase("DateRange")) {
					if (UtilMethods.isSet(dateOfBirthFromDate)) {
						dc.addParam(dateOfBirthFromDate);
					}
					if (UtilMethods.isSet(dateOfBirthToDate))	{
						dc.addParam(dateOfBirthToDate);
					}
				}
			}
			if (UtilMethods.isSet(lastLoginTypeSearch)) {
				if (lastLoginTypeSearch.equalsIgnoreCase("DateRange")) {
					if (UtilMethods.isSet(lastLoginFromDate)) {
						dc.addParam(lastLoginFromDate);
					}
					if (UtilMethods.isSet(lastLoginToDate))	{
						dc.addParam(lastLoginToDate);
					}
				}
				if (lastLoginTypeSearch.equalsIgnoreCase("Since")) {
					if (UtilMethods.isSet(lastLoginSince)){
						GregorianCalendar cal = new GregorianCalendar();
						cal.setTime(new java.util.Date());
						cal.add(GregorianCalendar.DATE, -new Integer(lastLoginSince).intValue());
						dc.addParam(cal.getTime());
					}
				}
			}
			if (UtilMethods.isSet(createdTypeSearch)) {
				if (createdTypeSearch.equalsIgnoreCase("DateRange")) {
					if (UtilMethods.isSet(createdDateFromDate)) {
						dc.addParam(createdDateFromDate);
					}
					if (UtilMethods.isSet(createdDateToDate))	{
						dc.addParam(createdDateToDate);
					}
				}
				if (createdTypeSearch.equalsIgnoreCase("Since")) {
					if (UtilMethods.isSet(createdSince)){
						GregorianCalendar cal = new GregorianCalendar();
						cal.setTime(new java.util.Date());
						cal.add(GregorianCalendar.DATE, -new Integer(createdSince).intValue());
						dc.addParam(cal.getTime());
					}
				}
			}
			if (UtilMethods.isSet(lastVisitTypeSearch)) {
				if (lastVisitTypeSearch.equalsIgnoreCase("DateRange")) {
					if (UtilMethods.isSet(lastVisitDateFromDate)) {
						dc.addParam(lastVisitDateFromDate);
					}
					if (UtilMethods.isSet(lastVisitDateToDate))	{
						dc.addParam(lastVisitDateFromDate);
					}
				}
				if (lastVisitTypeSearch.equalsIgnoreCase("Since")) {
					if (UtilMethods.isSet(lastVisitSince)){
						GregorianCalendar cal = new GregorianCalendar();
						cal.setTime(new java.util.Date());
						cal.add(GregorianCalendar.DATE, -new Integer(lastVisitSince).intValue());
						dc.addParam(cal.getTime());
					}
				}
			}
			if(UtilMethods.isSet(city))
			{
				dc.addParam(city);
			}
			if(UtilMethods.isSet(state))
			{
				dc.addParam(state);
			}
			if(UtilMethods.isSet(country))
			{
				dc.addParam(country);
			}
			if(UtilMethods.isSet(zip))
			{
				dc.addParam(zip);
			}
			if(UtilMethods.isSet(phone))
			{
				dc.addParam(phone);
			}
			if(UtilMethods.isSet(fax))
			{
				dc.addParam(fax);
			}
			if(UtilMethods.isSet(cellPhone))
			{
				dc.addParam(cellPhone);
			}

			if(UtilMethods.isSet(referer)){
				dc.addParam(referer);
				dc.addParam(referer);
			}

			//User Id Search
			if(UtilMethods.isSet(userIdSearch))
			{
				dc.addParam(userIdSearch);
			}


			int startRow = form.getStartRow();
			int maxRow = form.getMaxRow();

			if(form.getMaxRow() > 0)
			{
				dc.setStartRow(startRow);
				dc.setMaxRows(maxRow);
			}
		}
		if(runQuery)
			try {
				results = dc.getResults();
			} catch (DotDataException e) {
			Logger.error(UserManagerListBuilderFactory.class, e.getMessage(), e);
			}
		return results;
	}

	public static boolean isUserManagerAdmin (User user) throws PortalException, SystemException {
		List<Role> roles;
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		} catch (DotDataException e) {
			Logger.error(UserManagerListBuilderFactory.class,e.getMessage(),e);
			throw new SystemException(e);
		}
		Iterator<Role> rolesIt = roles.iterator();
		boolean isUserManagerAdmin = false;
		while (rolesIt.hasNext()) {
			Role role = (Role) rolesIt.next();
			if (role.getName().equals(Config.getStringProperty("USER_MANAGER_ADMIN_ROLE"))) {
				isUserManagerAdmin = true;
				break;
			}
		}
		return isUserManagerAdmin;
	}

	/**
	 * Check the request to know if the form have the fullCommmand value set is set or not
	 * @param req the request to be checked
	 * @return the value of the fullCommand parameter
	 */

	public static boolean isFullCommand(ActionRequest req)
	{
		boolean fullCommand = false;
		try
		{
			String fullCommandString = req.getParameter("fullCommand");
			fullCommand = Boolean.parseBoolean(fullCommandString);
		}
		catch(Exception ex){}
		return fullCommand;
	}

	/**
	 * return a String with the userIds that are retieved from a UserManager's  SearchForm, also save that String to the session in this
	 * variable "usersFullCommand"
	 * @param req the request where the UserManagerListSearchForm object is store
	 * @return String with the userIds, separated by ","
	 */

	public static String loadFullCommand(ActionRequest req)
	{
		String userIdFullCommand = "";
		if(isFullCommand(req))
		{
			HttpSession session = ((ActionRequestImpl) req).getHttpServletRequest().getSession();

			//Get all the user of the filter
			UserManagerListSearchForm searchFormFullCommand = (UserManagerListSearchForm) session.getAttribute(WebKeys.USERMANAGERLISTPARAMETERS);
			searchFormFullCommand.setStartRow(0);
			searchFormFullCommand.setMaxRow(0);
			List matches = UserManagerListBuilderFactory.doSearch(searchFormFullCommand);

			//Create the String buffer
			StringBuffer userFullCommandSB = new StringBuffer();

			//Get the Iterator and the userIds
			Iterator it = matches.iterator();
			for (int i = 0; it.hasNext(); i++)
			{
				String userId = (String) ((Map) it.next()).get("userid");
				userFullCommandSB.append(userId + ",");
			}
			userIdFullCommand = userFullCommandSB.toString();
			if(userIdFullCommand.indexOf(",") != -1)
			{
				userIdFullCommand = userIdFullCommand.substring(0,userIdFullCommand.lastIndexOf(","));
			}
			session.setAttribute("usersFullCommand",userIdFullCommand);
		}
		return userIdFullCommand;
	}

	/**
	 * Get a String with the userIds of the users to retrieve, separated by a "," and return an arraylist of the userproxies
	 * that represent those userIds
	 * @param userIdList userIds to be retrieved
	 * @return a list of userProxy that represent those user
	 */

	public static List<UserProxy> getUserProxiesFromList(String userIdList)
	{
		ArrayList<UserProxy> userProxyList = new ArrayList<UserProxy>();
		String[] userIdArray = userIdList.split(",");
		for(String userId : userIdArray)
		{
			UserProxy userProxy;
			try {
				userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userId,APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.error(UserManagerListBuilderFactory.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			if(InodeUtils.isSet(userProxy.getInode()))
			{
				userProxyList.add(userProxy);
			}
		}
		return userProxyList;
	}
}

package com.dotmarketing.portlets.usermanager.struts;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.util.Constants;

/**
 * 
 * @author Oswaldo Gallango
 *
 */
public class UserManagerListSearchForm extends ValidatorForm {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String userIdSearch;
	private String firstName;
	private String middleName;
	private String lastName;
	private String nickName;
	private String dateOfBirth;
	private Date dateOfBirthDate;
	private String sex;
	private String emailAddress;
	private String company;
	private String city;
	private String state;
	private String country;
	private String zip;
	private String phone;
	private String fax;
	private String cellPhone;

	private String dateOfBirthSince;
	private Date dateOfBirthSinceDate;
	private String dateOfBirthFrom;
	private String dateOfBirthTo;
	private Date dateOfBirthFromDate;
	private Date dateOfBirthToDate;
	private String dateOfBirthTypeSearch = "Since";

	private String lastLoginSince;
	private String lastLoginDateFrom;
	private String lastLoginDateTo;
	private Date lastLoginDateFromDate;
	private Date lastLoginDateToDate;
	private String lastLoginTypeSearch = "DateRange";

	private String createdSince;
	private String createdDateFrom;
	private String createdDateTo;
	private Date createdDateFromDate;
	private Date createdDateToDate;
	private String createdTypeSearch = "DateRange";

	private String lastVisitSince;
	private String lastVisitDateFrom;
	private String lastVisitDateTo;
	private Date lastVisitDateFromDate;
	private Date lastVisitDateToDate;
	private String lastVisitTypeSearch = "DateRange";

	private String usermanagerListTitle;
	private boolean allowPublicToSubscribe;
	private String usermanagerListInode;
	private String[] categories;
	private int startRow = 0;
	private int maxRow = 0;

	private String[] arrayUserIds;
	
	private String active;

    private String var1;
    private String var2;
    private String var3;
    private String var4;
    private String var5;
    private String var6;
    private String var7;
    private String var8;
    private String var9;
    private String var10;
    private String var11;
    private String var12;
    private String var13;
    private String var14;
    private String var15;
    private String var16;
    private String var17;
    private String var18;
    private String var19;
    private String var20;
    private String var21;
    private String var22;
    private String var23;
    private String var24;
    private String var25;

    private String tagName;

    private int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");

    private boolean ignoreHeaders;

    private String[] usersToDelete;

	private String userFilterTitle;

	private String userFilterListInode;
	
	private String userReferer;
	
	private boolean updateDuplicatedUsers;

	public String getUserReferer() {
		return userReferer;
	}
	public void setuserReferer(String userReferer) {
		this.userReferer = userReferer;
	}
	/**
	 * @return the usersToDelete
	 */
	public String[] getUsersToDelete() {
		return usersToDelete;
	}
	/**
	 * @param usersToDelete the usersToDelete to set
	 */
	public void setUsersToDelete(String[] usersToDelete) {
		this.usersToDelete = usersToDelete;
	}
	/**
	 * @return the ignoreHeaders
	 */
	public boolean isIgnoreHeaders() {
		return ignoreHeaders;
	}

	/**
	 * @param ignoreHeaders the ignoreHeaders to set
	 */
	public void setIgnoreHeaders(boolean ignoreHeaders) {
		this.ignoreHeaders = ignoreHeaders;
	}

	/**
	 * 
	 */
	public UserManagerListSearchForm() {
		super();
	}
	
	/**
	 * @return Returns the cellPhone.
	 */
	public String getCellPhone() {
		return cellPhone;
	}
	/**
	 * @param cellPhone The cellPhone to set.
	 */
	public void setCellPhone(String cellPhone) {
		this.cellPhone = cellPhone;
	}
	/**
	 * @return Returns the city.
	 */
	public String getCity() {
		return city;
	}
	/**
	 * @param city The city to set.
	 */
	public void setCity(String city) {
		this.city = city;
	}
	/**
	 * @return Returns the company.
	 */
	public String getCompany() {
		return company;
	}
	/**
	 * @param company The company to set.
	 */
	public void setCompany(String company) {
		this.company = company;
	}
	/**
	 * @return Returns the country.
	 */
	public String getCountry() {
		return country;
	}
	/**
	 * @param country The country to set.
	 */
	public void setCountry(String country) {
		this.country = country;
	}
	/**
	 * @return Returns the dateOfBirthFrom.
	 */
	public String getDateOfBirthFrom() {
		return dateOfBirthFrom;
	}
	/**
	 * @param dateOfBirthFrom The dateOfBirthFrom to set.
	 */
	public void setDateOfBirthFrom(String dateOfBirthFrom) {
		this.dateOfBirthFrom = dateOfBirthFrom;
		if (dateOfBirthFrom != null && !dateOfBirthFrom.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (dateOfBirthFrom, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
			cal.set(GregorianCalendar.MINUTE, 0);
			cal.set(GregorianCalendar.SECOND, 0);
			setDateOfBirthFromDate(cal.getTime());
		}
	}
	/**
	 * @return Returns the dateOfBirthTo.
	 */
	public String getDateOfBirthTo() {
		return dateOfBirthTo;
	}
	/**
	 * @param dateOfBirthTo The dateOfBirthTo to set.
	 */
	public void setDateOfBirthTo(String dateOfBirthTo) {
		this.dateOfBirthTo = dateOfBirthTo;
		if (dateOfBirthTo != null && !dateOfBirthTo.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (dateOfBirthTo, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 23);
			cal.set(GregorianCalendar.MINUTE, 59);
			cal.set(GregorianCalendar.SECOND, 59);
			setDateOfBirthToDate(cal.getTime());
		}
	}
	/**
	 * @return Returns the lastLoginDate.
	 */
	public String getCreatedDateFrom() {
		return createdDateFrom;
	}
	/**
	 * @param lastLoginDate The lastLoginDate to set.
	 */
	public void setCreatedDateFrom(String createdDateFrom) {
		this.createdDateFrom = createdDateFrom;
		if (createdDateFrom != null && !createdDateFrom.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (createdDateFrom, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
			cal.set(GregorianCalendar.MINUTE, 0);
			cal.set(GregorianCalendar.SECOND, 0);
			setCreatedDateFromDate(cal.getTime());
		}
	}
	/**
	 * @return Returns the lastLoginDateTo.
	 */
	public String getCreatedDateTo() {
		return createdDateTo;
	}
	/**
	 * @param lastLoginDateTo The lastLoginDateTo to set.
	 */
	public void setCreatedDateTo(String createdDateTo) {
		this.createdDateTo = createdDateTo;
		if (createdDateTo != null && !createdDateTo.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (createdDateTo, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 23);
			cal.set(GregorianCalendar.MINUTE, 59);
			cal.set(GregorianCalendar.SECOND, 59);
			setCreatedDateToDate(cal.getTime());
		}		
	}
	/**
	 * @return Returns the emailAddress.
	 */
	public String getEmailAddress() {
		return emailAddress;
	}
	/**
	 * @param emailAddress The emailAddress to set.
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	/**
	 * @return Returns the fax.
	 */
	public String getFax() {
		return fax;
	}
	/**
	 * @param fax The fax to set.
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}
	
	/**
	 * @return the userIdSearch
	 */
	public String getUserIdSearch() {
		return userIdSearch;
	}
	
	/**
	 * @param userIdSearch the userIdSearch to set
	 */
	public void setUserIdSearch(String userIdSearch) {
		this.userIdSearch = userIdSearch;
	}
	
	/**
	 * @return Returns the firstName.
	 */
	public String getFirstName() {
		return firstName;
	}
	/**
	 * @param firstName The firstName to set.
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @return Returns the lastLoginDate.
	 */
	public String getLastLoginDateFrom() {
		return lastLoginDateFrom;
	}
	/**
	 * @param lastLoginDate The lastLoginDate to set.
	 */
	public void setLastLoginDateFrom(String lastLoginDateFrom) {
		this.lastLoginDateFrom = lastLoginDateFrom;
		if (lastLoginDateFrom != null && !lastLoginDateFrom.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (lastLoginDateFrom, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
			cal.set(GregorianCalendar.MINUTE, 0);
			cal.set(GregorianCalendar.SECOND, 0);
			setLastLoginDateFromDate(cal.getTime());
		}
	}
	/**
	 * @return Returns the lastLoginDateTo.
	 */
	public String getLastLoginDateTo() {
		return lastLoginDateTo;
	}
	/**
	 * @param lastLoginDateTo The lastLoginDateTo to set.
	 */
	public void setLastLoginDateTo(String lastLoginDateTo) {
		this.lastLoginDateTo = lastLoginDateTo;
		if (lastLoginDateTo != null && !lastLoginDateTo.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (lastLoginDateTo, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 23);
			cal.set(GregorianCalendar.MINUTE, 59);
			cal.set(GregorianCalendar.SECOND, 59);
			setLastLoginDateToDate(cal.getTime());
		}		
	}
	/**
	 * @return Returns the lastName.
	 */
	public String getLastName() {
		return lastName;
	}
	/**
	 * @param lastName The lastName to set.
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @return Returns the middleName.
	 */
	public String getMiddleName() {
		return middleName;
	}
	/**
	 * @param middleName The middleName to set.
	 */
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}
	/**
	 * @return Returns the phone.
	 */
	public String getPhone() {
		return phone;
	}
	/**
	 * @param phone The phone to set.
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}
	/**
	 * @return Returns the state.
	 */
	public String getState() {
		return state;
	}
	/**
	 * @param state The state to set.
	 */
	public void setState(String state) {
		this.state = state;
	}
	/**
	 * @return Returns the zip.
	 */
	public String getZipStr() {
		return zip;
	}
	/**
	 * @param zip The zip to set.
	 */
	public void setZipStr(String zip) {
		this.zip = zip;
	}
	/*
	 * @return Returns the dateOfBirthFromDate.
	 */
	public Date getDateOfBirthFromDate() {
		return dateOfBirthFromDate;
	}
	/**
	 * @param dateOfBirthFromDate The dateOfBirthFromDate to set.
	 */
	public void setDateOfBirthFromDate(Date dateOfBirthFromDate) {
		this.dateOfBirthFromDate = dateOfBirthFromDate;
	}
	/**
	 * @return Returns the dateOfBirthToDate.
	 */
	public Date getDateOfBirthToDate() {
		return dateOfBirthToDate;
	}
	/**
	 * @param dateOfBirthToDate The dateOfBirthToDate to set.
	 */
	public void setDateOfBirthToDate(Date dateOfBirthToDate) {
		this.dateOfBirthToDate = dateOfBirthToDate;
	}
	/**
	 * @return Returns the createdDateFromDate.
	 */
	public Date getCreatedDateFromDate() {
		return createdDateFromDate;
	}
	/**
	 * @param createdDateFromDate The createdDateFromDate to set.
	 */
	public void setCreatedDateFromDate(Date createdDateFromDate) {
		this.createdDateFromDate = createdDateFromDate;
	}
	/**
	 * @return Returns the createdDateToDate.
	 */
	public Date getCreatedDateToDate() {
		return createdDateToDate;
	}
	/**
	 * @param createdDateToDate The createdDateToDate to set.
	 */
	public void setCreatedDateToDate(Date createdDateToDate) {
		this.createdDateToDate = createdDateToDate;
	}
	/**
	 * @return Returns the lastLoginDateFromDate.
	 */
	public Date getLastLoginDateFromDate() {
		return lastLoginDateFromDate;
	}
	/**
	 * @param lastLoginDateFromDate The lastLoginDateFromDate to set.
	 */
	public void setLastLoginDateFromDate(Date lastLoginDateFromDate) {
		this.lastLoginDateFromDate = lastLoginDateFromDate;
	}
	/**
	 * @return Returns the lastLoginDateToDate.
	 */
	public Date getLastLoginDateToDate() {
		return lastLoginDateToDate;
	}
	/**
	 * @param lastLoginDateToDate The lastLoginDateToDate to set.
	 */
	public void setLastLoginDateToDate(Date lastLoginDateToDate) {
		this.lastLoginDateToDate = lastLoginDateToDate;
	}
	/**
	 * @return Returns the createdDateFromDate.
	 */
	public Date getLastVisitDateFromDate() {
		return lastVisitDateFromDate;
	}
	/**
	 * @param createdDateFromDate The createdDateFromDate to set.
	 */
	public void setLastVisitDateFromDate(Date lastVisitDateFromDate) {
		this.lastVisitDateFromDate = lastVisitDateFromDate;
	}
	/**
	 * @return Returns the createdDateToDate.
	 */
	public Date getLastVisitDateToDate() {
		return lastVisitDateToDate;
	}
	/**
	 * @param createdDateToDate The createdDateToDate to set.
	 */
	public void setLastVisitDateToDate(Date lastVisitDateToDate) {
		this.lastVisitDateToDate = lastVisitDateToDate;
	}
	/**
	 * @return Returns the allowPublicToSubscribe.
	 */
	public boolean isAllowPublicToSubscribe() {
		return allowPublicToSubscribe;
	}
	/**
	 * @param allowPublicToSubscribe The allowPublicToSubscribe to set.
	 */
	public void setAllowPublicToSubscribe(boolean allowPublicToSubscribe) {
		this.allowPublicToSubscribe = allowPublicToSubscribe;
	}
	
	
	public String getUsermanagerListTitle() {
		return usermanagerListTitle;
	}
	
	public void setUsermanagerListTitle(String usermanagerListTitle) {
		this.usermanagerListTitle = usermanagerListTitle;
	}
	
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		
		ActionErrors errors = new ActionErrors ();
		boolean withNotFilter = false;
		if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.SEARCH)) { 
			if(arrayUserIds!=null && arrayUserIds.length > 0) {
				withNotFilter = false;
			}
			if(UtilMethods.isSet(firstName)){
				withNotFilter = false;
			} else if(UtilMethods.isSet(middleName)){
				withNotFilter = false;
			}else if(UtilMethods.isSet(lastName)){
				withNotFilter = false;
			}else if(UtilMethods.isSet(emailAddress)){
				withNotFilter = false;
			}else if(UtilMethods.isSet(dateOfBirthTypeSearch)){
				if(dateOfBirthTypeSearch.equalsIgnoreCase("DateRange")) {
					if(UtilMethods.isSet(dateOfBirthFrom)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(dateOfBirthTo)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(dateOfBirthFromDate)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(dateOfBirthToDate)){
						withNotFilter = false;
					}
				}
				else if(dateOfBirthTypeSearch.equalsIgnoreCase("Since")) {
					if(UtilMethods.isSet(dateOfBirthSince)){
						withNotFilter = false;
					}
				}
			}else if(UtilMethods.isSet(createdTypeSearch)){
				if(createdTypeSearch.equalsIgnoreCase("DateRange")) {
					if(UtilMethods.isSet(createdDateFrom)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(createdDateTo)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(createdDateFromDate)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(createdDateToDate)){
						withNotFilter = false;
					}
				}
				else if(createdTypeSearch.equalsIgnoreCase("Since")) {
					if(UtilMethods.isSet(createdSince)){
						withNotFilter = false;
					}
				}
			}else if(UtilMethods.isSet(lastLoginTypeSearch)){
				if(lastLoginTypeSearch.equalsIgnoreCase("DateRange")) {
					if(UtilMethods.isSet(lastLoginDateFrom)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(lastLoginDateTo)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(lastLoginDateFromDate)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(lastLoginDateToDate)){
						withNotFilter = false;
					}
				}
				else if(lastLoginTypeSearch.equalsIgnoreCase("Since")) {
					if(UtilMethods.isSet(lastLoginSince)){
						withNotFilter = false;
					}
				}
			}else if(UtilMethods.isSet(lastVisitTypeSearch)){
				if(lastVisitTypeSearch.equalsIgnoreCase("DateRange")) {
					if(UtilMethods.isSet(lastVisitDateFrom)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(lastVisitDateTo)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(lastVisitDateFromDate)){
						withNotFilter = false;
					}else if(UtilMethods.isSet(lastVisitDateToDate)){
						withNotFilter = false;
					}
				}
				else if(lastVisitTypeSearch.equalsIgnoreCase("Since")) {
					if(UtilMethods.isSet(lastVisitSince)){
						withNotFilter = false;
					}
				}
			}else if(UtilMethods.isSet(company)){
				withNotFilter = false;
			}else if(UtilMethods.isSet(city)){
				withNotFilter = false;
			}else if(UtilMethods.isSet(state)){
				withNotFilter = false;
			}else if(UtilMethods.isSet(country)){
				withNotFilter = false;
			}else if(UtilMethods.isSet(zip)){
				withNotFilter = false;
			}else if(UtilMethods.isSet(phone)){
				withNotFilter = false;
			}else if(UtilMethods.isSet(fax)){
				withNotFilter = false;
			}else if(UtilMethods.isSet(cellPhone)){
				withNotFilter = false;
			}else if(categories != null && categories.length > 0){
				withNotFilter = false;
			}else if(UtilMethods.isSet(active)) {
				withNotFilter = false;
			}else if(UtilMethods.isSet(tagName)){
				withNotFilter = false;
			}else {
				for (int i=1; i<=numberGenericVariables; i++) {
					if (UtilMethods.isSet(getVar(i))) {
						withNotFilter = false;
					}
				}
			}
			
			if(withNotFilter){
				ActionMessage error = new ActionMessage ("prompt.filterRequired");
				errors.add("filter", error);
				return errors;
			}else{
				return super.validate(mapping, request);
			}
		}
		return null;
		
	}
	
	public String getUsermanagerListInode() {
		return usermanagerListInode;
	}
	
	public void setUsermanagerListInode(String usermanagerListInode) {
		this.usermanagerListInode = usermanagerListInode;
	}
	
	public String[] getCategories() {
		return categories;
	}
	
	public String getCategoriesStr() {
		StringBuffer retVal = new StringBuffer();
		try {
			for(int i = 0; i < this.categories.length;++i){
				retVal.append(categories[i]+",");
			}
		}
		catch (Exception e) {}
		return retVal.toString();
	}
	
	public void setCategories(String[] categories) {
		this.categories = categories;
	}
	
	public boolean isCategorySelected(String category){
		if(this.categories != null){
			for(int i = 0; i < this.categories.length;++i){
				if(this.categories[i].equals(category))
					return true;
			}		
		}
		return false;
	}

	/**
	 * 
	 * @return maxRow
	 */
	public int getMaxRow() {
		return maxRow;
	}

	/**
	 * Set the number of row to get
	 * @param maxRow
	 */
	public void setMaxRow(int maxRow) {
		this.maxRow = maxRow;
	}

	/**
	 * 
	 * @return startRow
	 */
	public int getStartRow() {
		return startRow;
	}

	/**
	 * Set the first row to get
	 * @param startRow
	 */
	public void setStartRow(int startRow) {
		this.startRow = startRow;
	}

	/**
	 * @return Returns the active.
	 */
	public String getActive() {
		return active;
	}

	/**
	 * @param active The active to set.
	 */
	public void setActive(String active) {
		this.active = active;
	}

	/**
	 * @return Returns the arrayUserIds.
	 */
	public String[] getArrayUserIds() {
		return arrayUserIds;
	}

	/**
	 * @param arrayUserIds The arrayUserIds to set.
	 */
	public void setArrayUserIds(String[] arrayUserIds) {
		this.arrayUserIds = arrayUserIds;
	}

	/**
	 * @return Returns the var1.
	 */
	public String getVar1() {
		return var1;
	}

	/**
	 * @param var1 The var1 to set.
	 */
	public void setVar1(String var1) {
		this.var1 = var1;
	}

	/**
	 * @return Returns the var10.
	 */
	public String getVar10() {
		return var10;
	}

	/**
	 * @param var10 The var10 to set.
	 */
	public void setVar10(String var10) {
		this.var10 = var10;
	}

	/**
	 * @return Returns the var11.
	 */
	public String getVar11() {
		return var11;
	}

	/**
	 * @param var11 The var11 to set.
	 */
	public void setVar11(String var11) {
		this.var11 = var11;
	}

	/**
	 * @return Returns the var12.
	 */
	public String getVar12() {
		return var12;
	}

	/**
	 * @param var12 The var12 to set.
	 */
	public void setVar12(String var12) {
		this.var12 = var12;
	}

	/**
	 * @return Returns the var13.
	 */
	public String getVar13() {
		return var13;
	}

	/**
	 * @param var13 The var13 to set.
	 */
	public void setVar13(String var13) {
		this.var13 = var13;
	}

	/**
	 * @return Returns the var14.
	 */
	public String getVar14() {
		return var14;
	}

	/**
	 * @param var14 The var14 to set.
	 */
	public void setVar14(String var14) {
		this.var14 = var14;
	}

	/**
	 * @return Returns the var15.
	 */
	public String getVar15() {
		return var15;
	}

	/**
	 * @param var15 The var15 to set.
	 */
	public void setVar15(String var15) {
		this.var15 = var15;
	}

	/**
	 * @return Returns the var16.
	 */
	public String getVar16() {
		return var16;
	}

	/**
	 * @param var16 The var16 to set.
	 */
	public void setVar16(String var16) {
		this.var16 = var16;
	}

	/**
	 * @return Returns the var17.
	 */
	public String getVar17() {
		return var17;
	}

	/**
	 * @param var17 The var17 to set.
	 */
	public void setVar17(String var17) {
		this.var17 = var17;
	}

	/**
	 * @return Returns the var18.
	 */
	public String getVar18() {
		return var18;
	}

	/**
	 * @param var18 The var18 to set.
	 */
	public void setVar18(String var18) {
		this.var18 = var18;
	}

	/**
	 * @return Returns the var19.
	 */
	public String getVar19() {
		return var19;
	}

	/**
	 * @param var19 The var19 to set.
	 */
	public void setVar19(String var19) {
		this.var19 = var19;
	}

	/**
	 * @return Returns the var2.
	 */
	public String getVar2() {
		return var2;
	}

	/**
	 * @param var2 The var2 to set.
	 */
	public void setVar2(String var2) {
		this.var2 = var2;
	}

	/**
	 * @return Returns the var20.
	 */
	public String getVar20() {
		return var20;
	}

	/**
	 * @param var20 The var20 to set.
	 */
	public void setVar20(String var20) {
		this.var20 = var20;
	}

	/**
	 * @return Returns the var21.
	 */
	public String getVar21() {
		return var21;
	}

	/**
	 * @param var21 The var21 to set.
	 */
	public void setVar21(String var21) {
		this.var21 = var21;
	}

	/**
	 * @return Returns the var22.
	 */
	public String getVar22() {
		return var22;
	}

	/**
	 * @param var22 The var22 to set.
	 */
	public void setVar22(String var22) {
		this.var22 = var22;
	}

	/**
	 * @return Returns the var23.
	 */
	public String getVar23() {
		return var23;
	}

	/**
	 * @param var23 The var23 to set.
	 */
	public void setVar23(String var23) {
		this.var23 = var23;
	}

	/**
	 * @return Returns the var24.
	 */
	public String getVar24() {
		return var24;
	}

	/**
	 * @param var24 The var24 to set.
	 */
	public void setVar24(String var24) {
		this.var24 = var24;
	}

	/**
	 * @return Returns the var25.
	 */
	public String getVar25() {
		return var25;
	}

	/**
	 * @param var25 The var25 to set.
	 */
	public void setVar25(String var25) {
		this.var25 = var25;
	}

	/**
	 * @return Returns the var3.
	 */
	public String getVar3() {
		return var3;
	}

	/**
	 * @param var3 The var3 to set.
	 */
	public void setVar3(String var3) {
		this.var3 = var3;
	}

	/**
	 * @return Returns the var4.
	 */
	public String getVar4() {
		return var4;
	}

	/**
	 * @param var4 The var4 to set.
	 */
	public void setVar4(String var4) {
		this.var4 = var4;
	}

	/**
	 * @return Returns the var5.
	 */
	public String getVar5() {
		return var5;
	}

	/**
	 * @param var5 The var5 to set.
	 */
	public void setVar5(String var5) {
		this.var5 = var5;
	}

	/**
	 * @return Returns the var6.
	 */
	public String getVar6() {
		return var6;
	}

	/**
	 * @param var6 The var6 to set.
	 */
	public void setVar6(String var6) {
		this.var6 = var6;
	}

	/**
	 * @return Returns the var7.
	 */
	public String getVar7() {
		return var7;
	}

	/**
	 * @param var7 The var7 to set.
	 */
	public void setVar7(String var7) {
		this.var7 = var7;
	}

	/**
	 * @return Returns the var8.
	 */
	public String getVar8() {
		return var8;
	}

	/**
	 * @param var8 The var8 to set.
	 */
	public void setVar8(String var8) {
		this.var8 = var8;
	}

	/**
	 * @return Returns the var9.
	 */
	public String getVar9() {
		return var9;
	}

	/**
	 * @param var9 The var9 to set.
	 */
	public void setVar9(String var9) {
		this.var9 = var9;
	}
	
	public String getVar(int var) {
		switch (var) {
			case 1:
				return getVar1();
			case 2:
				return getVar2();
			case 3:
				return getVar3();
			case 4:
				return getVar4();
			case 5:
				return getVar5();
			case 6:
				return getVar6();
			case 7:
				return getVar7();
			case 8:
				return getVar8();
			case 9:
				return getVar9();
			case 10:
				return getVar10();
			case 11:
				return getVar11();
			case 12:
				return getVar12();
			case 13:
				return getVar13();
			case 14:
				return getVar14();
			case 15:
				return getVar15();
			case 16:
				return getVar16();
			case 17:
				return getVar17();
			case 18:
				return getVar18();
			case 19:
				return getVar19();
			case 20:
				return getVar20();
			case 21:
				return getVar21();
			case 22:
				return getVar22();
			case 23:
				return getVar23();
			case 24:
				return getVar24();
			case 25:
				return getVar25();
			default:
				return "";
		}
	}
	public void setVar(int var, String value) {
		switch (var) {
			case 1:
				setVar1(value);
				break;
			case 2:
				setVar2(value);
				break;
			case 3:
				setVar3(value);
				break;
			case 4:
				setVar4(value);
				break;
			case 5:
				setVar5(value);
				break;
			case 6:
				setVar6(value);
				break;
			case 7:
				setVar7(value);
				break;
			case 8:
				setVar8(value);
				break;
			case 9:
				setVar9(value);
				break;
			case 10:
				setVar10(value);
				break;
			case 11:
				setVar11(value);
				break;
			case 12:
				setVar12(value);
				break;
			case 13:
				setVar13(value);
				break;
			case 14:
				setVar14(value);
				break;
			case 15:
				setVar15(value);
				break;
			case 16:
				setVar16(value);
				break;
			case 17:
				setVar17(value);
				break;
			case 18:
				setVar18(value);
				break;
			case 19:
				setVar19(value);
				break;
			case 20:
				setVar20(value);
				break;
			case 21:
				setVar21(value);
				break;
			case 22:
				setVar22(value);
				break;
			case 23:
				setVar23(value);
				break;
			case 24:
				setVar24(value);
				break;
			case 25:
				setVar25(value);
				break;
		}
	}

	/**
	 * @return the dateOfBirth
	 */
	public String getDateOfBirth() {
		return dateOfBirth;
	}

	/**
	 * @param dateOfBirth the dateOfBirth to set
	 */
	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
		if (dateOfBirth != null && !dateOfBirth.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (dateOfBirth, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
			cal.set(GregorianCalendar.MINUTE, 0);
			cal.set(GregorianCalendar.SECOND, 0);
			setDateOfBirthDate(cal.getTime());
		}
	}

	/**
	 * @return the dateOfBirthDate
	 */
	public Date getDateOfBirthDate() {
		return dateOfBirthDate;
	}

	/**
	 * @param dateOfBirthDate the dateOfBirthDate to set
	 */
	public void setDateOfBirthDate(Date dateOfBirthDate) {
		this.dateOfBirthDate = dateOfBirthDate;
	}

	/**
	 * @return the nickName
	 */
	public String getNickName() {
		return nickName;
	}

	/**
	 * @param nickName the nickName to set
	 */
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	/**
	 * @return the sex
	 */
	public String getSex() {
		return sex;
	}

	/**
	 * @param sex the sex to set
	 */
	public void setSex(String sex) {
		this.sex = sex;
	}
	
	public boolean isSetVar() {
		for (int i=1; i <= numberGenericVariables; i++) {
			if (UtilMethods.isSet(getVar(i)))
				return true;
		}
		return false;
	}

	/**
	 * @return the tagName
	 */
	public String getTagName() {
		return tagName;
	}
	/**
	 * @param tagName the tagName to set
	 */
	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	/**
	 * @return the userFilterTitle
	 */
	public String getUserFilterTitle() {
		return userFilterTitle;
	}
	/**
	 * @param userFilterTitle the userFilterTitle to set
	 */
	public void setUserFilterTitle(String userFilterTitle) {
		this.userFilterTitle = userFilterTitle;
	}
	/**
	 * @return the userFilterListInode
	 */
	public String getUserFilterListInode() {
		return userFilterListInode;
	}
	/**
	 * @param userFilterListInode the userFilterListInode to set
	 */
	public void setUserFilterListInode(String userFilterListInode) {
		this.userFilterListInode = userFilterListInode;
	}
	/**
	 * @return the createdSince
	 */
	public String getCreatedSince() {
		return createdSince;
	}
	/**
	 * @param createdSince the createdSince to set
	 */
	public void setCreatedSince(String createdSince) {
		this.createdSince = createdSince;
	}
	/**
	 * @return the lastLoginSince
	 */
	public String getLastLoginSince() {
		return lastLoginSince;
	}
	/**
	 * @param lastLoginSince the lastLoginSince to set
	 */
	public void setLastLoginSince(String lastLoginSince) {
		this.lastLoginSince = lastLoginSince;
	}
	/**
	 * @return the lastVisitDateFrom
	 */
	public String getLastVisitDateFrom() {
		return lastVisitDateFrom;
	}
	/**
	 * @param lastVisitDateFrom the lastVisitDateFrom to set
	 */
	public void setLastVisitDateFrom(String lastVisitDateFrom) {
		this.lastVisitDateFrom = lastVisitDateFrom;
		if (lastVisitDateFrom != null && !lastVisitDateFrom.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (lastVisitDateFrom, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
			cal.set(GregorianCalendar.MINUTE, 0);
			cal.set(GregorianCalendar.SECOND, 0);
			setLastVisitDateFromDate(cal.getTime());
		}
	}
	/**
	 * @return the lastVisitDateTo
	 */
	public String getLastVisitDateTo() {
		return lastVisitDateTo;
	}
	/**
	 * @param lastVisitDateTo the lastVisitDateTo to set
	 */
	public void setLastVisitDateTo(String lastVisitDateTo) {
		this.lastVisitDateTo = lastVisitDateTo;
		if (lastVisitDateTo != null && !lastVisitDateTo.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (lastVisitDateTo, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
			cal.set(GregorianCalendar.MINUTE, 0);
			cal.set(GregorianCalendar.SECOND, 0);
			setLastVisitDateToDate(cal.getTime());
		}
	}
	/**
	 * @return the lastVisitSince
	 */
	public String getLastVisitSince() {
		return lastVisitSince;
	}
	/**
	 * @param lastVisitSince the lastVisitSince to set
	 */
	public void setLastVisitSince(String lastVisitSince) {
		this.lastVisitSince = lastVisitSince;
	}
	/**
	 * @return the createdTypeSearch
	 */
	public String getCreatedTypeSearch() {
		return createdTypeSearch;
	}
	/**
	 * @param createdTypeSearch the createdTypeSearch to set
	 */
	public void setCreatedTypeSearch(String createdTypeSearch) {
		this.createdTypeSearch = createdTypeSearch;
	}
	/**
	 * @return the lastLoginTypeSearch
	 */
	public String getLastLoginTypeSearch() {
		return lastLoginTypeSearch;
	}
	/**
	 * @param lastLoginTypeSearch the lastLoginTypeSearch to set
	 */
	public void setLastLoginTypeSearch(String lastLoginTypeSearch) {
		this.lastLoginTypeSearch = lastLoginTypeSearch;
	}
	/**
	 * @return the lastVisitTypeSearch
	 */
	public String getLastVisitTypeSearch() {
		return lastVisitTypeSearch;
	}
	/**
	 * @param lastVisitTypeSearch the lastVisitTypeSearch to set
	 */
	public void setLastVisitTypeSearch(String lastVisitTypeSearch) {
		this.lastVisitTypeSearch = lastVisitTypeSearch;
	}
	/**
	 * @return the dateOfBirthSince
	 */
	public String getDateOfBirthSince() {
		return dateOfBirthSince;
	}
	/**
	 * @param dateOfBirthSince the dateOfBirthSince to set
	 */
	public void setDateOfBirthSince(String dateOfBirthSince) {
		this.dateOfBirthSince = dateOfBirthSince;
		if (dateOfBirthSince != null && !dateOfBirthSince.trim().equals("")) {
			StringTokenizer tok = new StringTokenizer (dateOfBirthSince, "/");
			String month = "1";
			if (tok.hasMoreTokens())
				month = tok.nextToken();
			String day = "1";
			if (tok.hasMoreTokens())
				day = tok.nextToken();
			String year = "1900"; 
			if (tok.hasMoreTokens())
				year = tok.nextToken();
			GregorianCalendar cal = new GregorianCalendar ();
			cal.set(GregorianCalendar.DATE, Integer.parseInt(day));
			cal.set(GregorianCalendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(GregorianCalendar.YEAR, Integer.parseInt(year));
			cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
			cal.set(GregorianCalendar.MINUTE, 0);
			cal.set(GregorianCalendar.SECOND, 0);
			setDateOfBirthSinceDate(cal.getTime());
		}
	}
	/**
	 * @return the dateOfBirthTypeSearch
	 */
	public String getDateOfBirthTypeSearch() {
		return dateOfBirthTypeSearch;
	}
	/**
	 * @param dateOfBirthTypeSearch the dateOfBirthTypeSearch to set
	 */
	public void setDateOfBirthTypeSearch(String dateOfBirthTypeSearch) {
		this.dateOfBirthTypeSearch = dateOfBirthTypeSearch;
	}
	/**
	 * @return the dateOfBirthSinceDate
	 */
	public Date getDateOfBirthSinceDate() {
		return dateOfBirthSinceDate;
	}
	/**
	 * @param dateOfBirthSinceDate the dateOfBirthSinceDate to set
	 */
	public void setDateOfBirthSinceDate(Date dateOfBirthSinceDate) {
		this.dateOfBirthSinceDate = dateOfBirthSinceDate;
	}
	/**
	 * @return the updateDuplicatedUsers
	 */
	public boolean isUpdateDuplicatedUsers() {
		return updateDuplicatedUsers;
	}
	/**
	 * @param updateDuplicatedUsers the updateDuplicatedUsers to set
	 */
	public void setUpdateDuplicatedUsers(boolean updateDuplicatedUsers) {
		this.updateDuplicatedUsers = updateDuplicatedUsers;
	}
}

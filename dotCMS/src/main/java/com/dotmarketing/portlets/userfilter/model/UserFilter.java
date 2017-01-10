package com.dotmarketing.portlets.userfilter.model;

import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.util.Config;

public class UserFilter extends Inode implements Serializable{

	private static final long serialVersionUID = 1L;

	private String userFilterTitle; //persistent

	private String firstName; //persistent
	private String middleName; //persistent
	private String lastName; //persistent
	private String sex; //persistent
	private String emailAddress; //persistent

	private String dateOfBirthTypeSearch = "Since"; //persistent
	private String dateOfBirthSince;
	private Date dateOfBirthSinceDate; //persistent
	private String dateOfBirthFrom;
	private Date dateOfBirthFromDate; //persistent
	private String dateOfBirthTo;
	private Date dateOfBirthToDate; //persistent

	private String lastLoginTypeSearch = "DateRange"; //persistent
	private String lastLoginSince; //persistent
	private String lastLoginDateFrom;
	private Date lastLoginDateFromDate; //persistent
	private String lastLoginDateTo;
	private Date lastLoginDateToDate; //persistent

	private String createdTypeSearch = "DateRange"; //persistent
	private String createdSince; //persistent
	private String createdDateFrom;
	private Date createdDateFromDate; //persistent
	private String createdDateTo;
	private Date createdDateToDate; //persistent

	private String lastVisitTypeSearch = "DateRange"; //persistent
	private String lastVisitSince; //persistent
	private String lastVisitDateFrom;
	private Date lastVisitDateFromDate; //persistent
	private String lastVisitDateTo;
	private Date lastVisitDateToDate; //persistent

	private String company; //persistent
	private String city; //persistent
	private String state; //persistent
	private String country; //persistent
	private String zip; //persistent
	private String phone; //persistent
	private String fax; //persistent
	private String cellPhone; //persistent
	
	private String categories; //persistent
	
	private String active; //persistent

    private String var1; //persistent
    private String var2; //persistent
    private String var3; //persistent
    private String var4; //persistent
    private String var5; //persistent
    private String var6; //persistent
    private String var7; //persistent
    private String var8; //persistent
    private String var9; //persistent
    private String var10; //persistent
    private String var11; //persistent
    private String var12; //persistent
    private String var13; //persistent
    private String var14; //persistent
    private String var15; //persistent
    private String var16; //persistent
    private String var17; //persistent
    private String var18; //persistent
    private String var19; //persistent
    private String var20; //persistent
    private String var21; //persistent
    private String var22; //persistent
    private String var23; //persistent
    private String var24; //persistent
    private String var25; //persistent

    private String tagName; //persistent

    private int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");

    
	public UserFilter() {
		super.setType("user_filter");
	}

	/**
	 * @return the active
	 */
	public String getActive() {
		return active;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(String active) {
		this.active = active;
	}

	/**
	 * @return the categories
	 */
	public String getCategories() {
		return categories;
	}
	
	public String[] getCategoriesArray() {
		if (categories == null || categories.trim().equalsIgnoreCase("")) {
			return null;
		}
		else {
			StringTokenizer strTok = new StringTokenizer(categories, ",");
			String[] retVal = new String[strTok.countTokens()];
			int i = 0;
			while(strTok.hasMoreTokens()) {
				retVal[i++] = strTok.nextToken();
			}
			return retVal;
		}
	}

	/**
	 * @param categories the categories to set
	 */
	public void setCategories(String categories) {
		this.categories = categories;
	}

	/**
	 * @return the cellPhone
	 */
	public String getCellPhone() {
		return cellPhone;
	}

	/**
	 * @param cellPhone the cellPhone to set
	 */
	public void setCellPhone(String cellPhone) {
		this.cellPhone = cellPhone;
	}

	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}

	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * @return the company
	 */
	public String getCompany() {
		return company;
	}

	/**
	 * @param company the company to set
	 */
	public void setCompany(String company) {
		this.company = company;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the dateOfBirthFrom
	 */
	public String getDateOfBirthFrom() {
		return dateOfBirthFrom;
	}

	/**
	 * @param dateOfBirthFrom the dateOfBirthFrom to set
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
	 * @return the dateOfBirthFromDate
	 */
	public Date getDateOfBirthFromDate() {
		return dateOfBirthFromDate;
	}

	/**
	 * @param dateOfBirthFromDate the dateOfBirthFromDate to set
	 */
	public void setDateOfBirthFromDate(Date dateOfBirthFromDate) {
		this.dateOfBirthFromDate = dateOfBirthFromDate;
	}

	/**
	 * @return the dateOfBirthTo
	 */
	public String getDateOfBirthTo() {
		return dateOfBirthTo;
	}

	/**
	 * @param dateOfBirthTo the dateOfBirthTo to set
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
	 * @return the dateOfBirthToDate
	 */
	public Date getDateOfBirthToDate() {
		return dateOfBirthToDate;
	}

	/**
	 * @param dateOfBirthToDate the dateOfBirthToDate to set
	 */
	public void setDateOfBirthToDate(Date dateOfBirthToDate) {
		this.dateOfBirthToDate = dateOfBirthToDate;
	}

	/**
	 * @return the emailAddress
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/**
	 * @return the fax
	 */
	public String getFax() {
		return fax;
	}

	/**
	 * @param fax the fax to set
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return the lastLoginDateFrom
	 */
	public String getLastLoginDateFrom() {
		return lastLoginDateFrom;
	}

	/**
	 * @param lastLoginDateFrom the lastLoginDateFrom to set
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
	 * @return the lastLoginDateFromDate
	 */
	public Date getLastLoginDateFromDate() {
		return lastLoginDateFromDate;
	}

	/**
	 * @param lastLoginDateFromDate the lastLoginDateFromDate to set
	 */
	public void setLastLoginDateFromDate(Date lastLoginDateFromDate) {
		this.lastLoginDateFromDate = lastLoginDateFromDate;
	}

	/**
	 * @return the lastLoginDateTo
	 */
	public String getLastLoginDateTo() {
		return lastLoginDateTo;
	}

	/**
	 * @param lastLoginDateTo the lastLoginDateTo to set
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
	 * @return the lastLoginDateToDate
	 */
	public Date getLastLoginDateToDate() {
		return lastLoginDateToDate;
	}

	/**
	 * @param lastLoginDateToDate the lastLoginDateToDate to set
	 */
	public void setLastLoginDateToDate(Date lastLoginDateToDate) {
		this.lastLoginDateToDate = lastLoginDateToDate;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @return the middleName
	 */
	public String getMiddleName() {
		return middleName;
	}

	/**
	 * @param middleName the middleName to set
	 */
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	/**
	 * @return the phone
	 */
	public String getPhone() {
		return phone;
	}

	/**
	 * @param phone the phone to set
	 */
	public void setPhone(String phone) {
		this.phone = phone;
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

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
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
	 * @return the var1
	 */
	public String getVar1() {
		return var1;
	}

	/**
	 * @param var1 the var1 to set
	 */
	public void setVar1(String var1) {
		this.var1 = var1;
	}

	/**
	 * @return the var10
	 */
	public String getVar10() {
		return var10;
	}

	/**
	 * @param var10 the var10 to set
	 */
	public void setVar10(String var10) {
		this.var10 = var10;
	}

	/**
	 * @return the var11
	 */
	public String getVar11() {
		return var11;
	}

	/**
	 * @param var11 the var11 to set
	 */
	public void setVar11(String var11) {
		this.var11 = var11;
	}

	/**
	 * @return the var12
	 */
	public String getVar12() {
		return var12;
	}

	/**
	 * @param var12 the var12 to set
	 */
	public void setVar12(String var12) {
		this.var12 = var12;
	}

	/**
	 * @return the var13
	 */
	public String getVar13() {
		return var13;
	}

	/**
	 * @param var13 the var13 to set
	 */
	public void setVar13(String var13) {
		this.var13 = var13;
	}

	/**
	 * @return the var14
	 */
	public String getVar14() {
		return var14;
	}

	/**
	 * @param var14 the var14 to set
	 */
	public void setVar14(String var14) {
		this.var14 = var14;
	}

	/**
	 * @return the var15
	 */
	public String getVar15() {
		return var15;
	}

	/**
	 * @param var15 the var15 to set
	 */
	public void setVar15(String var15) {
		this.var15 = var15;
	}

	/**
	 * @return the var16
	 */
	public String getVar16() {
		return var16;
	}

	/**
	 * @param var16 the var16 to set
	 */
	public void setVar16(String var16) {
		this.var16 = var16;
	}

	/**
	 * @return the var17
	 */
	public String getVar17() {
		return var17;
	}

	/**
	 * @param var17 the var17 to set
	 */
	public void setVar17(String var17) {
		this.var17 = var17;
	}

	/**
	 * @return the var18
	 */
	public String getVar18() {
		return var18;
	}

	/**
	 * @param var18 the var18 to set
	 */
	public void setVar18(String var18) {
		this.var18 = var18;
	}

	/**
	 * @return the var19
	 */
	public String getVar19() {
		return var19;
	}

	/**
	 * @param var19 the var19 to set
	 */
	public void setVar19(String var19) {
		this.var19 = var19;
	}

	/**
	 * @return the var2
	 */
	public String getVar2() {
		return var2;
	}

	/**
	 * @param var2 the var2 to set
	 */
	public void setVar2(String var2) {
		this.var2 = var2;
	}

	/**
	 * @return the var20
	 */
	public String getVar20() {
		return var20;
	}

	/**
	 * @param var20 the var20 to set
	 */
	public void setVar20(String var20) {
		this.var20 = var20;
	}

	/**
	 * @return the var21
	 */
	public String getVar21() {
		return var21;
	}

	/**
	 * @param var21 the var21 to set
	 */
	public void setVar21(String var21) {
		this.var21 = var21;
	}

	/**
	 * @return the var22
	 */
	public String getVar22() {
		return var22;
	}

	/**
	 * @param var22 the var22 to set
	 */
	public void setVar22(String var22) {
		this.var22 = var22;
	}

	/**
	 * @return the var23
	 */
	public String getVar23() {
		return var23;
	}

	/**
	 * @param var23 the var23 to set
	 */
	public void setVar23(String var23) {
		this.var23 = var23;
	}

	/**
	 * @return the var24
	 */
	public String getVar24() {
		return var24;
	}

	/**
	 * @param var24 the var24 to set
	 */
	public void setVar24(String var24) {
		this.var24 = var24;
	}

	/**
	 * @return the var25
	 */
	public String getVar25() {
		return var25;
	}

	/**
	 * @param var25 the var25 to set
	 */
	public void setVar25(String var25) {
		this.var25 = var25;
	}

	/**
	 * @return the var3
	 */
	public String getVar3() {
		return var3;
	}

	/**
	 * @param var3 the var3 to set
	 */
	public void setVar3(String var3) {
		this.var3 = var3;
	}

	/**
	 * @return the var4
	 */
	public String getVar4() {
		return var4;
	}

	/**
	 * @param var4 the var4 to set
	 */
	public void setVar4(String var4) {
		this.var4 = var4;
	}

	/**
	 * @return the var5
	 */
	public String getVar5() {
		return var5;
	}

	/**
	 * @param var5 the var5 to set
	 */
	public void setVar5(String var5) {
		this.var5 = var5;
	}

	/**
	 * @return the var6
	 */
	public String getVar6() {
		return var6;
	}

	/**
	 * @param var6 the var6 to set
	 */
	public void setVar6(String var6) {
		this.var6 = var6;
	}

	/**
	 * @return the var7
	 */
	public String getVar7() {
		return var7;
	}

	/**
	 * @param var7 the var7 to set
	 */
	public void setVar7(String var7) {
		this.var7 = var7;
	}

	/**
	 * @return the var8
	 */
	public String getVar8() {
		return var8;
	}

	/**
	 * @param var8 the var8 to set
	 */
	public void setVar8(String var8) {
		this.var8 = var8;
	}

	/**
	 * @return the var9
	 */
	public String getVar9() {
		return var9;
	}

	/**
	 * @param var9 the var9 to set
	 */
	public void setVar9(String var9) {
		this.var9 = var9;
	}

	/**
	 * @return the zip
	 */
	public String getZip() {
		return zip;
	}

	/**
	 * @param zip the zip to set
	 */
	public void setZip(String zip) {
		this.zip = zip;
	}

	/**
	 * @return the title
	 */
	public String getUserFilterTitle() {
		return userFilterTitle;
	}

	/**
	 * @param title the title to set
	 */
	public void setUserFilterTitle(String userFilterTitle) {
		this.userFilterTitle = userFilterTitle;
	}

	/**
	 * @return the createdDateFrom
	 */
	public String getCreatedDateFrom() {
		return createdDateFrom;
	}

	/**
	 * @param createdDateFrom the createdDateFrom to set
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
	 * @return the createdDateFromDate
	 */
	public Date getCreatedDateFromDate() {
		return createdDateFromDate;
	}

	/**
	 * @param createdDateFromDate the createdDateFromDate to set
	 */
	public void setCreatedDateFromDate(Date createdDateFromDate) {
		this.createdDateFromDate = createdDateFromDate;
	}

	/**
	 * @return the createdDateTo
	 */
	public String getCreatedDateTo() {
		return createdDateTo;
	}

	/**
	 * @param createdDateTo the createdDateTo to set
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
	 * @return the createdDateToDate
	 */
	public Date getCreatedDateToDate() {
		return createdDateToDate;
	}

	/**
	 * @param createdDateToDate the createdDateToDate to set
	 */
	public void setCreatedDateToDate(Date createdDateToDate) {
		this.createdDateToDate = createdDateToDate;
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
	 * @return the lastVisitDateFromDate
	 */
	public Date getLastVisitDateFromDate() {
		return lastVisitDateFromDate;
	}

	/**
	 * @param lastVisitDateFromDate the lastVisitDateFromDate to set
	 */
	public void setLastVisitDateFromDate(Date lastVisitDateFromDate) {
		this.lastVisitDateFromDate = lastVisitDateFromDate;
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
	 * @return the lastVisitDateToDate
	 */
	public Date getLastVisitDateToDate() {
		return lastVisitDateToDate;
	}

	/**
	 * @param lastVisitDateToDate the lastVisitDateToDate to set
	 */
	public void setLastVisitDateToDate(Date lastVisitDateToDate) {
		this.lastVisitDateToDate = lastVisitDateToDate;
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

}

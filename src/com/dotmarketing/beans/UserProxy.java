package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 *
 * @author  maria
 */
public class UserProxy extends Inode implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;


    public UserProxy() {
    	this.setType("user_proxy");
    	//prefix = "";
		//suffix = "";
		//title = "";
		//school = "";
		contactMe = new String[0];
		noclicktracking = false;
		mailSubscription = false;
    }

	private String userId;	
	private String prefix;
	private String suffix;
	private String title;
	private String school;
	private Integer graduationYear;
	private String longLivedCookie;
	private String website;
	private String company;
	private String howHeard;
	private String[] contactMe;
	private String organization;
    private boolean mailSubscription;

	/** nullable persistent field */
    private Integer lastResult = 0;

    /** nullable persistent field */
    private String lastMessage;

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
	
	/*ClickStream*/
	private boolean noclicktracking;
	
    public void setMailSubscription(Boolean b){
        if(b != null){
            mailSubscription = b.booleanValue();
        }
        else{
            mailSubscription = false;
        }
    }

	public void setNoclicktracking(Boolean b){
	    if(b != null){
	        noclicktracking = b.booleanValue();
	    }   else{
	            mailSubscription = false;
	        }
	}
	
	private String challengeQuestionId;
	private String challengeQuestionAnswer;
	
	public boolean isNoclicktracking() {
		return noclicktracking;
	}

    /**
	 * @return Returns the lastMessage.
	 */
	public String getLastMessage() {
		return lastMessage;
	}
	/**
	 * @param lastMessage The lastMessage to set.
	 */
	public void setLastMessage(String lastMessage) {
		if(UtilMethods.isSet(lastMessage)){
			this.lastMessage = lastMessage;
		} else {
			this.lastMessage = null;
		}
	}
	/**
	 * @return Returns the lastResult.
	 */
	public Integer getLastResult() {
		return lastResult;
	}
	/**
	 * @param lastResult The lastResult to set.
	 */
	public void setLastResult(Integer lastResult) {
		this.lastResult = (lastResult == null?this.lastResult = 0:lastResult);
	}
	

	
	
	
	
	
	
	
	/**
     * @return Returns the userId.
     */
    public String getUserId() {
        return userId;
    }
    /**
     * @param userId The userId to set.
     */
    public void setUserId(String userId) {
		if(UtilMethods.isSet(userId)){
			this.userId = userId;
		} else {
			this.userId = null;
		}
	}
    
    public String getPrefix() {
		return prefix;
	}
    public void setPrefix(String prefix) {
		if(UtilMethods.isSet(prefix)){
			this.prefix = prefix;
		} else {
			this.prefix = null;
		}
	}
	public String getSuffix() {
		return suffix;
	}
	public void setSuffix(String suffix) {
		if(UtilMethods.isSet(suffix)){
			this.suffix = suffix;
		} else {
			this.suffix = null;
		}
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		if(UtilMethods.isSet(title)){
			this.title = title;
		} else {
			this.title = null;
		}
	}	
	public String getSchool() {
		return school;
	}
	public void setSchool(String school) {
		if(UtilMethods.isSet(school)){
			this.school = school;
		} else {
			this.school = null;
		}
	}
	public String[] getContactMe() {
		return contactMe;
	}
	public void setContactMe(String contactMe[]) {
		this.contactMe = contactMe;
	}
	public Integer getGraduation_year() {
		return graduationYear;
	}
	public void setGraduation_year(Integer graduation_year) {
		if(graduation_year != null){
			this.graduationYear = graduation_year;
		}else {
			this.graduationYear = null;
		}
	}
	public String getCompany() {
		return company;
	}
	public void setCompany(String company) {
		if(UtilMethods.isSet(company)){
			this.company = company;
		} else {
			this.company = null;
		}
	}
	public String getHowHeard() {
		return howHeard;
	}
	public void setHowHeard(String howHeard) {
		if(UtilMethods.isSet(howHeard)){
			this.howHeard = howHeard;
		} else {
			this.howHeard = null;
		}
	}
	public String getLongLivedCookie() {
		return longLivedCookie;
	}
	public void setLongLivedCookie(String longLivedCookie) {
		if(UtilMethods.isSet(longLivedCookie)){
			this.longLivedCookie = longLivedCookie;
		} else {
			this.longLivedCookie = null;
		}
	}
	public String getWebsite() {
		return website;
	}
	public void setWebsite(String website) {
		if(UtilMethods.isSet(website)){
			this.website = website;
		} else {
			this.website = null;
		}
	}
	public Integer getGraduationYear() {
		return graduationYear;
	}
	public void setGraduationYear(Integer graduationYear) {
		if(graduationYear != null){
			this.graduationYear = graduationYear;
		}else {
			this.graduationYear = null;
		}
	}
    public boolean isMailSubscription() {
        return mailSubscription;
    }

    public String getOrganization() {
        return organization;
    }
    public void setOrganization(String organization) {
		if(UtilMethods.isSet(organization)){
			this.organization = organization;
		} else {
			this.organization = null;
		}
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
		if(UtilMethods.isSet(var1)){
			this.var1 = var1;
		} else {
			this.var1 = null;
		}
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
		if(UtilMethods.isSet(var10)){
			this.var10 = var10;
		} else {
			this.var10 = null;
		}
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
		if(UtilMethods.isSet(var11)){
			this.var11 = var11;
		} else {
			this.var11 = null;
		}
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
		if(UtilMethods.isSet(var12)){
			this.var12 = var12;
		} else {
			this.var12=null;
		}
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
		if(UtilMethods.isSet(var13)){
			this.var13 = var13;
		} else {
			this.var13 = null;
		}
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
		if(UtilMethods.isSet(var14)){
			this.var14 = var14;
		} else {
			this.var14 = null;
		}
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
		if(UtilMethods.isSet(var15)){
			this.var15 = var15;
		} else {
			this.var15 = null;
		}
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
		if(UtilMethods.isSet(var16)){
			this.var16 = var16;
		} else {
			this.var16 = null;
		}
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
		if(UtilMethods.isSet(var17)){
			this.var17 = var17;
		} else {
			this.var17 = null;
		}
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
		if(UtilMethods.isSet(var18)){
			this.var18 = var18;
		} else {
			this.var18 = null;
		}
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
		if(UtilMethods.isSet(var19)){
			this.var19 = var19;
		} else {
			this.var19 = null;
		}
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
		if(UtilMethods.isSet(var2)){
			this.var2 = var2;
		} else {
			this.var2 = null;
		}
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
		if(UtilMethods.isSet(var20)){
			this.var20 = var20;
		} else {
			this.var20 = null;
		}
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
		if(UtilMethods.isSet(var21)){
			this.var21 = var21;
		} else {
			this.var21 = null;
		}
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
		if(UtilMethods.isSet(var22)){
			this.var22 = var22;
		} else {
			this.var22 = null;
		}
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
		if(UtilMethods.isSet(var23)){
			this.var23 = var23;
		} else {
			this.var23 = null;
		}
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
		if(UtilMethods.isSet(var24)){
			this.var24 = var24;
		} else {
			this.var24 = null;
		}
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
		if(UtilMethods.isSet(var25)){
			this.var25 = var25;
		} else {
			this.var25 = null;
		}
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
		if(UtilMethods.isSet(var3)){
			this.var3 = var3;
		} else {
			this.var3 = null;
		}
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
		if(UtilMethods.isSet(var4)){
			this.var4 = var4;
		} else {
			this.var4 = null;
		}
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
		if(UtilMethods.isSet(var5)){
			this.var5 = var5;
		} else {
			this.var5 = null;
		}
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
		if(UtilMethods.isSet(var6)){
			this.var6 = var6;
		} else {
			this.var6 = null;
		}
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
		if(UtilMethods.isSet(var7)){
			this.var7 = var7;
		} else {
			this.var7 = null;
		}
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
		if(UtilMethods.isSet(var8)){
			this.var8 = var8;
		} else {
			this.var8 = null;
		}
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
		if(UtilMethods.isSet(var9)){
			this.var9 = var9;
		} else {
			this.var9 = null;
		}
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
	
	public String getChallengeQuestionAnswer() {
		return challengeQuestionAnswer;
	}
	
	public void setChallengeQuestionAnswer(String challengeQuestionAnswer) {
		this.challengeQuestionAnswer = challengeQuestionAnswer;
	}
	
	public String getChallengeQuestionId() {
		return challengeQuestionId;
	}
	
	public void setChallengeQuestionId(String challengeQuestionId) {
		this.challengeQuestionId = challengeQuestionId;
	}
	
	private String chapterOfficer;
	
	public String getChapterOfficer() {
		return chapterOfficer;
	}
	
	public void setChapterOfficer(String chapterOfficer) {
		this.chapterOfficer = chapterOfficer;
	}
	
	public Map<String, Object> getMap () {
		Map<String, Object> map = UtilMethods.toMap(this);

		//Adding the user properties as well
		User user;
		try {
			user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return map;
		}
		Map<String, Object> usermap = UtilMethods.toMap(user);
		
		map.putAll(usermap);
		
		return map;
	}
	
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}
}
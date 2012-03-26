package com.dotmarketing.cms.createaccount.struts;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.FormSpamFilter;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class CreateAccountForm extends ActionForm implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String userName;
    private String password1;
    private String verifyPassword;
    private String firstName;
    private String lastName;
    private String prefix;
    private String suffix;
    private String title;
    private String school;
    private Integer graduationYear;
    private String organization;
    private String website;
    private String howHeard;
    private String chapterOfficer;
    
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
    
	private String description;
	private String street1;
	private String street2;
	private String city;
	private String state;
	private String zip;
	private String country;
	private String phone;
	private String fax;
	private String cell;
	private String emailAddress;
   
    private String[]  categories;


	public String[] getCategories() {
		return categories;
	}



	public void setCategories(String[]  categories) {
		this.categories = categories;
	}



	public static long getSerialversionuid() {
		return serialVersionUID;
	}



	



	public String getVerifyPassword() {
		return verifyPassword;
	}



	public void setVerifyPassword(String verifyPassword) {
		this.verifyPassword = verifyPassword;
	}



	public String getEmailAddress() {
		return emailAddress;
	}



	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}







	private String comments;
    private boolean mailSubscription;
    private boolean allowEditUser;



	public boolean isAllowEditUser() {
		return allowEditUser;
	}



	public void setAllowEditUser(boolean allowEditUser) {
		this.allowEditUser = allowEditUser;
	}
 
    /** default constructor */
    public CreateAccountForm() 
    {
    }

    

    public String getFirstName() {
        return firstName;
    }



    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }



    public String getLastName() {
        return lastName;
    }



    public void setLastName(String lastName) {
        this.lastName = lastName;
    }



    public boolean isMailSubscription() {
        return mailSubscription;
    }



    public void setMailSubscription(boolean mailSubscription) {
        this.mailSubscription = mailSubscription;
    }



    public String getOrganization() {
        return organization;
    }



    public void setOrganization(String organization) {
        this.organization = organization;
    }



    public String getPassword1() {
        return password1;
    }



    public void setPassword1(String password1) {
        this.password1 = password1;
    }



    



    public String getUserName() {
        return userName;
    }



    public void setUserName(String userName) {
        this.userName = userName;
    }



    public String getWebsite() {
        return website;
    }



    public void setWebsite(String website) {
        this.website = website;
    }



    public String getComments() {
        return comments;
    }



    public void setComments(String comments) {
        this.comments = comments;
    }



    public ActionErrors validate(ActionMapping arg0, HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();       
		if (!UtilMethods.isSet(password1) ){
			errors.add(Globals.MESSAGES_KEY, new ActionMessage("message.contentlet.required","password"));
		}
		if(UtilMethods.isSet(password1) && !password1.equals(verifyPassword)){    		    		    		
			errors.add(Globals.MESSAGES_KEY, new ActionMessage("error.passwordsDontMatch"));    		
		}
    	if (!UtilMethods.isSet(emailAddress)) 
    	{
    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","eMail"));    		
    	}
    	
    	if (UtilMethods.isSet(userName)) 
    	{
    		User user = null;
			try {
				user = APILocator.getUserAPI().loadByUserByEmail(userName, APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception ex) {
				Logger.error(this, ex.getMessage(), ex);
			}
    		if(user != null)
    		{
    			errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.inquiryUserExists"));
    		}    		    		
    	}
    	if (!UtilMethods.isSet(firstName)) 
    	{    		
    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","First Name"));    		
    	}
    	if (!UtilMethods.isSet(lastName)) 
    	{
    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Last Name"));
    	}
     	if(FormSpamFilter.isSpamRequest(request)){
     		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("Potential Spam Message"));
      	}
        
        
    	return errors;
    }



	public String getPrefix() {
		return prefix;
	}



	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}



	public String getSuffix() {
		return suffix;
	}



	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}



	public String getTitle() {
		return title;
	}



	public void setTitle(String title) {
		this.title = title;
	}



	public String getSchool() {
		return school;
	}



	public void setSchool(String school) {
		this.school = school;
	}



	public Integer getGraduationYear() {
		return graduationYear;
	}



	public void setGraduationYear(Integer graduationYear) {
		this.graduationYear = graduationYear;
	}



	public String getHowHeard() {
		return howHeard;
	}



	public void setHowHeard(String howHeard) {
		this.howHeard = howHeard;
	}



	public String getChapterOfficer() {
		return chapterOfficer;
	}



	public void setChapterOfficer(String chapterOfficer) {
		this.chapterOfficer = chapterOfficer;
	}



	public String getVar1() {
		return var1;
	}



	public void setVar1(String var1) {
		this.var1 = var1;
	}



	public String getVar2() {
		return var2;
	}



	public void setVar2(String var2) {
		this.var2 = var2;
	}



	public String getVar3() {
		return var3;
	}



	public void setVar3(String var3) {
		this.var3 = var3;
	}



	public String getVar4() {
		return var4;
	}



	public void setVar4(String var4) {
		this.var4 = var4;
	}



	public String getVar5() {
		return var5;
	}



	public void setVar5(String var5) {
		this.var5 = var5;
	}



	public String getVar6() {
		return var6;
	}



	public void setVar6(String var6) {
		this.var6 = var6;
	}



	public String getVar7() {
		return var7;
	}



	public void setVar7(String var7) {
		this.var7 = var7;
	}



	public String getVar8() {
		return var8;
	}



	public void setVar8(String var8) {
		this.var8 = var8;
	}



	public String getVar9() {
		return var9;
	}



	public void setVar9(String var9) {
		this.var9 = var9;
	}



	public String getVar10() {
		return var10;
	}



	public void setVar10(String var10) {
		this.var10 = var10;
	}



	public String getVar11() {
		return var11;
	}



	public void setVar11(String var11) {
		this.var11 = var11;
	}



	public String getVar12() {
		return var12;
	}



	public void setVar12(String var12) {
		this.var12 = var12;
	}



	public String getVar13() {
		return var13;
	}



	public void setVar13(String var13) {
		this.var13 = var13;
	}



	public String getVar14() {
		return var14;
	}



	public void setVar14(String var14) {
		this.var14 = var14;
	}



	public String getVar15() {
		return var15;
	}



	public void setVar15(String var15) {
		this.var15 = var15;
	}



	public String getVar16() {
		return var16;
	}



	public void setVar16(String var16) {
		this.var16 = var16;
	}



	public String getVar17() {
		return var17;
	}



	public void setVar17(String var17) {
		this.var17 = var17;
	}



	public String getVar18() {
		return var18;
	}



	public void setVar18(String var18) {
		this.var18 = var18;
	}



	public String getVar19() {
		return var19;
	}



	public void setVar19(String var19) {
		this.var19 = var19;
	}



	public String getVar20() {
		return var20;
	}



	public void setVar20(String var20) {
		this.var20 = var20;
	}



	public String getVar21() {
		return var21;
	}



	public void setVar21(String var21) {
		this.var21 = var21;
	}



	public String getVar22() {
		return var22;
	}



	public void setVar22(String var22) {
		this.var22 = var22;
	}



	public String getVar23() {
		return var23;
	}



	public void setVar23(String var23) {
		this.var23 = var23;
	}



	public String getVar24() {
		return var24;
	}



	public void setVar24(String var24) {
		this.var24 = var24;
	}



	public String getVar25() {
		return var25;
	}



	public void setVar25(String var25) {
		this.var25 = var25;
	}



	public String getDescription() {
		return description;
	}



	public void setDescription(String description) {
		this.description = description;
	}



	public String getStreet1() {
		return street1;
	}



	public void setStreet1(String street1) {
		this.street1 = street1;
	}



	public String getStreet2() {
		return street2;
	}



	public void setStreet2(String street2) {
		this.street2 = street2;
	}



	public String getCity() {
		return city;
	}



	public void setCity(String city) {
		this.city = city;
	}



	public String getState() {
		return state;
	}



	public void setState(String state) {
		this.state = state;
	}



	public String getZip() {
		return zip;
	}



	public void setZip(String zip) {
		this.zip = zip;
	}



	public String getCountry() {
		return country;
	}



	public void setCountry(String country) {
		this.country = country;
	}



	public String getPhone() {
		return phone;
	}



	public void setPhone(String phone) {
		this.phone = phone;
	}



	public String getFax() {
		return fax;
	}



	public void setFax(String fax) {
		this.fax = fax;
	}



	public String getCell() {
		return cell;
	}



	public void setCell(String cell) {
		this.cell = cell;
	}

}
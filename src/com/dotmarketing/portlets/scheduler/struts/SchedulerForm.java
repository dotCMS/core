package com.dotmarketing.portlets.scheduler.struts;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import com.liferay.portal.util.Constants;

/** @author Hibernate CodeGenerator */
public class SchedulerForm extends ValidatorForm {
	private boolean editMode = false;
	private String jobName;
    private String jobGroup;
    private String jobDescription;
    
    private String javaClass;
    
    private boolean haveStartDate;
    private String startDate;
    private boolean haveEndDate;
    private String endDate;
    
    private boolean atInfo;
    private String at;
    private int atTimeHour;
    private int atTimeMinute;
    private int atTimeSecond;
    private int betweenFromHour;
    private int betweenFromMinute;
    private int betweenFromSecond;
    private int betweenToHour;
    private int betweenToMinute;
    private int betweenToSecond;
    
    private boolean everyInfo;
    private String every;
    private int everyDateMonth;
    private int everyDateDay;
    private int everyDateYear;
    private boolean isMonday;
    private boolean isTuesday;
    private boolean isWednesday;
    private boolean isThusday;
    private boolean isFriday;
    private boolean isSaturday;
    private boolean isSunday;
    
    private boolean eachInfo;
    private int eachHours;
    private int eachMinutes;
    
    private String everyDate;
    private String atTime;
    private String cronExpression="";
    
    
    


	
	private Map map;
/*    private String type;
    private boolean schedulerEditable;

    public boolean isSchedulerEditable() {
		return schedulerEditable;
	}

	public void setSchedulerEditable(boolean schedulerEditable) {
		this.schedulerEditable = schedulerEditable;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
*/
	public int getBetweenFromHour() {
		return betweenFromHour;
	}

	public void setBetweenFromHour(int betweenFromHour) {
		this.betweenFromHour = betweenFromHour;
	}

	public int getBetweenFromMinute() {
		return betweenFromMinute;
	}

	public void setBetweenFromMinute(int betweenFromMinute) {
		this.betweenFromMinute = betweenFromMinute;
	}

	public int getBetweenFromSecond() {
		return betweenFromSecond;
	}

	public void setBetweenFromSecond(int betweenFromSecond) {
		this.betweenFromSecond = betweenFromSecond;
	}

	public String getAt() {
		return at;
	}

	public void setAt(String at) {
		this.at = at;
	}

	public boolean isAtInfo() {
		return atInfo;
	}

	public void setAtInfo(boolean atInfo) {
		this.atInfo = atInfo;
	}

	public int getAtTimeHour() {
		return atTimeHour;
	}

	public void setAtTimeHour(int atTimeHour) {
		this.atTimeHour = atTimeHour;
	}

	public int getAtTimeMinute() {
		return atTimeMinute;
	}

	public void setAtTimeMinute(int atTimeMinute) {
		this.atTimeMinute = atTimeMinute;
	}

	public int getAtTimeSecond() {
		return atTimeSecond;
	}

	public void setAtTimeSecond(int atTimeSecond) {
		this.atTimeSecond = atTimeSecond;
	}

	/** default constructor */
    public SchedulerForm() {
    }
    
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {
            return super.validate(mapping, request);
        }
        return null;
    }
    
	public String getEndDate() {
		return endDate;
	}
	
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	
	public String getJavaClass() {
		return javaClass;
	}
	
	public void setJavaClass(String javaClass) {
		this.javaClass = javaClass;
	}
	
	public String getJobDescription() {
		return jobDescription;
	}
	
	public void setJobDescription(String jobDescription) {
		this.jobDescription = jobDescription;
	}
	
	public String getJobName() {
		return jobName;
	}
	
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	
	public String getStartDate() {
		return startDate;
	}
	
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getJobGroup() {
		return jobGroup;
	}

	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}

	public boolean isHaveEndDate() {
		return haveEndDate;
	}

	public void setHaveEndDate(boolean haveEndDate) {
		this.haveEndDate = haveEndDate;
	}

	public boolean isHaveStartDate() {
		return haveStartDate;
	}

	public void setHaveStartDate(boolean haveStartDate) {
		this.haveStartDate = haveStartDate;
	}

	public int getBetweenToHour() {
		return betweenToHour;
	}

	public void setBetweenToHour(int betweenToHour) {
		this.betweenToHour = betweenToHour;
	}

	public int getBetweenToMinute() {
		return betweenToMinute;
	}

	public void setBetweenToMinute(int betweenToMinute) {
		this.betweenToMinute = betweenToMinute;
	}

	public int getBetweenToSecond() {
		return betweenToSecond;
	}

	public void setBetweenToSecond(int betweenToSecond) {
		this.betweenToSecond = betweenToSecond;
	}

	public String getEvery() {
		return every;
	}

	public void setEvery(String every) {
		this.every = every;
	}

	public int getEveryDateDay() {
		return everyDateDay;
	}

	public void setEveryDateDay(int everyDateDay) {
		this.everyDateDay = everyDateDay;
	}

	public int getEveryDateMonth() {
		return everyDateMonth;
	}

	public void setEveryDateMonth(int everyDateMonth) {
		this.everyDateMonth = everyDateMonth;
	}

	public int getEveryDateYear() {
		return everyDateYear;
	}

	public void setEveryDateYear(int everyDateYear) {
		this.everyDateYear = everyDateYear;
	}

	public boolean isEveryInfo() {
		return everyInfo;
	}

	public void setEveryInfo(boolean everyInfo) {
		this.everyInfo = everyInfo;
	}

	public boolean isFriday() {
		return isFriday;
	}

	public void setFriday(boolean isFriday) {
		this.isFriday = isFriday;
	}

	public boolean isMonday() {
		return isMonday;
	}

	public void setMonday(boolean isMonday) {
		this.isMonday = isMonday;
	}

	public boolean isSaturday() {
		return isSaturday;
	}

	public void setSaturday(boolean isSaturday) {
		this.isSaturday = isSaturday;
	}

	public boolean isSunday() {
		return isSunday;
	}

	public void setSunday(boolean isSunday) {
		this.isSunday = isSunday;
	}

	public boolean isThusday() {
		return isThusday;
	}

	public void setThusday(boolean isThusday) {
		this.isThusday = isThusday;
	}

	public boolean isTuesday() {
		return isTuesday;
	}

	public void setTuesday(boolean isTuesday) {
		this.isTuesday = isTuesday;
	}

	public boolean isWednesday() {
		return isWednesday;
	}

	public void setWednesday(boolean isWednesday) {
		this.isWednesday = isWednesday;
	}

	public int getEachHours() {
		return eachHours;
	}

	public void setEachHours(int eachHours) {
		this.eachHours = eachHours;
	}

	public boolean isEachInfo() {
		return eachInfo;
	}

	public void setEachInfo(boolean eachInfo) {
		this.eachInfo = eachInfo;
	}

	public int getEachMinutes() {
		return eachMinutes;
	}

	public void setEachMinutes(int eachMinutes) {
		this.eachMinutes = eachMinutes;
	}

	/**
	 * @return the hashMap
	 */
	public Map getMap() {
		return map;
	}

	/**
	 * @param Map the hashMap to set
	 */
	public void setMap(Map map) {
		this.map = map;
	}

	public boolean isEditMode() {
		return editMode;
	}

	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}
	
    public String getEveryDate() {
		return everyDate;
	}

	public void setEveryDate(String everyDate) {
		this.everyDate = everyDate;
	}

	public String getAtTime() {
		return atTime;
	}

	public void setAtTime(String atTime) {
		this.atTime = atTime;
	}
	
	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}
}
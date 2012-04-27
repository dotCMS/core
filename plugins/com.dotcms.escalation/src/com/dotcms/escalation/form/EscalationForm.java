package	com.dotcms.escalation.form;

import org.apache.struts.action.ActionForm;


public class EscalationForm extends ActionForm {

	private int days;
	public int getDays() {
		return days;
	}
	public void setDays(int days) {
		this.days = days;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	private String user;

	

}

package com.dotmarketing.portlets.templates.struts;


import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.START_COMMENT;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.END_COMMENT;
import com.liferay.portal.util.Constants;

/** @author Hibernate CodeGenerator */
public class TemplateForm extends ValidatorForm {

    private static final long serialVersionUID = 1L;

	/** nullable persistent field */
    private String body;

	/*** WEB ASSET FIELDS FOR THE FORM ***/
    /** nullable persistent field */
    private String title;

    /** nullable persistent field */
    private String friendlyName;

    /** nullable persistent field */
    private boolean showOnMenu;

    /** nullable persistent field */
    private int sortOrder;
	/*** WEB ASSET FIELDS FOR THE FORM ***/

    /** nullable persistent field */
	private String image;
	
    /** nullable persistent field */
    private String header;
    /** nullable persistent field */
    private String footer;
    
    /** nullable persistent field */
    private String hostId;
    
    // BEGIN GRAZIANO issue-12-dnd-template
    private String drawedBody;
    
    private boolean drawed;
    
    private int countAddContainer;
    
    private int countContainers;
    
    private String headCode;
    // END GRAZIANO issue-12-dnd-template    
    
    private String owner;  // dotcms 472
    


	public TemplateForm() {
    }
	
	

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

	/**
	 * @return Returns the image.
	 */
	public String getImage() {
		return image;
	}
	/**
	 * @param image The image to set.
	 */
	public void setImage(String image) {
		this.image = image;
	}
    
	/**
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the showOnMenu.
	 * @return boolean
	 */
	public boolean isShowOnMenu() {
		return showOnMenu;
	}

	/**
	 * Sets the showOnMenu.
	 * @param showOnMenu The showOnMenu to set
	 */
	public void setShowOnMenu(boolean showOnMenu) {
		this.showOnMenu = showOnMenu;
	}

	/**
	 * Returns the sortOrder.
	 * @return int
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * Sets the sortOrder.
	 * @param sortOrder The sortOrder to set
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {
            return super.validate(mapping, request);
        }
        return null;
    }

	/**
	 * Returns the friendlyName.
	 * @return String
	 */
	public String getFriendlyName() {
		return friendlyName;
	}

	/**
	 * Sets the friendlyName.
	 * @param friendlyName The friendlyName to set
	 */
	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	/**
	 * Returns the body.
	 * @return String
	 */
	public String getBody() {
		if(!drawed)
			return body;
		else
			return START_COMMENT+body+END_COMMENT;
	}

	/**
	 * Sets the body.
	 * @param body The body to set
	 */
	public void setBody(String body) {
		this.body = body;
	}
	
	// BEGIN GRAZIANO issue-12-dnd-template
	public String getDrawedBody() {
		return drawedBody;
	}

	public void setDrawedBody(String drawedBody) {
		this.drawedBody = drawedBody;
	}

	public boolean isDrawed() {
		return drawed;
	}

	public void setDrawed(boolean drawed) {
		this.drawed = drawed;
	}
	
	public int getCountAddContainer() {
		return countAddContainer;
	}

	public void setCountAddContainer(int countAddContainer) {
		this.countAddContainer = countAddContainer;
	}

	public int getCountContainers() {
		return countContainers;
	}

	public void setCountContainers(int countContainers) {
		this.countContainers = countContainers;
	}

	public String getHeadCode() {
		return headCode;
	}

	public void setHeadCode(String headCode) {
		this.headCode = headCode;
	}	
	// END GRAZIANO issue-12-dnd-template

	/**
	 * Returns the footer.
	 * @return String
	 */
	public String getFooter() {
		return footer;
	}

	/**
	 * Returns the header.
	 * @return String
	 */
	public String getHeader() {
		return header;
	}

	/**
	 * Sets the footer.
	 * @param footer The footer to set
	 */
	public void setFooter(String footer) {
		this.footer = footer;
	}

	/**
	 * Sets the header.
	 * @param header The header to set
	 */
	public void setHeader(String header) {
		this.header = header;
	}

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

}

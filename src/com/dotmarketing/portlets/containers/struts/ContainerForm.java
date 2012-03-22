package com.dotmarketing.portlets.containers.struts;


import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.util.Constants;

/** @author Hibernate CodeGenerator */
public class ContainerForm extends ValidatorForm {

	private static final long serialVersionUID = 1L;

	/** nullable persistent field */
    private String code;

    /** nullable persistent field */
	private int maxContentlets;

	/*** WEB ASSET FIELDS FOR THE FORM ***/
    /** nullable persistent field */
    private String title;

    private String preLoop;
    private String postLoop;
    
    private boolean staticify;
    
    /** nullable persistent field */
    private String friendlyName;

    /** nullable persistent field */
    private boolean showOnMenu;

    /** nullable persistent field */
    private int sortOrder;
	/*** WEB ASSET FIELDS FOR THE FORM ***/

    private boolean useDiv;
    private String sortContentletsBy;
    
	private String structureInode;

    private String hostId;

    private String luceneQuery;
    private String notes;
    
    private boolean dynamic;
    
    
    
    private String owner; // dotcms 472
    
    public ContainerForm() {
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
        ActionErrors errors = super.validate(mapping, request);
        if (errors == null) errors = new ActionErrors ();
        
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {
            if (maxContentlets > 0) {
                if (dynamic && !UtilMethods.isSet(luceneQuery)) {
                    errors.add("luceneQuery", new ActionMessage("error.containers.query.required"));
                } 
            }
            if (!dynamic || maxContentlets == 0) {
                dynamic = false;
                luceneQuery = "";
                sortContentletsBy = "";
            }
        }
        return errors;
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
	 * Returns the code.
	 * @return String
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Sets the code.
	 * @param code The code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Returns the maxContentlets.
	 * @return int
	 */
	public int getMaxContentlets() {
		return maxContentlets;
	}

	/**
	 * Sets the maxContentlets.
	 * @param maxContentlets The maxContentlets to set
	 */
	public void setMaxContentlets(int maxContentlets) {
		this.maxContentlets = maxContentlets;
	}

	/**
	 * Returns the sortContentletsBy.
	 * @return String
	 */
	public String getSortContentletsBy() {
		return sortContentletsBy;
	}


	/**
	 * Sets the sortContentletsBy.
	 * @param sortContentletsBy The sortContentletsBy to set
	 */
	public void setSortContentletsBy(String sortContentletsBy) {
		this.sortContentletsBy = sortContentletsBy;
	}

	/**
	 * Returns the useDiv.
	 * @return boolean
	 */
	public boolean isUseDiv() {
		return useDiv;
	}

	/**
	 * Sets the useDiv.
	 * @param useDiv The useDiv to set
	 */
	public void setUseDiv(boolean useDiv) {
		this.useDiv = useDiv;
	}

	/**
	 * Returns the postLoop.
	 * @return String
	 */
	public String getPostLoop() {
		return postLoop;
	}

	/**
	 * Returns the preLoop.
	 * @return String
	 */
	public String getPreLoop() {
		return preLoop;
	}

	/**
	 * Sets the postLoop.
	 * @param postLoop The postLoop to set
	 */
	public void setPostLoop(String postLoop) {
		this.postLoop = postLoop;
	}

	/**
	 * Sets the preLoop.
	 * @param preLoop The preLoop to set
	 */
	public void setPreLoop(String preLoop) {
		this.preLoop = preLoop;
	}

	/**
	 * Returns the staticify.
	 * @return boolean
	 */
	public boolean isStaticify() {
		return staticify;
	}

	/**
	 * Sets the staticify.
	 * @param staticify The staticify to set
	 */
	public void setStaticify(boolean staticify) {
		this.staticify = staticify;
	}

	public String getStructureInode() {
		return structureInode;
	}


	public void setStructureInode(String structureInode) {
		this.structureInode = structureInode;
	}

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getLuceneQuery() {
        return luceneQuery;
    }

    public void setLuceneQuery(String luceneQuery) {
        this.luceneQuery = luceneQuery;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
	
    
}

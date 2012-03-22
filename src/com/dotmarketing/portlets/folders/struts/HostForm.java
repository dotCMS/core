package com.dotmarketing.portlets.folders.struts;

import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.InodeUtils;

/**
 *
 * @author  David
 */
public class HostForm  extends ValidatorForm {

	private static final long serialVersionUID = 1L;

    public HostForm() {   }

	private String inode;

	private String hostname;
	
	private boolean isDefault;
	
	private String aliases;
	
  
		
    public String getAliases() {
        return aliases;
    }
    public void setAliases(String aliases) {
        this.aliases = aliases;
    }
    public String getHostname() {
        return hostname;
    }
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    public boolean isDefault() {
        return isDefault;
    }
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    public String getInode() {
    	if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
    }
    public void setInode(String inode) {
        this.inode = inode;
    }
}
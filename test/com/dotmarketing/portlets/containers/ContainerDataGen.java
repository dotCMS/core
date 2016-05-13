package com.dotmarketing.portlets.containers;

import java.util.Date;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;

public class ContainerDataGen {

	private String code;
	private String friendlyName;
	private String notes;
	private String title;
	
	private static final HostAPI hostAPI = APILocator.getHostAPI();
	private static final String type = "containers";
	private static final Host defaultHost;
	private static final User user;
	
	static {
        try {
            user = APILocator.getUserAPI().getSystemUser();
            defaultHost = hostAPI.findDefaultHost(user, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	public Container next(){
		//Create the new container
        Container container = new Container();

        container.setCode(this.code);
        container.setFriendlyName(this.friendlyName);
        container.setIDate(new Date());
        container.setMaxContentlets(1);
        container.setModDate(new Date());
        container.setModUser(user.getUserId());
        container.setNotes(this.notes);
        container.setOwner(user.getUserId());
        container.setPostLoop("");
        container.setPreLoop("");
        container.setShowOnMenu( true );
        container.setSortContentletsBy("");
        container.setSortOrder(2);
        container.setStaticify( true );
        container.setTitle( this.title);
        container.setType(type);
        container.setUseDiv( true );
        
        return container;
	}
	
	public Container nextPersisted() throws DotStateException, DotDataException, DotSecurityException{
		Container container = next();
		WebAssetFactory.createAsset(container, user.getUserId(), defaultHost);
		return container;
	}
	
	public void remove(Container container) throws Exception{
		WebAssetFactory.deleteAsset(container, user);
	}
	
	public ContainerDataGen code(String code){
		this.code = code;
		return this;
	}
	
	public ContainerDataGen friendlyName(String friendlyName){
		this.friendlyName = friendlyName;
		return this;
	}

	public ContainerDataGen notes(String notes){
		this.notes = notes;
		return this;
	}
	
	public ContainerDataGen title(String title){
		this.title = title;
		return this;
	}
}

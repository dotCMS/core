package com.dotcms.datagen;

import java.util.Date;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.model.Container;

/**
 * Class used to create {@link Container} objects for test purposes
 * 
 * @author Nollymar Longa
 *
 */
public class ContainerDataGen extends AbstractDataGen<Container> {

	private String code;
	private String friendlyName;
	private String notes;
	private String title;

	private static final String type = "containers";

	/**
	 * Creates a new {@link Container} instance kept in memory (not persisted)
	 * 
	 * @return Container instance created
	 */
	@Override
	public Container next() {
		// Create the new container
		Container container = new Container();

		//TODO: pending remove
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
		container.setShowOnMenu(true);
		container.setSortContentletsBy("");
		container.setSortOrder(2);
		container.setStaticify(true);
		container.setTitle(this.title);
		container.setType(type);
		container.setUseDiv(true);

		return container;
	}

	/**
	 * Creates a new {@link Container} instance and persists it in DB
	 * 
	 * @return A new Container instance persisted in DB
	 */
	@Override
	public Container nextPersisted() {
		return persist(next());
	}
	
	/* (non-Javadoc)
	 * @see com.dotcms.datagen.DataGen#persist(java.lang.Object)
	 */
	@Override
	public Container persist(Container object) {
		try{
			WebAssetFactory.createAsset(object, user.getUserId(), defaultHost);
		}catch(DotStateException | DotDataException | DotSecurityException e){
			throw new RuntimeException(e);
		}
		
		return object;
	}

	/**
	 * Deletes a given {@link Container} instance
	 * 
	 * @param container
	 *            to be removed
	 */
	@Override
	public void remove(Container container) {
		try{
			WebAssetFactory.deleteAsset(container, user);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets code property to the ContainerDataGen instance. This will be used
	 * when a new {@link Container} instance is created
	 * 
	 * @param code
	 * @return ContainerDataGen with code property set
	 */
	public ContainerDataGen code(String code) {
		this.code = code;
		return this;
	}

	/**
	 * Sets friendlyName property to the ContainerDataGen instance. This will be
	 * used when a new {@link Container} instance is created
	 * 
	 * @param friendlyName
	 * @return ContainerDataGen with friendlyName property set
	 */
	public ContainerDataGen friendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
		return this;
	}

	/**
	 * Sets notes property to the ContainerDataGen instance. This will be used
	 * when a new {@link Container} instance is created
	 * 
	 * @param notes
	 * @return ContainerDataGen with notes property set
	 */
	public ContainerDataGen notes(String notes) {
		this.notes = notes;
		return this;
	}

	/**
	 * Sets title property to the ContainerDataGen instance. This will be used
	 * when a new {@link Container} instance is created
	 * 
	 * @param title
	 * @return ContainerDataGen with title property set
	 */
	public ContainerDataGen title(String title) {
		this.title = title;
		return this;
	}
}

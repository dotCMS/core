package com.dotcms.datagen;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.structure.model.Structure;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to create {@link Container} objects for test purposes
 * 
 * @author Nollymar Longa
 *
 */
public class ContainerDataGen extends AbstractDataGen<Container> {

	private String friendlyName;
	private String notes;
	private String title;
    private Map<Structure, String> structures = new HashMap<>();

	private static final String type = "containers";

	/**
	 * Sets friendlyName property to the ContainerDataGen instance. This will be
	 * used when a new {@link Container} instance is created
	 * 
	 * @param friendlyName the friendly name
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
	 * @param notes the notes
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
	 * @param title the title
	 * @return ContainerDataGen with title property set
	 */
	public ContainerDataGen title(String title) {
		this.title = title;
		return this;
	}

    /**
     * Adds a structure to the list of structures to be included in the {@link Container} this data-gen will create.
     * Takes the structure to be included and the code to be displayed in the container
     * @param structure the structure to include
     * @param codeForStructure the code to be displayed
     * @return the data-gen with the added structure
     */
    public ContainerDataGen withStructure(Structure structure, String codeForStructure) {
        structures.put(structure, codeForStructure);
        return this;
    }

    /**
     * Removes a structure from the list of structures be included to the {@link Container} this data-gen will create
     * @param structure the structure to remove
     * @return the data-gen with the removed structure
     */
    public ContainerDataGen withoutStructure(Structure structure) {
        structures.remove(structure);
        return this;
    }

    /**
     * Clears the list of structures be included to the {@link Container} this data-gen will create
     * @return the data-gen with the cleared list of structures
     */
    public ContainerDataGen clearStructures() {
        structures.clear();
        return this;
    }

    /**
     * Creates a new {@link Container} instance kept in memory (not persisted)
     *
     * @return Container instance created
     */
    @Override
    public Container next() {
        // Create the new container
        Container container = new Container();

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
    public Container persist(Container container) {
        try{
            WebAssetFactory.createAsset(container, user.getUserId(), host);
        }catch(DotStateException | DotDataException | DotSecurityException e){
            throw new RuntimeException(e);
        }

        // Container structures
        List<ContainerStructure> csList = new ArrayList<>();

        for (Structure structure : structures.keySet()) {

            ContainerStructure cs = new ContainerStructure();
            cs.setContainerId(container.getIdentifier());
            cs.setContainerInode(container.getInode());
            cs.setStructureId(structure.getInode());
            cs.setCode(structures.get(structure));
            csList.add(cs);
        }

        if(!csList.isEmpty()) {
            try {
                APILocator.getContainerAPI().saveContainerStructures(csList);
            } catch (DotDataException | DotSecurityException e) {
                throw new RuntimeException("Error saving container-structure relationships", e);
            }
        }

        return container;
    }

    /**
     * Deletes a given {@link Container} instance
     *
     * @param container
     *            to be removed
     */
    public static void remove(Container container) {
        try{
            APILocator.getContainerAPI().delete(container, user, false);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

}

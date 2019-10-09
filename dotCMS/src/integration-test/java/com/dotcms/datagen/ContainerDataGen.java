package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
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

    private String friendlyName = "testFriendlyName" + System.currentTimeMillis();
    private String notes = "testNotes" + System.currentTimeMillis();
    private String title = "testTitle" + System.currentTimeMillis();
    private int maxContentlets = 5;
    private User modUser = user;
    private User owner = user;
    private Host site = host;
    private String code;
    private String preLoop = "<div>";
    private String postLoop = "</div>";
    private Boolean showOnMenu = Boolean.FALSE;
    private String sortContenletsBy;
    private int sortOrder;
    private Boolean staticify = Boolean.TRUE;
    private Boolean useDiv = Boolean.TRUE;

    private static final String type = "containers";

    private Map<ContentType, String> contentTypes = new HashMap<>();

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
     * Adds a structure to the list of contentTypes to be included in the {@link Container} this data-gen will create.
     * Takes the structure to be included and the code to be displayed in the container
     * @param structure the structure to include
     * @param codeForStructure the code to be displayed
     * @return the data-gen with the added structure
     */
    public ContainerDataGen withStructure(Structure structure, String codeForStructure) {

        final ContentType toContentType = new StructureTransformer(structure).asList().get(0);
        return withContentType(toContentType, codeForStructure);
    }

    public ContainerDataGen withContentType(ContentType contentType, String codeForStructure) {
        contentTypes.put(contentType, codeForStructure);
        return this;
    }

    /**
     * Removes a structure from the list of contentTypes be included to the {@link Container} this data-gen will create
     * @param structure the structure to remove
     * @return the data-gen with the removed structure
     */
    public ContainerDataGen withoutStructure(Structure structure) {
        final ContentType toContentType = new StructureTransformer(structure).asList().get(0);
        return withoutContentType(toContentType);
    }

    public ContainerDataGen withoutContentType(ContentType contentType) {
        contentTypes.remove(contentType);
        return this;
    }

    /**
     * Clears the list of contentTypes be included to the {@link Container} this data-gen will create
     * @return the data-gen with the cleared list of contentTypes
     */
    public ContainerDataGen clearContentTypes() {
        contentTypes.clear();
        return this;
    }

    public ContainerDataGen maxContentlets(int maxContentlets) {
        this.maxContentlets = maxContentlets;
        return this;
    }

    public ContainerDataGen modUser(User modUser) {
        this.modUser = modUser;
        return this;
    }

    public ContainerDataGen owner(User owner) {
        this.owner = owner;
        return this;
    }

    public ContainerDataGen site(Host site) {
        this.site = site;
        return this;
    }

    public ContainerDataGen code(String code) {
        this.code = code;
        return this;
    }

    public ContainerDataGen preLoop(String preLoop) {
        this.preLoop = preLoop;
        return this;
    }

    public ContainerDataGen postLoop(String postLoop) {
        this.postLoop = postLoop;
        return this;
    }

    public ContainerDataGen showOnMenu(Boolean showOnMenu) {
        this.showOnMenu = showOnMenu;
        return this;
    }

    public ContainerDataGen sortContenletsBy(String sortContenletsBy) {
        this.sortContenletsBy = sortContenletsBy;
        return this;
    }

    public ContainerDataGen sortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public ContainerDataGen staticify(Boolean staticify) {
        this.staticify = staticify;
        return this;
    }

    public ContainerDataGen useDiv(Boolean useDiv) {
        this.useDiv = useDiv;
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

        container.setFriendlyName(friendlyName);
        container.setIDate(new Date());
        container.setMaxContentlets(maxContentlets);
        container.setModDate(new Date());
        container.setModUser(modUser.getUserId());
        container.setNotes(notes);
        container.setOwner(owner.getUserId());
        container.setCode(code);
        container.setPreLoop(preLoop);
        container.setPostLoop(postLoop);
        container.setShowOnMenu(showOnMenu);
        container.setSortContentletsBy(sortContenletsBy);
        container.setSortOrder(sortOrder);
        container.setStaticify(staticify);
        container.setTitle(title);
        container.setType(type);
        container.setUseDiv(useDiv);

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
    @WrapInTransaction
    @Override
    public Container persist(Container container) {

        try {

            if (contentTypes.isEmpty()) {
                ContentType pageContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find("htmlpageasset");
                withContentType(pageContentType, "Sample Code" + System.currentTimeMillis());
            }

            // Container contentTypes
            List<ContainerStructure> csList = new ArrayList<>();
            for (ContentType contentType : contentTypes.keySet()) {
                ContainerStructure cs = new ContainerStructure();
                cs.setContainerId(container.getIdentifier());
                cs.setContainerInode(container.getInode());
                cs.setStructureId(contentType.id());
                cs.setCode(contentTypes.get(contentType));
                csList.add(cs);
            }

            container = APILocator.getContainerAPI()
                    .save(container, csList, site, owner, false);
        } catch (Exception e) {
            throw new RuntimeException("Error persisting Container", e);
        }

        return container;
    }

    /**
     * Deletes a given {@link Container} instance
     *
     * @param container
     *            to be removed
     */
    @WrapInTransaction
    public static void remove(Container container) {
        try{
            APILocator.getContainerAPI().delete(container, user, false);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

}

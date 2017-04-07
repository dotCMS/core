package com.dotmarketing.business;

import com.dotcms.business.TypeDAO;
import com.dotcms.business.TypeFactory;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.ImmutableBiMap;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

import java.util.Collections;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.entry;
import static com.dotcms.util.CollectionsUtils.mapEntries;

/**
 * Default implementation
 * @author jsanca
 */
public class PermissionableAPIImpl implements PermissionableAPI {

    /**
     * Key for {@link Config} to determine if the mapping of the class permissionable is exact match or can use superclasses matching too.
     * For instance if there is {@link PermissionableResolver} for {@link Category}, if the value is false superclasses from {@link Category} will use the
     * same {@link PermissionableResolver}.
     * By default the value is true, means exact match
     */
    public static final String DOTCMS_PERMISSIONAPI_EXACTMATCH_CLASS_PERMISSIONABLE = "dotcms.permissionapi.exactmatch.class.permissionable";

    private final TypeDAO typeDAO;
    private final ContentletAPI contentletAPI;
    private final boolean         useExactMatchForClassPermissionableMap;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final CategoryAPI categoryAPI;
    private final ContainerAPI    containerAPI;
    private final TemplateAPI     templateAPI;
    private final RelationshipAPI relationshipAPI;
    private final UserProxyAPI    userProxyAPI;
    private final Map<Class, PermissionableResolver> classPermissionableByINodeIdMap;
    private final Map<Class, PermissionableResolver> classPermissionableByIdentifierMap;


    public PermissionableAPIImpl() {
        this(TypeFactory.getInstance().getTypeDAO(),
                APILocator.getContentletAPI(),
                APILocator.getHostAPI(),
                APILocator.getFolderAPI(),
                APILocator.getCategoryAPI(),
                APILocator.getContainerAPI(),
                APILocator.getTemplateAPI(),
                APILocator.getRelationshipAPI(),
                APILocator.getUserProxyAPI()
        );
    }


    @VisibleForTesting
    public PermissionableAPIImpl(final TypeDAO           typeDAO,
                                final ContentletAPI     contentletAPI,
                                final HostAPI           hostAPI,
                                final FolderAPI         folderAPI,
                                final CategoryAPI       categoryAPI,
                                final ContainerAPI containerAPI,
                                final TemplateAPI templateAPI,
                                final RelationshipAPI   relationshipAPI,
                                final UserProxyAPI      userProxyAPI) {

        this.typeDAO         = typeDAO;
        this.contentletAPI   = contentletAPI;
        this.hostAPI         = hostAPI;
        this.folderAPI       = folderAPI;
        this.categoryAPI     = categoryAPI;
        this.containerAPI    = containerAPI;
        this.templateAPI     = templateAPI;
        this.relationshipAPI = relationshipAPI;
        this.userProxyAPI    = userProxyAPI;
        this.useExactMatchForClassPermissionableMap =
                Config.getBooleanProperty(DOTCMS_PERMISSIONAPI_EXACTMATCH_CLASS_PERMISSIONABLE, true);
        this.classPermissionableByINodeIdMap    = getClassPermissionableByINodeIdMap();
        this.classPermissionableByIdentifierMap = getClassPermissionableByIdentifierMap();
    }

    @Override
    public Permissionable resolvePermissionable(final String id, final Long language,
                                                final User user, final boolean respectFrontendRoles) {

        Permissionable permissionable = this.tryFindHost(id, user, respectFrontendRoles);

        if (null == permissionable) {

            permissionable = this.resolvePermissionableByIdentifier
                    (id, language, user, respectFrontendRoles);
            if (null == permissionable) {

                permissionable = this.resolvePermissionableByINode (id, language, user, respectFrontendRoles);

                // if (null == permissionable) { // todo: another fallback
            }
        }

        return permissionable;
    } // resolvePermissionable

    private Permissionable tryFindHost (final String id,
                                        final User user,
                                        final boolean respectFrontendRoles) {

        Permissionable permissionable = null;

        try { //Host?
            permissionable = this.hostAPI.find(id, user, respectFrontendRoles);
        } catch(Exception e) {}

        return permissionable;
    } // tryFindHost.

    @Override
    public Permissionable resolvePermissionableByIdentifier (final String identifier, final Long language,
                                                             final User user, final boolean respectFrontendRoles) {

        Permissionable permissionable   = null;
        PermissionableResolver resolver = null;
        final Class classByIdentifier   = this.typeDAO.getIdentifierType(identifier); // resolve by identifier

        if (null != classByIdentifier) {

            resolver = this.getPermissionableResolverForIdentifier(classByIdentifier);

            if (null != resolver) {

                permissionable =
                        resolver.resolve(identifier, language, user, respectFrontendRoles);
            }
        }

        return permissionable;
    } // resolvePermissionableByIdentifier.

    @Override
    public Permissionable resolvePermissionableByINode (final String inodeId, final Long language,
                                                        final User user, final boolean respectFrontendRoles) {

        Permissionable permissionable   = null;
        PermissionableResolver resolver = null;
        final Class classByINode        = this.typeDAO.getInodeType(inodeId); // resolve by Inode

        if (null != classByINode) {

            resolver = this.getPermissionableResolverForINode(classByINode);

            if (null != resolver) {

                permissionable =
                        resolver.resolve(inodeId, language, user, respectFrontendRoles);
            }
        }

        return permissionable;
    } // resolvePermissionableByINode.

    private PermissionableResolver getPermissionableResolverForINode (final Class aClass) {

        PermissionableResolver permissionableResolver = null;

        if (this.classPermissionableByINodeIdMap.containsKey(aClass)) {

            permissionableResolver = this.classPermissionableByINodeIdMap.get(aClass);
        } else if (!this.useExactMatchForClassPermissionableMap) {

            for (Class clazzKey : this.classPermissionableByINodeIdMap.keySet()) {

                if (clazzKey.isAssignableFrom(aClass)) {

                    permissionableResolver = this.classPermissionableByINodeIdMap.get(clazzKey); break;
                }
            }
        }

        return permissionableResolver;
    } // getPermissionableResolver.

    /**
     * Returns the mapping for the Inode class permissionable.
     * @return Map
     */
    protected Map<Class, PermissionableResolver> getClassPermissionableByINodeIdMap() {

        final Map<Class, PermissionableResolver> customExtraEntries   = this.getCustomExtraEntriesForInodes();
        final Map<Class, PermissionableResolver> defaultEntries       = mapEntries(
                entry(Category.class,     (id, language, user, respectFrontendRoles) -> this.categoryAPI.find(id, user, respectFrontendRoles) ),
                entry(Container.class,    (id, language, user, respectFrontendRoles) -> this.containerAPI.find(id, user, respectFrontendRoles) ),
                entry(Contentlet.class,   (id, language, user, respectFrontendRoles) -> this.contentletAPI.find(id, user, respectFrontendRoles) ),
                entry(Field.class,        (id, language, user, respectFrontendRoles) -> this.typeDAO.findByInode(id, Field.class) ),
                entry(Folder.class,       (id, language, user, respectFrontendRoles) -> this.folderAPI.find(id, user, respectFrontendRoles) ),
                entry(Link.class,         (id, language, user, respectFrontendRoles) -> this.typeDAO.findByInode(id, Link.class) ),
                entry(Relationship.class, (id, language, user, respectFrontendRoles) -> this.relationshipAPI.byInode(id) ),
                entry(Structure.class,    (id, language, user, respectFrontendRoles) -> new StructureTransformer(APILocator.getContentTypeAPI(user).find(id)).asStructure() ),
                entry(Template.class,     (id, language, user, respectFrontendRoles) -> this.templateAPI.find(id, user, respectFrontendRoles) ),
                entry(UserProxy.class,    (id, language, user, respectFrontendRoles) -> this.userProxyAPI.getUserProxy(id, user, respectFrontendRoles) ),
                entry(VirtualLink.class,  (id, language, user, respectFrontendRoles) -> this.typeDAO.findByInode(id, VirtualLink.class) )
        );

        // custom Entries will overrides default Entries if needed.
        defaultEntries.putAll(customExtraEntries);

        return new ImmutableBiMap.Builder<Class, PermissionableResolver>().putAll(defaultEntries).build();
    } // getClassPermissionableByINodeIdMap.

    /**
     * This method will provide a custom entries for Inodes, by default there is not any entry.
     * In the future an easier mechanism might be provided
     * @return Map
     */
    protected Map<Class, PermissionableResolver>  getCustomExtraEntriesForInodes() {
        return Collections.EMPTY_MAP;
    } // getCustomExtraEntriesForInodes.

    //// BY IDENTIFIER
    private PermissionableResolver getPermissionableResolverForIdentifier (final Class aClass) {

        PermissionableResolver permissionableResolver = null;

        if (this.classPermissionableByIdentifierMap.containsKey(aClass)) {

            permissionableResolver = this.classPermissionableByIdentifierMap.get(aClass);
        } else if (!this.useExactMatchForClassPermissionableMap) {

            for (Class clazzKey : this.classPermissionableByIdentifierMap.keySet()) {

                if (clazzKey.isAssignableFrom(aClass)) {

                    permissionableResolver = this.classPermissionableByIdentifierMap.get(clazzKey); break;
                }
            }
        }

        return permissionableResolver;
    } // getPermissionableResolverForIdentifier.

    /**
     * Returns the mapping for the Identifier class permissionable.
     * @return Map
     */
    protected  Map<Class, PermissionableResolver>  getClassPermissionableByIdentifierMap() {

        final Map<Class, PermissionableResolver> customExtraEntries   = this.getCustomExtraEntriesForIdentifier();
        final Map<Class, PermissionableResolver> defaultEntries       = mapEntries(
                entry(Folder.class,        (id, language, user, respectFrontendRoles) -> this.typeDAO.findFirstInodeByIdentifier(id) ),
                entry(Contentlet.class,    (id, language, user, respectFrontendRoles) -> this.contentletAPI.findContentletByIdentifier(id, false, (null != language)?language:0, user, respectFrontendRoles) ),
                entry(HTMLPageAsset.class, (id, language, user, respectFrontendRoles) -> this.contentletAPI.findContentletByIdentifier(id, false, (null != language)?language:0, user, respectFrontendRoles) ),
                entry(Template.class,      (id, language, user, respectFrontendRoles) -> this.typeDAO.findFirstInodeByIdentifier(id) ),
                entry(Link.class,          (id, language, user, respectFrontendRoles) -> this.typeDAO.findFirstInodeByIdentifier(id) ),
                entry(Container.class,     (id, language, user, respectFrontendRoles) -> this.typeDAO.findFirstInodeByIdentifier(id) )
        );

        // custom Entries will overrides default Entries if needed.
        defaultEntries.putAll(customExtraEntries);

        return new ImmutableBiMap.Builder<Class, PermissionableResolver>().putAll(defaultEntries).build();
    } // getClassPermissionableByIdentifierMap.

    /**
     * This method will provide a custom entries for Identifier, by default there is not any entry.
     * In the future an easier mechanism might be provided
     * @return Map
     */
    protected Map<Class, PermissionableResolver>  getCustomExtraEntriesForIdentifier() {
        return Collections.EMPTY_MAP;
    } // getCustomExtraEntriesForIdentifier.

    /**
     * Permissionable Resolver is a class that resolve based on id (could be an identifier or inode id)
     */
    public interface PermissionableResolver {

        /**
         * Resolve the {@link Permissionable}
         * @param id
         * @param language
         * @param user
         * @param respectFrontendRoles
         * @return
         */
        Permissionable resolve (String id, Long language, User user, boolean respectFrontendRoles);
    } // PermissionableResolver.

} // E:O:F:PermissionableAPIImpl.

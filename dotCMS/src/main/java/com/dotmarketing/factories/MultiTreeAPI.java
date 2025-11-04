package com.dotmarketing.factories;

import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.design.bean.LayoutChanges;
import com.google.common.collect.Table;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This API provides access to HTML Page Multi-Tree data in dotCMS.
 * <p>In simple words, this is how HTML Pages in the system are put together:</p>
 * <ul>
 *     <li>The HTML Page.</li>
 *     <li>One or more Containers.</li>
 *     <li>One Container can have one or more Contentlets.</li>
 * </ul>
 * So, {@link MultiTree} objects contain important information on what Contentlets are added to Containers in a page.
 * This API allows developers to access and modify this information as required. Every time Users edit an HTML Page --
 * either adding or deleting Contentlets from it -- the Multi-Tree data is updated to reflect such an update.
 *
 * @author root
 */
public interface MultiTreeAPI {

    /**
     * Saves MultiTrees for a given page
     * 
     * @param mTrees
     * @throws DotDataException
     */
    void saveMultiTrees(String pageId, String variantName, List<MultiTree> mTrees) throws DotDataException;

    /**
     * Saves a specific MultiTree as it comes
     * 
     * @param multiTree
     * @throws DotDataException
     */
    void saveMultiTree(MultiTree multiTree) throws DotDataException;

    /**
     * Save the multi tree and does a reorder
     * @param mTree
     * @throws DotDataException
     */
    void saveMultiTreeAndReorder(final MultiTree mTree) throws DotDataException;
    /**
     * Deletes a specific MultiTree
     * 
     * @param multiTree
     * @throws DotDataException
     */
    void deleteMultiTree(MultiTree multiTree) throws DotDataException;

    /**
     * Deletes a MultiTrees related to the identifier
     *
     * @param identifier {@link Identifier}
     * @throws DotDataException
     */
    void deleteMultiTreeByIdentifier(Identifier identifier) throws DotDataException;

    /**
     * Removes any mutlitree that has the identifiers as either a parent or as a child
     * 
     * @param identifiers
     * @throws DotDataException
     */
    void deleteMultiTreesForIdentifiers(List<String> identifiers) throws DotDataException;


    /**
     * deletes all the multi tress related to the identifier, including parents and child in addition
     * for the pages related refresh the cache and publish relationships
     * 
     * @param identifier String
     * @throws DotDataException
     */
    void deleteMultiTreesRelatedToIdentifier(final String identifier) throws DotDataException;

    /**
     * This method returns ALL MultiTree entries (in all languages) for a given page and DEFAULT variant. It is up to what
     * ever page renderer to properly choose which MultiTree children to show for example, show an
     * english content on a spanish page when language fallback=true
     *
     *
     *  @param page {@link IHTMLPage}
     *  @param liveMode {@link Boolean}
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Table<String, String, Set<PersonalizedContentlet>> getPageMultiTrees(final IHTMLPage page, final boolean liveMode)
            throws DotDataException, DotSecurityException;


    /**
     * This method returns ALL MultiTree entries (in all languages) for a given page and variant. It is up to what
     * ever page renderer to properly choose which MultiTree children to show for example, show an
     * english content on a spanish page when language fallback=true.
     *
     * If the page does not have any {@link MultiTree} to the specific variant then this method return the
     * Page's {@link MultiTree} for the DEFAULT variant.
     *
     * @param page
     * @param liveMode
     * @param variantName
     *
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Table<String, String, Set<PersonalizedContentlet>> getPageMultiTrees(final IHTMLPage page, final String variantName, final boolean liveMode)
            throws DotDataException, DotSecurityException;


    /**
     * Saves a list of MultiTrees
     * 
     * @param mTrees
     * @throws DotDataException
     */
    void saveMultiTrees(List<MultiTree> mTrees) throws DotDataException;

    /**
     * Deletes a list of MultiTrees
     * 
     * @param mTree
     * @throws DotDataException
     */
    void deleteMultiTree(List<MultiTree> mTree) throws DotDataException;

    /**
     * Deletes all MultiTrees that have either this page id or container id as a parent
     * 
     * @param pageOrContainer
     * @throws DotDataException
     */
    void deleteMultiTreeByParent(String pageOrContainer) throws DotDataException;

    /**
     * Deletes all MultiTrees that have contentlet id as a child
     * 
     * @param contentIdentifier
     * @throws DotDataException
     */
    void deleteMultiTreeByChild(String contentIdentifier) throws DotDataException;

    void deleteMultiTree(final String pageId, final String variant)throws DotDataException;

    /**
     * Gets them all
     * 
     * @return
     */
    List<MultiTree> getAllMultiTrees();

    /**
     * Get MultiTrees on a page and personalization
     * @param pageId String
     * @param personalization String
     * @return List MultiTree
     * @throws DotDataException
     */
    List<MultiTree> getMultiTreesByPersonalizedPage(final String pageId,
            final String personalization, final String targetVariantName) throws DotDataException;

    /**
     * Gets a list of all MultiTrees on a page (even if they are bad)
     * 
     * @param parentInode
     * @return
     * @throws DotDataException
     */
    List<MultiTree> getMultiTreesByPage(String parentInode) throws DotDataException;

    /**
     * Gets a list of MultiTrees that belong to the page+container+container instance
     * 
     * @param htmlPage
     * @param container
     * @param containerInstance
     * @return
     */
    List<MultiTree> getMultiTrees(String htmlPage, String container, String containerInstance);

    /**
     * Gets a list of MultiTrees that belong to the page+container+container instance
     *
     * @param htmlPage
     * @param container
     * @param containerInstance
     * @param personalization
     * @return List of Multitree
     */
    List<MultiTree> getMultiTrees(String htmlPage, String container, String containerInstance, String personalization);

    /**
     * Gets a list of MultiTrees that belong to the page+container
     * 
     * @param htmlPage
     * @param container
     * @return
     */
    List<MultiTree> getMultiTrees(IHTMLPage htmlPage, Container container) throws DotDataException;

    /**
     * Gets a list of MultiTrees that belong to the page+container
     * 
     * @param htmlPage
     * @param container
     * @return
     */
    List<MultiTree> getMultiTrees(String htmlPage, String container) throws DotDataException;

    /**
     * Gets a list of MultiTrees that belong to the page+container+container instance
     * 
     * @param htmlPage
     * @param container
     * @param containerInstance
     * @return
     */
    List<MultiTree> getMultiTrees(IHTMLPage htmlPage, Container container, String containerInstance);

    /**
     * Gets a list of MultiTrees that belong to the container on any page
     * 
     * @param containerIdentifier
     * @return List Multitree
     */
    List<MultiTree> getContainerMultiTrees(String containerIdentifier) throws DotDataException;

    /**
     * gets all MultiTrees that have contentlet id as a child
     * 
     * @param contentIdentifier page Or Container
     * @throws DotDataException
     */
    List<MultiTree> getMultiTreesByChild(String contentIdentifier) throws DotDataException;

    /**
     * gets all MultiTrees that in a container of a particular content type
     * 
     * @param containerIdentifier
     * @param structureInode
     * @return
     */
    List<MultiTree> getContainerStructureMultiTree(String containerIdentifier, String structureInode);

    /**
     * gets the containers with MultiTree entries on a page (The containers may or may not exist)
     * 
     * @param pageId
     * @return
     * @throws DotDataException
     */
    List<String> getContainersId(String pageId) throws DotDataException;

    /**
     * Gets a specific MultiTree entry
     * 
     * @param htmlPage
     * @param container
     * @param childContent
     * @param containerInstance
     * @return
     * @throws DotDataException
     */
    MultiTree getMultiTree(Identifier htmlPage, Identifier container, Identifier childContent, String containerInstance)
            throws DotDataException;

    /**
     * Gets a specific MultiTree entry
     * 
     * @param htmlPage
     * @param container
     * @param childContent
     * @param containerInstance
     * @return
     * @throws DotDataException
     */
    MultiTree getMultiTree(String htmlPage, String container, String childContent, String containerInstance) throws DotDataException;

    /**
     * Gets a specific MultiTree entry
     *
     * @param htmlPage
     * @param container
     * @param childContent
     * @param containerInstance
     * @return
     * @throws DotDataException
     */
    MultiTree getMultiTree(String htmlPage, String container, String childContent, String containerInstance, String personalization, String variantId) throws DotDataException;

    /**
     * Gets a specific MultiTree entry
     *
     * @param htmlPage
     * @param container
     * @param childContent
     * @param containerInstance
     * @param personalization
     * @return MultiTree
     * @throws DotDataException
     */
    MultiTree getMultiTree(String htmlPage, String container, String childContent, String containerInstance, String personalization) throws DotDataException;

    /**
     * Gets a specific MultiTree entry regardless of containerInstance
     * 
     * @param htmlPage
     * @param container
     * @param childContent
     * @return
     * @throws DotDataException
     */
    MultiTree getMultiTree(String htmlPage, String container, String childContent) throws DotDataException;

    /**
     * Gets a multi-tree by html page, container, child content, container instance,
     * personalization, variant id, and style properties
     * @param htmlPage html page
     * @param container container
     * @param childContent child content
     * @param containerInstance container instance
     * @param personalization personalization
     * @param variantId variant id
     * @param styleProperties style properties
     * @return multi-tree
     * @throws DotDataException thrown if an error occurs while executing the task
     */
    MultiTree getMultiTree(String htmlPage, String container, String childContent,
            String containerInstance, String personalization, String variantId,
            String styleProperties) throws DotDataException;
    /**
     * Gets a list of MultiTrees that have the Identifier as a Parent
     * 
     * @param parent Container or Page
     * @return
     */
    List<MultiTree> getMultiTrees(Identifier parent) throws DotDataException;

    /**
     * Gets a list of MultiTrees that have the Identifiers as a Parent Page+Container
     * 
     * @param htmlPage
     * @param container
     * @return
     */
    List<MultiTree> getMultiTrees(Identifier htmlPage, Identifier container) throws DotDataException;

    /**
     * Gets a list of MultiTrees that has the parentId as a parent
     * 
     * @param parentId page id
     * @return
     */
    List<MultiTree> getMultiTrees(String parentId) throws DotDataException;

    /**
     * Gets a list of MultiTrees that has the parentId as a parent
     *
     * @param parentId
     * @param variantName
     * @return
     * @throws DotDataException
     */
    List<MultiTree> getMultiTreesByVariant(final String parentId, final String variantName) throws DotDataException;

    /**
     * Get an unique set of the personalization for a page
     * @param pageId String
     * @return unique Set of personalization values per the page
     */
    Set<String> getPersonalizationsForPage(final IHTMLPage page) throws DotDataException;

    /**
     * Get an unique set of the personalization for a page for a specific variant, this set include
     * all the Persona that has version on the page for the Specific Variant and the Default Variant.
     *
     * @param pageId String Page's Id
     * @return unique Set of personalization values per the page
     */
    Set<String> getPersonalizationsForPage(final IHTMLPage page, final String variantName) throws DotDataException;


    /**
     * Get an unique set of the personalization for a page
     * @param pageID
     * @return
     * @throws DotDataException
     */
    Set<String> getPersonalizationsForPage(String pageID) throws DotDataException;
    /**
     * Get all unique set of the personalization
     * @return unique Set of personalization values
     */
    Set<String> getPersonalizations () throws DotDataException;

    /**
     * Clean up all unused personalization (based on the personalizationFilter)
     * Returns the personalizations unused
     * @param personalizationFilter {@link Predicate}
     * @return Set
     * @throws DotDataException
     */
    Set<String> cleanUpUnusedPersonalization(final Predicate<String> personalizationFilter) throws DotDataException;

    /**
     * Take a set of containers with a based personalization and set to new personalization, for a page.
     * @param pageId String page id
     * @param basePersonalization String this personalization will use to get the containers and them apply a new personalization over a copy of the containers on the page.
     * @param newPersonalization String this is the new personalization for the set of containers
     * @return List MultiTree
     */
    List<MultiTree> copyPersonalizationForPage (String pageId, String basePersonalization,
            String newPersonalization, final String targetVariantName ) throws DotDataException;

    /**
     * Take a set of containers with a based personalization (the default one) and set to new personalization, for a page.
     * @param pageId String page id
     * @param newPersonalization String this is the new personalization for the set of containers
     * @return List MultiTree
     */
    default List<MultiTree> copyPersonalizationForPage (final String pageId, final String newPersonalization,
            final String targetVariantName)
                throws DotDataException {

        return this.copyPersonalizationForPage(pageId, MultiTree.DOT_PERSONALIZATION_DEFAULT,
                newPersonalization, targetVariantName);
    }

    /**
     * Deletes the personalization for the page
     * @param pageId {@link String} page id
     * @param personalization {@link String} personalization
     */
    void deletePersonalizationForPage(String pageId, String personalization, String variantName) throws DotDataException;

    /**
     * Overrides: removes the current multitrees by page + personalization and adds the multiTress
     * @param pageId {@link String}
     * @param personalization {@link String}
     * @param multiTrees {@link List}
     * @throws DotDataException
     */
    void overridesMultitreesByPersonalization(String pageId, String personalization, List<MultiTree> multiTrees)  throws DotDataException ;

    /**
     * Save a collection of {@link MultiTree} and link them with a page, Also delete all the
     * {@link MultiTree} linked previously with the page.
     *
     * @param pageId {@link String} Page's identifier
     * @param personalization {@link String} personalization token
     * @param multiTrees {@link List} of {@link MultiTree} to safe
     * @param languageIdOpt {@link Optional} {@link Long}  optional language, if present will deletes only the contentlets that have a version on this language.
     *                                      Since it is by identifier, when deleting for instance in spanish, will remove the english and any other lang version too.
     * @throws DotDataException
     */
    void overridesMultitreesByPersonalization(final String pageId,
            final String personalization,
            final List<MultiTree> multiTrees,
            final Optional<Long> languageIdOpt
    ) throws DotDataException;

    /**
     * Save a collection of {@link MultiTree} and link them with a page, Also delete all the
     * {@link MultiTree} linked previously with the page.
     *
     * @param pageId {@link String} Page's identifier
     * @param personalization {@link String} personalization token
     * @param multiTrees {@link List} of {@link MultiTree} to safe
     * @param languageIdOpt {@link Optional} {@link Long}  optional language, if present will deletes only the contentlets that have a version on this language.
     *                                      Since it is by identifier, when deleting for instance in spanish, will remove the english and any other lang version too.
     * @param variantId {@link com.dotcms.variant.model.Variant}'s id
     *
     * @throws DotDataException
     */
    void overridesMultitreesByPersonalization(final String pageId,
                                             final String personalization,
                                             final List<MultiTree> multiTrees,
                                             final Optional<Long> languageIdOpt,
                                             final String variantId
                                        ) throws DotDataException;

    /**
     * Copy a collection of {@link MultiTree} but to a different {@link Variant}.
     *
     * @param pageId {@link String} Page's identifier
     * @param multiTrees {@link List} of {@link MultiTree} to copy
     * @param variantName {@link String} name of the variant to copy to
     * @throws DotDataException
     */
    void copyMultiTree(final String pageId, final List<MultiTree> multiTrees,
            String variantName)
            throws DotDataException;

    /**
     * Updates the current personalization to a new personalization
     *
     * @param currentPersonalization {@link String}  current existing personalization
     * @param newPersonalization     {@link String}  new personalization to replace the current one
     */
    void updatePersonalization(String currentPersonalization, String newPersonalization) throws DotDataException;

    /**
     * Queries the database to return the number of Containers that include the specified Contentlet ID.
     * <p>The result provided by this method can be used to customize or determine specific behaviors. For example,
     * this piece of information is used by the dotCMS UI to ask the User whether they want to edit a Contentlet
     * referenced everywhere, or if dotCMS should create a copy of such a Contentlet so they can edit that one
     * version.</p>
     *
     * @param contentletId The Contentlet ID whose references will be retrieved.
     *
     * @return The number of times the specified Contentlet is added to a Container in any HTML Page.
     */
    int getAllContentletReferencesCount(final String contentletId) throws DotDataException;

    /**
     * Return all the {@link MultiTree} for a {@link Variant}
     *
     * @param variant
     * @return
     */
    List<MultiTree> getMultiTrees(final Variant variant) throws DotDataException;

    /**
     * After layout changes, this method updates the UUID (relation_type field) of a set of pages in a
     * MultiTree according to changes in the layout.
     *
     * For example, if you have a layout with one instance of a container,
     * the UUID for this container is initially set to 1. If later you add a second instance on top of the layout,
     * the UUID of the original instance will change from 1 to 2. Consequently, the MultiTree of the pages using
     * this layout needs to be updated as well.
     *
     * @param layoutChanges
     * @param pageIds
     * @throws DotDataException
     */
    void updateMultiTrees(final LayoutChanges layoutChanges, final Collection<String> pageIds, final String variantName)
            throws DotDataException;
}

package com.dotcms.util.transform;

import com.dotcms.contenttype.transform.relationship.DbRelationshipTransformer;
import com.dotcms.contenttype.transform.relationship.RelationshipTransformer;
import com.dotcms.variant.model.transform.VariantTransformer;
import com.dotcms.experiments.business.ExperimentTransformer;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.transform.IdentifierTransformer;
import com.dotmarketing.beans.transform.MultiTreeTransformer;
import com.dotmarketing.beans.transform.TreeTransformer;
import com.dotmarketing.business.ContentletVersionInfoTransformer;
import com.dotmarketing.business.transform.UserTransformer;
import com.dotmarketing.portlets.categories.business.CategoryTransformer;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.transform.ContainerTransformer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletTransformer;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.transform.TemplateTransformer;
import com.dotmarketing.portlets.hostvariable.transform.HostVariableTransformer;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.transform.LanguageTransformer;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.transform.LinkTransformer;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.folders.transform.FolderTransformer;
import com.dotmarketing.portlets.workflows.model.transform.WorkflowCommentTransformer;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Factory class used to instantiate DBTransformer objects
 * Default DBTransformer objects supported so far are: FolderTransformer, TemplateTransformer,
 * ContainerTransformer, LinkTransformer and IdentifierTransformer
 * However, new implementations can be included calling the addTransformer
 * method and removed calling removeTransformer.
 * @author nollymar
 */
public class TransformerLocator {

    private TransformerLocator() {

    }

    /**
     * Map that contains DbTransformer implementations
     */
    private static Map<Class, Function<List<Map<String, Object>>, DBTransformer>> transformerMapping = new ConcurrentHashMap<>();

    static {
        transformerMapping.put (Folder.class, TransformerLocator::createFolderTransformer);
        transformerMapping.put (Template.class, TransformerLocator::createTemplateTransformer);
        transformerMapping.put (Container.class, TransformerLocator::createContainerTransformer);
        transformerMapping.put (Link.class, TransformerLocator::createLinkTransformer);
        transformerMapping.put (MultiTree.class, TransformerLocator::createMultiTreeTransformer);
        transformerMapping.put (Identifier.class, TransformerLocator::createIdentifierTransformer);
        transformerMapping.put (Tree.class, TransformerLocator::createTreeTransformer);
        transformerMapping.put (Contentlet.class, TransformerLocator::createContentletTransformer);
        transformerMapping.put (Language.class, TransformerLocator::createLanguageTransformer);
        transformerMapping.put (Relationship.class, TransformerLocator::createRelationshipTransformer);
        transformerMapping.put (User.class, TransformerLocator::createUserTransformer);
    }

    public static DBTransformer createDBTransformer(List<Map<String, Object>> list, Class clazz) {
        DBTransformer transformer = null;

        if (transformerMapping.containsKey(clazz)){
            transformer = transformerMapping.get(clazz).apply(list);
        }
        return transformer;
    }

    /**
     * Extends default DBTransformer map
     * @param clazz Class which DB results will be transformed to
     * @param transformerFunction DBTransformer that contains the implementation
     */
    public static void addTransformer(Class clazz,
            Function<List<Map<String, Object>>, DBTransformer> transformerFunction) {
        transformerMapping.put(clazz, transformerFunction);
    }

    /**
     * Remove a DBTransformer implementation from the map given the class as a key
     * @param clazz
     */
    public static void removeTransformer(Class clazz){
        transformerMapping.remove(clazz);
    }

    /**
     * Creates a DBTransformer for Folder objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static FolderTransformer createFolderTransformer(List<Map<String, Object>> initList) {

        return new FolderTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Template objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static TemplateTransformer createTemplateTransformer(
            List<Map<String, Object>> initList) {

        return new TemplateTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Container objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static ContainerTransformer createContainerTransformer(
            List<Map<String, Object>> initList) {

        return new ContainerTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Contentlet objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static ContentletTransformer createContentletTransformer(
            List<Map<String, Object>> initList, final boolean ignoreStoryBlock) {

        return new ContentletTransformer(initList, ignoreStoryBlock);
    }

    public static ContentletTransformer createContentletTransformer(
            List<Map<String, Object>> initList) {

        return new ContentletTransformer(initList, false);
    }

    /**
     * Creates a DBTransformer for MultiTree objects
     * @param initList List of DB results to be transformed
     */
    public static MultiTreeTransformer createMultiTreeTransformer(
            List<Map<String, Object>> initList) {
        return new MultiTreeTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Link objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static LinkTransformer createLinkTransformer(List<Map<String, Object>> initList) {

        return new LinkTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Identifier objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static IdentifierTransformer createIdentifierTransformer(
            List<Map<String, Object>> initList) {

        return new IdentifierTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Language objects
     *
     * @param initList List of DB results to be transformed
     */
    public static LanguageTransformer createLanguageTransformer(
            List<Map<String, Object>> initList) {

        return new LanguageTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Tree objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static TreeTransformer createTreeTransformer(
        List<Map<String, Object>> initList) {

        return new TreeTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Relationship objects
     *
     * @param initList List of DB results to be transformed
     */
    public static RelationshipTransformer createRelationshipTransformer(
            List<Map<String, Object>> initList) {

        return new DbRelationshipTransformer(initList);
    }

    /**
     * Creates a DBTransformer for WorkflowComment objects
     * @param initList WorkflowComment of DB results to be transformed
     * @return
     */
    public static WorkflowCommentTransformer createWorkflowCommentTransformer(List<Map<String, Object>> initList) {

        return new WorkflowCommentTransformer(initList);
    }

    /**
     * Creates a DBTransformer for HostVariable objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static HostVariableTransformer createHostVariableTransformer(List<Map<String, Object>> initList) {

        return new HostVariableTransformer(initList);
    }

    /**
     * Creates a DBTransformer for {@link com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo} objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static ContentletVersionInfoTransformer createContentletVersionInfoTransformer(
            List<Map<String, Object>> initList) {
        return new ContentletVersionInfoTransformer(initList);
    }

    /**
     * Creates a DBTransformer for User objects
     *
     * @param initList List of DB results to be transformed
     */
    public static UserTransformer createUserTransformer(
            List<Map<String, Object>> initList) {

        return new UserTransformer(initList);
    }

    /**
     * Creates a Map to object Transformer for Categories
     * @param initList
     * @return
     */
    public static CategoryTransformer createCategoryTransformer(List<Map<String, Object>> initList){
        return new CategoryTransformer(initList);
    }

    /**
     * Creates a DBTransformer for {@link com.dotcms.variant.model.Variant} objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static VariantTransformer createVariantTransformer(
            List<Map<String, Object>> initList) {

        return new VariantTransformer(initList);
    }

    /**
     * Creates a DBTransformer for {@link com.dotcms.experiments.model.Experiment} objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static ExperimentTransformer createExperimentTransformer(
            List<Map<String, Object>> initList) {

        return new ExperimentTransformer(initList);
    }
}

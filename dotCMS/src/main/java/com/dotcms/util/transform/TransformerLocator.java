package com.dotcms.util.transform;

import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.transform.IdentifierTransformer;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.transform.ContainerTransformer;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.folders.transform.TemplateTransformer;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.transform.LinkTransformer;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.templates.transform.FolderTransformer;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Factory class used to instantiate DBTransformer objects
 * Default DBTransformer objects supported so far are: FolderTransformer, TemplateTransformer,
 * ContainerTransformer, LinkTransformer and IdentifierTransformer
 * However, custom implementations of default transformers are supported via dotmarketing-config.properties
 * setting these properties: FOLDER_TRANSFORMER_IMPLEMENTATION, TEMPLATE_TRANSFORMER_IMPLEMENTATION,
 * CONTAINER_TRANSFORMER_IMPLEMENTATION, LINK_TRANSFORMER_IMPLEMENTATION and IDENTIFIER_TRANSFORMER_IMPLEMENTATION
 *
 * Additionally, new implementations can be included calling the addTransformer
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
        transformerMapping.put (Identifier.class, TransformerLocator::createIdentifierTransformer);
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
        String transformerImplementation = Config
                .getStringProperty("FOLDER_TRANSFORMER_IMPLEMENTATION", null, false);
        if (transformerImplementation != null) {
            try {
                return (FolderTransformer) ReflectionUtils
                        .newInstance(Class.forName(transformerImplementation), initList);
            } catch (ClassNotFoundException e) {
                Logger.error(TransformerLocator.class, e.getMessage(), e);
            }
        }

        return new FolderTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Template objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static TemplateTransformer createTemplateTransformer(
            List<Map<String, Object>> initList) {
        String transformerImplementation = Config
                .getStringProperty("TEMPLATE_TRANSFORMER_IMPLEMENTATION", null, false);
        if (transformerImplementation != null) {
            try {
                return (TemplateTransformer) ReflectionUtils
                        .newInstance(Class.forName(transformerImplementation), initList);
            } catch (ClassNotFoundException e) {
                Logger.error(TransformerLocator.class, e.getMessage(), e);
            }
        }

        return new TemplateTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Container objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static ContainerTransformer createContainerTransformer(
            List<Map<String, Object>> initList) {
        String transformerImplementation = Config
                .getStringProperty("CONTAINER_TRANSFORMER_IMPLEMENTATION", null, false);
        if (transformerImplementation != null) {
            try {
                return (ContainerTransformer) ReflectionUtils
                        .newInstance(Class.forName(transformerImplementation), initList);
            } catch (ClassNotFoundException e) {
                Logger.error(TransformerLocator.class, e.getMessage(), e);
            }
        }

        return new ContainerTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Link objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static LinkTransformer createLinkTransformer(List<Map<String, Object>> initList) {
        String transformerImplementation = Config
                .getStringProperty("LINK_TRANSFORMER_IMPLEMENTATION", null, false);
        if (transformerImplementation != null) {
            try {
                return (LinkTransformer) ReflectionUtils
                        .newInstance(Class.forName(transformerImplementation), initList);
            } catch (ClassNotFoundException e) {
                Logger.error(TransformerLocator.class, e.getMessage(), e);
            }
        }

        return new LinkTransformer(initList);
    }

    /**
     * Creates a DBTransformer for Identifier objects
     * @param initList List of DB results to be transformed
     * @return
     */
    public static IdentifierTransformer createIdentifierTransformer(
            List<Map<String, Object>> initList) {
        String transformerImplementation = Config
                .getStringProperty("IDENTIFIER_TRANSFORMER_IMPLEMENTATION", null, false);
        if (transformerImplementation != null) {
            try {
                return (IdentifierTransformer) ReflectionUtils
                        .newInstance(Class.forName(transformerImplementation), initList);
            } catch (ClassNotFoundException e) {
                Logger.error(TransformerLocator.class, e.getMessage(), e);
            }
        }

        return new IdentifierTransformer(initList);
    }
}

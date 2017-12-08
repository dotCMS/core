package com.dotmarketing.util;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class used to map query results to POJO objects
 * 
 * @author nollymar
 */
public class ConvertToPOJOUtil {


    private static final String SYSPUBLISH_DATE = "syspublish_date";
    private static final String SYSEXPIRE_DATE = "sysexpire_date";
    private static final String TITLE = "title";
    private static final String MOD_DATE = "mod_date";
    private static final String MOD_USER = "mod_user";
    private static final String SORT_ORDER = "sort_order";
    private static final String IDENTIFIER = "identifier";
    private static final String SHOW_ON_MENU = "show_on_menu";
    private static final String FRIENDLY_NAME = "friendly_name";
    private static final String DRAWED = "drawed";
    private static final String MAX_CONTENTLETS = "max_contentlets";
    private static final String USE_DIV = "use_div";
    private static final String STATICIFY = "staticify";
    private static final String ADD_CONTAINER_LINKS = "add_container_links";
    private static final String CONTAINERS_ADDED = "containers_added";
    private static final String INODE = "inode";
    private static final String OWNER = "owner";
    private static final String IDATE = "idate";

    private ConvertToPOJOUtil() {

    }

    /**
     * Creates new instances of T given a List<Map> of DB results
     * 
     * @param results
     * @param classToUse
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> List<T> convertDotConnectMapToPOJO(List<Map<String, Object>> results, final Class classToUse)
            throws ParseException, IllegalAccessException, InvocationTargetException, InstantiationException,
            NoSuchMethodException, NoSuchFieldException {

        List<T> ret = null;

        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        if (Folder.class.equals(classToUse)) {
            ret = (List<T>) convertDotConnectMapToFolder(results);
        }

        if (Container.class.equals(classToUse)) {
            ret = (List<T>) convertDotConnectMapToContainer(results);
        }

        if (Link.class.equals(classToUse)) {
            ret = (List<T>) convertDotConnectMapToLink(results);
        }

        if (Identifier.class.equals(classToUse)) {
            ret = (List<T>) convertDotConnectMapToIdentifier(results);
        }

        if (Template.class.equals(classToUse)) {
            ret = (List<T>) convertDotConnectMapToTemplate(results);
        }


        return ret;
        


    }



    /**
     *
     * @param results
     * @return
     */
    public static List<Link> convertDotConnectMapToLink(final List<Map<String, Object>> results) throws ParseException {

        List<Link> ret = new ArrayList<>();

        if (results != null && !results.isEmpty()) {
            for (Map<String, Object> map : results) {
                ret.add(getLinkFields(map));
            }
        }

        return ret;
    }

    @NotNull
    private static Link getLinkFields(Map<String, Object> map) throws ParseException {
        final Link link = new Link();
        link.setInode((String) map.get(INODE));
        link.setOwner((String) map.get(OWNER));
        link.setIDate((Date) map.get(IDATE));
        link.setShowOnMenu((Boolean) map.getOrDefault(SHOW_ON_MENU,false));
        link.setTitle((String) map.get(TITLE));
        link.setModDate((Date) map.get(MOD_DATE));
        link.setModUser((String) map.get(MOD_USER));
        link.setSortOrder((Integer) map.getOrDefault(SORT_ORDER,0));
        link.setFriendlyName((String) map.get(FRIENDLY_NAME));
        link.setIdentifier((String) map.get(IDENTIFIER));
        link.setProtocal((String) map.get("protocal"));
        link.setUrl((String) map.get("url"));
        link.setTarget((String) map.get("target"));
        link.setInternalLinkIdentifier((String) map.get("internal_link_identifier"));
        link.setLinkType((String) map.get("link_type"));
        link.setLinkCode((String) map.get("link_code"));
        return link;
    }

    /**
     *
     * @param results
     * @return
     */
    public static List<Identifier> convertDotConnectMapToIdentifier(final List<Map<String, Object>> results)
            throws ParseException {
        final List<Identifier> ret = new ArrayList<>();
        if (results == null || results.isEmpty()) {
            return ret;
        }

        for (Map<String, Object> map : results) {
            final Identifier i = new Identifier();
            i.setAssetName((String) map.get("asset_name"));
            i.setAssetType((String) map.get("asset_type"));
            i.setHostId((String) map.get("host_inode"));
            i.setId((String) map.get("id"));
            i.setParentPath((String) map.get("parent_path"));
            i.setSysPublishDate((Date) map.get(SYSPUBLISH_DATE));
            i.setSysExpireDate((Date) map.get(SYSEXPIRE_DATE));
            ret.add(i);
        }
        return ret;
    }

    public static List<Folder> convertDotConnectMapToFolder(final List<Map<String, Object>> results) throws ParseException {

        List<Folder> ret = new ArrayList<>();

        if (results != null && !results.isEmpty()) {
            for (Map<String, Object> map : results) {
                ret.add(getFolderFields(map));
            }
        }
        return ret;
    }

    @NotNull
    private static Folder getFolderFields(Map<String, Object> map) throws ParseException {
        Folder folder;
        folder = new Folder();
        folder.setInode((String) map.get(INODE));
        folder.setOwner((String) map.get(OWNER));
        folder.setIDate((Date) map.get(IDATE));
        folder.setName((String) map.get("name"));
        folder.setTitle((String) map.get(TITLE));
        folder.setShowOnMenu((Boolean) map.getOrDefault(SHOW_ON_MENU,false));
        folder.setSortOrder((Integer) map.getOrDefault(SORT_ORDER,0));
        folder.setFilesMasks((String) map.get("files_masks"));
        folder.setIdentifier((String) map.get(IDENTIFIER));
        folder.setDefaultFileType((String) map.get("default_file_type"));
        folder.setModDate((Date) map.get(MOD_DATE));
        return folder;
    }

    public static List<Container> convertDotConnectMapToContainer(final List<Map<String, Object>> results) throws ParseException {

        List<Container> ret = new ArrayList<>();

        if (results != null && !results.isEmpty()) {
            for (Map<String, Object> map : results) {
                ret.add(getContainerFields(map));
            }
        }
        return ret;
    }

    @NotNull
    private static Container getContainerFields(Map<String, Object> map) throws ParseException {
        Container container;
        container = new Container();
        container.setInode((String) map.get(INODE));
        container.setOwner((String) map.get(OWNER));
        container.setIDate((Date) map.get(IDATE));
        container.setCode((String) map.get("code"));
        container.setPreLoop((String) map.get("pre_loop"));
        container.setPostLoop((String) map.get("post_loop"));
        container.setShowOnMenu((Boolean) map.getOrDefault(SHOW_ON_MENU,false));
        container.setTitle((String) map.get(TITLE));
        container.setModDate((Date) map.get(MOD_DATE));
        container.setModUser((String) map.get(MOD_USER));
        container.setSortOrder((Integer) map.getOrDefault(SORT_ORDER,0));
        container.setFriendlyName((String) map.get(FRIENDLY_NAME));
        container.setMaxContentlets((Integer) map.getOrDefault(MAX_CONTENTLETS,0));
        container.setUseDiv((Boolean) map.getOrDefault(USE_DIV,false));
        container.setStaticify((Boolean) map.getOrDefault(STATICIFY,false));
        container.setSortContentletsBy((String) map.get("sort_contentlets_by"));
        container.setLuceneQuery((String) map.get("lucene_query"));
        container.setNotes((String) map.get("notes"));
        container.setIdentifier((String) map.get(IDENTIFIER));
        return container;
    }

    public static List<Template> convertDotConnectMapToTemplate(final List<Map<String, Object>> results) throws ParseException {

        List<Template> ret = new ArrayList<>();

        if (results != null && !results.isEmpty()) {
            for (Map<String, Object> map : results) {
                ret.add(getTemplateFields(map));
            }
        }
        return ret;
    }

    @NotNull
    private static Template getTemplateFields(Map<String, Object> map) throws ParseException {
        Template template;
        template = new Template();
        template.setInode(String.valueOf(map.get(INODE)));
        template.setOwner(String.valueOf(map.get(OWNER)));
        template.setIDate((Date) map.get(IDATE));
        template.setShowOnMenu((Boolean) map.getOrDefault(SHOW_ON_MENU,false));
        template.setTitle(String.valueOf(map.get(TITLE)));
        template.setModDate((Date) map.get(MOD_DATE));
        template.setModUser(String.valueOf(map.get(MOD_USER)));
        template.setSortOrder((Integer) map.getOrDefault(SORT_ORDER,0));
        template.setFriendlyName(String.valueOf(map.get(FRIENDLY_NAME)));
        template.setBody(String.valueOf(map.get("body")));
        template.setHeader(String.valueOf(map.get("header")));
        template.setFooter(String.valueOf(map.get("footer")));
        template.setImage(String.valueOf(map.get("image")));
        template.setIdentifier(String.valueOf(map.get(IDENTIFIER)));
        template.setDrawed((Boolean) map.get(DRAWED));
        template.setDrawedBody((String) map.get("drawed_body"));
        template.setCountAddContainer((Integer) map.getOrDefault(ADD_CONTAINER_LINKS,0));
        template.setCountContainers((Integer) map.getOrDefault(CONTAINERS_ADDED,0));
        template.setHeadCode((String) map.get("head_code"));
        template.setTheme((String) map.get("theme"));
        return template;
    }

}

package com.dotmarketing.util;

import com.dotcms.repackage.org.apache.commons.beanutils.PropertyUtils;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.google.common.base.CaseFormat;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class used to map query results to POJO objects
 * @author nollymar
 */
public class ConvertToPOJOUtil {

    public static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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

    private ConvertToPOJOUtil(){

    }

    /**
     * Creates new instances of T given a List<Map> of DB results
     * @param results
     * @param classToUse
     * @param <T>
     * @return
     * @throws Exception
     */
    public static<T> List<T> convertDotConnectMapToPOJO(List<Map<String,String>> results,
            final Class classToUse) throws ParseException, IllegalAccessException,
            InvocationTargetException, InstantiationException, NoSuchMethodException, NoSuchFieldException {

        List<T> ret = null;

        if(results == null || results.isEmpty()){
            return Collections.emptyList();
        }

        if (Folder.class.equals(classToUse)){
            ret = (List<T>) convertDotConnectMapToFolder(results);
        }

        if (Container.class.equals(classToUse)){
            ret = (List<T>) convertDotConnectMapToContainer(results);
        }

        if (Link.class.equals(classToUse)){
            ret = (List<T>) convertDotConnectMapToLink(results);
        }

        if (Identifier.class.equals(classToUse)){
            ret = (List<T>) convertDotConnectMapToIdentifier(results);
        }

        if (Template.class.equals(classToUse)){
            ret = (List<T>) convertDotConnectMapToTemplate(results);
        }

        if (ret != null){
            return ret;
        }

        return getMapFields(results, classToUse);
    }

    @NotNull
    private static <T> List<T> getMapFields(List<Map<String, String>> results, Class classToUse)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, ParseException {
        List<T> ret;
        Map<String, String> properties;
        ret = new ArrayList<>();

        for (final Map<String, String> map : results) {
            Constructor<?> ctor = classToUse.getConstructor();
            final T object = (T) ctor.newInstance();

            properties = map.entrySet().stream().collect(Collectors
                    .toMap(entry -> CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, entry.getKey()), entry ->map.get(entry.getKey())));

            for (final Entry entry: properties.entrySet()){
                String property = (String) entry.getKey();
                if (properties.get(property) != null){
                    if (isFieldPresent(classToUse, String.class, property)){
                        PropertyUtils.setProperty(object, property, properties.get(property));
                    }else if (isFieldPresent(classToUse, Integer.TYPE, property)){
                        PropertyUtils.setProperty(object, property, Integer.parseInt(properties.get(property)));
                    }else if (isFieldPresent(classToUse, Boolean.TYPE, property)){
                        PropertyUtils.setProperty(object, property, Boolean.parseBoolean(properties.get(property)));
                    }else if (isFieldPresent(classToUse, Date.class, property)){
                        PropertyUtils.setProperty(object, property, df.parse(properties.get(property)));
                    }else{
                        Logger.warn(classToUse, "Property " + property + "not set for " + classToUse.getName());
                    }
                }
            }

            ret.add(object);
        }
        return ret;
    }

    /**
     * Searches a property recursively through classes and superclasses
     * @param classToUse
     * @param fieldType
     * @param property
     * @return
     * @throws NoSuchFieldException
     */
    private static boolean isFieldPresent(final Class classToUse, Class fieldType, String property)
            throws NoSuchFieldException {

        try{
            return classToUse.getDeclaredField(property).getType() == fieldType;
        }catch(NoSuchFieldException e){
            if (classToUse.getSuperclass()!=null) {
                return isFieldPresent(classToUse.getSuperclass(), fieldType, property);
            }
            Logger.debug(e, e.getMessage());
        }
        return false;
    }

    /**
     *
     * @param results
     * @return
     */
    public static List<Link> convertDotConnectMapToLink(final List<Map<String,String>> results)
            throws ParseException {

        List<Link> ret = new ArrayList<>();

        if(results != null && !results.isEmpty()){
            for (Map<String, String> map : results) {
                ret.add(getLinkFields(map));
            }
        }

        return ret;
    }

    @NotNull
    private static Link getLinkFields(Map<String, String> map) throws ParseException {
        final Link link = new Link();
        link.setInode(map.get(INODE));
        link.setOwner(map.get(OWNER));

        if (map.get(IDATE) != null && !map.get(IDATE).isEmpty()) {
            link.setIDate(map.get(IDATE));
        }

        if (map.get(SHOW_ON_MENU) != null && !map.get(SHOW_ON_MENU).isEmpty()){
            link.setShowOnMenu(Boolean.parseBoolean(map.get(SHOW_ON_MENU)));
        }

        link.setTitle(map.get(TITLE));

        if (map.get(MOD_DATE) != null && !map.get(MOD_DATE).isEmpty()){
            link.setModDate(df.parse(map.get(MOD_DATE)));
        }

        link.setModUser(map.get(MOD_USER));

        if (map.get(SORT_ORDER) != null && !map.get(SORT_ORDER).isEmpty()){
            link.setSortOrder(Integer.parseInt(map.get(SORT_ORDER)));
        }

        link.setFriendlyName(map.get(FRIENDLY_NAME));

        link.setIdentifier(map.get(IDENTIFIER));

        link.setProtocal(map.get("protocal"));

        link.setUrl(map.get("url"));

        link.setTarget(map.get("target"));

        link.setInternalLinkIdentifier(map.get("internal_link_identifier"));

        link.setLinkType(map.get("link_type"));

        link.setLinkCode(map.get("link_code"));
        return link;
    }

    /**
     *
     * @param results
     * @return
     */
    public static List<Identifier> convertDotConnectMapToIdentifier(final List<Map<String,String>> results)
            throws ParseException {
        final List<Identifier> ret = new ArrayList<>();
        if(results == null || results.isEmpty()){
            return ret;
        }

        for (Map<String, String> map : results) {
            final Identifier i = new Identifier();
            i.setAssetName(map.get("asset_name"));
            i.setAssetType(map.get("asset_type"));
            i.setHostId(map.get("host_inode"));
            i.setId(map.get("id"));
            i.setParentPath(map.get("parent_path"));

            if (map.get(SYSPUBLISH_DATE) != null && !map.get(SYSPUBLISH_DATE).isEmpty()){
                i.setSysPublishDate(df.parse(map.get(SYSPUBLISH_DATE) ));
            }

            if (map.get(SYSEXPIRE_DATE) != null && !map.get(SYSEXPIRE_DATE).isEmpty()){
                i.setSysExpireDate(df.parse(map.get(SYSEXPIRE_DATE) ));
            }

            ret.add(i);
        }
        return ret;
    }

    public static List<Folder> convertDotConnectMapToFolder(final List<Map<String,String>> results)
            throws ParseException {

        List<Folder> ret = new ArrayList<>();

        if(results != null && !results.isEmpty()){
            for (Map<String, String> map : results) {
                ret.add(getFolderFields(map));
            }
        }
        return ret;
    }

    @NotNull
    private static Folder getFolderFields(Map<String, String> map) throws ParseException {
        Folder folder;
        folder = new Folder();
        folder.setInode(map.get(INODE));
        folder.setOwner(map.get(OWNER));

        if (map.get(IDATE) != null && !map.get(IDATE).isEmpty()) {
            folder.setIDate(map.get(IDATE));
        }


        folder.setName(map.get("name"));
        folder.setTitle(map.get(TITLE));

        if (map.get(SHOW_ON_MENU) != null && !map.get(SHOW_ON_MENU).isEmpty()) {
            folder.setShowOnMenu(Boolean.parseBoolean(map.get(SHOW_ON_MENU)));
        }

        if (map.get(SORT_ORDER) != null && !map.get(SORT_ORDER).isEmpty()){
            folder.setSortOrder(Integer.parseInt(map.get(SORT_ORDER)));
        }

        folder.setFilesMasks(map.get("files_masks"));

        folder.setIdentifier(map.get(IDENTIFIER));

        folder.setDefaultFileType(map.get("default_file_type"));

        if (map.get(MOD_DATE) != null && !map.get(MOD_DATE).isEmpty()){
            folder.setModDate(df.parse(map.get(MOD_DATE)));
        }
        return folder;
    }

    public static List<Container> convertDotConnectMapToContainer(final List<Map<String,String>> results)
            throws ParseException {

        List<Container> ret = new ArrayList<>();

        if(results != null && !results.isEmpty()){
            for (Map<String, String> map : results) {
                ret.add(getContainerFields(map));
            }
        }
        return ret;
    }

    @NotNull
    private static Container getContainerFields(Map<String, String> map) throws ParseException {
        Container container;
        container = new Container();
        container.setInode(map.get(INODE));
        container.setOwner(map.get(OWNER));

        if (map.get(IDATE) != null && !map.get(IDATE).isEmpty()) {
            container.setIDate(map.get(IDATE));
        }


        container.setCode(map.get("code"));
        container.setPreLoop(map.get("pre_loop"));
        container.setPostLoop(map.get("post_loop"));
        if (map.get(SHOW_ON_MENU) != null && !map.get(SHOW_ON_MENU).isEmpty()) {
            container.setShowOnMenu(Boolean.parseBoolean(map.get(SHOW_ON_MENU)));
        }

        container.setTitle(map.get(TITLE));

        if (map.get(MOD_DATE) != null && !map.get(MOD_DATE).isEmpty()){
            container.setModDate(df.parse(map.get(MOD_DATE)));
        }

        container.setModUser(map.get(MOD_USER));

        if (map.get(SORT_ORDER) != null && !map.get(SORT_ORDER).isEmpty()){
            container.setSortOrder(Integer.parseInt(map.get(SORT_ORDER)));
        }

        container.setFriendlyName(map.get(FRIENDLY_NAME));

        if (map.get(MAX_CONTENTLETS) != null && !map.get(MAX_CONTENTLETS).isEmpty()){
            container.setMaxContentlets(Integer.parseInt(map.get(MAX_CONTENTLETS)));
        }

        if (map.get(USE_DIV) != null && !map.get(USE_DIV).isEmpty()) {
            container.setUseDiv(Boolean.parseBoolean(map.get(USE_DIV)));
        }

        if (map.get(STATICIFY) != null && !map.get(STATICIFY).isEmpty()) {
            container.setStaticify(Boolean.parseBoolean(map.get(STATICIFY)));
        }

        container.setSortContentletsBy(map.get("sort_contentlets_by"));

        container.setLuceneQuery(map.get("lucene_query"));

        container.setNotes(map.get("notes"));

        container.setIdentifier(map.get(IDENTIFIER));
        return container;
    }

    public static List<Template> convertDotConnectMapToTemplate(final List<Map<String,String>> results)
            throws ParseException {

        List<Template> ret = new ArrayList<>();

        if(results != null && !results.isEmpty()){
            for (Map<String, String> map : results) {
                ret.add(getTemplateFields(map));
            }
        }
        return ret;
    }

    @NotNull
    private static Template getTemplateFields(Map<String, String> map) throws ParseException {
        Template template;
        template = new Template();
        template.setInode(map.get(INODE));
        template.setOwner(map.get(OWNER));

        if (map.get(IDATE) != null && !map.get(IDATE).isEmpty()) {
            template.setIDate(map.get(IDATE));
        }

        if (map.get(SHOW_ON_MENU) != null && !map.get(SHOW_ON_MENU).isEmpty()) {
            template.setShowOnMenu(Boolean.parseBoolean(map.get(SHOW_ON_MENU)));
        }

        template.setTitle(map.get(TITLE));

        if (map.get(MOD_DATE) != null && !map.get(MOD_DATE).isEmpty()){
            template.setModDate(df.parse(map.get(MOD_DATE)));
        }

        template.setModUser(map.get(MOD_USER));

        if (map.get(SORT_ORDER) != null && !map.get(SORT_ORDER).isEmpty()){
            template.setSortOrder(Integer.parseInt(map.get(SORT_ORDER)));
        }

        template.setFriendlyName(map.get(FRIENDLY_NAME));

        template.setBody(map.get("body"));
        template.setHeader(map.get("header"));
        template.setFooter(map.get("footer"));
        template.setImage(map.get("image"));
        template.setIdentifier(map.get(IDENTIFIER));

        if (map.get(DRAWED) != null && !map.get(DRAWED).isEmpty()) {
            template.setDrawed(Boolean.parseBoolean(map.get(DRAWED)));
        }

        template.setDrawedBody(map.get("drawed_body"));

        if (map.get(ADD_CONTAINER_LINKS) != null && !map.get(ADD_CONTAINER_LINKS).isEmpty()){
            template.setCountAddContainer(Integer.parseInt(map.get(ADD_CONTAINER_LINKS)));
        }

        if (map.get(CONTAINERS_ADDED) != null && !map.get(CONTAINERS_ADDED).isEmpty()){
            template.setCountContainers(Integer.parseInt(map.get(CONTAINERS_ADDED)));
        }

        template.setHeadCode(map.get("head_code"));

        template.setTheme(map.get("theme"));
        return template;
    }

}

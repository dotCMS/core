package com.dotmarketing.util;

import com.dotcms.repackage.org.apache.commons.beanutils.PropertyUtils;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.google.common.base.CaseFormat;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class used to map query results to POJO objects
 * @author nollymar
 */
public class ConvertToPOJOUtil {

    /**
     * Creates new instances of T given a List<Map> of DB results
     * @param results
     * @param classToUse
     * @param <T>
     * @return
     * @throws Exception
     */
    public static<T> List<T> convertDotConnectMapToPOJO(List<Map<String,String>> results, Class classToUse)
            throws Exception {

        DateFormat df;
        List<T> ret;
        Map<String, String> properties;

        ret = new ArrayList<>();

        if(results == null || results.size()==0){
            return ret;
        }

        df = new SimpleDateFormat("yyyy-MM-dd");

        for (Map<String, String> map : results) {
            Constructor<?> ctor = classToUse.getConstructor();
            T object = (T) ctor.newInstance();

            properties = map.keySet().stream().collect(Collectors
                    .toMap(key -> CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key), key ->map.get(key)));

            for (String property: properties.keySet()){
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
    private static boolean isFieldPresent(Class classToUse, Class fieldType, String property)
            throws NoSuchFieldException {

        try{
            return classToUse.getDeclaredField(property).getType() == fieldType;
        }catch(NoSuchFieldException e){
            if (classToUse.getSuperclass()!=null) {
                return isFieldPresent(classToUse.getSuperclass(), fieldType, property);
            }
        }
        return false;
    }

    /**
     *
     * @param results
     * @return
     */
    public static List<Link> convertDotConnectMapToLink(List<Map<String,String>> results)
            throws ParseException {

        DateFormat df;
        List<Link> ret;


        df  = new SimpleDateFormat("yyyy-MM-dd");
        ret = new ArrayList<>();

        if(results != null && !results.isEmpty()){
            for (Map<String, String> map : results) {
                Link link = new Link();
                link.setInode(map.get("inode"));

                if (map.get("show_on_menu") != null && !map.get("show_on_menu").isEmpty()){
                    link.setShowOnMenu(Boolean.parseBoolean(map.get("show_on_menu")));
                }

                link.setTitle(map.get("title"));

                if (map.get("mod_date") != null && !map.get("mod_date").isEmpty()){
                    link.setModDate(df.parse(map.get("mod_date")));
                }

                link.setModUser(map.get("mod_user"));

                if (map.get("sort_order") != null && !map.get("sort_order").isEmpty()){
                    link.setSortOrder(Integer.parseInt(map.get("sort_order")));
                }

                link.setFriendlyName(map.get("friendly_name"));

                link.setIdentifier(map.get("identifier"));

                link.setProtocal(map.get("protocal"));

                link.setUrl(map.get("url"));

                link.setTarget(map.get("target"));

                link.setInternalLinkIdentifier(map.get("internal_link_identifier"));

                link.setLinkType(map.get("link_type"));

                link.setLinkCode(map.get("link_code"));

                ret.add(link);
            }
        }


        return ret;
    }

    /**
     *
     * @param results
     * @return
     */
    public static List<Identifier> convertDotConnectMapToIdentifier(List<Map<String,String>> results){
        List<Identifier> ret = new ArrayList<>();
        if(results == null || results.size()==0){
            return ret;
        }

        for (Map<String, String> map : results) {
            Identifier i = new Identifier();
            i.setAssetName(map.get("asset_name"));
            i.setAssetType(map.get("asset_type"));
            i.setHostId(map.get("host_inode"));
            i.setId(map.get("id"));
            i.setParentPath(map.get("parent_path"));
            ret.add(i);
        }
        return ret;
    }

    public static List<Folder> convertDotConnectMapToFolder(List<Map<String,String>> results)
            throws ParseException {

        Folder folder;
        DateFormat df;
        List<Folder> ret;

        df  = new SimpleDateFormat("yyyy-MM-dd");
        ret = new ArrayList<>();

        if(results != null && !results.isEmpty()){
            for (Map<String, String> map : results) {
                folder = new Folder();
                folder.setInode(map.get("inode"));
                folder.setName(map.get("name"));
                folder.setTitle(map.get("title"));

                if (map.get("show_on_menu") != null && !map.get("show_on_menu").isEmpty()) {
                    folder.setShowOnMenu(Boolean.parseBoolean(map.get("show_on_menu")));
                }

                if (map.get("sort_order") != null && !map.get("sort_order").isEmpty()){
                    folder.setSortOrder(Integer.parseInt(map.get("sort_order")));
                }

                folder.setFilesMasks(map.get("files_masks"));

                folder.setIdentifier(map.get("identifier"));

                folder.setDefaultFileType(map.get("default_file_type"));

                if (map.get("mod_date") != null && !map.get("mod_date").isEmpty()){
                    folder.setModDate(df.parse(map.get("mod_date")));
                }

                ret.add(folder);
            }
        }
        return ret;
    }

    public static List<Container> convertDotConnectMapToContainer(List<Map<String,String>> results)
            throws ParseException {
        Container container;
        DateFormat df;
        List<Container> ret;

        df  = new SimpleDateFormat("yyyy-MM-dd");
        ret = new ArrayList<>();

        if(results != null && !results.isEmpty()){
            for (Map<String, String> map : results) {
                container = new Container();
                container.setInode(map.get("inode"));
                container.setCode(map.get("code"));
                container.setPreLoop(map.get("pre_loop"));
                container.setPostLoop(map.get("post_loop"));
                if (map.get("show_on_menu") != null && !map.get("show_on_menu").isEmpty()) {
                    container.setShowOnMenu(Boolean.parseBoolean(map.get("show_on_menu")));
                }

                container.setTitle(map.get("title"));

                if (map.get("mod_date") != null && !map.get("mod_date").isEmpty()){
                    container.setModDate(df.parse(map.get("mod_date")));
                }

                container.setModUser(map.get("mod_user"));

                if (map.get("sort_order") != null && !map.get("sort_order").isEmpty()){
                    container.setSortOrder(Integer.parseInt(map.get("sort_order")));
                }

                container.setFriendlyName(map.get("friendly_name"));

                if (map.get("max_contentlets") != null && !map.get("max_contentlets").isEmpty()){
                    container.setMaxContentlets(Integer.parseInt(map.get("max_contentlets")));
                }

                if (map.get("use_div") != null && !map.get("use_div").isEmpty()) {
                    container.setUseDiv(Boolean.parseBoolean(map.get("use_div")));
                }

                if (map.get("staticify") != null && !map.get("staticify").isEmpty()) {
                    container.setStaticify(Boolean.parseBoolean(map.get("staticify")));
                }

                container.setSortContentletsBy(map.get("sort_contentlets_by"));

                container.setLuceneQuery(map.get("lucene_query"));

                container.setNotes(map.get("notes"));

                container.setIdentifier(map.get("identifier"));

                ret.add(container);
            }
        }
        return ret;
    }

    public static List<Template> convertDotConnectMapToTemplate(List<Map<String,String>> results)
            throws ParseException {
        Template template;
        DateFormat df;
        List<Template> ret;

        df  = new SimpleDateFormat("yyyy-MM-dd");
        ret = new ArrayList<>();

        if(results != null && !results.isEmpty()){
            for (Map<String, String> map : results) {
                template = new Template();
                template.setInode(map.get("inode"));

                if (map.get("show_on_menu") != null && !map.get("show_on_menu").isEmpty()) {
                    template.setShowOnMenu(Boolean.parseBoolean(map.get("show_on_menu")));
                }

                template.setTitle(map.get("title"));

                if (map.get("mod_date") != null && !map.get("mod_date").isEmpty()){
                    template.setModDate(df.parse(map.get("mod_date")));
                }

                template.setModUser(map.get("mod_user"));

                if (map.get("sort_order") != null && !map.get("sort_order").isEmpty()){
                    template.setSortOrder(Integer.parseInt(map.get("sort_order")));
                }

                template.setFriendlyName(map.get("friendly_name"));

                template.setBody(map.get("body"));
                template.setHeader(map.get("header"));
                template.setFooter(map.get("footer"));
                template.setImage(map.get("image"));
                template.setIdentifier(map.get("identifier"));

                if (map.get("drawed") != null && !map.get("drawed").isEmpty()) {
                    template.setDrawed(Boolean.parseBoolean(map.get("drawed")));
                }

                template.setDrawedBody(map.get("drawed_body"));

                if (map.get("add_container_links") != null && !map.get("add_container_links").isEmpty()){
                    template.setCountAddContainer(Integer.parseInt(map.get("add_container_links")));
                }

                if (map.get("containers_added") != null && !map.get("containers_added").isEmpty()){
                    template.setCountContainers(Integer.parseInt(map.get("containers_added")));
                }

                template.setHeadCode(map.get("head_code"));

                template.setTheme(map.get("theme"));

                ret.add(template);
            }
        }
        return ret;
    }

}

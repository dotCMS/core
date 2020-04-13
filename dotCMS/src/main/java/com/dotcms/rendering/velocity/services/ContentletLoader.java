package com.dotcms.rendering.velocity.services;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.comparators.ContentComparator;
import com.dotmarketing.comparators.WebAssetSortOrderComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import org.apache.velocity.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author will
 */
public class ContentletLoader implements DotLoader {

    private CategoryAPI categoryAPI = APILocator.getCategoryAPI();

    public CategoryAPI getCategoryAPI() {
        return categoryAPI;
    }

    public void setCategoryAPI(CategoryAPI categoryAPI) {
        this.categoryAPI = categoryAPI;
    }

    private final long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();


    public InputStream buildVelocity(Contentlet content, PageMode mode, String filePath)
            throws DotDataException, DotSecurityException {
        StringBuilder sb = new StringBuilder();

        ContentletAPI conAPI = APILocator.getContentletAPI();

        User systemUser = sysUser();

        // let's write this puppy out to our file



        // CONTENTLET CONTROLS BEGIN
        sb.append("#if($EDIT_MODE)");
        sb.append("#set( $EDIT_CONTENT_PERMISSION=$EDIT_CONTENT_PERMISSION").append(content.getIdentifier()).append(")");
        sb.append("#end");

        sb.append("#set($CONTENT_INODE='")
            .append(content.getInode())
            .append("' )")
            .append("#set($IDENTIFIER_INODE='")
            .append(content.getIdentifier())
            .append("' )");

        sb.append("#set($CONTENT_TYPE='").append(content.getContentType().variable()).append("' )");

        final BaseContentType baseType = content.get("formId") == null ? content.getContentType().baseType() : BaseContentType.FORM;
        sb.append("#set($CONTENT_BASE_TYPE='").append(baseType).append("' )");
        final String contentTypeId = content.get("formId") == null ? content.getContentType().id() : content.get("formId").toString();
        sb.append("#set($CONTENT_TYPE_ID='").append(contentTypeId).append("' )");
        sb.append("#set($CONTENT_LANGUAGE='").append(content.getLanguageId()).append("' )");


        // set all properties from the contentlet
        sb.append("#set($ContentInode='")
            .append(content.getInode())
            .append("' )")
            .append("#set($ContentIdentifier='")
            .append(content.getIdentifier())
            .append("' )")
            .append("#set($ContentletTitle=\"")
            .append(UtilMethods.espaceForVelocity(conAPI.getName(content, APILocator.getUserAPI().getSystemUser(), true)))
            .append("\" )");
        String modDateStr = UtilMethods.dateToHTMLDate((Date) content.getModDate(), "yyyy-MM-dd H:mm:ss");
        sb.append("#set($ContentLastModDate= $date.toDate(\"yyyy-MM-dd H:mm:ss\", \"")
            .append(modDateStr)
            .append("\"))")
            .append("#set($ContentLastModUserId= \"")
            .append(content.getModUser())
            .append("\")");
        if (content.getOwner() != null)
            sb.append("#set($ContentOwnerId= \"").append(content.getOwner()).append("\")");

        // Structure fields

        ContentType type = content.getContentType();
        sb.append("#set($structureName='").append(type.name()).append("' )");

        /*
        It is better if we calculate if it is a Widget before to try to evaluate
        the Widget code, if the code evaluation fails the $isWidget could be false.
         */
        if (type.baseType() == BaseContentType.WIDGET) {
            sb.append("#set( $isWidget= \"").append(true).append("\")");
            if (type.name().equals(FormAPI.FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME)) {
                sb.append("#set($isFormWidget= \"").append(true).append("\")");
            } else {
                sb.append("#set($isFormWidget= \"").append(false).append("\")");
            }

            //Cleaning up already loaded Widgets code
            sb.append("#set($widgetCode= \"\")");
        } else {
            sb.append("#set($isWidget= \"").append(false).append("\")");
        }


        List<Field> fields = type.fields();
        String widgetCode = "";
        for (Field field : fields) {

            String contField = field.dbColumn();
            String contFieldValue = null;
            Object contFieldValueObject = null;

            if (field instanceof HiddenField || field instanceof ConstantField) {
              String velPath = new VelocityResourceKey(field, Optional.empty(), mode).path ;
                if (field.variable().equals("widgetPreexecute")) {
                    continue;
                }
                if (field.variable().equals("widgetCode")) {
                    widgetCode = "#set($" + field.variable() + "=$velutil.mergeTemplate(\"" +   velPath + "\"))";
                    continue;
                } else {
                    String fieldValues = field.values() == null ? "" : field.values();
                    if (fieldValues.contains("$") || fieldValues.contains("#")) {
                        sb.append("#set($")
                                .append(field.variable())
                                .append("= $velutil.mergeTemplate(\"")
                                .append(velPath)
  
                                .append("\"))");
                    } else {
                        sb.append("#set($")
                                .append(field.variable())
                                .append("= \"")
                                .append(UtilMethods.espaceForVelocity(fieldValues).trim())
                                .append("\")");
                    }
                    continue;
                }
            }


            if (UtilMethods.isSet(contField)) {
                try {
                    contFieldValueObject = conAPI.getFieldValue(content, field);
                    contFieldValue = contFieldValueObject == null ? "" : contFieldValueObject.toString();
                } catch (final Exception e) {
                  try {
                    Logger.warnAndDebug(this.getClass(), "writeContentletToFile error: "+ e.getMessage() , e);
                    Logger.warn(this.getClass(), "writeContentletToFile error: "+ e.getStackTrace()[1].toString());
                    Logger.warn(this.getClass(), "writeContentletToFile error: values:" + content.getIdentifier() +  "/" + content.getTitle() + " : " + contField + " field:" + field + " contFieldValueObject:" + contFieldValueObject + " "  );
                  }
                  catch(Exception ex) {
                    Logger.error(this.getClass(), e.getMessage(), e);
                  }
                    continue;
                }
                if (!(field instanceof DateTimeField || field instanceof DateField || field instanceof TimeField)) {
                    if (contFieldValue.contains("$") || contFieldValue.contains("#")) {
                        sb.append("#set($")
                                .append(field.variable())
                                .append("=$velutil.mergeTemplate(\"")
                                .append(new VelocityResourceKey(field, Optional.ofNullable(content), mode).path)
                                .append("\"))");
                    } else {
                        sb.append("#set($")
                                .append(field.variable())
                                .append("=\"")
                                .append(UtilMethods.espaceForVelocity(contFieldValue).trim())
                                .append("\")");
                    }
                }

            }

            if (field instanceof ImageField || field instanceof FileField) {
                String identifierValue = content.getStringProperty(field.variable());
                if (InodeUtils.isSet(identifierValue)) {
                    sb.append("#set($")
                            .append(field.variable())
                            .append("Object= $filetool.getFile('")
                            .append(identifierValue)
                            .append("'," + mode.showLive + ",")
                            .append(content.getLanguageId())
                            .append(" ))");

                    if (field instanceof ImageField) {
                        sb.append("#set($").append(field.variable()).append("ImageInode=$!{").append(field.variable()).append(
                                "Object.getInode()} )");
                        sb.append("#set($")
                                .append(field.variable())
                                .append("ImageIdentifier=$!{")
                                .append(field.variable())
                                .append("Object.getIdentifier()} )");
                        sb.append("#set($").append(field.variable()).append("ImageWidth=$!{").append(field.variable()).append(
                                "Object.getWidth()} )");
                        sb.append("#set($").append(field.variable()).append("ImageHeight=$!{").append(field.variable()).append(
                                "Object.getHeight()} )");
                        sb.append("#set($").append(field.variable()).append("ImageExtension=$!{").append(field.variable()).append(
                                "Object.getExtension()} )");
                        sb.append("#set($")
                                .append(field.variable())
                                .append("ImageURI=$filetool.getURI($!{")
                                .append(field.variable())
                                .append("Object}, ")
                                .append(content.getLanguageId())
                                .append(" ))");
                        sb.append("#set($")
                                .append(field.variable())
                                .append("ImageTitle=$UtilMethods.espaceForVelocity($!{")
                                .append(field.variable())
                                .append("Object.getTitle()}) )");
                        sb.append("#set($")
                                .append(field.variable())
                                .append("ImageFriendlyName=$UtilMethods.espaceForVelocity($!{")
                                .append(field.variable())
                                .append("Object.getFriendlyName()}) )");

                        sb.append("#set($").append(field.variable()).append("ImagePath=$!{").append(field.variable()).append(
                                "Object.getPath()})");
                        sb.append("#set($").append(field.variable()).append("ImageName=$!{").append(field.variable()).append(
                                "Object.getFileName()})");

                    } else {
                        sb.append("#set($").append(field.variable()).append("FileInode=$!{").append(field.variable()).append(
                                "Object.getInode()} )");
                        sb.append("#set($").append(field.variable()).append("FileIdentifier=$!{").append(field.variable()).append(
                                "Object.getIdentifier()} )");
                        sb.append("#set($").append(field.variable()).append("FileExtension=$!{").append(field.variable()).append(
                                "Object.getExtension()} )");
                        sb.append("#set($")
                                .append(field.variable())
                                .append("FileURI=$filetool.getURI($!{")
                                .append(field.variable())
                                .append("Object}, ")
                                .append(content.getLanguageId())
                                .append(" ))");
                        sb.append("#set($").append(field.variable()).append("FileTitle=$!{").append(field.variable()).append(
                                "Object.getTitle()} )");
                        sb.append("#set($")
                                .append(field.variable())
                                .append("FileFriendlyName=$UtilMethods.espaceForVelocity($!{")
                                .append(field.variable())
                                .append("Object.getFriendlyName()} ))");

                        sb.append("#set($")
                                .append(field.variable())
                                .append("FilePath=$UtilMethods.espaceForVelocity($!{")
                                .append(field.variable())
                                .append("Object.getPath()}) )");
                        sb.append("#set($")
                                .append(field.variable())
                                .append("FileName=$UtilMethods.espaceForVelocity($!{")
                                .append(field.variable())
                                .append("Object.getFileName()} ))");
                    }

                } else {
                    sb.append("#set($").append(field.variable()).append("Object= $filetool.getNewFile())");
                }
            } // http://jira.dotmarketing.net/browse/DOTCMS-2178
            else if (field instanceof BinaryField) {
                java.io.File binFile;
                String fileName = "";
                String filesize = "";
                try {
                    binFile = content.getBinary(field.variable());
                    if (binFile != null) {
                        fileName = binFile.getName();
                        filesize = FileUtil.getsize(binFile);
                    }
                } catch (IOException e) {
                    Logger.error(this.getClass(), "Unable to retrive binary file for content id " + content.getIdentifier()
                            + " field " + field.variable(), e);
                    continue;
                }
                sb.append("#set($")
                        .append(field.variable())
                        .append("BinaryFileTitle=\"")
                        .append(UtilMethods.espaceForVelocity(fileName))
                        .append("\" )");
                sb.append("#set($")
                        .append(field.variable())
                        .append("BinaryFileSize=\"")
                        .append(UtilMethods.espaceForVelocity(filesize))
                        .append("\" )");
                String binaryFileURI = fileName.length() > 0
                        ? UtilMethods.espaceForVelocity("/contentAsset/raw-data/" + content.getIdentifier() + "/"
                        + field.variable() + "/" + content.getInode())
                        : "";
                sb.append("#set($").append(field.variable()).append("BinaryFileURI=\"").append(binaryFileURI).append("\" )");
            } else if (field instanceof SelectField) {
                sb.append("#set($")
                        .append(field.variable())
                        .append("SelectLabelsValues=\"")
                        .append(field.values().replaceAll("\\r\\n", " ").replaceAll("\\n", " "))
                        .append("\")");
            } else if (field instanceof RadioField) {
                sb.append("#set($")
                        .append(field.variable())
                        .append("RadioLabelsValues=\"")
                        .append(field.values().replaceAll("\\r\\n", " ").replaceAll("\\n", " "))
                        .append("\" )");
            } else if (field instanceof CheckboxField) {
                sb.append("#set($")
                        .append(field.variable())
                        .append("CheckboxLabelsValues=\"")
                        .append(field.values().replaceAll("\\r\\n", " ").replaceAll("\\n", " "))
                        .append("\" )");
            } else if (field instanceof DateField) {
                String shortFormat = "";
                String dbFormat = "";
                if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
                    shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy");
                    dbFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "yyyy-MM-dd");
                }
                sb.append("#set($").append(field.variable()).append("=$date.toDate(\"yyyy-MM-dd\", \"").append(dbFormat).append(
                        "\"))");
                sb.append("#set($").append(field.variable()).append("ShortFormat=\"").append(shortFormat).append("\" )");
                sb.append("#set($").append(field.variable()).append("DBFormat=\"").append(dbFormat).append("\" )");
            } else if (field instanceof TimeField) {
                String shortFormat = "";
                if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
                    shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "H:mm:ss");
                }
                sb.append("#set( $").append(field.variable()).append("ShortFormat=\"").append(shortFormat).append("\" )");
                sb.append("#set( $").append(field.variable()).append("= $date.toDate(\"H:mm:ss\", \"").append(shortFormat).append(
                        "\"))");
            } else if (field instanceof DateTimeField) {
                String shortFormat = "";
                String longFormat = "";
                String dbFormat = "";
                if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
                    shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy");
                    longFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy H:mm:ss");
                    dbFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "yyyy-MM-dd H:mm:ss");
                }
                sb.append("#set( $")
                        .append(field.variable())
                        .append("= $date.toDate(\"yyyy-MM-dd H:mm:ss\", \"")
                        .append(dbFormat)
                        .append("\"))");
                sb.append("#set( $").append(field.variable()).append("ShortFormat=\"").append(shortFormat).append("\" )");
                sb.append("#set( $").append(field.variable()).append("LongFormat=\"").append(longFormat).append("\" )");
                sb.append("#set( $").append(field.variable()).append("DBFormat=\"").append(dbFormat).append("\" )");
            } // http://jira.dotmarketing.net/browse/DOTCMS-2869
            // else if (field.getFieldType().equals(Field.FieldType.CUSTOM_FIELD.toString())){
            // sb.append("#set( $" + field.variable() + "Code=\"" +
            // UtilMethods.espaceForVelocity(field.getValues()) + "\" )");
            // }//http://jira.dotmarketing.net/browse/DOTCMS-3232
            else if (field instanceof HostFolderField) {
                if (InodeUtils.isSet(content.getFolder())) {
                    sb.append("#set( $ConHostFolder='").append(content.getFolder()).append("' )");
                } else {
                    sb.append("#set( $ConHostFolder='").append(content.getHost()).append("' )");
                }
            } else if (field instanceof CategoryField) {

                // Get the Category Field
                Category category = categoryAPI.find(field.values(), systemUser, false);
                // Get all the Contentlets Categories
                List<Category> selectedCategories = categoryAPI.getParents(content, systemUser, false);

                // Initialize variables
                String catInodes = "";
                Set<Category> categoryList = new HashSet<Category>();
                List<Category> categoryTree = categoryAPI.getAllChildren(category, systemUser, false);

                if (selectedCategories.size() > 0 && categoryTree != null) {
                    for (int k = 0; k < categoryTree.size(); k++) {
                        Category cat = (Category) categoryTree.get(k);
                        for (Category categ : selectedCategories) {
                            if (categ.getInode().equalsIgnoreCase(cat.getInode())) {
                                categoryList.add(cat);
                            }
                        }
                    }
                }

                if (categoryList.size() > 0) {
                    StringBuilder catbuilder = new StringBuilder();
                    Iterator<Category> it = categoryList.iterator();
                    while (it.hasNext()) {
                        Category cat = (Category) it.next();
                        catbuilder.append("\"").append(cat.getInode()).append("\"");
                        if (it.hasNext()) {
                            catbuilder.append(",");
                        }
                    }
                    catInodes = catbuilder.toString();

                    sb.append("#set( $")
                            .append(field.variable())
                            .append("FilteredCategories=$categories.filterCategoriesByUserPermissions([")
                            .append(catInodes)
                            .append("] ))");
                    sb.append("#set( $")
                            .append(field.variable())
                            .append("Categories=$categories.fetchCategoriesInodes($")
                            .append(field.variable())
                            .append("FilteredCategories))");
                    sb.append("#set( $")
                            .append(field.variable())
                            .append("CategoriesNames=$categories.fetchCategoriesNames($")
                            .append(field.variable())
                            .append("FilteredCategories))");
                    sb.append("#set( $").append(field.variable()).append("=$").append(field.variable()).append("Categories)");
                    sb.append("#set( $")
                            .append(field.variable())
                            .append("CategoriesKeys=$categories.fetchCategoriesKeys($")
                            .append(field.variable())
                            .append("FilteredCategories))");
                } else {
                    sb.append("#set( $").append(field.variable()).append("FilteredCategories=$contents.getEmptyList())");
                    sb.append("#set( $").append(field.variable()).append("Categories=$contents.getEmptyList())");
                    sb.append("#set( $").append(field.variable()).append("CategoriesNames=$contents.getEmptyList())");
                    sb.append("#set( $").append(field.variable()).append("=$contents.getEmptyList())");
                    sb.append("#set( $").append(field.variable()).append("CategoriesKeys=$contents.getEmptyList())");
                }
            } else if (field instanceof TagField) {
                content.setTags();
                String value = content.getStringProperty(field.variable());
                sb.append("#set($")
                        .append(field.variable())
                        .append("=\"")
                        .append(UtilMethods.espaceForVelocity(value).trim())
                        .append("\")");
            }
        }


        // get the contentlet categories to make a list
        String categories = "";
        Set<Category> categoryList = new HashSet<Category>(categoryAPI.getParents(content, systemUser, false));
        if (categoryList != null && categoryList.size() > 0) {
            StringBuilder catbuilder = new StringBuilder();
            Iterator<Category> it = categoryList.iterator();
            while (it.hasNext()) {
                Category category = (Category) it.next();
                catbuilder.append("\"").append(category.getInode()).append("\"");
                if (it.hasNext()) {
                    catbuilder.append(",");
                }
            }
            categories = catbuilder.toString();

            sb.append("#set($ContentletFilteredCategories=$categories.filterCategoriesByUserPermissions([")
                    .append(categories)
                    .append("] ))");
            sb.append("#set($ContentletCategories=$categories.fetchCategoriesInodes($ContentletFilteredCategories))");
            sb.append("#set($ContentletCategoryNames=$categories.fetchCategoriesNames($ContentletFilteredCategories))");
            sb.append("#set($ContentletCategoryKeys=$categories.fetchCategoriesKeys($ContentletFilteredCategories))");
        } else {
            sb.append("#set($ContentletFilteredCategories=$contents.getEmptyList())");
            sb.append("#set($ContentletCategories=$contents.getEmptyList())");
            sb.append("#set($ContentletCategoryNames=$contents.getEmptyList())");
            sb.append("#set($ContentletCategoryKeys=$contents.getEmptyList())");
        }

        // This needs to be here because the all fields like cats etc.. need to be parsed first and
        // it needs to be before
        // the $CONTENT_INODE is reset sb.append("#set( $CONTENT_INODE=\"" + content.getInode() +
        // "\" )");
        // http://jira.dotmarketing.net/browse/DOTCMS-2808
        sb.append(widgetCode);
        sb.append("#set($isForm= \"").append(false).append("\")");
        
        
        
        // This is code is repeated because the bug GETTYS-268, the content
        // variables were been overwritten
        // by the parse inside the some of the content fields
        // To edit the look, see
        // WEB-INF/velocity/static/preview/content_controls.vtl

        if (PageMode.EDIT_MODE == mode) {
            sb.append("#set( $EDIT_CONTENT_PERMISSION=$EDIT_CONTENT_PERMISSION").append(content.getIdentifier()).append(" )");
        }

        sb.append("#set( $CONTENT_INODE=\"").append(content.getInode()).append("\" )");
        sb.append("#set( $IDENTIFIER_INODE=\"").append(content.getIdentifier()).append("\" )");

        sb.append("#set( $ContentInode=\"").append(content.getInode()).append("\" )");
        sb.append("#set( $ContentIdentifier=\"").append(content.getIdentifier()).append("\" )");
        sb.append("#set( $ContentletTitle=\"")
            .append(UtilMethods.espaceForVelocity(conAPI.getName(content, APILocator.getUserAPI().getSystemUser(), true)))
            .append("\" )");
        sb.append("#set( $ContentletStructure=\"").append(content.getContentTypeId()).append("\" )");
        sb.append("#set( $ContentletContentType=\"").append(content.getContentTypeId()).append("\" )");

        return writeOutVelocity(filePath, sb.toString());
    }

    @SuppressWarnings("unchecked")
    public List<Contentlet> sortContentlets(List<Contentlet> contentletList, String sortBy) {

        Logger.debug(this.getClass(), "I'm ordering by " + sortBy);

        if (sortBy.equals("tree_order")) {
            Collections.sort(contentletList, new WebAssetSortOrderComparator());
        } else {
            Collections.sort(contentletList, new ContentComparator("asc"));
        }

        return contentletList;

    }

    /**
     * Will remove all contentlet files within a structure for both live and working. Uses the
     * system user.
     * 
     * @param structure
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public void invalidate(Structure structure) throws DotDataException, DotSecurityException {
        ContentletAPI conAPI = APILocator.getContentletAPI();
        int limit = 500;
        int offset = 0;
        List<Contentlet> contentlets =
                conAPI.findByStructure(structure, APILocator.getUserAPI().getSystemUser(), false, limit, offset);
        int size = contentlets.size();
        while (size > 0) {
            for (Contentlet contentlet : contentlets) {
                invalidate(contentlet);
            }
            offset += limit;
            contentlets = conAPI.findByStructure(structure, APILocator.getUserAPI().getSystemUser(), false, limit, offset);
            size = contentlets.size();
        }
    }



    @Override
    public InputStream writeObject(final VelocityResourceKey key)
            throws DotStateException, DotDataException, DotSecurityException {

        long language = new Long(key.language);
        ContentletVersionInfo info = APILocator.getVersionableAPI().getContentletVersionInfo(key.id1, language);

        if (!isLiveVersionAvailable(key, info) && language != defaultLang && APILocator.getLanguageAPI().canDefaultContentToDefaultLanguage()) {
            info = APILocator.getVersionableAPI().getContentletVersionInfo(key.id1, defaultLang);
        }

        if (!isLiveVersionAvailable(key, info)) {
            throw new ResourceNotFoundException("cannot find content for: " + key);
        }
        Contentlet contentlet =
                (key.mode.showLive) ? APILocator.getContentletAPI().find(info.getLiveInode(), APILocator.systemUser(), false)
                        : APILocator.getContentletAPI().find(info.getWorkingInode(), APILocator.systemUser(), false);

        Logger.debug(this, "DotResourceLoader:\tWriting out contentlet inode = " + contentlet.getInode());
        if (null == contentlet) {
            throw new ResourceNotFoundException("cannot find content for: " + key);
        }
        return buildVelocity(contentlet, key.mode, key.path);


    }

    private boolean isLiveVersionAvailable(VelocityResourceKey key, ContentletVersionInfo info) {
        return info != null && key.mode.showLive && UtilMethods.isSet(info.getLiveInode());
    }


    @Override
    public void invalidate(Object obj, PageMode mode) {
        Contentlet asset = (Contentlet) obj;

        VelocityResourceKey key = new VelocityResourceKey(asset, mode, asset.getLanguageId());
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(key);

        List<Field> fields;

        fields = asset.getContentType().fields();
        for (Field field : fields) {
            new FieldLoader().invalidate(field, Optional.<Contentlet>ofNullable(asset));
        }


        if (asset.getContentType().baseType() == BaseContentType.HTMLPAGE) {
            new PageLoader().invalidate(asset, mode);
        }

    }

}

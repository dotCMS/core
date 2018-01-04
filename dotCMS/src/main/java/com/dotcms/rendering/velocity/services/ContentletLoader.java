package com.dotcms.rendering.velocity.services;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.LanguageWebAPI;
import com.dotcms.services.VanityUrlServices;

import com.dotmarketing.beans.Identifier;
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
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.ResourceManager;

import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

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



    public InputStream buildVelocity(Contentlet content, Identifier identifier, PageMode mode, String filePath)
            throws DotDataException, DotSecurityException {
        StringBuilder sb = new StringBuilder();

        ContentletAPI conAPI = APILocator.getContentletAPI();

        User systemUser = sysUser();

        // let's write this puppy out to our file





        // CONTENTLET CONTROLS BEGIN
        sb.append("#if($EDIT_MODE)");
        sb.append("#set( $EDIT_CONTENT_PERMISSION=$EDIT_CONTENT_PERMISSION")
            .append(identifier.getId())
            .append(")");
        sb.append("#end");

        sb.append("#set($CONTENT_INODE='")
            .append(content.getInode())
            .append("' )")
            .append("#set($IDENTIFIER_INODE='")
            .append(identifier.getId())
            .append("' )");
        
        sb.append("#set($CONTENT_TYPE='")
            .append(content.getContentType().variable())
            .append("' )");

        sb.append("#set($CONTENT_BASE_TYPE='")
        .append(content.getContentType().baseType())
        .append("' )");
        
        sb.append("#set($CONTENT_LANGUAGE='")
        .append(content.getLanguageId())
        .append("' )");
        
        
        // set all properties from the contentlet
        sb.append("#set($ContentInode='")
            .append(content.getInode())
            .append("' )")
            .append("#set($ContentIdentifier='")
            .append(identifier.getId())
            .append("' )")
            .append("#set($ContentletTitle=\"")
            .append(UtilMethods.espaceForVelocity(conAPI.getName(content, APILocator.getUserAPI()
                .getSystemUser(), true)))
            .append("\" )");
        String modDateStr = UtilMethods.dateToHTMLDate((Date) content.getModDate(), "yyyy-MM-dd H:mm:ss");
        sb.append("#set($ContentLastModDate= $date.toDate(\"yyyy-MM-dd H:mm:ss\", \"")
            .append(modDateStr)
            .append("\"))")
            .append("#set($ContentLastModUserId= \"")
            .append(content.getModUser())
            .append("\")");
        if (content.getOwner() != null)
            sb.append("#set($ContentOwnerId= \"")
                .append(content.getOwner())
                .append("\")");

        // Structure fields

        ContentType type = content.getContentType();

        List<Field> fields = type.fields();


        String widgetCode = "";

        for (Field field : fields) {

            String contField = field.dbColumn();
            String contFieldValue = null;
            Object contFieldValueObject = null;
            String velPath = mode.name() + "/";
            if (field instanceof HiddenField || field instanceof ConstantField) {
                if (field.variable()
                    .equals("widgetPreexecute")) {
                    continue;
                }
                if (field.variable()
                    .equals("widgetCode")) {
                    widgetCode = "#set($" + field.variable() + "=$velutil.mergeTemplate(\"" + velPath + content.getInode()
                            + File.separator + field.inode() + "." + VelocityType.FIELD.fileExtension + "\"))";
                    continue;
                } else {
                    String fieldValues = field.values() == null ? "" : field.values();
                    if (fieldValues.contains("$") || fieldValues.contains("#")) {
                        sb.append("#set($")
                            .append(field.variable())
                            .append("= $velutil.mergeTemplate(\"")
                            .append(velPath)
                            .append(content.getInode())
                            .append(File.separator)
                            .append(field.inode())
                            .append(".")
                            .append(VelocityType.FIELD.fileExtension)
                            .append("\"))");
                    } else {
                        sb.append("#set($")
                            .append(field.variable())
                            .append("= \"")
                            .append(UtilMethods.espaceForVelocity(fieldValues)
                                .trim())
                            .append("\")");
                    }
                    continue;
                }
            }


            if (UtilMethods.isSet(contField)) {
                try {
                    contFieldValueObject = conAPI.getFieldValue(content, field);
                    contFieldValue = contFieldValueObject == null ? "" : contFieldValueObject.toString();
                } catch (Exception e) {
                    Logger.error(this.getClass(), "writeContentletToFile: " + e.getMessage());
                }
                if (!(field instanceof DateTimeField || field instanceof DateField || field instanceof TimeField)) {
                    if (contFieldValue.contains("$") || contFieldValue.contains("#")) {
                        sb.append("#set($")
                            .append(field.variable())
                            .append("=$velutil.mergeTemplate(\"")
                            .append(velPath)
                            .append(content.getInode())
                            .append(File.separator)
                            .append(field.inode())
                            .append(".")
                            .append(VelocityType.FIELD.fileExtension)
                            .append("\"))");
                    } else {
                        sb.append("#set($")
                            .append(field.variable())
                            .append("=\"")
                            .append(UtilMethods.espaceForVelocity(contFieldValue)
                                .trim())
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
                        sb.append("#set($")
                            .append(field.variable())
                            .append("ImageInode=$!{")
                            .append(field.variable())
                            .append("Object.getInode()} )");
                        sb.append("#set($")
                            .append(field.variable())
                            .append("ImageIdentifier=$!{")
                            .append(field.variable())
                            .append("Object.getIdentifier()} )");
                        sb.append("#set($")
                            .append(field.variable())
                            .append("ImageWidth=$!{")
                            .append(field.variable())
                            .append("Object.getWidth()} )");
                        sb.append("#set($")
                            .append(field.variable())
                            .append("ImageHeight=$!{")
                            .append(field.variable())
                            .append("Object.getHeight()} )");
                        sb.append("#set($")
                            .append(field.variable())
                            .append("ImageExtension=$!{")
                            .append(field.variable())
                            .append("Object.getExtension()} )");
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
    
                        sb.append("#set($")
                            .append(field.variable())
                            .append("ImagePath=$!{")
                            .append(field.variable())
                            .append("Object.getPath()})");
                        sb.append("#set($")
                            .append(field.variable())
                            .append("ImageName=$!{")
                            .append(field.variable())
                            .append("Object.getFileName()})");
    
                    } else {
                        sb.append("#set($")
                            .append(field.variable())
                            .append("FileInode=$!{")
                            .append(field.variable())
                            .append("Object.getInode()} )");
                        sb.append("#set($")
                            .append(field.variable())
                            .append("FileIdentifier=$!{")
                            .append(field.variable())
                            .append("Object.getIdentifier()} )");
                        sb.append("#set($")
                            .append(field.variable())
                            .append("FileExtension=$!{")
                            .append(field.variable())
                            .append("Object.getExtension()} )");
                        sb.append("#set($")
                            .append(field.variable())
                            .append("FileURI=$filetool.getURI($!{")
                            .append(field.variable())
                            .append("Object}, ")
                            .append(content.getLanguageId())
                            .append(" ))");
                        sb.append("#set($")
                            .append(field.variable())
                            .append("FileTitle=$!{")
                            .append(field.variable())
                            .append("Object.getTitle()} )");
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
                    sb.append("#set($")
                        .append(field.variable())
                        .append("Object= $filetool.getNewFile())");
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
                sb.append("#set($")
                    .append(field.variable())
                    .append("BinaryFileURI=\"")
                    .append(binaryFileURI)
                    .append("\" )");
            } else if (field instanceof SelectField) {
                sb.append("#set($")
                    .append(field.variable())
                    .append("SelectLabelsValues=\"")
                    .append(field.values()
                        .replaceAll("\\r\\n", " ")
                        .replaceAll("\\n", " "))
                    .append("\")");
            } else if (field instanceof RadioField) {
                sb.append("#set($")
                    .append(field.variable())
                    .append("RadioLabelsValues=\"")
                    .append(field.values()
                        .replaceAll("\\r\\n", " ")
                        .replaceAll("\\n", " "))
                    .append("\" )");
            } else if (field instanceof CheckboxField) {
                sb.append("#set($")
                    .append(field.variable())
                    .append("CheckboxLabelsValues=\"")
                    .append(field.values()
                        .replaceAll("\\r\\n", " ")
                        .replaceAll("\\n", " "))
                    .append("\" )");
            } else if (field instanceof DateField) {
                String shortFormat = "";
                String dbFormat = "";
                if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
                    shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy");
                    dbFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "yyyy-MM-dd");
                }
                sb.append("#set($")
                    .append(field.variable())
                    .append("=$date.toDate(\"yyyy-MM-dd\", \"")
                    .append(dbFormat)
                    .append("\"))");
                sb.append("#set($")
                    .append(field.variable())
                    .append("ShortFormat=\"")
                    .append(shortFormat)
                    .append("\" )");
                sb.append("#set($")
                    .append(field.variable())
                    .append("DBFormat=\"")
                    .append(dbFormat)
                    .append("\" )");
            } else if (field instanceof TimeField) {
                String shortFormat = "";
                if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
                    shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "H:mm:ss");
                }
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ShortFormat=\"")
                    .append(shortFormat)
                    .append("\" )");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("= $date.toDate(\"H:mm:ss\", \"")
                    .append(shortFormat)
                    .append("\"))");
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
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ShortFormat=\"")
                    .append(shortFormat)
                    .append("\" )");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("LongFormat=\"")
                    .append(longFormat)
                    .append("\" )");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("DBFormat=\"")
                    .append(dbFormat)
                    .append("\" )");
            } // http://jira.dotmarketing.net/browse/DOTCMS-2869
              // else if (field.getFieldType().equals(Field.FieldType.CUSTOM_FIELD.toString())){
              // sb.append("#set( $" + field.variable() + "Code=\"" +
              // UtilMethods.espaceForVelocity(field.getValues()) + "\" )");
              // }//http://jira.dotmarketing.net/browse/DOTCMS-3232
            else if (field instanceof HostFolderField) {
                if (InodeUtils.isSet(content.getFolder())) {
                    sb.append("#set( $ConHostFolder='")
                        .append(content.getFolder())
                        .append("' )");
                } else {
                    sb.append("#set( $ConHostFolder='")
                        .append(content.getHost())
                        .append("' )");
                }
            }

            else if (field instanceof CategoryField) {

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
                            if (categ.getInode()
                                .equalsIgnoreCase(cat.getInode())) {
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
                        catbuilder.append("\"")
                            .append(cat.getInode())
                            .append("\"");
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
                    sb.append("#set( $")
                        .append(field.variable())
                        .append("=$")
                        .append(field.variable())
                        .append("Categories)");
                    sb.append("#set( $")
                        .append(field.variable())
                        .append("CategoriesKeys=$categories.fetchCategoriesKeys($")
                        .append(field.variable())
                        .append("FilteredCategories))");
                } else {
                    sb.append("#set( $")
                        .append(field.variable())
                        .append("FilteredCategories=$contents.getEmptyList())");
                    sb.append("#set( $")
                        .append(field.variable())
                        .append("Categories=$contents.getEmptyList())");
                    sb.append("#set( $")
                        .append(field.variable())
                        .append("CategoriesNames=$contents.getEmptyList())");
                    sb.append("#set( $")
                        .append(field.variable())
                        .append("=$contents.getEmptyList())");
                    sb.append("#set( $")
                        .append(field.variable())
                        .append("CategoriesKeys=$contents.getEmptyList())");
                }
            } else if (field instanceof TagField) {
                content.setTags();
                String value = content.getStringProperty(field.variable());
                sb.append("#set($")
                    .append(field.variable())
                    .append("=\"")
                    .append(UtilMethods.espaceForVelocity(value)
                        .trim())
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
                catbuilder.append("\"")
                    .append(category.getInode())
                    .append("\"");
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

        // This is code is repeated because the bug GETTYS-268, the content
        // variables were been overwritten
        // by the parse inside the some of the content fields
        // To edit the look, see
        // WEB-INF/velocity/static/preview/content_controls.vtl

        if (PageMode.EDIT_MODE == mode) {
            sb.append("#set( $EDIT_CONTENT_PERMISSION=$EDIT_CONTENT_PERMISSION")
                .append(identifier.getId())
                .append(" )");
        }

        sb.append("#set( $CONTENT_INODE=\"")
            .append(content.getInode())
            .append("\" )");
        sb.append("#set( $IDENTIFIER_INODE=\"")
            .append(identifier.getId())
            .append("\" )");

        sb.append("#set( $ContentInode=\"")
            .append(content.getInode())
            .append("\" )");
        sb.append("#set( $ContentIdentifier=\"")
            .append(identifier.getId())
            .append("\" )");
        sb.append("#set( $ContentletTitle=\"")
            .append(UtilMethods.espaceForVelocity(conAPI.getName(content, APILocator.getUserAPI()
                .getSystemUser(), true)))
            .append("\" )");
        sb.append("#set( $ContentletStructure=\"")
            .append(content.getContentTypeId())
            .append("\" )");
        sb.append("#set( $ContentletContentType=\"")
            .append(content.getContentTypeId())
            .append("\" )");
        if (type.baseType() == BaseContentType.WIDGET) {
            sb.append("#set( $isWidget= \"")
                .append(true)
                .append("\")");
            if (type.name()
                .equals(FormAPI.FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME)) {
                sb.append("#set($isFormWidget= \"")
                    .append(true)
                    .append("\")");
            } else {
                sb.append("#set($isFormWidget= \"")
                    .append(false)
                    .append("\")");
            }
        } else {
            sb.append("#set($isWidget= \"")
                .append(false)
                .append("\")");
        }

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
     * @param contentlets
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public void invalidate(Structure structure) throws DotDataException, DotSecurityException {
        ContentletAPI conAPI = APILocator.getContentletAPI();
        int limit = 500;
        int offset = 0;
        List<Contentlet> contentlets = conAPI.findByStructure(structure, APILocator.getUserAPI()
            .getSystemUser(), false, limit, offset);
        int size = contentlets.size();
        while (size > 0) {
            for (Contentlet contentlet : contentlets) {
                invalidate(contentlet);
            }
            offset += limit;
            contentlets = conAPI.findByStructure(structure, APILocator.getUserAPI()
                .getSystemUser(), false, limit, offset);
            size = contentlets.size();
        }
    }



    @Override
    public InputStream writeObject(String id1, String id2, PageMode mode, String language, String filePath) {
        try {

            Identifier identifier = APILocator.getIdentifierAPI()
                .find(id1);
            Contentlet contentlet = null;
            if (CacheLocator.getVeloctyResourceCache()
                .isMiss(filePath)) {
                if (LanguageWebAPI.canDefaultContentToDefaultLanguage()) {
                    LanguageAPI langAPI = APILocator.getLanguageAPI();
                    language = Long.toString(langAPI.getDefaultLanguage()
                        .getId());
                } else {
                    throw new ResourceNotFoundException("Contentlet is a miss in the cache");
                }
            }

            try {
                contentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifier(identifier.getId(), mode.showLive, new Long(language), APILocator.getUserAPI()
                        .getSystemUser(), true);
            } catch (DotContentletStateException e) {
                contentlet = null;
            }

            if (contentlet == null || !InodeUtils.isSet(contentlet.getInode()) || contentlet.isArchived()) {

                LanguageAPI langAPI = APILocator.getLanguageAPI();
                long lid = langAPI.getDefaultLanguage()
                    .getId();
                if (lid != Long.parseLong(language)) {
                    Contentlet cc = APILocator.getContentletAPI()
                        .findContentletByIdentifier(identifier.getId(), mode.showLive, lid, APILocator.getUserAPI()
                            .getSystemUser(), true);
                    if (cc != null && UtilMethods.isSet(cc.getInode()) && !cc.isArchived()
                            && LanguageWebAPI.canApplyToAllLanguages(cc)) {
                        contentlet = cc;
                    } else {
                        CacheLocator.getVeloctyResourceCache()
                            .addMiss(filePath);
                        throw new ResourceNotFoundException("Contentlet Velocity file not found:" + filePath);
                    }
                }
            }

            Logger.debug(this, "DotResourceLoader:\tWriting out contentlet inode = " + contentlet.getInode());
            return buildVelocity(contentlet, identifier, mode, filePath);

        } catch (Exception e) {
            throw new ResourceNotFoundException("Contentlet Velocity file not found:" + filePath);
        }
    }




    @Override
    public void invalidate(Object obj, PageMode mode) {
        Contentlet asset = (Contentlet) obj;
        CacheLocator.getContentletCache()
            .remove(asset.getInode());

        String folderPath = mode.name() + java.io.File.separator;
        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        velocityRootPath += java.io.File.separator;

        Set<Long> langs = new HashSet<Long>();
        langs.add(asset.getLanguageId());
        if (LanguageWebAPI.canApplyToAllLanguages(asset)) {
            for (Language ll : APILocator.getLanguageAPI()
                .getLanguages()) {
                langs.add(ll.getId());
            }
        }
        for (Long langId : langs) {
            String filePath = folderPath + asset.getIdentifier() + "_" + langId + "." + VelocityType.CONTENT.fileExtension;
            java.io.File f = new java.io.File(velocityRootPath + filePath);
            f.delete();
            DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
            vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath);
        }
        List<Field> fields;
        try {
            fields = asset.getContentType()
                .fields();
            for (Field field : fields) {
                new FieldLoader().invalidate(field, asset, mode);
            }
        } catch (DotDataException | DotSecurityException e) {
            throw new DotStateException(e);
        }


        try {
            if (asset.getContentType()
                .baseType() == BaseContentType.HTMLPAGE) {
                new PageLoader().invalidate(asset, mode);
            }

            if (asset != null && asset.isVanityUrl()) {
                // remove from cache
                VanityUrlServices.getInstance()
                    .invalidateVanityUrl(asset);
            }
            if (asset != null && asset.isKeyValue()) {
                // remove from cache
                CacheLocator.getKeyValueCache()
                    .remove(asset);
            }
        } catch (DotDataException | DotSecurityException e) {
            throw new DotStateException(e);
        }
    }

}

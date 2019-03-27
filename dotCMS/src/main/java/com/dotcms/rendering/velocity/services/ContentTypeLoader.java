package com.dotcms.rendering.velocity.services;



import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.rendering.velocity.util.VelocityUtil;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.apache.velocity.runtime.resource.ResourceManager;

/**
 * @author will
 */
public class ContentTypeLoader implements DotLoader {

    private long fakeIdentifier = Long.MAX_VALUE;
    private long fakeInode = Long.MAX_VALUE;
    private String fakeTitle = "Content Title";



    public void invalidate(ContentType contentType, PageMode mode) {
        removeContentTypeFile(contentType);
    }



    public InputStream buildVelocity(ContentType type, String filePath, PageMode mode ) {

        // let's write this puppy out to our file
        StringBuilder sb = new StringBuilder();

        // CONTENTLET CONTROLS BEGIN
        sb.append("#set( $EDIT_CONTENT_PERMISSION =$EDIT_CONTENT_PERMISSION")
            .append(fakeInode)
            .append(" )");
        sb.append("#set( $CONTENT_INODE ='")
            .append(fakeInode)
            .append("' )");
        sb.append("#set( $IDENTIFIER_INODE ='")
            .append(fakeIdentifier)
            .append("' )");

        // set all properties from the contentlet
        sb.append("#set( $ContentInode ='")
            .append(fakeInode)
            .append("' )");
        sb.append("#set( $ContentIdentifier ='")
            .append(fakeIdentifier)
            .append("' )");
        sb.append("#set( $ContentletTitle =\"")
            .append(UtilMethods.espaceForVelocity(fakeTitle))
            .append("\" )");

        // Structure fields
        List<Field> fields = type.fields();


        for (Field field : fields) {


            String contField = field.dbColumn();
            String contFieldValue = null;
            Object contFieldValueObject = null;
            if (UtilMethods.isSet(contField)) {
                try {
                    contFieldValueObject = field.name();
                    contFieldValue = contFieldValueObject == null ? "" : contFieldValueObject.toString();
                } catch (Exception e) {
                    Logger.error(this.getClass(), "writeContentletToFile: " + e.getMessage());
                }
                if (!(field instanceof DateTimeField || field instanceof DateField || field instanceof TimeField)) {
                    sb.append("#set( $")
                        .append(field.variable())
                        .append(" =\"")
                        .append(UtilMethods.espaceForVelocity(contFieldValue)
                            .trim())
                        .append("\" )");
                }
            }
            String fv = field.values() != null ? field.values() : "";
            if (field instanceof TextField || field instanceof TextAreaField || field instanceof WysiwygField) {
                sb.append("#set( $")
                    .append(field.variable())
                    .append(" =\"[ #fixBreaks($")
                    .append(field.variable())
                    .append(") ]\")");
            } else if (field instanceof ImageField) {
                // Identifier id = (Identifier) InodeFactory.getChildOfClassByRelationType(content,
                // Identifier.class, field.getFieldRelationType());
                String uri = "/html/images/shim.gif";
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ImageInode =\"")
                    .append(Long.MAX_VALUE)
                    .append("\" )");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ImageWidth =\"")
                    .append(150)
                    .append("\" )"); // Original value was 165
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ImageHeight =\"")
                    .append(150)
                    .append("\" )"); // Originak value was 65
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ImageExtension =\"gif\" )");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ImageURI =\"")
                    .append(uri)
                    .append("\" )");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ImageTitle =\"[ Test Image Structure ]\" )");
            } else if (field instanceof FileField) {
                // Identifier id = (Identifier) InodeFactory.getChildOfClassByRelationType(content,
                // Identifier.class, field.getFieldRelationType());
                String uri = "/html/images/shim.gif";
                sb.append("#set( $")
                    .append(field.variable())
                    .append("FileInode =\"")
                    .append(Long.MAX_VALUE)
                    .append("\" )");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("FileExtension =\"gif\" )");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("FileURI =\"")
                    .append(uri)
                    .append("\" )");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("FileTitle =\"[ Test File Structure ]\" )");
            } else if (field instanceof SelectField) {
                sb.append("#set( $")
                    .append(field.variable())
                    .append("SelectLabelsValues = \"")
                    .append(fv.replaceAll("\\r\\n", " ")
                        .replaceAll("\\n", " "))
                    .append("\")");
            } else if (field instanceof RadioField) {
                sb.append("#set( $")
                    .append(field.variable())
                    .append("RadioLabelsValues = \"")
                    .append(fv.replaceAll("\\r\\n", " ")
                        .replaceAll("\\n", " "))
                    .append("\" )");
            } else if (field instanceof CheckboxField) {
                sb.append("#set( $")
                    .append(field.variable())
                    .append("CheckboxLabelsValues = \"")
                    .append(fv.replaceAll("\\r\\n", " ")
                        .replaceAll("\\n", " "))
                    .append("\" )");
            } else if (field instanceof DateField) {
                sb.append("#set( $")
                    .append(field.variable())
                    .append(" =\"[ ")
                    .append(field.variable())
                    .append(" ]\")");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ShortFormat =\"[ ")
                    .append(field.variable())
                    .append(" ]\")");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("DBFormat =\"[ ")
                    .append(field.variable())
                    .append(" ]\")");
            } else if (field instanceof TimeField) {
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ShortFormat =\"[ ")
                    .append(field.variable())
                    .append(" ]\")");
                sb.append("#set( $")
                    .append(field.variable())
                    .append(" =\"[ ")
                    .append(field.variable())
                    .append("\" ])");
            } else if (field instanceof DateTimeField) {
                sb.append("#set( $")
                    .append(field.variable())
                    .append(" =\"[ ")
                    .append(field.variable())
                    .append(" ] \")");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("ShortFormat =\"[ ")
                    .append(field.variable())
                    .append(" ]\")");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("LongFormat =\"[ ")
                    .append(field.variable())
                    .append(" ]\")");
                sb.append("#set( $")
                    .append(field.variable())
                    .append("DBFormat =\"[ ")
                    .append(field.variable())
                    .append(" ]\")");
            }
        }

        // sets the categories as a list on velocity
        sb.append("#set( $ContentletCategories =[] )");
        sb.append("#set( $ContentletCategoryNames =[] )");


        // This is code is repeated becuase the bug GETTYS-268, the content variables were been
        // overwritten
        // by the parse inside the some of the content fields
        // To edit the look, see WEB-INF/velocity/static/preview/content_controls.vtl

        sb.append("#set( $EDIT_CONTENT_PERMISSION =$EDIT_CONTENT_PERMISSION")
            .append(fakeIdentifier)
            .append(" )");
        sb.append("#set( $CONTENT_INODE ='")
            .append(fakeInode)
            .append("' )");
        sb.append("#set( $IDENTIFIER_INODE ='")
            .append(fakeIdentifier)
            .append("' )");

        sb.append("#set( $ContentInode ='")
            .append(fakeInode)
            .append("' )");
        sb.append("#set( $ContentIdentifier ='")
            .append(fakeIdentifier)
            .append("' )");
        sb.append("#set( $ContentletTitle =\"")
            .append(UtilMethods.espaceForVelocity(fakeTitle))
            .append("\" )");
        sb.append("#set( $isWidget = false)");

        return writeOutVelocity(filePath, sb.toString());
    }

    public void removeContentTypeFile(ContentType contentType) {

    }

    @Override
    public InputStream writeObject(final VelocityResourceKey key) throws DotDataException, DotSecurityException {

        ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(sysUser());


        // Search for the given ContentType inode
        ContentType foundContentType = contentTypeAPI.find(key.id1);


        return buildVelocity(foundContentType,  key.path, key.mode);

    }



    @Override
    public void invalidate(Object obj, PageMode mode) {
        
        ContentType contentType = null;
        if(obj instanceof Structure) {
            contentType = new StructureTransformer((Structure) obj).from();
        }
        else if(obj instanceof ContentType) {
            contentType = (ContentType) obj;
        }

        if(contentType==null)return;
        String folderPath =  mode.name() + File.separator;
        String filePath = folderPath + contentType.inode() + "." + VelocityType.CONTENT_TYPE.fileExtension;

        String velocityRootPath = VelocityUtil.getVelocityRootPath();
        String absolutPath = velocityRootPath + File.separator + filePath;
        java.io.File f = new java.io.File(absolutPath);
        f.delete();
        DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath);

    }
}

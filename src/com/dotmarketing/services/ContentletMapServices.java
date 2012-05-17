package com.dotmarketing.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.velocity.runtime.resource.ResourceManager;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.velocity.DotResourceCache;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * @author will
 */
public class ContentletMapServices {

	private static CategoryAPI categoryAPI = APILocator.getCategoryAPI();

	public static CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public static void setCategoryAPI(CategoryAPI categoryAPI) {
		ContentletMapServices.categoryAPI = categoryAPI;
	}

	public static void invalidate(Contentlet contentlet) throws DotDataException, DotSecurityException {
		invalidate(contentlet, true);
		invalidate(contentlet, false);
	}

	public static void invalidate(Contentlet content, boolean EDIT_MODE) throws DotDataException, DotSecurityException {
		removeContentletMapFile(content, EDIT_MODE);
	}

	public static InputStream buildVelocity(Contentlet content, boolean EDIT_MODE) throws DotDataException, DotSecurityException, DotContentletStateException {
		InputStream result;
		ContentletAPI conAPI = APILocator.getContentletAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();

		if (!InodeUtils.isSet(content.getInode())|| !InodeUtils.isSet(content.getIdentifier())) {
			throw new DotContentletStateException("The contentlet inode and identifier must be set");
		}
		// let's write this puppy out to our file
		StringBuilder sb = new StringBuilder();
		String conTitle = conAPI.getName(content, APILocator.getUserAPI().getSystemUser(), true);
		// CONTENTLET CONTROLS BEGIN
		// To edit the look, see
		// WEB-INF/velocity/static/preview/content_controls.vtl
		sb.append("#set( $dotcms_content_" + content.getIdentifier() + " = ${contents.getEmptyMap()})\n");
//		Was put in to fix DOTCMS-995 but it caused DOTCMS-1210.
//      I actually think it should be fine passed the ctx which is a chained context here
//		sb.append("#set($velocityContext = $UtilMethods.pushVelocityContext($velocityContext))\n");
//		sb.append("$!velocityContext.put(\"content\",$content)\n");

		sb.append("$!dotcms_content_" + content.getIdentifier() + ".put(\"permission\", $EDIT_CONTENT_PERMISSION" + content.getIdentifier() + " )\n");
		sb.append("$!dotcms_content_" + content.getIdentifier() + ".put(\"inode\", '" + content.getInode() + "'  )\n");
		sb.append("$!dotcms_content_" + content.getIdentifier() + ".put(\"identifier\", '" + content.getIdentifier() + "'  )\n");
		sb.append("$!dotcms_content_" + content.getIdentifier() + ".put(\"structureInode\", '" + content.getStructureInode() + "'  )\n");
		sb.append("$!dotcms_content_" + content.getIdentifier() + ".put(\"contentTitle\", \"" + UtilMethods.espaceForVelocity(conTitle) + "\" )\n");
		sb.append("$!dotcms_content_" + content.getIdentifier() + ".put(\"detailPageURI\", \"" + getDetailPageURI(content) + "\"  )\n");
		Structure structure = content.getStructure();

		String modDateStr = UtilMethods.dateToHTMLDate((Date) content.getModDate(), "yyyy-MM-dd H:mm:ss");
		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"modDate\", $date.toDate(\"yyyy-MM-dd H:mm:ss\", \"" + modDateStr + "\")))\n");
		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"contentLastModDate\", $date.toDate(\"yyyy-MM-dd H:mm:ss\", \"" + modDateStr + "\")))\n");
		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"contentLastModUserId\", \"" + content.getModUser() + "\"))\n");
		if (content.getOwner() != null)
			sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"contentOwnerId\", \"" + content.getOwner() + "\"))\n");

		// Structure fields
		List<Field> fields = FieldsCache.getFieldsByStructureInode(content.getStructureInode());
		Iterator<Field> fieldsIt = fields.iterator();

		String widgetCode = "";

		while (fieldsIt.hasNext()) {
			Field field = (Field) fieldsIt.next();

			sb.append("\n\n##Set Field " + field.getFieldName() + " properties\n");

			String contField = field.getFieldContentlet();
			String contFieldValue = null;
			Object contFieldValueObject = null;
			FieldAPI fdAPI = APILocator.getFieldAPI();
			String velPath = (!EDIT_MODE) ? "live/" : "working/";
			if(fdAPI.isElementConstant(field)){
				if(field.getVelocityVarName().equals("widgetPreexecute")){
					continue;
				}
				if(field.getVelocityVarName().equals("widgetCode")){
//					widgetCode = "#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('"
//					+ UtilMethods.espaceVariableForVelocity(field.getValues()) + "'), $velocityContext)))\n";
//					widgetCode = "#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", \"" + UtilMethods.espaceForVelocity(field.getValues()).trim() + "\"))\n";
//					widgetCode = "#set( $fieldStringWriter" + content.getInode() + field.getInode() + " = $stringsapi.getEmptyStringWriter())\n";
//					widgetCode += "$UtilMethods.getVelocityTemplate(\"" + folderPath + content.getInode() + "_" + field.getInode()  + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") +  "\").merge($context, $fieldStringWriter" + content.getInode() + field.getInode()  + ")\n";
//					widgetCode += "#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $fieldStringWriter" + content.getInode() +  field.getInode()  + ".toString()))\n";
					widgetCode = "#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $velutil.mergeTemplate(\"" + velPath + content.getInode() + "_" + field.getInode()  + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\")))\n";
					continue;
				}else{
//					sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('"
//							+ UtilMethods.espaceVariableForVelocity(field.getValues()) + "'), $velocityContext)))\n");
//					sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", \"" + UtilMethods.espaceForVelocity(field.getValues()).trim() + "\"))\n");
//					sb.append("#set( $fieldStringWriter" +  content.getInode() + field.getInode()  + " = $stringsapi.getEmptyStringWriter())\n");
//					sb.append("$UtilMethods.getVelocityTemplate(\"" + folderPath +  content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\").merge($context, $fieldStringWriter" + content.getInode() + field.getInode()  + ")\n");
//					sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $fieldStringWriter" + content.getInode() + field.getInode()  + ".toString()))\n");
					if(field.getValues().contains("$") || field.getValues().contains("#")){
						sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $velutil.mergeTemplate(\"" + velPath +  content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\")))\n");
					}else{
						sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", \"" + UtilMethods.espaceForVelocity(field.getValues()).trim() + "\"))\n");
					}
					continue;
				}

			}
			if (UtilMethods.isSet(contField)) {
				try {
					contFieldValueObject = conAPI.getFieldValue(content, field);
					contFieldValue = contFieldValueObject == null ? "" : contFieldValueObject.toString();
					// contFieldValueObject = PropertyUtils.getProperty(content,
					// contField);
					// contFieldValue = contFieldValueObject == null ? "" :
					// contFieldValueObject.toString();
				} catch (Exception e) {
					Logger.error(ContentletMapServices.class, "writeContentletToFile: " + e.getMessage());
				}
				if (!field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()) && !field.getFieldType().equals(Field.FieldType.DATE.toString())
						&& !field.getFieldType().equals(Field.FieldType.TIME.toString())) {
					if (fdAPI.isNumeric(field)) {
						sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", "+ contFieldValue +"))\n");
					} else {
//						sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('"
//								+ UtilMethods.espaceVariableForVelocity(contFieldValue) + "'), $velocityContext)))\n");
//						sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", \"" + UtilMethods.espaceForVelocity(contFieldValue).trim() + "\"))\n");
//						sb.append("#set( $fieldStringWriter" + content.getInode() + field.getInode()  + " = $stringsapi.getEmptyStringWriter())\n");
//						sb.append("$UtilMethods.getVelocityTemplate(\"" + folderPath + content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\").merge($context, $fieldStringWriter" + content.getInode() + field.getInode()  + ")\n");
//						sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $fieldStringWriter" + content.getInode() + field.getInode()  +".toString()))\n");
						if(contFieldValue.contains("$") || contFieldValue.contains("#")){
							sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $velutil.mergeTemplate(\"" + velPath +  content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\")))\n");
						}else{
							sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", \"" + UtilMethods.espaceForVelocity(contFieldValue).trim() + "\"))\n");
						}
					}
				}

			}

			if (field.getFieldType().equals(Field.FieldType.TEXT.toString()) || field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString())
					|| field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
				// sb.append("#set( $" + field.getVelocityVarName() + "
				// =\"#fixBreaks($" + field.getVelocityVarName() + ")\")\n");
				// sb.append("$!dotcms_content_" + content.getIdentifier() + ".put(\""+field.getVelocityVarName()+"\",
				// \"" + UtilMethods.fixBreaks(contFieldValue) + " \" )\n");

			} else if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())) {
				String identifierValue = content.getStringProperty(field.getVelocityVarName());
				if( InodeUtils.isSet(identifierValue) ) {
					if (EDIT_MODE){
						sb.append("#set( $" + field.getVelocityVarName() + " = $filetool.getFile('" + identifierValue + "',false))\n");
					}else{
						sb.append("#set( $" + field.getVelocityVarName() + " = $filetool.getFile('" + identifierValue + "',true))\n");
					}
				}else{
					sb.append("#set( $" + field.getVelocityVarName() + " = $filetool.getNewFile())\n");
				}

				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ImageInode\", $" + field.getVelocityVarName() + ".getInode() ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ImageIdentifier\", $" + field.getVelocityVarName() + ".getIdentifier() ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ImageWidth\", $" + field.getVelocityVarName() + ".getWidth() ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ImageHeight\", $" + field.getVelocityVarName() + ".getHeight() ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ImageExtension\", $UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + ".getExtension()) ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ImageURI\", $filetool.getURI($" + field.getVelocityVarName() + ") ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ImageTitle\", $UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + ".getTitle()) ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ImageFriendlyName\", $UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + ".getFriendlyName()) ))\n");

				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ImagePath\", $UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + ".getPath()) ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ImageName\", $UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + ".getFileName()) ))\n");

			} else if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
				String identifierValue = content.getStringProperty(field.getVelocityVarName());
				if( InodeUtils.isSet(identifierValue) ) {
					if (EDIT_MODE){
						sb.append("#set( $" + field.getVelocityVarName() + "Object = $filetool.getFile('" + identifierValue + "',false))\n");
					}else{
						sb.append("#set( $" + field.getVelocityVarName() + "Object = $filetool.getFile('" + identifierValue + "',true))\n");
					}
				}else{
					sb.append("#set( $" + field.getVelocityVarName() + "Object = $filetool.getNewFile())\n");
				}


				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "FileInode\", $" + field.getVelocityVarName() + "Object.getInode() ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "FileIdentifier\", $" + field.getVelocityVarName() + "Object.getIdentifier() ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "FileFriendlyName\", $UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + "Object.getFriendlyName()) ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "FileExtension\", $UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + "Object.getExtension()) ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "FileURI\", $filetool.getURI($" + field.getVelocityVarName() + "Object) ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "FileTitle\", $" + field.getVelocityVarName() + "Object.getTitle() ))\n");

				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "FilePath\", $UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + "Object.getPath()) ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "FileName\", $UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + "Object.getFileName()) ))\n");

			} //http://jira.dotmarketing.net/browse/DOTCMS-2178
			else if (field.getFieldType().equals(Field.FieldType.BINARY.toString())) {
				java.io.File binFile;
				String fileName = "";
				String filesize = "";
				try {
					binFile = content.getBinary(field.getVelocityVarName());
					if(binFile != null) {
						fileName = binFile.getName();
						filesize = FileUtil.getsize(binFile);
					}
				} catch (IOException e) {
					Logger.error(ContentletServices.class, "Unable to retrive binary file for content id " + content.getIdentifier() + " field " + field.getVelocityVarName(), e);
					continue;
				}
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "BinaryFileTitle\", \"" + UtilMethods.espaceForVelocity(fileName) + "\"))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "BinaryFileSize\", \"" + UtilMethods.espaceForVelocity(filesize) + "\"))\n");
				String binaryFileURI = fileName.length()>0? UtilMethods.espaceForVelocity("/contentAsset/raw-data/"+content.getIdentifier()+"/"+ field.getVelocityVarName() + "/" + content.getInode()):"";
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "BinaryFileURI\", \""	+ binaryFileURI + "\"))\n");
			} else if (field.getFieldType().equals(Field.FieldType.SELECT.toString())) {
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "SelectLabelsValues\", \""
						+ field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") + "\"))\n");

			} else if (field.getFieldType().equals(Field.FieldType.RADIO.toString())) {
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "RadioLabelsValues\", \""
						+ field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") + "\"))\n");

			} else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())) {
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "CheckboxLabelsValues\", \""
						+ field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") + "\"))\n");

			} else if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
				String shortFormat = "";
				String dbFormat = "";
				if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
					shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy");
					dbFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "yyyy-MM-dd");
				}
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $date.toDate(\"yyyy-MM-dd\", \"" + dbFormat + "\")))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ShortFormat\", \"" + shortFormat + "\"))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "DBFormat\", \"" + dbFormat + "\"))\n");
			} else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
				String shortFormat = "";
				if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
					shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "H:mm:ss");
				}
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $date.toDate(\"H:mm:ss\", \"" + shortFormat + "\")))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ShortFormat\", \"" + shortFormat + "\"))\n");

			} else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
				String shortFormat = "";
				String longFormat = "";
				String dbFormat = "";
				if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
					shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy");
					longFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy H:mm:ss");
					dbFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "yyyy-MM-dd H:mm:ss");
				}

				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $date.toDate(\"yyyy-MM-dd H:mm:ss\", \"" + dbFormat + "\")))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ShortFormat\", \"" + shortFormat + "\"))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "DBFormat\", \"" + dbFormat + "\"))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "LongFormat\", \"" + longFormat + "\"))\n");

			} else if (field.getFieldType().equals(Field.FieldType.BUTTON.toString())) {
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ButtonValue\", \"" + (field.getFieldName() == null ? "" : field.getFieldName())
						+ "\"))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "ButtonCode\", \"" + (field.getValues() == null ? "" : field.getValues()) + "\"))\n");

			} else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {

				// Get the Category Field
				Category category = categoryAPI.find(field.getValues(), systemUser, false);

				// Get all the Contentlets Categories
				List<Category> selectedCategories = categoryAPI.getParents(content, systemUser, false);

				// Initialize variables
				String catNames = "";
				String catKeys = "";
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
					Iterator<Category> it = categoryList.iterator();
					while (it.hasNext()) {
						Category cat = (Category) it.next();
						catInodes += "\"" +cat.getInode()+ "\"" ;
						catNames += "\"" + cat.getCategoryName() + "\"";
						catKeys += "\"" + cat.getKey() + "\"";
						if (it.hasNext()) {
							catInodes += ",";
							catNames += ",";
							catKeys += ",";
						}
					}
				}

				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategories = $categories.filterCategoriesByUserPermissions([" + catInodes + "] ))\n");

				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes = $contents.getEmptyList())\n");
				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames = $contents.getEmptyList())\n");
				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys = $contents.getEmptyList())\n");
				sb.append("#foreach ($dotcms_content_" + content.getIdentifier() + "_filteredCategory in $dotcms_content_" + content.getIdentifier() + "_filteredCategories)\n");
				sb.append("#set($_dummy = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes.add($dotcms_content_" + content.getIdentifier() + "_filteredCategory.inode))\n");
				sb.append("#set($_dummy = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames.add($dotcms_content_" + content.getIdentifier() + "_filteredCategory.categoryName))\n");
				sb.append("#if ($UtilMethods.isSet($dotcms_content_" + content.getIdentifier() + "_filteredCategory.key))\n");
				sb.append("#set($_dummy = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys.add($dotcms_content_" + content.getIdentifier() + "_filteredCategory.key))\n");
				sb.append("#else\n");
				sb.append("#set($_dummy = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys.add(''))\n");
				sb.append("#end\n");
				sb.append("#end\n");

				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "CategoryObjects\", $dotcms_content_" + content.getIdentifier() + "_filteredCategories ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "Categories\", $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes ))\n");
				//http://jira.dotmarketing.net/browse/DOTCMS-2288
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "\", $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "CategoriesNames\", $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames ))\n");
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"" + field.getVelocityVarName() + "CategoriesKeys\", $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys ))\n");

				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategories = $contents.getEmptyList())\n");
				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes = $contents.getEmptyList())\n");
				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames = $contents.getEmptyList())\n");
				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys = $contents.getEmptyList())\n");
			}

		}

		// get the contentlet categories to make a list
		String categories = "";
		String categoryNames = "";
		String categoryKeys = "";
		Set<Category> categoryList = new HashSet<Category>(categoryAPI.getParents(content, systemUser, false));
		if (categoryList != null) {
			Iterator<Category> it = categoryList.iterator();
			while (it.hasNext()) {
				Category category = it.next();
				categories += "\"" +category.getInode()+"\"";
				categoryNames += "\"" + category.getCategoryName() + "\"";
				categoryKeys += "\"" + category.getKey() + "\"";
				if (it.hasNext()) {
					categories += ",";
					categoryNames += ",";
					categoryKeys += ",";
				}
			}
		}

		// sets the categories as a list on velocity
		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategories = $categories.filterCategoriesByUserPermissions([" + categories + "] ))\n");

		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes = $contents.getEmptyList())\n");
		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames = $contents.getEmptyList())\n");
		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys = $contents.getEmptyList())\n");
		sb.append("#foreach ($dotcms_content_" + content.getIdentifier() + "_filteredCategory in $dotcms_content_" + content.getIdentifier() + "_filteredCategories)\n");
		sb.append("#set($_dummy = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes.add($dotcms_content_" + content.getIdentifier() + "_filteredCategory.inode))\n");
		sb.append("#set($_dummy = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames.add($dotcms_content_" + content.getIdentifier() + "_filteredCategory.categoryName))\n");
		sb.append("#if ($UtilMethods.isSet($dotcms_content_" + content.getIdentifier() + "_filteredCategory.key))\n");
		sb.append("#set($_dummy = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys.add($dotcms_content_" + content.getIdentifier() + "_filteredCategory.key))\n");
		sb.append("#else\n");
		sb.append("#set($_dummy = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys.add(''))\n");
		sb.append("#end\n");
		sb.append("#end\n");

		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"ContentletCategoryObjects\", $dotcms_content_" + content.getIdentifier() + "_filteredCategories ))\n");
		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"contentletCategoryObjects\", $dotcms_content_" + content.getIdentifier() + "_filteredCategories ))\n");
		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"ContentletCategories\", $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes ))\n");
		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"contentletCategories\", $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes ))\n");
		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"ContentletCategoriesNames\", $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames ))\n");
		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"contentletCategoriesNames\", $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames ))\n");
		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"ContentletCategoriesKeys\", $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys ))\n");
		sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"contentletCategoriesKeys\", $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys ))\n");

		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategories = $contents.getEmptyList())\n");
		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes = $contents.getEmptyList())\n");
		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames = $contents.getEmptyList())\n");
		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys = $contents.getEmptyList())\n");
//		Was put in to fix DOTCMS-995 but it caused DOTCMS-1210.
//      I actually think it should be fine passed the ctx which is a chained context here
//		sb.append("#set($velocityContext = $UtilMethods.popVelocityContext($velocityContext))\n");

		sb.append(widgetCode);

		if(structure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET){
			sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"isWidget\", \"" + true + "\"  ))\n");
			if(structure.getName().equals(FormAPI.FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME)){
				sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"isFormWidget\", \"" + true + "\"  ))\n");
			}
		}else{
			sb.append("#set($_dummy = $!dotcms_content_" + content.getIdentifier() + ".put(\"isWidget\", \"" + false + "\"  ))\n");
		}

		sb.append("#set ($content = $dotcms_content_" + content.getIdentifier() + ")");

		if(Config.getBooleanProperty("SHOW_VELOCITYFILES", false)){
			try {

				String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");

				if (velocityRootPath.startsWith("/WEB-INF")) {
					velocityRootPath = Config.CONTEXT.getRealPath(velocityRootPath);
				}
				velocityRootPath += java.io.File.separator;

				String veloExt = Config.getStringProperty("VELOCITY_CONTENT_MAP_EXTENSION");
				String baseFilename = String.format("%s_%d.%s", content.getIdentifier(), content.getLanguageId(), veloExt);

				// Save always to working
				String filePath = "working" + java.io.File.separator + baseFilename;
				saveToDisk(ConfigUtils.getDynamicVelocityPath()+java.io.File.separator,filePath, sb.toString());

				// Save to live, if publishing
				if (!EDIT_MODE) {
					filePath = "live" + java.io.File.separator + baseFilename;
					saveToDisk(ConfigUtils.getDynamicVelocityPath()+java.io.File.separator,filePath, sb.toString());
				}

			} catch (Exception e) {
				Logger.error(ContentletMapServices.class, e.toString(), e);
			}
		}
		try {
			result = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			result = new ByteArrayInputStream(sb.toString().getBytes());
			Logger.error(ContainerServices.class,e1.getMessage(), e1);
		}
        return result;

	}

	public static void unpublishContentletMapFile(Contentlet asset) throws DotDataException {

		Identifier identifier = APILocator.getIdentifierAPI().find(asset);
		removeContentletMapFile(asset, identifier, false);
	}

	/**
	 * Will remove all contentlet map files within a structure for both live and working. Uses the system user.
	 * @param contentlets
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public static void removeContentletMapFile(Structure structure) throws DotDataException, DotSecurityException{
		ContentletAPI conAPI = APILocator.getContentletAPI();
		int limit = 500;
		int offset = 0;
		List<Contentlet> contentlets = conAPI.findByStructure(structure, APILocator.getUserAPI().getSystemUser(), false, limit, offset);
		int size = contentlets.size();
		while(size > 0){
			for (Contentlet contentlet : contentlets) {
				removeContentletMapFile(contentlet);
			}
			offset += limit;
			contentlets = conAPI.findByStructure(structure, APILocator.getUserAPI().getSystemUser(), false, limit, offset);
			size = contentlets.size();
		}
	}

	/**
	 * Will remove all contentlet map files for both live and working
	 * @param contentlets
	 * @throws DotDataException
	 */
	public static void removeContentletMapFile(Contentlet contentlet) throws DotDataException{
		removeContentletMapFile(contentlet, true);
		removeContentletMapFile(contentlet, false);
	}

	/**
	 * Will remove all contentlet map files for both live and working
	 * @param contentlets
	 */
	public static void removeContentletMapFile(List<Contentlet> contentlets) throws DotDataException{
		for (Contentlet contentlet : contentlets) {
			removeContentletMapFile(contentlet);
		}
	}

	public static void removeContentletMapFile(Contentlet asset, boolean EDIT_MODE) throws DotDataException {

		Identifier identifier = APILocator.getIdentifierAPI().find(asset);
		removeContentletMapFile(asset, identifier, EDIT_MODE);
	}

	public static void removeContentletMapFile(Contentlet asset, Identifier identifier, boolean EDIT_MODE) {
		String folderPath = (!EDIT_MODE) ? "live/" : "working/";
		String velocityRoot = Config.CONTEXT.getRealPath("/WEB-INF/velocity/") + folderPath;
		String filePath=  folderPath + identifier.getInode() + "_" + asset.getLanguageId() + "." + Config.getStringProperty("VELOCITY_CONTENT_MAP_EXTENSION");
		java.io.File f = new java.io.File (velocityRoot + filePath);
		f.delete();
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
        List<Field> fields = FieldsCache.getFieldsByStructureInode(asset.getStructureInode());
        for (Field field : fields) {
			try {
				FieldServices.invalidate(field.getInode(), asset.getInode(), EDIT_MODE);
			} catch (DotDataException e) {
				Logger.error(ContentletServices.class,e.getMessage(),e);
			} catch (DotSecurityException e) {
				Logger.error(ContentletServices.class,e.getMessage(),e);
			}
		}
	}

	/**
	 * Returns the details page URI for a given <tt>contentlet</tt>. (Used by
	 * #detailPageLink macro)
	 *
	 * @param the
	 *            given <tt>contentlet</tt>
	 * @return the details page URI
	 *
	 * @author Dimitris Zavaliadis
	 * @version 1.0
	 */
	private static String getDetailPageURI(Contentlet contentlet) {
		String detailPageURI = null;
		Structure structure = contentlet.getStructure();
		String detailPageId = structure.getDetailPage();
		try {
			Identifier pageIdentifier = APILocator.getIdentifierAPI().find(detailPageId);
			if (!InodeUtils.isSet(pageIdentifier.getInode()) && UtilMethods.isSet(detailPageId)) {
				pageIdentifier = APILocator.getIdentifierAPI().find((HTMLPage) InodeFactory.getInode(detailPageId, HTMLPage.class));
			}
			detailPageURI = pageIdentifier.getURI();
		} catch (Exception e) {
			Logger.error(ContentletMapServices.class, e.getMessage());
		}
		return detailPageURI;
	}

	private static void saveToDisk(String folderPath, String filePath, String data) throws IOException {

		java.io.BufferedOutputStream tmpOut = new java.io.BufferedOutputStream(new java.io.FileOutputStream(new java.io.File(folderPath+ filePath)));

		// Specify a proper character encoding
		OutputStreamWriter out = new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration());

		out.write(data);

		out.flush();
		out.close();
		tmpOut.close();
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );

	}

}
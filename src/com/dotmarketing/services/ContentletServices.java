package com.dotmarketing.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.velocity.runtime.resource.ResourceManager;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.comparators.ContentComparator;
import com.dotmarketing.comparators.WebAssetSortOrderComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.form.business.FormAPI;
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
public class ContentletServices {

	private static CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	
	public static CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public static void setCategoryAPI(CategoryAPI categoryAPI) {
		ContentletServices.categoryAPI = categoryAPI;
	}
     

	public static void invalidate(Contentlet contentlet) throws DotDataException, DotSecurityException {

		Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
		invalidate(contentlet, identifier, false);
		invalidate(contentlet, identifier, true);
	}

	public static void invalidate(Contentlet contentlet, boolean EDIT_MODE) throws DotDataException, DotSecurityException {

		Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
		invalidate(contentlet, identifier, EDIT_MODE);
	}

	public static void invalidate(Contentlet content, Identifier identifier, boolean EDIT_MODE) throws DotDataException, DotSecurityException {
		removeContentletFile(content, identifier, EDIT_MODE);
	}

	public static InputStream buildVelocity(Contentlet content, Identifier identifier, boolean EDIT_MODE) throws DotDataException, DotSecurityException {
		InputStream result;
		ContentletAPI conAPI = APILocator.getContentletAPI();
		FieldAPI fAPI = APILocator.getFieldAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();
		

		// let's write this puppy out to our file
		StringBuilder sb = new StringBuilder();

		// CONTENTLET CONTROLS BEGIN
		sb.append("#if($EDIT_MODE)");
		sb.append("     #set( $EDIT_CONTENT_PERMISSION =$EDIT_CONTENT_PERMISSION" + identifier.getInode() + " )\n");
		sb.append("#end");
		
		sb.append("#set( $CONTENT_INODE ='" + content.getInode() + "' )\n");
		sb.append("#set( $IDENTIFIER_INODE ='" + identifier.getInode() + "' )\n");

		// set all properties from the contentlet
		sb.append("##Set Content properties\n");
		sb.append("#set( $ContentInode ='" + content.getInode() + "' )\n");
		sb.append("#set( $ContentIdentifier ='" + identifier.getInode() + "' )\n");
		sb.append("#set( $ContentletTitle =\"" + UtilMethods.espaceForVelocity(conAPI.getName(content, APILocator.getUserAPI().getSystemUser(), true)) + "\" )\n");
		String modDateStr = UtilMethods.dateToHTMLDate((Date) content.getModDate(), "yyyy-MM-dd H:mm:ss");
		sb.append("#set( $ContentLastModDate = $date.toDate(\"yyyy-MM-dd H:mm:ss\", \"" + modDateStr + "\"))\n");
		sb.append("#set( $ContentLastModUserId = \"" + content.getModUser() + "\")\n");
		if (content.getOwner() != null)
			sb.append("#set( $ContentOwnerId = \"" + content.getOwner() + "\")\n");

		// Structure fields

		Structure structure = content.getStructure();
		
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
		Iterator<Field> fieldsIt = fields.iterator();

		String widgetCode = "";
		
		while (fieldsIt.hasNext()) {
			Field field = (Field) fieldsIt.next();

			sb.append("\n\n##Set Field " + field.getFieldName() + " properties\n");

			String contField = field.getFieldContentlet();
			String contFieldValue = null;
			Object contFieldValueObject = null;
			String velPath = (!EDIT_MODE) ? "live/" : "working/";
			if(fAPI.isElementConstant(field)){
			    if(field.getVelocityVarName().equals("widgetPreexecute")){
					continue;
				}
				if(field.getVelocityVarName().equals("widgetCode")){
//					widgetCode = "#set( $" + field.getVelocityVarName() + " = $UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('"
//					+ UtilMethods.espaceVariableForVelocity(field.getValues()) + "'), $velocityContext))\n";
//					widgetCode = "#set( $" + field.getVelocityVarName() + " = '" + field.getValues() + "')\n";
//					widgetCode = "#set( $" + field.getVelocityVarName() + " =\"" + UtilMethods.espaceForVelocity(field.getValues()).trim() + "\" )\n";
//					widgetCode = "#set( $fieldStringWriter" + content.getInode() + field.getInode()  + " = $stringsapi.getEmptyStringWriter())\n";
//					widgetCode += "$UtilMethods.getVelocityTemplate(\"" + folderPath + content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\").merge($context, $fieldStringWriter" + content.getInode() + field.getInode()  + ")\n";
//					widgetCode += "#set( $" + field.getVelocityVarName() + " = $fieldStringWriter" +  content.getInode() + field.getInode()  + ".toString())\n";
					widgetCode = "#set( $" + field.getVelocityVarName() + " = $velutil.mergeTemplate(\"" + velPath +  content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\"))\n";
					continue;
				}else{
//					sb.append("#set( $" + field.getVelocityVarName() + " = $UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('"
//							+ UtilMethods.espaceVariableForVelocity(field.getValues()) + "'), $velocityContext))\n");
//					sb.append("#set( $" + field.getVelocityVarName() + " = \"" + UtilMethods.espaceForVelocity(field.getValues()).trim() + "\")\n");
//					sb.append("#set( $fieldStringWriter" + content.getInode() + field.getInode()  + " = $stringsapi.getEmptyStringWriter())\n");
//					sb.append("$UtilMethods.getVelocityTemplate(\"" + folderPath + content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\").merge($context, $fieldStringWriter" + content.getInode() + field.getInode()  + ")\n");
//					sb.append("#set( $" + field.getVelocityVarName() + " = $fieldStringWriter" + content.getInode() + field.getInode()  + ".toString())\n");
					String fieldValues=field.getValues()==null?"":field.getValues();
					if(fieldValues.contains("$") || fieldValues.contains("#")){
						sb.append("#set( $" + field.getVelocityVarName() + " = $velutil.mergeTemplate(\"" + velPath +  content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\"))\n");
					}else{
						sb.append("#set( $" + field.getVelocityVarName() + " = \"" + UtilMethods.espaceForVelocity(fieldValues).trim() + "\")\n");
					}
					continue;
				}
			}
			if (UtilMethods.isSet(contField)) {
				try {
					contFieldValueObject = conAPI.getFieldValue(content, field);
					contFieldValue = contFieldValueObject == null ? "" : contFieldValueObject.toString();
				} catch (Exception e) {
					Logger.error(ContentletServices.class, "writeContentletToFile: " + e.getMessage());
				}
				if (!field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()) && !field.getFieldType().equals(Field.FieldType.DATE.toString())
						&& !field.getFieldType().equals(Field.FieldType.TIME.toString())){
//					sb.append("#set( $" + field.getVelocityVarName() + " = $UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('"
//							+ UtilMethods.espaceVariableForVelocity(contFieldValue) + "'), $velocityContext))\n");
//					sb.append("#set( $" + field.getVelocityVarName() + " = \"" + UtilMethods.espaceForVelocity(contFieldValue).trim() + "\")\n");
//					sb.append("#set( $fieldStringWriter" + content.getInode() + field.getInode()  + " = $stringsapi.getEmptyStringWriter())\n");
//					sb.append("$UtilMethods.getVelocityTemplate(\"" + folderPath + content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\").merge($context, $fieldStringWriter" + content.getInode() + field.getInode()  + ")\n");
//					sb.append("#set( $" + field.getVelocityVarName() + " = $fieldStringWriter" + content.getInode() + field.getInode()  + ".toString())\n");
					if(contFieldValue.contains("$") || contFieldValue.contains("#")){
						sb.append("#set( $" + field.getVelocityVarName() + " = $velutil.mergeTemplate(\"" + velPath +  content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\"))\n");
					}else{
						sb.append("#set( $" + field.getVelocityVarName() + " = \"" + UtilMethods.espaceForVelocity(contFieldValue).trim() + "\")\n");
					}
				}
				
			}

			if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())) {
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
				
				sb.append("#set( $" + field.getVelocityVarName() + "ImageInode =$" + field.getVelocityVarName() + "Object.getInode() )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageIdentifier =$" + field.getVelocityVarName() + "Object.getIdentifier() )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageWidth =$" + field.getVelocityVarName() + "Object.getWidth() )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageHeight =$" + field.getVelocityVarName() + "Object.getHeight() )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageExtension =$" + field.getVelocityVarName() + "Object.getExtension() )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageURI =$filetool.getURI($" + field.getVelocityVarName() + "Object))\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageTitle =$UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + "Object.getTitle()) )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageFriendlyName =$UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + "Object.getFriendlyName()) )\n");
				
				sb.append("#set( $" + field.getVelocityVarName() + "ImagePath =$" + field.getVelocityVarName() + "Object.getPath() )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageName =$" + field.getVelocityVarName() + "Object.getNameOnly() )\n");
				
			}//	http://jira.dotmarketing.net/browse/DOTCMS-2178
			else if (field.getFieldType().equals(Field.FieldType.BINARY.toString())){
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
			   	sb.append("#set( $" + field.getVelocityVarName() + "BinaryFileTitle =\"" + UtilMethods.espaceForVelocity(fileName) + "\" )\n");	
				sb.append("#set( $" + field.getVelocityVarName() + "BinaryFileSize =\"" + UtilMethods.espaceForVelocity(filesize) + "\" )\n");
				String binaryFileURI = fileName.length()>0? UtilMethods.espaceForVelocity("/contentAsset/raw-data/"+content.getIdentifier()+"/"+ field.getVelocityVarName() + "/" + content.getInode()):"";
				sb.append("#set( $" + field.getVelocityVarName() + "BinaryFileURI =\"" + binaryFileURI + "\" )\n");	
			}else if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
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
				
				sb.append("#set( $" + field.getVelocityVarName() + "FileInode =$" + field.getVelocityVarName() + "Object.getInode() )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "FileIdentifier =$" + field.getVelocityVarName() + "Object.getIdentifier() )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "FileExtension =$" + field.getVelocityVarName() + "Object.getExtension() )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageURI =$filetool.getURI($" + field.getVelocityVarName() + "Object))\n");
				sb.append("#set( $" + field.getVelocityVarName() + "FileTitle =$" + field.getVelocityVarName() + "Object.getTitle() )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "FileFriendlyName =$UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + "Object.getFriendlyName() ))\n");
				
				sb.append("#set( $" + field.getVelocityVarName() + "FilePath =$UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + "Object.getPath()) )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "FileName =$UtilMethods.espaceForVelocity($" + field.getVelocityVarName() + "Object.getNameOnly()) )\n");
				
				
			} else if (field.getFieldType().equals(Field.FieldType.SELECT.toString())) {
				sb.append("#set( $" + field.getVelocityVarName() + "SelectLabelsValues = \"" + field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") + "\")\n");
			} else if (field.getFieldType().equals(Field.FieldType.RADIO.toString())) {
				sb.append("#set( $" + field.getVelocityVarName() + "RadioLabelsValues = \"" + field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") + "\" )\n");
			} else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())) {
				sb.append("#set( $" + field.getVelocityVarName() + "CheckboxLabelsValues = \"" + field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") + "\" )\n");
			} else if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
				String shortFormat = "";
				String dbFormat = "";
				if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
					shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy");
					dbFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "yyyy-MM-dd");
				}
				sb.append("#set( $" + field.getVelocityVarName() + " = $date.toDate(\"yyyy-MM-dd\", \"" + dbFormat + "\"))\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ShortFormat =\"" + shortFormat + "\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "DBFormat =\"" + dbFormat + "\" )\n");
			} else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
				String shortFormat = "";
				if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
					shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "H:mm:ss");
				}
				sb.append("#set( $" + field.getVelocityVarName() + "ShortFormat =\"" + shortFormat + "\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + " = $date.toDate(\"H:mm:ss\", \"" + shortFormat + "\"))\n");
			} else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
				String shortFormat = "";
				String longFormat = "";
				String dbFormat = "";
				if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
					shortFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy");
					longFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy H:mm:ss");
					dbFormat = UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "yyyy-MM-dd H:mm:ss");
				}
				sb.append("#set( $" + field.getVelocityVarName() + " = $date.toDate(\"yyyy-MM-dd H:mm:ss\", \"" + dbFormat + "\"))\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ShortFormat =\"" + shortFormat + "\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "LongFormat =\"" + longFormat + "\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "DBFormat =\"" + dbFormat + "\" )\n");
			} else if (field.getFieldType().equals(Field.FieldType.BUTTON.toString())) {
				sb.append("#set( $" + field.getVelocityVarName() + "ButtonValue =\"" + (field.getFieldName() == null ? "" : field.getFieldName()) + "\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ButtonCode =\"" + (field.getValues() == null ? "" : field.getValues()) + "\" )\n");
			}//http://jira.dotmarketing.net/browse/DOTCMS-2869
//			else if (field.getFieldType().equals(Field.FieldType.CUSTOM_FIELD.toString())){
//				 sb.append("#set( $" + field.getVelocityVarName() + "Code =\"" + UtilMethods.espaceForVelocity(field.getValues()) + "\" )\n");
//			}//http://jira.dotmarketing.net/browse/DOTCMS-3232
			else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
				if(InodeUtils.isSet(content.getFolder())){
					sb.append("#set( $ConHostFolder ='" + content.getFolder() + "' )\n");
				}else{
					sb.append("#set( $ConHostFolder ='" + content.getHost() + "' )\n");
				}
			}

			else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {

				// Get the Category Field
				Category category = categoryAPI.find(field.getValues(), systemUser, false);
				// Get all the Contentlets Categories
				List<Category> selectedCategories = categoryAPI.getParents(content, systemUser, false);

        		//Initialize variables
        		String catNames  = "";
        		String catInodes = "";
        		String catKeys = "";
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
						catInodes += "\"" +cat.getInode()+ "\"";
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
				
				sb.append("#set( $" + field.getVelocityVarName() + "FilteredCategories = $dotcms_content_" + content.getIdentifier() + "_filteredCategories )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "Categories = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "CategoriesNames = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames )\n");
				//http://jira.dotmarketing.net/browse/DOTCMS-2288
				sb.append("#set( $" + field.getVelocityVarName() + " = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "CategoriesKeys = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys )\n");
				
				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategories = $contents.getEmptyList())\n");
				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes = $contents.getEmptyList())\n");
				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames = $contents.getEmptyList())\n");
				sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys = $contents.getEmptyList())\n");
			}
		}

		
        // get the contentlet categories to make a list
        String categories = "";
        String categoryNames = "";
		String catKeys = "";
        Set<Category> categoryList = new HashSet<Category>(categoryAPI.getParents(content, systemUser, false)); 
        if (categoryList!=null) {
			Iterator<Category> it = categoryList.iterator();
			while (it.hasNext()) {
				Category category = (Category) it.next();
	        	categories += "\"" + category.getInode()+ "\"" ;
	        	categoryNames += "\"" + category.getCategoryName() + "\"";
				catKeys += "\"" + category.getKey() + "\"";
				if (it.hasNext()) {
	        		categories += ",";
	        		categoryNames += ",";
					catKeys += ",";
	        	}
	        }
        }


        //sets the categories as a list on velocity
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
        
		sb.append("#set( $ContentletFilteredCategories = $dotcms_content_" + content.getIdentifier() + "_filteredCategories )\n");
        sb.append("#set( $ContentletCategories = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes )\n");
        sb.append("#set( $ContentletCategoryNames = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames )\n");
        sb.append("#set( $ContentletCategoryKeys = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys )\n");
        sb.append("#set( $contentletFilteredCategories = $dotcms_content_" + content.getIdentifier() + "_filteredCategories )\n");
        sb.append("#set( $contentletCategories = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes )\n");
        sb.append("#set( $contentletCategoryNames = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames )\n");
        sb.append("#set( $contentletCategoryKeys = $dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys )\n");
        
        sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategories = $contents.getEmptyList())\n");
		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesInodes = $contents.getEmptyList())\n");
		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesNames = $contents.getEmptyList())\n");
		sb.append("#set($dotcms_content_" + content.getIdentifier() + "_filteredCategoriesKeys = $contents.getEmptyList())\n");

		//This needs to be here because the all fields like cats etc.. need to be parsed first and it needs to be before
		// the $CONTENT_INODE is reset sb.append("#set( $CONTENT_INODE =\"" + content.getInode() + "\" )\n");
		//http://jira.dotmarketing.net/browse/DOTCMS-2808
		sb.append(widgetCode);
		
		// This is code is repeated because the bug GETTYS-268, the content
		// variables were been overwritten
		// by the parse inside the some of the content fields
		// To edit the look, see
		// WEB-INF/velocity/static/preview/content_controls.vtl

		if(EDIT_MODE) {
			sb.append("#set( $EDIT_CONTENT_PERMISSION =$EDIT_CONTENT_PERMISSION" + identifier.getInode() + " )\n");
		}
	
		sb.append("#set( $CONTENT_INODE =\"" + content.getInode() + "\" )\n");
		sb.append("#set( $IDENTIFIER_INODE =\"" + identifier.getInode() + "\" )\n");
		sb.append("##Set Content properties\n");
		sb.append("#set( $ContentInode =\"" + content.getInode() + "\" )\n");
		sb.append("#set( $ContentIdentifier =\"" + identifier.getInode() + "\" )\n");
		sb.append("#set( $ContentletTitle =\"" + UtilMethods.espaceForVelocity(conAPI.getName(content, APILocator.getUserAPI().getSystemUser(), true)) + "\" )\n");
		
		if(structure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET){
			sb.append("#set( $isWidget = \"" + true + "\")\n");
			if(structure.getName().equals(FormAPI.FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME)){
				sb.append("#set( $isFormWidget = \"" + true + "\")\n");
			}
		}else{
			sb.append("#set( $isWidget = \"" + false + "\")\n");
		}
		
		if(Config.getBooleanProperty("SHOW_VELOCITYFILES", false)){
			try {

				String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");

				if (velocityRootPath.startsWith("/WEB-INF")) {
					velocityRootPath = Config.CONTEXT.getRealPath(velocityRootPath);
				}
				velocityRootPath += java.io.File.separator;

				String veloExt = Config.getStringProperty("VELOCITY_CONTENT_EXTENSION");
				String baseFilename = String.format("%s_%d.%s", identifier.getInode(), content.getLanguageId(), veloExt);

				// Save always to working
				String filePath = "working" + java.io.File.separator + baseFilename;

				saveToDisk(ConfigUtils.getDynamicVelocityPath()+java.io.File.separator,filePath, sb.toString());

				// Save to live, if publishing
				if (!EDIT_MODE) {
					filePath = "live" + java.io.File.separator + baseFilename;
					saveToDisk(ConfigUtils.getDynamicVelocityPath()+java.io.File.separator,filePath, sb.toString());
				}

			} catch (Exception e) {
				Logger.error(ContentletServices.class, e.toString(), e);
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
	
	@SuppressWarnings("unchecked")
	public static List<Contentlet> sortContentlets(List<Contentlet> contentletList, String sortBy) {

		Logger.debug(ContentletServices.class, "I'm ordering by " + sortBy);

		if (sortBy.equals("tree_order")) {
			Collections.sort(contentletList, new WebAssetSortOrderComparator());
		} else {
			Collections.sort(contentletList, new ContentComparator("asc"));
		}

		return contentletList;

	}

	public static void unpublishContentletFile(Contentlet asset) throws DotStateException, DotDataException {

		Identifier identifier;
		try {
			identifier = APILocator.getIdentifierAPI().find(asset);
			removeContentletFile(asset, identifier, false);
		} catch (DotHibernateException e) {
			Logger.error(ContainerServices.class, "Unable to retrieve Identifier", e);
		}

	}

	public static void removeContentletFile(Contentlet asset, boolean EDIT_MODE) throws DotStateException, DotDataException {
		try {
			Identifier identifier = APILocator.getIdentifierAPI().find(asset);
			removeContentletFile(asset, identifier, EDIT_MODE);
		} catch (DotHibernateException e) {
			Logger.error(ContainerServices.class,"Unable to retrieve Identifier", e);
		}
	}

	/**
	 * Will remove all contentlet files within a structure for both live and working. Uses the system user.
	 * @param contentlets
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public static void removeContentletFile(Structure structure) throws DotDataException, DotSecurityException{
		ContentletAPI conAPI = APILocator.getContentletAPI();
		int limit = 500;
		int offset = 0;
		List<Contentlet> contentlets = conAPI.findByStructure(structure, APILocator.getUserAPI().getSystemUser(), false, limit, offset);
		int size = contentlets.size();
		while(size > 0){
			for (Contentlet contentlet : contentlets) {
				removeContentletFile(contentlet);
			}
			offset += limit;
			contentlets = conAPI.findByStructure(structure, APILocator.getUserAPI().getSystemUser(), false, limit, offset);
			size = contentlets.size();
		}
	}
	
	/**
	 * Will remove all contentlet files for both live and working
	 * @param contentlets
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public static void removeContentletFile(Contentlet contentlet) throws DotStateException, DotDataException{
		removeContentletFile(contentlet, true);
		removeContentletFile(contentlet, false);
	}
	
	/**
	 * Will remove all contentlet files for both live and working
	 * @param contentlets
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public static void removeContentletFile(List<Contentlet> contentlets) throws DotStateException, DotDataException{
		for (Contentlet contentlet : contentlets) {
			removeContentletFile(contentlet);
		}
	}
	
	public static void removeContentletFile(Contentlet asset, Identifier identifier, boolean EDIT_MODE) {
		String folderPath = (!EDIT_MODE) ? "live" + java.io.File.separator : "working" + java.io.File.separator;
		String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
		if (velocityRootPath.startsWith("/WEB-INF")) {
			velocityRootPath = Config.CONTEXT.getRealPath(velocityRootPath);
		}
		velocityRootPath += java.io.File.separator;
		String filePath= folderPath + identifier.getInode() + "_" + asset.getLanguageId() + "." + Config.getStringProperty("VELOCITY_CONTENT_EXTENSION");
		java.io.File f = new java.io.File(velocityRootPath + filePath);
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
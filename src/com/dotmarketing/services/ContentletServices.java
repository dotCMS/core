package com.dotmarketing.services;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.comparators.ContentComparator;
import com.dotmarketing.comparators.WebAssetSortOrderComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.velocity.DotResourceCache;
import com.dotmarketing.viewtools.LanguageWebAPI;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import org.apache.velocity.runtime.resource.ResourceManager;

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

/**
 * @author will
 */
public class ContentletServices {

	private static CategoryAPI categoryAPI= APILocator.getCategoryAPI();

	public static CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public static void setCategoryAPI(CategoryAPI categoryAPI) {
		ContentletServices.categoryAPI= categoryAPI;
	}

    /**
     * Invalidate contentlet that is live and working
     *
     * @param contentlet
     * @throws DotDataException
     * @throws DotSecurityException
     */
	public static void invalidateAll(Contentlet contentlet) throws DotDataException, DotSecurityException {
		Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);

		// Live
		LiveCache.removeAssetFromCache(contentlet);
		invalidate(contentlet, identifier, false);

		// Working
		WorkingCache.removeAssetFromCache(contentlet);
		invalidate(contentlet, identifier, true);

		// Writes the contentlet object to a file
		ContentletMapServices.invalidateAll(contentlet);
	}

    /**
     * Will remove all contentlet files for both live and working
     *
     * @param contentlets
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotStateException
     */
    public static void invalidateAll(List<Contentlet> contentlets) throws DotDataException, DotSecurityException {
        for (Contentlet contentlet : contentlets) {
            invalidateAll(contentlet);
        }
    }

    /**
     * Invalidate contentlet that is live.
     *
     * @param contentlet
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static void invalidateLive(Contentlet contentlet) throws DotDataException, DotSecurityException {
        invalidateLive(contentlet, null);
    }

    /**
     * Invalidate contentlet that is live. <p>NOTE: This method is helpful when
     * you would like to save calls finding the identifier, otherwise you should
     * use the other method {@link #invalidateLive(Contentlet) invalidateLive} where
     * you only pass the contentlet</p>
     *
     * @param contentlet
     * @param identifier
     *            of the contentlet, if its null then it will do a search using
     *            contentlet parameter
     * @throws DotDataException
     * @throws DotSecurityException
     */
	public static void invalidateLive(Contentlet contentlet, Identifier identifier) throws DotDataException, DotSecurityException {
	    Identifier localIdentifier = (identifier == null) ? APILocator.getIdentifierAPI().find(contentlet) : identifier;

        LiveCache.removeAssetFromCache(contentlet);
        invalidate(contentlet, localIdentifier, false);

        // Writes the contentlet object to a file
        ContentletMapServices.invalidateLive(contentlet);
    }

	/**
     * Invalidate contentlet that is working
     *
     * @param contentlet
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static void invalidateWorking(Contentlet contentlet) throws DotDataException, DotSecurityException {
        invalidateWorking(contentlet, null);
    }

    /**
     * Invalidate contentlet that is working. <p>NOTE: This method is helpful when
     * you would like to save calls finding the identifier, otherwise you should
     * use the other method {@link #invalidateWorking(Contentlet) invalidateWorking} where
     * you only pass the contentlet</p>
     *
     * @param contentlet
     * @param identifier
     *            of the contentlet, if its null then it will do a search using
     *            contentlet parameter
     * @throws DotDataException
     * @throws DotSecurityException
     */
	public static void invalidateWorking(Contentlet contentlet, Identifier identifier) throws DotDataException, DotSecurityException {
        Identifier localIdentifier = (identifier == null) ? APILocator.getIdentifierAPI().find(contentlet) : identifier;

        WorkingCache.removeAssetFromCache(contentlet);
        invalidate(contentlet, localIdentifier, true);

        // Writes the contentlet object to a file
        ContentletMapServices.invalidateWorking(contentlet);
    }

	private static void invalidate(Contentlet content, Identifier identifier, boolean EDIT_MODE) throws DotDataException, DotSecurityException {
		removeContentletFile(content, identifier, EDIT_MODE);

		if(content.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_HTMLPAGE) {
		    PageServices.removePageFile(APILocator.getHTMLPageAssetAPI().fromContentlet(content), identifier, EDIT_MODE);
		}
	}

	public static InputStream buildVelocity(Contentlet content, Identifier identifier, boolean EDIT_MODE) throws DotDataException, DotSecurityException {
		InputStream result;
		ContentletAPI conAPI= APILocator.getContentletAPI();
		FieldAPI fAPI= APILocator.getFieldAPI();
		User systemUser= APILocator.getUserAPI().getSystemUser();


		// let's write this puppy out to our file
		StringBuilder sb= new StringBuilder();

		// CONTENTLET CONTROLS BEGIN
		sb.append("#if($EDIT_MODE)");
		sb.append("#set( $EDIT_CONTENT_PERMISSION=$EDIT_CONTENT_PERMISSION" ).append( identifier.getInode() ).append( ")");
		sb.append("#end");

		sb.append("#set($CONTENT_INODE='" ).append( content.getInode() ).append( "' )");
		sb.append("#set($IDENTIFIER_INODE='" ).append( identifier.getInode() ).append( "' )");

		// set all properties from the contentlet
		sb.append("#set($ContentInode='" ).append( content.getInode() ).append( "' )");
		sb.append("#set($ContentIdentifier='" ).append( identifier.getInode() ).append( "' )");
		sb.append("#set($ContentletTitle=\"" ).append( UtilMethods.espaceForVelocity(conAPI.getName(content, APILocator.getUserAPI().getSystemUser(), true)) ).append( "\" )");
		String modDateStr= UtilMethods.dateToHTMLDate((Date) content.getModDate(), "yyyy-MM-dd H:mm:ss");
		sb.append("#set($ContentLastModDate= $date.toDate(\"yyyy-MM-dd H:mm:ss\", \"" ).append( modDateStr ).append( "\"))");
		sb.append("#set($ContentLastModUserId= \"" ).append( content.getModUser() ).append( "\")");
		if (content.getOwner() != null)
			sb.append("#set($ContentOwnerId= \"" ).append( content.getOwner() ).append( "\")");

		// Structure fields

		Structure structure= content.getStructure();

		List<Field> fields= FieldsCache.getFieldsByStructureInode(structure.getInode());
		Iterator<Field> fieldsIt= fields.iterator();

		String widgetCode= "";

		while (fieldsIt.hasNext()) {
			Field field= (Field) fieldsIt.next();
			String contField= field.getFieldContentlet();
			String contFieldValue= null;
			Object contFieldValueObject= null;
			String velPath= (!EDIT_MODE) ? "live/" : "working/";
			if(fAPI.isElementConstant(field)){
			    if(field.getVelocityVarName().equals("widgetPreexecute")){
					continue;
				}
				if(field.getVelocityVarName().equals("widgetCode")){
					widgetCode= "#set($" + field.getVelocityVarName() + "=$velutil.mergeTemplate(\"" + velPath +  content.getInode() + "_" + field.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION") + "\"))";
					continue;
				}else{
					String fieldValues=field.getValues()==null?"":field.getValues();
					if(fieldValues.contains("$") || fieldValues.contains("#")){
						sb.append("#set($" ).append( field.getVelocityVarName() ).append( "= $velutil.mergeTemplate(\"" ).append( velPath ).append(  content.getInode() ).append( "_" ).append( field.getInode() ).append( "." ).append( Config.getStringProperty("VELOCITY_FIELD_EXTENSION") ).append( "\"))");
					}else{
						sb.append("#set($" ).append( field.getVelocityVarName() ).append( "= \"" ).append( UtilMethods.espaceForVelocity(fieldValues).trim() ).append( "\")");
					}
					continue;
				}
			}
			if (UtilMethods.isSet(contField)) {
				try {
					contFieldValueObject= conAPI.getFieldValue(content, field);
					contFieldValue= contFieldValueObject== null ? "" : contFieldValueObject.toString();
				} catch (Exception e) {
					Logger.error(ContentletServices.class, "writeContentletToFile: " + e.getMessage());
				}
				if (!field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()) && !field.getFieldType().equals(Field.FieldType.DATE.toString())
						&& !field.getFieldType().equals(Field.FieldType.TIME.toString())){
					if(contFieldValue.contains("$") || contFieldValue.contains("#")){
						sb.append("#set($" ).append( field.getVelocityVarName() ).append( "=$velutil.mergeTemplate(\"" ).append( velPath ).append(  content.getInode() ).append( "_" ).append( field.getInode() ).append( "." ).append( Config.getStringProperty("VELOCITY_FIELD_EXTENSION") ).append( "\"))");
					}else{
						sb.append("#set($" ).append( field.getVelocityVarName() ).append( "=\"" ).append( UtilMethods.espaceForVelocity(contFieldValue).trim() ).append( "\")");
					}
				}

			}

			if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())) {
				String identifierValue= content.getStringProperty(field.getVelocityVarName());
				if( InodeUtils.isSet(identifierValue) ) {
					if (EDIT_MODE){
						sb.append("#set($" ).append( field.getVelocityVarName() ).append( "Object= $filetool.getFile('" ).append( identifierValue ).append( "',false,").append(content.getLanguageId()).append(" ))");
					}else{
						sb.append("#set($" ).append( field.getVelocityVarName() ).append( "Object= $filetool.getFile('" ).append( identifierValue ).append( "',true,").append(content.getLanguageId()).append(" ))");
					}
				}else{
					sb.append("#set($" ).append( field.getVelocityVarName() ).append( "Object= $filetool.getNewFile())");
				}

				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ImageInode=$" ).append( field.getVelocityVarName() ).append( "Object.getInode() )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ImageIdentifier=$" ).append( field.getVelocityVarName() ).append( "Object.getIdentifier() )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ImageWidth=$" ).append( field.getVelocityVarName() ).append( "Object.getWidth() )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ImageHeight=$" ).append( field.getVelocityVarName() ).append( "Object.getHeight() )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ImageExtension=$" ).append( field.getVelocityVarName() ).append( "Object.getExtension() )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ImageURI=$filetool.getURI($" ).append( field.getVelocityVarName() ).append( "Object, ").append(content.getLanguageId()).append(" ))");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ImageTitle=$UtilMethods.espaceForVelocity($" ).append( field.getVelocityVarName() ).append( "Object.getTitle()) )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ImageFriendlyName=$UtilMethods.espaceForVelocity($" ).append( field.getVelocityVarName() ).append( "Object.getFriendlyName()) )");

				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ImagePath=$" ).append( field.getVelocityVarName() ).append( "Object.getPath())");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ImageName=$" ).append( field.getVelocityVarName() ).append( "Object.getFileName())");

			}//	http://jira.dotmarketing.net/browse/DOTCMS-2178
			else if (field.getFieldType().equals(Field.FieldType.BINARY.toString())){
				java.io.File binFile;
				String fileName= "";
				String filesize= "";
				try {
					binFile= content.getBinary(field.getVelocityVarName());
					if(binFile != null) {
						fileName= binFile.getName();
						filesize= FileUtil.getsize(binFile);
					}
				} catch (IOException e) {
					Logger.error(ContentletServices.class, "Unable to retrive binary file for content id " + content.getIdentifier() + " field " + field.getVelocityVarName(), e);
					continue;
				}
			   	sb.append("#set($" ).append( field.getVelocityVarName() ).append( "BinaryFileTitle=\"" ).append( UtilMethods.espaceForVelocity(fileName) ).append( "\" )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "BinaryFileSize=\"" ).append( UtilMethods.espaceForVelocity(filesize) ).append( "\" )");
				String binaryFileURI= fileName.length()>0? UtilMethods.espaceForVelocity("/contentAsset/raw-data/"+content.getIdentifier()+"/"+ field.getVelocityVarName() + "/" + content.getInode()):"";
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "BinaryFileURI=\"" ).append( binaryFileURI).append("\" )");
			}else if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
				String identifierValue= content.getStringProperty(field.getVelocityVarName());
				if( InodeUtils.isSet(identifierValue) ) {
					if (EDIT_MODE){
						sb.append("#set($" ).append( field.getVelocityVarName() ).append( "Object= $filetool.getFile('" ).append( identifierValue ).append( "',false,").append(content.getLanguageId()).append(" ))");
					}else{
						sb.append("#set($" ).append( field.getVelocityVarName() ).append( "Object= $filetool.getFile('" ).append( identifierValue ).append( "',true,").append(content.getLanguageId()).append(" ))");
					}
				}else{
					sb.append("#set($" ).append( field.getVelocityVarName() ).append( "Object= $filetool.getNewFile())");
				}

				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "FileInode=$" ).append( field.getVelocityVarName() ).append( "Object.getInode() )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "FileIdentifier=$" ).append( field.getVelocityVarName() ).append( "Object.getIdentifier() )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "FileExtension=$" ).append( field.getVelocityVarName() ).append( "Object.getExtension() )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "FileURI=$filetool.getURI($" ).append( field.getVelocityVarName() ).append( "Object, ").append(content.getLanguageId()).append(" ))");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "FileTitle=$" ).append( field.getVelocityVarName() ).append( "Object.getTitle() )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "FileFriendlyName=$UtilMethods.espaceForVelocity($" ).append( field.getVelocityVarName() ).append( "Object.getFriendlyName() ))");

				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "FilePath=$UtilMethods.espaceForVelocity($" ).append( field.getVelocityVarName() ).append( "Object.getPath()) )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "FileName=$UtilMethods.espaceForVelocity($" ).append( field.getVelocityVarName() ).append( "Object.getFileName()) )");


			} else if (field.getFieldType().equals(Field.FieldType.SELECT.toString())) {
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "SelectLabelsValues=\"" ).append( field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") ).append( "\")");
			} else if (field.getFieldType().equals(Field.FieldType.RADIO.toString())) {
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "RadioLabelsValues=\"" ).append( field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") ).append( "\" )");
			} else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())) {
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "CheckboxLabelsValues=\"" ).append( field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") ).append( "\" )");
			} else if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
				String shortFormat= "";
				String dbFormat= "";
				if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
					shortFormat= UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy");
					dbFormat= UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "yyyy-MM-dd");
				}
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "=$date.toDate(\"yyyy-MM-dd\", \"" ).append( dbFormat ).append( "\"))");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "ShortFormat=\"" ).append( shortFormat ).append( "\" )");
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "DBFormat=\"" ).append( dbFormat ).append( "\" )");
			} else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
				String shortFormat= "";
				if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
					shortFormat= UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "H:mm:ss");
				}
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ShortFormat=\"" ).append( shortFormat ).append( "\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "= $date.toDate(\"H:mm:ss\", \"" ).append( shortFormat ).append( "\"))");
			} else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
				String shortFormat= "";
				String longFormat= "";
				String dbFormat= "";
				if (contFieldValueObject != null && contFieldValueObject instanceof Date) {
					shortFormat= UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy");
					longFormat= UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "MM/dd/yyyy H:mm:ss");
					dbFormat= UtilMethods.dateToHTMLDate((Date) contFieldValueObject, "yyyy-MM-dd H:mm:ss");
				}
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "= $date.toDate(\"yyyy-MM-dd H:mm:ss\", \"" ).append( dbFormat ).append( "\"))");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ShortFormat=\"" ).append( shortFormat ).append( "\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "LongFormat=\"" ).append( longFormat ).append( "\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "DBFormat=\"" ).append( dbFormat ).append( "\" )");
			} else if (field.getFieldType().equals(Field.FieldType.BUTTON.toString())) {
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ButtonValue=\"" ).append( (field.getFieldName()== null ? "" : field.getFieldName()) ).append( "\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ButtonCode=\"" ).append( (field.getValues()== null ? "" : field.getValues()) ).append( "\" )");
			}//http://jira.dotmarketing.net/browse/DOTCMS-2869
//			else if (field.getFieldType().equals(Field.FieldType.CUSTOM_FIELD.toString())){
//				 sb.append("#set( $" + field.getVelocityVarName() + "Code=\"" + UtilMethods.espaceForVelocity(field.getValues()) + "\" )");
//			}//http://jira.dotmarketing.net/browse/DOTCMS-3232
			else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
				if(InodeUtils.isSet(content.getFolder())){
					sb.append("#set( $ConHostFolder='" ).append( content.getFolder() ).append( "' )");
				}else{
					sb.append("#set( $ConHostFolder='" ).append( content.getHost() ).append( "' )");
				}
			}

			else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {

				// Get the Category Field
				Category category= categoryAPI.find(field.getValues(), systemUser, false);
				// Get all the Contentlets Categories
				List<Category> selectedCategories= categoryAPI.getParents(content, systemUser, false);

        		//Initialize variables
        		String catInodes= "";
        		Set<Category> categoryList= new HashSet<Category>();
				List<Category> categoryTree= categoryAPI.getAllChildren(category, systemUser, false);

				if (selectedCategories.size() > 0 && categoryTree != null) {
					for (int k= 0; k < categoryTree.size(); k++) {
						Category cat= (Category) categoryTree.get(k);
						for (Category categ : selectedCategories) {
							if (categ.getInode().equalsIgnoreCase(cat.getInode())) {
								categoryList.add(cat);
							}
						}
					}
				}

				if (categoryList.size() > 0) {
				    StringBuilder catbuilder=new StringBuilder();
					Iterator<Category> it= categoryList.iterator();
					while (it.hasNext()) {
						Category cat= (Category) it.next();
						catbuilder.append("\"").append(cat.getInode()).append("\"");
						if (it.hasNext()) {
							catbuilder.append(",");
						}
					}
					catInodes=catbuilder.toString();

					sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "FilteredCategories=$categories.filterCategoriesByUserPermissions([" ).append( catInodes ).append( "] ))");
	                sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "Categories=$categories.fetchCategoriesInodes($").append(field.getVelocityVarName()).append("FilteredCategories))");
	                sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "CategoriesNames=$categories.fetchCategoriesNames($").append(field.getVelocityVarName()).append("FilteredCategories))");
	                sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "=$").append(field.getVelocityVarName()).append("Categories)");
	                sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "CategoriesKeys=$categories.fetchCategoriesKeys($").append(field.getVelocityVarName()).append("FilteredCategories))");
				}
				else {
				    sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "FilteredCategories=$contents.getEmptyList())");
	                sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "Categories=$contents.getEmptyList())");
	                sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "CategoriesNames=$contents.getEmptyList())");
	                sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "=$contents.getEmptyList())");
	                sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "CategoriesKeys=$contents.getEmptyList())");
				}
			} else if (field.getFieldType().equals(Field.FieldType.TAG.toString())) {
				content.setTags();
				String value = content.getStringProperty(field.getVelocityVarName());
				sb.append("#set($" ).append( field.getVelocityVarName() ).append( "=\"" ).append( UtilMethods.espaceForVelocity(value).trim() ).append( "\")");
			}
		}


        // get the contentlet categories to make a list
        String categories= "";
        Set<Category> categoryList= new HashSet<Category>(categoryAPI.getParents(content, systemUser, false));
        if (categoryList!=null && categoryList.size()>0) {
            StringBuilder catbuilder=new StringBuilder();
			Iterator<Category> it= categoryList.iterator();
			while (it.hasNext()) {
				Category category= (Category) it.next();
	        	catbuilder.append("\"").append(category.getInode()).append("\"") ;
				if (it.hasNext()) {
	        		catbuilder.append(",");
	        	}
	        }
			categories=catbuilder.toString();

			sb.append("#set($ContentletFilteredCategories=$categories.filterCategoriesByUserPermissions([" ).append( categories ).append( "] ))");
	        sb.append("#set($ContentletCategories=$categories.fetchCategoriesInodes($ContentletFilteredCategories))");
	        sb.append("#set($ContentletCategoryNames=$categories.fetchCategoriesNames($ContentletFilteredCategories))");
	        sb.append("#set($ContentletCategoryKeys=$categories.fetchCategoriesKeys($ContentletFilteredCategories))");
        }
        else {
            sb.append("#set($ContentletFilteredCategories=$contents.getEmptyList())");
            sb.append("#set($ContentletCategories=$contents.getEmptyList())");
            sb.append("#set($ContentletCategoryNames=$contents.getEmptyList())");
            sb.append("#set($ContentletCategoryKeys=$contents.getEmptyList())");
        }

		//This needs to be here because the all fields like cats etc.. need to be parsed first and it needs to be before
		// the $CONTENT_INODE is reset sb.append("#set( $CONTENT_INODE=\"" + content.getInode() + "\" )");
		//http://jira.dotmarketing.net/browse/DOTCMS-2808
		sb.append(widgetCode);

		// This is code is repeated because the bug GETTYS-268, the content
		// variables were been overwritten
		// by the parse inside the some of the content fields
		// To edit the look, see
		// WEB-INF/velocity/static/preview/content_controls.vtl

		if(EDIT_MODE) {
			sb.append("#set( $EDIT_CONTENT_PERMISSION=$EDIT_CONTENT_PERMISSION" ).append( identifier.getInode() ).append( " )");
		}

		sb.append("#set( $CONTENT_INODE=\"" ).append( content.getInode() ).append( "\" )");
		sb.append("#set( $IDENTIFIER_INODE=\"" ).append( identifier.getInode() ).append( "\" )");

		sb.append("#set( $ContentInode=\"" ).append( content.getInode() ).append( "\" )");
		sb.append("#set( $ContentIdentifier=\"" ).append( identifier.getInode() ).append( "\" )");
		sb.append("#set( $ContentletTitle=\"" ).append( UtilMethods.espaceForVelocity(conAPI.getName(content, APILocator.getUserAPI().getSystemUser(), true)) ).append( "\" )");
		sb.append("#set( $ContentletStructure=\"" ).append( content.getStructureInode() ).append( "\" )");

		if(structure.getStructureType()== Structure.STRUCTURE_TYPE_WIDGET){
			sb.append("#set( $isWidget= \"" ).append( true ).append( "\")");
			if(structure.getName().equals(FormAPI.FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME)){
				sb.append("#set($isFormWidget= \"" ).append( true ).append( "\")");
			}else{
				sb.append("#set($isFormWidget= \"" ).append( false ).append( "\")");
			}
		}else{
			sb.append("#set($isWidget= \"" ).append( false ).append( "\")");
		}

		if(Config.getBooleanProperty("SHOW_VELOCITYFILES", false)){
			try {

				String velocityRootPath= Config.getStringProperty("VELOCITY_ROOT");

				if (velocityRootPath.startsWith("/WEB-INF")) {
					velocityRootPath= FileUtil.getRealPath(velocityRootPath);
				}
				velocityRootPath += java.io.File.separator;

				String veloExt= Config.getStringProperty("VELOCITY_CONTENT_EXTENSION");
				String baseFilename= String.format("%s_%d.%s", identifier.getInode(), content.getLanguageId(), veloExt);

				// Save always to working
				String filePath= "working" + java.io.File.separator + baseFilename;

				saveToDisk(ConfigUtils.getDynamicVelocityPath()+java.io.File.separator,filePath, sb.toString());

				// Save to live, if publishing
				if (!EDIT_MODE) {
					filePath= "live" + java.io.File.separator + baseFilename;
					saveToDisk(ConfigUtils.getDynamicVelocityPath()+java.io.File.separator,filePath, sb.toString());
				}

			} catch (Exception e) {
				Logger.error(ContentletServices.class, e.toString(), e);
			}
		}
		try {
			result= new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			result= new ByteArrayInputStream(sb.toString().getBytes());
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

	/**
	 * Will remove all contentlet files within a structure for both live and working. Uses the system user.
	 * @param contentlets
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public static void removeContentletFile(Structure structure) throws DotDataException, DotSecurityException{
		ContentletAPI conAPI= APILocator.getContentletAPI();
		int limit= 500;
		int offset= 0;
		List<Contentlet> contentlets= conAPI.findByStructure(structure, APILocator.getUserAPI().getSystemUser(), false, limit, offset);
		int size= contentlets.size();
		while(size > 0){
			for (Contentlet contentlet : contentlets) {
				invalidateAll(contentlet);
			}
			offset += limit;
			contentlets= conAPI.findByStructure(structure, APILocator.getUserAPI().getSystemUser(), false, limit, offset);
			size= contentlets.size();
		}
	}

	private static void removeContentletFile(Contentlet asset, Identifier identifier, boolean EDIT_MODE) {
	    CacheLocator.getContentletCache().remove(asset.getInode());

		String folderPath= (!EDIT_MODE) ? "live" + java.io.File.separator : "working" + java.io.File.separator;
		String velocityRootPath= Config.getStringProperty("VELOCITY_ROOT");
		if (velocityRootPath.startsWith("/WEB-INF")) {
			velocityRootPath= FileUtil.getRealPath(velocityRootPath);
		}
		velocityRootPath += java.io.File.separator;

		Set<Long> langs = new HashSet<Long>();
		langs.add(asset.getLanguageId());
		if(LanguageWebAPI.canApplyToAllLanguages(asset)) {
		    for(Language ll : APILocator.getLanguageAPI().getLanguages()) {
		        langs.add(ll.getId());
		    }
		}
		for(Long langId : langs) {
    		String filePath= folderPath + identifier.getInode() + "_" + langId + "." + Config.getStringProperty("VELOCITY_CONTENT_EXTENSION");
    		java.io.File f= new java.io.File(velocityRootPath + filePath);
    		f.delete();
    		DotResourceCache vc= CacheLocator.getVeloctyResourceCache();
            vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
		}
        List<Field> fields= FieldsCache.getFieldsByStructureInode(asset.getStructureInode());
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

		java.io.BufferedOutputStream tmpOut= new java.io.BufferedOutputStream(new java.io.FileOutputStream(new java.io.File(folderPath+ filePath)));

		// Specify a proper character encoding
		OutputStreamWriter out= new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration());

		out.write(data);

		out.flush();
		out.close();
		tmpOut.close();
		DotResourceCache vc= CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
	}

}
package com.dotmarketing.services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import org.apache.velocity.runtime.resource.ResourceManager;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.velocity.DotResourceCache;

/**
 * @author will
 */
public class StructureServices {

	private static long fakeIdentifier = Long.MAX_VALUE;
	private static long fakeInode = Long.MAX_VALUE;
	private static String fakeTitle = "Content Title";

	public static void invalidate(Structure structure) {
		invalidate(structure, true);	
		invalidate(structure, false);	
	}

	public static void invalidate(Structure structure,boolean EDIT_MODE) {
		removeStructureFile(structure);
	}

	public static InputStream buildVelocity(Structure structure) {
		return buildVelocity(structure, true);
	}
	
	@SuppressWarnings("deprecation")
	public static InputStream buildVelocity(Structure structure,boolean EDIT_MODE) {

		//  let's write this puppy out to our file
		StringBuilder sb = new StringBuilder();
		
		// CONTENTLET CONTROLS BEGIN
		sb.append("#set( $EDIT_CONTENT_PERMISSION =$EDIT_CONTENT_PERMISSION" + fakeInode + " )\n");
		sb.append("#set( $CONTENT_INODE ='" + fakeInode + "' )\n");
		sb.append("#set( $IDENTIFIER_INODE ='" + fakeIdentifier + "' )\n");

		//set all properties from the contentlet
		sb.append("##Set Content properties\n");
		sb.append("#set( $ContentInode ='" + fakeInode + "' )\n");
		sb.append("#set( $ContentIdentifier ='" + fakeIdentifier + "' )\n");
		sb.append("#set( $ContentletTitle =\"" + UtilMethods.espaceForVelocity(fakeTitle) + "\" )\n");
		
		//Structure fields
		List<Field> fields = structure.getFields();
		Iterator<Field> fieldsIt = fields.iterator();

		while (fieldsIt.hasNext()) {
			Field field = (Field) fieldsIt.next();

			sb.append("\n\n##Set Field " + field.getFieldName() + " properties\n");

			String contField = field.getFieldContentlet();
			String contFieldValue = null;
			Object contFieldValueObject = null;
			if (UtilMethods.isSet(contField)) {
				try {
					//contFieldValueObject = PropertyUtils.getProperty(content, contField);
					contFieldValueObject = field.getFieldName();
					contFieldValue = contFieldValueObject == null?"":contFieldValueObject.toString();
				} catch (Exception e) {
					Logger.error(ContentletServices.class, "writeContentletToFile: " + e.getMessage());
				}
				if (!field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()) && 
						!field.getFieldType().equals(Field.FieldType.DATE.toString()) && 
						!field.getFieldType().equals(Field.FieldType.TIME.toString())) 
					sb.append("#set( $" + field.getVelocityVarName() + " =\"" + UtilMethods.espaceForVelocity(contFieldValue).trim() + "\" )\n");
			}

			if (field.getFieldType().equals(Field.FieldType.TEXT.toString()) || field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
				sb.append("#set( $" + field.getVelocityVarName() + " =\"[ #fixBreaks($" + field.getVelocityVarName() + ") ]\")\n");
			} else if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())) {
				//Identifier id = (Identifier) InodeFactory.getChildOfClassByRelationType(content, Identifier.class, field.getFieldRelationType());				
				String uri = "/html/images/shim.gif";				       		
				sb.append("#set( $" + field.getVelocityVarName() + "ImageInode =\"" + Long.MAX_VALUE + "\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageWidth =\"" + 150 + "\" )\n"); //Original value was 165
				sb.append("#set( $" + field.getVelocityVarName() + "ImageHeight =\"" + 150 + "\" )\n"); //Originak value was 65
				sb.append("#set( $" + field.getVelocityVarName() + "ImageExtension =\"gif\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageURI =\"" + uri + "\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ImageTitle =\"[ Test Image Structure ]\" )\n");
			} else if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
				//Identifier id = (Identifier) InodeFactory.getChildOfClassByRelationType(content, Identifier.class, field.getFieldRelationType());
				String uri = "/html/images/shim.gif";				       		
				sb.append("#set( $" + field.getVelocityVarName() + "FileInode =\"" + Long.MAX_VALUE + "\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "FileExtension =\"gif\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "FileURI =\"" + uri + "\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "FileTitle =\"[ Test File Structure ]\" )\n");
			} else if (field.getFieldType().equals(Field.FieldType.SELECT.toString())) {
				sb.append("#set( $" + field.getVelocityVarName() + "SelectLabelsValues = \"" + field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") + "\")\n");
			} else if (field.getFieldType().equals(Field.FieldType.RADIO.toString())) {
				sb.append("#set( $" + field.getVelocityVarName() + "RadioLabelsValues = \"" + field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") + "\" )\n");
			} else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())) {
				sb.append("#set( $" + field.getVelocityVarName() + "CheckboxLabelsValues = \"" + field.getValues().replaceAll("\\r\\n", " ").replaceAll("\\n", " ") + "\" )\n");
			} else if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {				
				sb.append("#set( $" + field.getVelocityVarName() + " =\"[ " + field.getVelocityVarName() + " ]\")\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ShortFormat =\"[ " + field.getVelocityVarName() + " ]\")\n");
				sb.append("#set( $" + field.getVelocityVarName() + "DBFormat =\"[ " + field.getVelocityVarName() + " ]\")\n");
			} else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {				
				sb.append("#set( $" + field.getVelocityVarName() + "ShortFormat =\"[ " + field.getVelocityVarName() + " ]\")\n");
				sb.append("#set( $" + field.getVelocityVarName() + " =\"[ " + field.getVelocityVarName() + "\" ])\n");
			} else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
				sb.append("#set( $" + field.getVelocityVarName() + " =\"[ " + field.getVelocityVarName() + " ] \")\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ShortFormat =\"[ " + field.getVelocityVarName() + " ]\")\n");
				sb.append("#set( $" + field.getVelocityVarName() + "LongFormat =\"[ " + field.getVelocityVarName() + " ]\")\n");
				sb.append("#set( $" + field.getVelocityVarName() + "DBFormat =\"[ " + field.getVelocityVarName() + " ]\")\n");
			} else if (field.getFieldType().equals(Field.FieldType.BUTTON.toString())) {
				sb.append("#set( $" + field.getVelocityVarName() + "ButtonValue =\"" + (field.getFieldName() == null?"":field.getFieldName()) + "\" )\n");
				sb.append("#set( $" + field.getVelocityVarName() + "ButtonCode =\"" + (field.getValues() == null?"":field.getValues()) + "\" )\n");
			}
		}

		//sets the categories as a list on velocity
		sb.append("#set( $ContentletCategories =[] )\n");
		sb.append("#set( $ContentletCategoryNames =[] )\n");


		// This is code is repeated becuase the bug  GETTYS-268, the content variables were been overwritten 
		// by the parse inside the some of the content fields  
		// To edit the look, see WEB-INF/velocity/static/preview/content_controls.vtl

		sb.append("#set( $EDIT_CONTENT_PERMISSION =$EDIT_CONTENT_PERMISSION" + fakeIdentifier + " )\n");
		sb.append("#set( $CONTENT_INODE ='" +fakeInode + "' )\n");
		sb.append("#set( $IDENTIFIER_INODE ='" + fakeIdentifier + "' )\n");
		sb.append("##Set Content properties\n");
		sb.append("#set( $ContentInode ='" + fakeInode + "' )\n");
		sb.append("#set( $ContentIdentifier ='" + fakeIdentifier + "' )\n");
		sb.append("#set( $ContentletTitle =\"" + UtilMethods.espaceForVelocity(fakeTitle) + "\" )\n");
		sb.append("#set( $isWidget = false)\n");

		InputStream result;
		try {
			result = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			result = new ByteArrayInputStream(sb.toString().getBytes());
			Logger.error(StructureServices.class,e1.getMessage(), e1);
		}
		
		try {

			String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
			if (velocityRootPath.startsWith("/WEB-INF")) {
				velocityRootPath = Config.CONTEXT.getRealPath(velocityRootPath);
			}
			velocityRootPath += java.io.File.separator;

			String relativePath = "/working/" + structure.getInode() + "." + Config.getStringProperty("VELOCITY_STRUCTURE_EXTENSION");

	
			if(Config.getBooleanProperty("SHOW_VELOCITYFILES", false)){
					String absolutePath = ConfigUtils.getDynamicVelocityPath()+java.io.File.separator + relativePath;
					java.io.BufferedOutputStream tmpOut = new java.io.BufferedOutputStream(new java.io.FileOutputStream(new java.io.File(absolutePath)));

				//Specify a proper character encoding
                OutputStreamWriter out = new OutputStreamWriter(tmpOut, UtilMethods.getCharsetConfiguration());

                out.write(sb.toString());

                out.flush();
                out.close();
                tmpOut.close();
			}
            
		} catch (Exception e) {
			Logger.error(StructureServices.class, e.toString(), e);
		}
		return result;
	}
	
	public static void removeStructureFile(Structure structure) {
		String folderPath = "working/";
		String filePath=folderPath + structure.getInode() + "." + Config.getStringProperty("VELOCITY_STRUCTURE_EXTENSION");
		String absolutPath = Config.CONTEXT.getRealPath("/WEB-INF/velocity/" +filePath);
		java.io.File f = new java.io.File(absolutPath);
		f.delete();
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
	}
}
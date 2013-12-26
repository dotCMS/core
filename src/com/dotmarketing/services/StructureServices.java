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
import com.liferay.util.FileUtil;

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
		sb.append("#set( $EDIT_CONTENT_PERMISSION =$EDIT_CONTENT_PERMISSION" ).append( fakeInode ).append( " )");
		sb.append("#set( $CONTENT_INODE ='" ).append( fakeInode ).append( "' )");
		sb.append("#set( $IDENTIFIER_INODE ='" ).append( fakeIdentifier ).append( "' )");

		//set all properties from the contentlet
		sb.append("#set( $ContentInode ='" ).append( fakeInode ).append( "' )");
		sb.append("#set( $ContentIdentifier ='" ).append( fakeIdentifier ).append( "' )");
		sb.append("#set( $ContentletTitle =\"" ).append( UtilMethods.espaceForVelocity(fakeTitle) ).append( "\" )");
		
		//Structure fields
		List<Field> fields = structure.getFields();
		Iterator<Field> fieldsIt = fields.iterator();

		while (fieldsIt.hasNext()) {
			Field field = (Field) fieldsIt.next();

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
					sb.append("#set( $" ).append( field.getVelocityVarName() ).append( " =\"" ).append( UtilMethods.espaceForVelocity(contFieldValue).trim() ).append( "\" )");
			}
			String fv=field.getValues()!=null ? field.getValues() : "";
			if (field.getFieldType().equals(Field.FieldType.TEXT.toString()) || field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( " =\"[ #fixBreaks($" ).append( field.getVelocityVarName() ).append( ") ]\")");
			} else if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())) {
				//Identifier id = (Identifier) InodeFactory.getChildOfClassByRelationType(content, Identifier.class, field.getFieldRelationType());				
				String uri = "/html/images/shim.gif";				       		
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ImageInode =\"" ).append( Long.MAX_VALUE ).append( "\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ImageWidth =\"" ).append( 150 ).append( "\" )"); //Original value was 165
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ImageHeight =\"" ).append( 150 ).append( "\" )"); //Originak value was 65
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ImageExtension =\"gif\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ImageURI =\"" ).append( uri ).append( "\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ImageTitle =\"[ Test Image Structure ]\" )");
			} else if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
				//Identifier id = (Identifier) InodeFactory.getChildOfClassByRelationType(content, Identifier.class, field.getFieldRelationType());
				String uri = "/html/images/shim.gif";				       		
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "FileInode =\"" ).append( Long.MAX_VALUE ).append( "\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "FileExtension =\"gif\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "FileURI =\"" ).append( uri ).append( "\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "FileTitle =\"[ Test File Structure ]\" )");
			} else if (field.getFieldType().equals(Field.FieldType.SELECT.toString())) {
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "SelectLabelsValues = \"" ).append( fv.replaceAll("\\r\\n", " ").replaceAll("\\n", " ") ).append( "\")");
			} else if (field.getFieldType().equals(Field.FieldType.RADIO.toString())) {
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "RadioLabelsValues = \"" ).append( fv.replaceAll("\\r\\n", " ").replaceAll("\\n", " ") ).append( "\" )");
			} else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())) {
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "CheckboxLabelsValues = \"" ).append( fv.replaceAll("\\r\\n", " ").replaceAll("\\n", " ") ).append( "\" )");
			} else if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {				
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( " =\"[ " ).append( field.getVelocityVarName() ).append( " ]\")");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ShortFormat =\"[ " ).append( field.getVelocityVarName() ).append( " ]\")");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "DBFormat =\"[ " ).append( field.getVelocityVarName() ).append( " ]\")");
			} else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {				
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ShortFormat =\"[ " ).append( field.getVelocityVarName() ).append( " ]\")");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( " =\"[ " ).append( field.getVelocityVarName() ).append( "\" ])");
			} else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( " =\"[ " ).append( field.getVelocityVarName() ).append( " ] \")");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ShortFormat =\"[ " ).append( field.getVelocityVarName() ).append( " ]\")");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "LongFormat =\"[ " ).append( field.getVelocityVarName() ).append( " ]\")");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "DBFormat =\"[ " ).append( field.getVelocityVarName() ).append( " ]\")");
			} else if (field.getFieldType().equals(Field.FieldType.BUTTON.toString())) {
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ButtonValue =\"" ).append( fv ).append( "\" )");
				sb.append("#set( $" ).append( field.getVelocityVarName() ).append( "ButtonCode =\"" ).append( fv ).append( "\" )");
			}
		}

		//sets the categories as a list on velocity
		sb.append("#set( $ContentletCategories =[] )");
		sb.append("#set( $ContentletCategoryNames =[] )");


		// This is code is repeated becuase the bug  GETTYS-268, the content variables were been overwritten 
		// by the parse inside the some of the content fields  
		// To edit the look, see WEB-INF/velocity/static/preview/content_controls.vtl

		sb.append("#set( $EDIT_CONTENT_PERMISSION =$EDIT_CONTENT_PERMISSION" ).append( fakeIdentifier ).append( " )");
		sb.append("#set( $CONTENT_INODE ='" ).append(fakeInode ).append( "' )");
		sb.append("#set( $IDENTIFIER_INODE ='" ).append( fakeIdentifier ).append( "' )");
		
		sb.append("#set( $ContentInode ='" ).append( fakeInode ).append( "' )");
		sb.append("#set( $ContentIdentifier ='" ).append( fakeIdentifier ).append( "' )");
		sb.append("#set( $ContentletTitle =\"" ).append( UtilMethods.espaceForVelocity(fakeTitle) ).append( "\" )");
		sb.append("#set( $isWidget = false)");

		InputStream result;
		try {
			result = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			result = new ByteArrayInputStream(sb.toString().getBytes());
			Logger.error(StructureServices.class,e1.getMessage(), e1);
		}
		
		try {
			if(Config.getBooleanProperty("SHOW_VELOCITYFILES", false)){
			    String velocityRootPath = Config.getStringProperty("VELOCITY_ROOT");
	            if (velocityRootPath.startsWith("/WEB-INF")) {
	                velocityRootPath = FileUtil.getRealPath(velocityRootPath);
	            }
	            velocityRootPath += java.io.File.separator;

	            String relativePath = "/working/" + structure.getInode() + "." + Config.getStringProperty("VELOCITY_STRUCTURE_EXTENSION");
	            
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
		String absolutPath = FileUtil.getRealPath("/WEB-INF/velocity/" +filePath);
		java.io.File f = new java.io.File(absolutPath);
		f.delete();
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
        vc.remove(ResourceManager.RESOURCE_TEMPLATE + filePath );
	}
}
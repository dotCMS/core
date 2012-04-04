package com.dotmarketing.portlets.contentlet.ajax;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ContentletAjaxTest extends ServletTestCase {

	UserAPI ua;
	ContentletAPI conAPI;
	LanguageAPI lai;
	PermissionAPI pa;
	Contentlet newCont;
	String structureName = "News Item";

	@Override
	protected void setUp() throws Exception {

		ua = APILocator.getUserAPI();
		conAPI = APILocator.getContentletAPI();
		lai = APILocator.getLanguageAPI();
		pa = APILocator.getPermissionAPI();
		newCont = null;

		UserAPI ua = APILocator.getUserAPI();
		ContentletAPI conAPI = APILocator.getContentletAPI();
		HostAPI hostAPI = APILocator.getHostAPI();
		LanguageAPI lai = APILocator.getLanguageAPI();
		PermissionAPI pa = APILocator.getPermissionAPI();
		newCont = null;


		//Get the System User
		User user = ua.getSystemUser();

		//Get the News Item Stucture
		Structure structure = new StructureFactory().getStructureByType(structureName);

		//Get the default languaje
		Language defaultLanguage = lai.getDefaultLanguage();
		Host defaultHost = hostAPI.findDefaultHost(user, false);
		//Create the new Contentlet
		newCont = new Contentlet();
		newCont.setLive(true);
		newCont.setWorking(true);
		newCont.setStructureInode(structure.getInode());
		newCont.setLanguageId(defaultLanguage.getId());			
		newCont.setHost(defaultHost.getIdentifier());

		//Get all the fields of the structure
		List<Field> fields = structure.getFields();

		//Fill the new contentlet with the data
		for (Field field : fields) 
		{ 
			Object value = null;
			if(field.getVelocityVarName().equals("comments")) {
				value = "off";
			} else if(field.getFieldType().equals(Field.FieldType.TEXT.toString()) || field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()))
			{
				if(field.getFieldContentlet().startsWith("text"))
				{
					value = "Test Text";
				}
				else if (field.getFieldContentlet().startsWith("float"))
				{
					value = 0;
				} 					
			}
			else if(field.getFieldType().equals(Field.FieldType.WYSIWYG.toString()))
			{					
				value = "<p>Test Text,Test Text,Test Text,Test Text,Test Text,Test Text,Test Text.</p><p>Test Text,Test Text,Test Text,Test Text,Test Text,Test Text,Test Text.</p>";					 					
			}
			else if(field.getFieldType().equals(Field.FieldType.TAG.toString()))
			{
				value = "Test Tag";					
			}
			else if(field.getFieldType().equals(Field.FieldType.DATE.toString()) || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()))
			{
				value = new Date();
			}
			if(UtilMethods.isSet(value))
			{
				conAPI.setContentletProperty(newCont, field, value);
			}
		}

		//Check the new contentlet with the validator
		try
		{
			//Get the News Item Parent Category 
			CategoryAPI ca = APILocator.getCategoryAPI();
			Category parentCategory = ca.findByKey("nptype", user, true);				
			//Get the News Item Child Categories
			List<Category> categories = ca.getChildren(parentCategory, user, true);
			//Get The permissions of the structure
			List<Permission> structurePermissions = pa.getPermissions(structure);						

			//Validate if the contenlet is OK
			conAPI.validateContentlet(newCont,categories);

			//Save the contentlet and update the global variable for the tearDown
			newCont = conAPI.checkin(newCont,categories, structurePermissions, user, true);							
		}
		catch(DotContentletValidationException ex)
		{
			StringBuffer sb = new StringBuffer("contains errors\n");
			//check fields 
			HashMap<String,List<Field>> errors = (HashMap<String,List<Field>>) ex.getNotValidFields();
			Set<String> keys = errors.keySet();					
			for(String key : keys)
			{
				sb.append(key + ": ");
				List<Field> errorFields = errors.get(key);
				for(Field field : errorFields)
				{
					sb.append(field.getFieldName() + ",");
				}
				sb.append("\n");
			}
			//check relationship
			Map<String,Map<Relationship,List<Contentlet>>> notValidRelationships = ex.getNotValidRelationship();
			Set<String> auxKeys = notValidRelationships.keySet();
			for(String key : auxKeys)
			{
				String errorMessage = "";
				if(key.equals(DotContentletValidationException.VALIDATION_FAILED_REQUIRED_REL))
				{
					errorMessage = "Required Relationship";
				}
				else if(key.equals(DotContentletValidationException.VALIDATION_FAILED_INVALID_REL_CONTENT))
				{
					errorMessage = "Invalid Relationship-Contentlet";
				}
				else if(key.equals(DotContentletValidationException.VALIDATION_FAILED_BAD_REL))
				{
					errorMessage = "Bad Relationship";
				}

				sb.append(errorMessage + ":\n");
				Map<Relationship,List<Contentlet>> relationshipContentlets = notValidRelationships.get(key);			
				for(Entry<Relationship,List<Contentlet>> relationship : relationshipContentlets.entrySet())
				{			
					sb.append(relationship.getKey().getRelationTypeValue() + ", ");
				}					
				sb.append("\n");			
			}
			sb.append("\n");
			throw new DotRuntimeException(sb.toString());
		}
	} 

	@Override
	protected void tearDown() throws Exception 
	{
		//Get the System User
		User user = ua.getSystemUser();

		//Unpublish the contenlet
		conAPI.unpublish(newCont, user, true);

		//Archive the contentlet
		conAPI.archive(newCont, user, true);

		//Delete the contentlet
		conAPI.delete(newCont, user, true);
	}		


	//Test => true
	public void testWord() throws DotDataException
	{
		String text1 = "test";
		String float1 = "";
		String integer2 = "";
		boolean expectedValue = true;
		boolean greaterThanZero = doSearch(text1,float1,integer2);
		assertEquals(expectedValue,greaterThanZero);
	}

	//Test Text => true
	public void testPhrase() throws DotDataException
	{
		String text1 = "test text";
		String float1 = "";
		String integer2 = "";
		boolean expectedValue = true;
		boolean greaterThanZero = doSearch(text1,float1,integer2);
		assertEquals(expectedValue,greaterThanZero);
	}

	//Te?t => true
	public void testWordWildcardQuestionMark() throws DotDataException
	{
		String text1 = "te?t";
		String float1 = "";
		String integer2 = "";
		boolean expectedValue = true;
		boolean greaterThanZero = doSearch(text1,float1,integer2);
		assertEquals(expectedValue,greaterThanZero);
	}

	//Te* => true
	public void testWordWildcardStar() throws DotDataException
	{
		String text1 = "te*";
		String float1 = "";
		String integer2 = "";
		boolean expectedValue = true;
		boolean greaterThanZero = doSearch(text1,float1,integer2);
		assertEquals(expectedValue,greaterThanZero);
	}

	//Test te?t => True
	public void testPhraseWildcardQuestionMark() throws DotDataException
	{
		String text1 = "test te?t";
		String float1 = "";
		String integer2 = "";
		boolean expectedValue = true;
		boolean greaterThanZero = doSearch(text1,float1,integer2);
		assertEquals(expectedValue,greaterThanZero);
	}

	//Test Te* => True
	public void testPhraseWildcardStar() throws DotDataException
	{
		String text1 = "test te*";
		String float1 = "";
		String integer2 = "";
		boolean expectedValue = true;
		boolean greaterThanZero = doSearch(text1,float1,integer2);
		assertEquals(expectedValue,greaterThanZero);
	}

	//?est => true
	public void testWordStartWildcardQuestionMark() throws DotDataException
	{
		String text1 = "?est";
		String float1 = "";
		String integer2 = "";
		boolean expectedValue = true;
		boolean greaterThanZero = doSearch(text1,float1,integer2);
		assertEquals(expectedValue,greaterThanZero);
	}

	//*est => true
	public void testWordStartWildcardStar() throws DotDataException
	{
		String text1 = "*est";
		String float1 = "";
		String integer2 = "";
		boolean expectedValue = true;
		boolean greaterThanZero = doSearch(text1,float1,integer2);
		assertEquals(expectedValue,greaterThanZero);
	}

	private boolean doSearch(String text1,String float1,String integer2) throws DotDataException
	{		
		Structure structure = StructureFactory.getStructureByType(structureName);
		boolean showDeleted = false;
		int page = 0;
		String orderBy = "modDate";
		int perPage = 50;
		User currentUser = ua.getSystemUser();
		HttpSession sess = null;
		String modDateFrom = null;
		String modDateTo = null;
		ArrayList<String> fields = new ArrayList<String>();
		
		List<Field> structFields = structure.getFields();
		
		for (Field field : structFields) 
		{ 
			if(field.getFieldType().equals(Field.FieldType.TEXT.toString()) || field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()))
			{
				if(field.getFieldContentlet().startsWith("text")&& UtilMethods.isSet(text1))
				{
					fields.add(structure.getVelocityVarName()+"."+field.getVelocityVarName());
					fields.add(text1);
				}
				else if (field.getFieldContentlet().startsWith("float")&& UtilMethods.isSet(float1))
				{
					fields.add(structure.getVelocityVarName()+"."+field.getVelocityVarName());
					fields.add(float1);
				} 
				else if (field.getFieldContentlet().startsWith("integer")&& UtilMethods.isSet(integer2))
				{
					fields.add(structure.getVelocityVarName()+"."+field.getVelocityVarName());
					fields.add(integer2);
				}
			}
		}
		fields.add("languageId");
		fields.add("1");
		
		ArrayList<String> categories = new ArrayList<String>();
		ContentletAjax ca = new ContentletAjax();
		List list = ca.searchContentletsByUser(structure.getInode(),fields,categories,showDeleted, false, page,  orderBy,perPage,currentUser,sess,modDateFrom,modDateTo);
		long size = (Long) ((HashMap) list.get(0)).get("total"); 
		boolean greaterThanZero = (size > 0 ? true : false);
		return greaterThanZero;
	}
}

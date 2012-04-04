package com.dotmarketing.portlets.contentlet.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.IdentifierCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.viewtools.CommentsWebAPI;
import com.liferay.portal.model.User;
import com.liferay.util.Html;

public class ContentletAPICommentTest extends ServletTestCase {

	UserAPI ua;
	ContentletAPI conAPI;
	LanguageAPI lai;
	PermissionAPI pa;
	CategoryAPI catAPI;
	List<Contentlet> newContentlets;
	List<Contentlet> newComments;
	String structureName = "News Item";
	Language language;

	@Override
	protected void setUp() throws Exception {

		ua = APILocator.getUserAPI();
		conAPI = APILocator.getContentletAPI();
		lai = APILocator.getLanguageAPI();
		pa = APILocator.getPermissionAPI();
		catAPI = APILocator.getCategoryAPI();
		newContentlets = new ArrayList<Contentlet>();
		newComments = new ArrayList<Contentlet>();

		//NO set the language
		String textValue = "NO Language";
		float floatValue = 0;
		String wysiwygValue = "<p>Test Text,Test Text,Test Text,Test Text,Test Text,Test Text,Test Text.</p><p>Test Text,Test Text,Test Text,Test Text,Test Text,Test Text,Test Text.</p>";
		Date dateValue = new Date();
		newContentlets.add(createContentlet(textValue,floatValue,wysiwygValue,dateValue,null));
		//Set the language to default value
		this.language = lai.getDefaultLanguage();		
		List<Language> languages = lai.getLanguages();
		for(Language localLanguage : languages)
		{
			if(localLanguage.getId() != language.getId())
			{
				language = localLanguage;
				break;
			}
		}
		textValue = language.getCountry() + " Language";
		newContentlets.add(createContentlet(textValue,floatValue,wysiwygValue,dateValue,language));
	}

	/**
	 * This method create the new contentlets
	 * @param conAPI
	 * @param lai
	 * @param pa
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private Contentlet createContentlet(String textValue,float floatValue,String wysiwygValue,Date dateValue,Language language) throws DotDataException,
	DotSecurityException {
		//Get the System User
		User user = ua.getSystemUser();

		//Get the News Item Stucture
		Structure structure = new StructureFactory().getStructureByType(structureName);
		HostAPI hostAPI = APILocator.getHostAPI();

		//Get the default languaje
		Language defaultLanguage = lai.getDefaultLanguage();

		//Create the new Contentlet
		Host defaultHost = hostAPI.findDefaultHost(user, false);
		Contentlet newCont = new Contentlet();
		newCont.setLive(true);
		newCont.setWorking(true);
		newCont.setStructureInode(structure.getInode());
		newCont.setHost(defaultHost.getIdentifier());
		if(UtilMethods.isSet(language))
		{
			newCont.setLanguageId(language.getId());
		}

		//Get all the fields of the structure
		List<Field> fields = structure.getFields();

		//Fill the new contentlet with the data
		for (Field field : fields) 
		{ 
			Object value = null;
			if(field.getVelocityVarName().equals("comments")) {
				value = "off";
			} else if(field.getFieldType().equals(Field.FieldType.TEXT.toString()))
			{
				if(field.getFieldContentlet().startsWith("text"))
				{
					value = textValue;
				}
				else if (field.getFieldContentlet().startsWith("float"))
				{
					value = floatValue;
				} 					
			}
			else if(field.getFieldType().equals(Field.FieldType.WYSIWYG.toString()))
			{					
				value = wysiwygValue;					 					
			}
			else if(field.getFieldType().equals(Field.FieldType.TAG.toString()))
			{
				value = "Test Tag";					
			}
			else if(field.getFieldType().equals(Field.FieldType.DATE.toString()) || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()))
			{
				value = dateValue;
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
			return newCont;
		}
		catch(DotContentletValidationException ex)
		{
			StringBuffer sb = new StringBuffer("contains errors\n");
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
			throw new DotRuntimeException(sb.toString());
		}
	} 

	@Override
	protected void tearDown() throws Exception 
	{
		//Get the System User
		User user = ua.getSystemUser();

		for(Contentlet newCont : newContentlets)
		{
			//Unpublish the contenlet
			conAPI.unpublish(newCont, user, true);

			//Archive the contentlet
			conAPI.archive(newCont, user, true);

			//Delete the contentlet
			conAPI.delete(newCont, user, true);
		}
	}

	/**
	 * This methods add the comments to the contentlets that has been created
	 * @throws DotHibernateException,DotSecurityException,DotDataException
	 */
	private Contentlet saveComments(Contentlet contentlet) throws DotHibernateException,DotSecurityException,DotDataException
	{
		User user = ua.getSystemUser();
		String userId = user.getUserId();
		// Contentlet Data			
		Structure contentletStructure = StructureCache.getStructureByInode(contentlet.getStructureInode());
		Identifier contentletIdentifier = APILocator.getIdentifierAPI().find(contentlet);

		/*make sure we have a structure in place before saving */
		CommentsWebAPI cAPI = new CommentsWebAPI();
		cAPI.validateComments(contentlet.getInode());

		Structure commentsStructure = StructureCache.getStructureByVelocityVarName(CommentsWebAPI.commentsVelocityStructureName);

		Contentlet contentletComment = new Contentlet();

		// set the default language
		com.dotmarketing.portlets.contentlet.business.Contentlet beanContentlet = (com.dotmarketing.portlets.contentlet.business.Contentlet) InodeFactory.getInode(contentlet.getInode(), com.dotmarketing.portlets.contentlet.business.Contentlet.class);

		contentletComment.setLanguageId(beanContentlet.getLanguageId());

		// Add the default fields
		contentletComment.setStructureInode(commentsStructure.getInode());

		Field field;

		/* Set the title if we have one*/
		field = commentsStructure.getField("Title");				
		conAPI.setContentletProperty(contentletComment, field,"Test Comment");

		/* Validate if a CommentsCount field exists in the contentlet structure
			   if not, then create it and populate it.*/		   
		if (!InodeUtils.isSet(contentletStructure.getField("CommentsCount").getInode())) {
			List<Field> fields = new ArrayList<Field>();
			field = new Field("CommentsCount", Field.FieldType.TEXT, Field.DataType.INTEGER, contentletStructure,
					false, false, true, Integer.MAX_VALUE, "0", "0", "",true, true, true);
			FieldFactory.saveField(field);
			for(Field structureField: contentletStructure.getFields()){
				fields.add(structureField);
			}
			fields.add(field);
			FieldsCache.removeFields(contentletStructure);
			FieldsCache.addFields(contentletStructure,fields); 
		}

		/* Get the  value from the CommentsCount field for this contentlet, if the value
		 * is null, then the contentlet has no comments, otherwise increment its value by one
		 * and set it to the contentlet.
		 */
		field = contentletStructure.getField("CommentsCount");
		String velVar = field.getVelocityVarName();

		int comentsCount = -1;
		try {
			Long countValue = contentlet.getLongProperty(velVar);
			comentsCount  = countValue.intValue();
		} catch (Exception e) {
			Logger.debug(this, e.toString());
		}

		if (comentsCount == -1) {
			try {
				String countValue = (contentlet.getStringProperty(velVar) ==  null) ? field.getDefaultValue() : contentlet.getStringProperty(velVar);
				comentsCount  = countValue.equals("") ? 0 : new Integer(countValue).intValue();
			} catch (Exception e) {
				Logger.debug(this, e.toString());
			}
		}

		++comentsCount;
		conAPI.setContentletProperty(contentlet, field, comentsCount);
		//Update the contentlet with the new comment count
		List<Category> cats = catAPI.getParents(contentlet, user, true);
		Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<Relationship, List<Contentlet>>();

		List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(contentlet.getStructure());
		for (Relationship r : rels) {
			if(!contentRelationships.containsKey(r)){
				contentRelationships.put(r, new ArrayList<Contentlet>());
			}
			List<Contentlet> cons = conAPI.getRelatedContent(contentlet, r, user, true);
			for (Contentlet co : cons) {
				List<Contentlet> l2 = contentRelationships.get(r);
				l2.add(co);
			}
		}			
		contentlet = conAPI.checkinWithoutVersioning(contentlet, contentRelationships, cats, APILocator.getPermissionAPI().getPermissions(contentlet), user, true);

		// Date
		field = commentsStructure.getField("DatePublished");
		conAPI.setContentletProperty(contentletComment, field, new Date());

		// User Id
		field = commentsStructure.getField("UserId");
		conAPI.setContentletProperty(contentletComment, field, userId);

		// Author
		field = commentsStructure.getField("Author");
		conAPI.setContentletProperty(contentletComment, field, VelocityUtil.cleanVelocity("Comment Author"));

		// Email
		field = commentsStructure.getField("Email");
		conAPI.setContentletProperty(contentletComment, field, VelocityUtil.cleanVelocity("test@email.com"));

		// WebSite
		field = commentsStructure.getField("Website");
		conAPI.setContentletProperty(contentletComment, field, VelocityUtil.cleanVelocity("www.dotcms.org"));

		// EmailResponse
		field = commentsStructure.getField("Email Response");
		conAPI.setContentletProperty(contentletComment, field, ("no"));			

		// IP Address
		field = commentsStructure.getField("IP Address");
		conAPI.setContentletProperty(contentletComment, field,"127.0.0.1");

		// Comment
		field = commentsStructure.getField("Comment");
		String comment = "This is a test comment";
		comment=VelocityUtil.cleanVelocity(comment);
		if (true) {
			comment = Html.stripHtml(comment);
		}
		conAPI.setContentletProperty(contentletComment, field, comment);

		// If live I have to publish the asset
		if (false) {
			contentletComment.setLive(true);
		}

		// Add the permission
		PermissionAPI perAPI = APILocator.getPermissionAPI();			
		List<Permission> pers = perAPI.getPermissions(commentsStructure);

		// Save the comment			
		contentletComment = conAPI.checkin(contentletComment, new HashMap<Relationship, List<Contentlet>>(), new ArrayList<Category>(), pers, user, true);
		newComments.add(contentletComment);

		// Set the relation between the content and the comments
		Identifier contentletCommentIdentifier = APILocator.getIdentifierAPI().find(contentletComment);
		String commentRelationStructureName = commentsStructure.getName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "");
		String contentletRelationStructureName = contentletStructure.getName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "");
		String relationName = contentletRelationStructureName + "-" + commentRelationStructureName;

		/* get the next in the order */
		int order  = RelationshipFactory.getMaxInSortOrder(contentletIdentifier.getInode(), relationName);


		contentletIdentifier.addChild(contentletCommentIdentifier, relationName, ++order);

		beanContentlet = (com.dotmarketing.portlets.contentlet.business.Contentlet) InodeFactory.getInode(contentletComment.getInode(), com.dotmarketing.portlets.contentlet.business.Contentlet.class);			
		beanContentlet = conAPI.convertContentletToFatContentlet(contentletComment, beanContentlet);
		HibernateUtil.saveOrUpdate(beanContentlet);	
		return contentlet;
	}
	
	/**
	 * This methods remove the comments from the contentlets
	 * @throws DotDataException, DotSecurityException
	 */
	private void removeComments() throws DotDataException, DotSecurityException
	{
		//Get the System User
		User user = ua.getSystemUser();

		for(Contentlet newComment: newComments)
		{
			//Unpublish the contenlet
			conAPI.unpublish(newComment, user, true);

			//Archive the contentlet
			conAPI.archive(newComment, user, true);

			//Delete the contentlet
			conAPI.delete(newComment, user, true);
		}
	}
	
	/**
	 * This methods check the comments count on a new content
	 */
	public void testNoCommentCount()
	{
		boolean returnValue = false;
		if(newContentlets.size() > 0)
		{
			Contentlet contentlet = newContentlets.get(0);
			int commentCounts = (int) contentlet.getLongProperty("commentscount");
			if(commentCounts == 0)
			{
				returnValue = true;
			}			
		}
		assertEquals(true,returnValue);
	}

	/**
	 * This methods check the comment counts after some comments has been added 
	 * @throws Exception
	 */
	public void testAddCommentCount() throws Exception
	{
		try
		{
			boolean returnValue = false;
			//if there are contentlets
			if(newContentlets.size() > 0)
			{
				//Get the first one and add some comments
				Contentlet contentlet = newContentlets.get(0);
				//Add Comment 1
				contentlet = saveComments(contentlet);
				//Add Comment 2
				contentlet = saveComments(contentlet);
				//Add Comment 3
				contentlet = saveComments(contentlet);
				//Check the value of the comment counts
				int commentCounts = (int) contentlet.getLongProperty("commentscount");
				if(commentCounts == 3)
				{
					returnValue = true;
				}			
			}
			assertEquals(true,returnValue);
			//Unpublish, archive and delete all the comments
			removeComments();
		}
		catch(Exception ex)
		{
			throw ex;
		}
	}

	/**
	 * This methods check the comment counts after some comments has been added and then deleted
	 * @throws Exception
	 */
	public void testDeleteCommentCount() throws Exception
	{
		//Get the System User
		User user = ua.getSystemUser();
		try
		{
			boolean returnValue = false;
			//if there are contentlets
			if(newContentlets.size() > 0)
			{
				//Get the first one and add some comments
				Contentlet contentlet = newContentlets.get(0);
				//Add Comment 1
				contentlet = saveComments(contentlet);
				//Add Comment 1				
				contentlet = saveComments(contentlet);
				//Add Comment 1				
				contentlet = saveComments(contentlet);
				//unpublish, archive and delete all the comments
				removeComments();
				
				//reload the contentlet
				contentlet = conAPI.find(contentlet.getInode(), user, true);
				//Check the value of the comment counts
				int commentCounts = (int) contentlet.getLongProperty("commentscount");
				if(commentCounts == 0)
				{
					returnValue = true;
				}
			}
			assertEquals(true,returnValue);			
		}
		catch(Exception ex)
		{
			throw ex;
		}
	}
}

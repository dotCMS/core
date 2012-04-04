package com.dotmarketing.portlets.contentlet.business;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.cactus.ServletTestCase;
import org.apache.lucene.queryParser.ParseException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.ajax.ContentletAjax;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.entities.model.Entity;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.factories.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.model.Field.DataType;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class ContentletAPITest extends ServletTestCase {

	UserAPI ua;
	ContentletAPI conAPI;
	LanguageAPI lai;
	PermissionAPI pa;
	List<Contentlet> newContentlets;
	String structureName = "JUnit Test Structure";
	String binaryFieldName = "JUnit Test Binary";
	String structureVelocityVarName= "junit_test_structure";
	String hostFolderFieldName = "JUnit Test Host Folder";
	Language language;
	HostAPI hostAPI;
	FolderAPI folderAPI;
	FileAPI fileAPI;
	RoleAPI roleAPI;
	
	final static String textValue = "NO Language";
	final static String longTextValue = "<p>Test Text,Test Text,Test Text,Test Text,Test Text,Test Text,Test Text.</p><p>Test Text,Test Text,Test Text,Test Text,Test Text,Test Text,Test Text.</p>";
	final static Date dateValue = new Date(50, 5, 15, 13, 10, 20);
	final static long longValue = 1;
	final static float floatValue = 2.0f;
	final static boolean booleanValue = true;
	
	static File testFile;
	static File testImage;
	static String testBinaryFileName = "junit_test_binary.txt";
	static Folder testFolder;
	static Host testHost1;
	static Host testHost2;
	
	private static List<Permission> permissionList;

	@Override
	protected void setUp() throws Exception {
		ua = APILocator.getUserAPI();
		conAPI = APILocator.getContentletAPI();
		lai = APILocator.getLanguageAPI();
		pa = APILocator.getPermissionAPI();
		newContentlets = new ArrayList<Contentlet>();
		folderAPI = APILocator.getFolderAPI();
		fileAPI = APILocator.getFileAPI();
		hostAPI = APILocator.getHostAPI();
		roleAPI = APILocator.getRoleAPI();
		
		createJUnitTestFiles();
		createJUnitTestBinary();
		createJUnitTestStructure();
		
		java.io.File binaryFile = new java.io.File(testBinaryFileName);
		newContentlets.add(createContentlet(textValue, longTextValue, dateValue, longValue, floatValue, booleanValue, testFile.getIdentifier(), testImage.getIdentifier(), binaryFile, FolderAPI.SYSTEM_FOLDER, null, APILocator.getUserAPI().getSystemUser()));
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
		String textLanguageValue = language.getCountry() + " Language";
		createJUnitTestBinary();
		newContentlets.add(createContentlet(textLanguageValue, longTextValue, dateValue, longValue, floatValue, booleanValue, testFile.getIdentifier(), testImage.getIdentifier(), binaryFile, FolderAPI.SYSTEM_FOLDER, language, APILocator.getUserAPI().getSystemUser()));
	}
	
	/*
	 * Create a test structure
	 * 
	 */
	
	private void createJUnitTestStructure() throws DotDataException, DotSecurityException {
		//Create the Structure
		Structure jUnitTestStructure = new Structure();
		jUnitTestStructure.setDefaultStructure(false);
		jUnitTestStructure.setName(structureName);
		jUnitTestStructure.setDescription(structureName + " Description");
		jUnitTestStructure.setVelocityVarName(structureVelocityVarName);
		jUnitTestStructure.setFixed(false);
		StructureFactory.saveStructure(jUnitTestStructure);

		//Create the Entity
		Entity entity = new Entity();
		entity.setEntityName(jUnitTestStructure.getName());
		HibernateUtil.saveOrUpdate(entity);
		jUnitTestStructure.addParent(entity, WebKeys.Structure.STRUCTURE_ENTITY);
		
		Field field = new Field("JUnit Test Text", FieldType.TEXT, DataType.TEXT, jUnitTestStructure, false, true, false, 1, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field("JUnit Test Text Area", FieldType.TEXT_AREA, DataType.LONG_TEXT, jUnitTestStructure, false, false, false, 2, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field("JUnit Test Wysiwyg", FieldType.WYSIWYG, DataType.LONG_TEXT, jUnitTestStructure, false, false, false, 3, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field("JUnit Test Date", FieldType.DATE, DataType.DATE, jUnitTestStructure, false, false, false, 4, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field("JUnit Test Time", FieldType.TIME, DataType.DATE, jUnitTestStructure, false, false, false, 5, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field("JUnit Test Date Time", FieldType.DATE_TIME, DataType.DATE, jUnitTestStructure, false, false, false, 6, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field("JUnit Test Integer", FieldType.TEXT, DataType.INTEGER, jUnitTestStructure, false, false, false, 7, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field("JUnit Test Float", FieldType.TEXT, DataType.FLOAT, jUnitTestStructure, false, false, false, 8, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field("JUnit Test Boolean", FieldType.RADIO, DataType.BOOL, jUnitTestStructure, false, false, false, 9, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field("JUnit Test File", FieldType.FILE, DataType.TEXT, jUnitTestStructure, false, false, false, 10, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field("JUnit Test Image", FieldType.IMAGE, DataType.TEXT, jUnitTestStructure, false, false, false, 11, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field(binaryFieldName, FieldType.BINARY, DataType.BINARY, jUnitTestStructure, false, false, false, 12, false, false, false);
		FieldFactory.saveField(field);
		
		field = new Field(hostFolderFieldName, FieldType.HOST_OR_FOLDER, DataType.TEXT, jUnitTestStructure, false, false, false, 12, false, false, false);
		FieldFactory.saveField(field);
		
		FieldsCache.removeFields(jUnitTestStructure);
		StructureCache.removeStructure(jUnitTestStructure);
		StructureServices.removeStructureFile(jUnitTestStructure);
		StructureFactory.saveStructure(jUnitTestStructure);
		
		permissionList = new ArrayList<Permission>();
		permissionList.add(new Permission("", roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ));
		
		Permission newPermission;
		for (Permission permission: permissionList) {
			newPermission = new Permission(jUnitTestStructure.getPermissionId(), permission.getRoleId(), permission.getPermission(), true);
			pa.save(newPermission, jUnitTestStructure, ua.getSystemUser(), false);
		}
	}
	
	private static void copy(java.io.File source, java.io.File destination) throws Exception {
		InputStream in = new FileInputStream(source);
		OutputStream out = new FileOutputStream(destination);
			
			byte[] buf = new byte[1024];
			int len;
			while (0 < (len = in.read(buf))){
				out.write(buf, 0, len);
			}
	}
	
	private void createJUnitTestFiles() throws Exception {
		String testFilePath = ".." + java.io.File.separator +
							  "test" + java.io.File.separator +
							  "com" + java.io.File.separator +
							  "dotmarketing" + java.io.File.separator +
							  "portlets" + java.io.File.separator +
							  "contentlet" + java.io.File.separator +
							  "business" + java.io.File.separator +
							  "test_files" + java.io.File.separator +
							  "test.txt";

		String copyTestFilePath = ".." + java.io.File.separator +
								  "test" + java.io.File.separator +
								  "com" + java.io.File.separator +
								  "dotmarketing" + java.io.File.separator +
								  "portlets" + java.io.File.separator +
								  "contentlet" + java.io.File.separator +
								  "business" + java.io.File.separator +
								  "test.txt";
		
		String testImagePath = ".." + java.io.File.separator +
							   "test" + java.io.File.separator +
							   "com" + java.io.File.separator +
							   "dotmarketing" + java.io.File.separator +
							   "portlets" + java.io.File.separator +
							   "contentlet" + java.io.File.separator +
							   "business" + java.io.File.separator +
							   "test_files" + java.io.File.separator +
							   "test.gif";

		String copyTestImagePath = ".." + java.io.File.separator +
								   "test" + java.io.File.separator +
								   "com" + java.io.File.separator +
								   "dotmarketing" + java.io.File.separator +
								   "portlets" + java.io.File.separator +
								   "contentlet" + java.io.File.separator +
								   "business" + java.io.File.separator +
								   "test.gif";
		
		String testBinaryPath = ".." + java.io.File.separator +
								"test" + java.io.File.separator +
								"com" + java.io.File.separator +
								"dotmarketing" + java.io.File.separator +
								"portlets" + java.io.File.separator +
								"contentlet" + java.io.File.separator +
								"business" + java.io.File.separator +
								"test_files" + java.io.File.separator +
								"binary_test.txt";
		
		String testFileFullPath = Config.CONTEXT.getRealPath(testFilePath);
		String testImageFullPath = Config.CONTEXT.getRealPath(testImagePath);
		String testBinaryFullPath = Config.CONTEXT.getRealPath(testBinaryPath);
		
		java.io.File tempTestFile = new java.io.File(testFileFullPath);
		if (!tempTestFile.exists()) {
			String message = "File does not exist: '" + testFileFullPath + "'";
			Logger.error(this, message);
			throw new Exception(message);
		}
		
		java.io.File tempTestImage = new java.io.File(testImageFullPath);
		if (!tempTestImage.exists()) {
			String message = "File does not exist: '" + testImageFullPath + "'";
			Logger.error(this, message);
			throw new Exception(message);
		}
		
		java.io.File tempTestBinary = new java.io.File(testBinaryFullPath);
		if (!tempTestBinary.exists()) {
			String message = "File does not exist: '" + testBinaryFullPath + "'";
			Logger.error(this, message);
			throw new Exception(message);
		}
		
		String copyTestFileFullPath = Config.CONTEXT.getRealPath(copyTestFilePath);
		java.io.File copyTempTestFile = new java.io.File(copyTestFileFullPath);
		if (!copyTempTestFile.exists()) {
			if (!copyTempTestFile.createNewFile()) {
				String message = "Cannot create copy of the test file: '" + copyTestFileFullPath + "'";
				Logger.error(this, message);
				throw new Exception(message);
			}
		}
		
		String copyTestImageFullPath = Config.CONTEXT.getRealPath(copyTestImagePath);
		java.io.File copyTempTestImage = new java.io.File(copyTestImageFullPath);
		if (!copyTempTestImage.exists()) {
			if (!copyTempTestImage.createNewFile()) {
				String message = "Cannot create copy of the test file: '" + copyTestImageFullPath + "'";
				Logger.error(this, message);
				throw new Exception(message);
			}
		}
		
		copy(tempTestFile, copyTempTestFile);
		copy(tempTestImage, copyTempTestImage);
		
		User user = ua.getSystemUser();
		
		testFile = new File();
		testFile.setAuthor(user.getUserId());
		testFile.setDeleted(false);
		testFile.setFileName("junit_test_file.txt");
		testFile.setFriendlyName("JUnit Test File Friendly Name");
		testFile.setIDate(new Date());
		testFile.setLive(false);
		testFile.setLocked(true);
		testFile.setMaxSize(1024);;
		testFile.setMimeType("text/plain");
		testFile.setModDate(new Date());
		testFile.setModUser(user.getUserId());
		testFile.setOwner(user.getUserId());
		testFile.setPublishDate(new Date());
		testFile.setShowOnMenu(true);
		testFile.setSize((int) copyTempTestFile.length());
		testFile.setSortOrder(2);
		testFile.setTitle("JUnit Test File");
		testFile.setType("file_asset");
		testFile.setWorking(true);
		
		BufferedImage img = javax.imageio.ImageIO.read(copyTempTestImage);
		
		testImage = new File();
		testImage.setAuthor(user.getUserId());
		testImage.setDeleted(false);
		testImage.setFileName("junit_test_image.gif");
		testImage.setFriendlyName("JUnit Test Image Friendly Name");
		testImage.setHeight(img.getHeight());
		testImage.setIDate(new Date());
		testImage.setLive(false);
		testImage.setLocked(true);
		testImage.setMaxHeight(600);
		testImage.setMaxSize(1024);
		testImage.setMaxWidth(800);
		testImage.setMimeType("image/gif");
		testImage.setMinHeight(240);
		testImage.setModDate(new Date());
		testImage.setModUser(user.getUserId());
		testImage.setOwner(user.getUserId());
		testImage.setPublishDate(new Date());
		testImage.setShowOnMenu(true);
		testImage.setSize((int) copyTempTestImage.length());
		testImage.setSortOrder(2);
		testImage.setTitle("JUnit Test Image");
		testImage.setType("file_asset");
		testImage.setWidth(img.getWidth());
		testImage.setWorking(true);
		
		testHost1 = new Host();
		testHost1.setHostname("dotcms_junit_test_host_1");
		testHost1.setModDate(new Date());
		testHost1.setModUser(user.getUserId());
		testHost1.setOwner(user.getUserId());
		testHost1.setProperty("theme", "default");
		testHost1 = hostAPI.save(testHost1, user, false);
		
		testHost2 = new Host();
		testHost2.setHostname("dotcms_junit_test_host_2");
		testHost2.setModDate(new Date());
		testHost2.setModUser(user.getUserId());
		testHost2.setOwner(user.getUserId());
		testHost2.setProperty("theme", "default");
		testHost2 = hostAPI.save(testHost2, user, false);
		
		testFolder = (Folder) InodeFactory.getInode(null, Folder.class);
		testFolder.setFilesMasks("");
		testFolder.setIDate(new Date());
		testFolder.setName("dotcms_junit_test_folder");
		testFolder.setOwner(user.getUserId());
		//testFolder.setPath("/dotcms_junit_test_folder/");
		testFolder.setShowOnMenu(false);
		testFolder.setSortOrder(0);
		testFolder.setTitle("dotcms_junit_test_folder");
		testFolder.setType("folder");
		testFolder.setHostId(testHost1.getIdentifier());
		
		folderAPI.save(testFolder,user,false);
		
		fileAPI.saveFile(testFile, copyTempTestFile, testFolder, user, false);
		fileAPI.saveFile(testImage, copyTempTestImage, testFolder, user, false);
		
		if (copyTempTestFile.exists())
			copyTempTestFile.delete();
		
		if (copyTempTestImage.exists())
			copyTempTestImage.delete();
		
		java.io.File tempLocationFolder = new java.io.File(Config.CONTEXT.getRealPath(com.dotmarketing.util.Constants.TEMP_BINARY_PATH) + java.io.File.separator + user.getUserId());
		if (!tempLocationFolder.exists())
			tempLocationFolder.mkdirs();
		
		String copyTestBinaryFullPath = tempLocationFolder.getAbsolutePath() + java.io.File.separator + testBinaryFileName;
		java.io.File copyTempTestBinary = new java.io.File(copyTestBinaryFullPath);
		if (!copyTempTestBinary.exists()) {
			if (!copyTempTestBinary.createNewFile()) {
				String message = "Cannot create copy of the test file: '" + copyTestBinaryFullPath + "'";
				Logger.error(this, message);
				throw new Exception(message);
			}
		}
		
		copy(tempTestBinary, copyTempTestBinary);
	}
	
	private void createJUnitTestBinary() throws Exception {
		String testBinaryPath = ".." + java.io.File.separator +
								"test" + java.io.File.separator +
								"com" + java.io.File.separator +
								"dotmarketing" + java.io.File.separator +
								"portlets" + java.io.File.separator +
								"contentlet" + java.io.File.separator +
								"business" + java.io.File.separator +
								"test_files" + java.io.File.separator +
								"binary_test.txt";
		
		String testBinaryFullPath = Config.CONTEXT.getRealPath(testBinaryPath);
		
		java.io.File tempTestBinary = new java.io.File(testBinaryFullPath);
		if (!tempTestBinary.exists()) {
			String message = "File does not exist: '" + testBinaryFullPath + "'";
			Logger.error(this, message);
			throw new Exception(message);
		}
		
		User user = ua.getSystemUser();
		
		java.io.File tempLocationFolder = new java.io.File(Config.CONTEXT.getRealPath(com.dotmarketing.util.Constants.TEMP_BINARY_PATH) + java.io.File.separator + user.getUserId());
		if (!tempLocationFolder.exists())
			tempLocationFolder.mkdirs();
		
		String copyTestBinaryFullPath = tempLocationFolder.getAbsolutePath() + java.io.File.separator + testBinaryFileName;
		java.io.File copyTempTestBinary = new java.io.File(copyTestBinaryFullPath);
		if (!copyTempTestBinary.exists()) {
			if (!copyTempTestBinary.createNewFile()) {
				String message = "Cannot create copy of the test file: '" + copyTestBinaryFullPath + "'";
				Logger.error(this, message);
				throw new Exception(message);
			}
		}
		
		copy(tempTestBinary, copyTempTestBinary);
	}

	/**
	 * @param user TODO
	 * @param conAPI
	 * @param lai
	 * @param pa
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private Contentlet createContentlet(String textValue, String longTextValue, Date dateValue, long longValue, float floatValue, boolean booleanValue, String fileValue, String imageValue, java.io.File binaryValue, String hostFolderValue, Language language, User user) throws DotDataException,
			DotSecurityException {


		//Get the JUnit Test Stucture
		Structure structure = new StructureFactory().getStructureByType(structureName);

		//Get the default languaje
		Language defaultLanguage = lai.getDefaultLanguage();

		//Create the new Contentlet
		Contentlet newCont = new Contentlet();
		newCont.setLive(false);
		newCont.setWorking(true);
		newCont.setStructureInode(structure.getInode());
		newCont.setHost(testHost1.getIdentifier());
		newCont.setFolder(FolderAPI.SYSTEM_FOLDER);
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
			if(field.getFieldType().equals(Field.FieldType.TEXT.toString()))
			{
				if(field.getFieldContentlet().startsWith("text"))
				{
					value = textValue;
				}
				else if (field.getFieldContentlet().startsWith("integer"))
				{
					value = longValue;
				}
				else if (field.getFieldContentlet().startsWith("float"))
				{
					value = floatValue;
				}
			}
			else if(field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || field.getFieldType().equals(Field.FieldType.WYSIWYG.toString()))
			{					
				value = longTextValue;					 					
			}
			else if(field.getFieldType().equals(Field.FieldType.TAG.toString()))
			{
				value = "Test Tag";					
			}
			else if(field.getFieldType().equals(Field.FieldType.DATE.toString()) || field.getFieldType().equals(Field.FieldType.TIME.toString()) || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()))
			{
				value = dateValue;
			}
			else if(field.getFieldContentlet().startsWith("bool"))
			{
				value = booleanValue;					
			}
			else if(field.getFieldType().equals(Field.FieldType.FILE.toString()))
			{
				value = fileValue;
			}
			else if(field.getFieldType().equals(Field.FieldType.IMAGE.toString()))
			{
				value = imageValue;
			}
			else if(field.getFieldType().equals(Field.FieldType.IMAGE.toString()))
			{
				value = imageValue;
			}
			else if(field.getFieldType().equals(Field.FieldType.BINARY.toString()))
			{
				value = null;
			}
			else if(field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString()))
			{
				value = hostFolderValue;
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
	
	private Relationship createRelationShip(Structure structure,boolean required) throws DotHibernateException
	{
		Relationship relationship = new Relationship();
		//Set Parent Info
		relationship.setParentStructureInode(structure.getInode());
		relationship.setParentRelationName("parent");
		relationship.setParentRequired(required);
		//Set Child Info
		relationship.setChildStructureInode(structure.getInode());
		relationship.setChildRelationName("child");
		relationship.setChildRequired(required);
		//Set general info
		relationship.setRelationTypeValue("parent-child");
		relationship.setCardinality(0);
		
		RelationshipFactory.saveRelationship(relationship);
		return relationship;		
	}

	@Override
	protected void tearDown() throws Exception 
	{
		//Get the System User
		User user = ua.getSystemUser();

		Structure jUnitTestStructure = StructureCache.getStructureByName(structureName);
		List<Contentlet> contentlets = conAPI.findByStructure(jUnitTestStructure, user, false, 0, 0);
		for(Contentlet newCont : contentlets)
		{
			try {
				//Unpublish the contenlet
				conAPI.unpublish(newCont, user, true);
	
				//Archive the contentlet
				conAPI.archive(newCont, user, true);
	
				//Delete the contentlet
				
				conAPI.delete(newCont, user, true);
			} catch (Exception e) {
				Logger.error(this, "Couldn't remove content", e);
			}
		}
		
		deleteJUnitTestStructure();
		
		try { fileAPI.delete(testFile, user, false); } catch (Exception e) { }
		try { fileAPI.delete(testImage, user, false); } catch (Exception e) { }
		try { folderAPI.delete(testFolder,ua.getSystemUser(),false); } catch (Exception e) { }
		try { hostAPI.delete(testHost1, user, false); } catch (Exception e) { }
		try { hostAPI.delete(testHost2, user, false); } catch (Exception e) { }
	}
	
	private void deleteJUnitTestStructure() throws DotDataException {
		Structure jUnitTestStructure = StructureCache.getStructureByName(structureName);
		List<Field> fields = FieldFactory.getFieldsByStructure(jUnitTestStructure.getInode());
		for (Field field: fields) {
			FieldFactory.deleteField(field);
		}
		
		FieldsCache.removeFields(jUnitTestStructure);
		
		pa.removePermissions(jUnitTestStructure);
		
		StructureFactory.deleteStructure(jUnitTestStructure);
		
		StructureCache.removeStructure(jUnitTestStructure);
		StructureServices.removeStructureFile(jUnitTestStructure);
	}
	
	public void testDelete() throws DotDataException, DotSecurityException
	{
		User user=ua.getSystemUser();
		
		Contentlet c=newContentlets.get(0);
		String ident= c.getIdentifier();
		DotConnect conn=new DotConnect();		
		conn.setSQL("select count(*) as count from contentlet where inode in (select inode from inode where identifier='" +ident+"' );");
		ArrayList<Map<String,String>> list=conn.loadResults();
		int count=Integer.parseInt(list.get(0).get("count"));
		assertEquals("Should have one inode (1 versions)",1,count);
		
		HibernateUtil.startTransaction();
		//Archive the contentlet
		conAPI.archive(c, user, true);
		//Delete the contentlet
		conAPI.delete(c, user, true);
		HibernateUtil.commitTransaction();
		newContentlets.remove(c);
		
		
		
		 conn=new DotConnect();
			conn.setSQL("select count(*) as count from contentlet where inode in (select inode from inode where identifier='" +ident+"' );");
			list=conn.loadResults();
			count=Integer.parseInt(list.get(0).get("count"));
			assertEquals("Should have no inodes (we deleted them)",0,count);
		
	}
	
	
	public void testDeleteMultipleVersions() throws DotDataException, DotSecurityException
	{
		//TESTING DOTCMSEE-279
		User user=ua.getSystemUser();		
		Contentlet orgContent=newContentlets.get(0);		
		Contentlet c =createNewVersion(orgContent);
		String ident= c.getIdentifier();
		DotConnect conn=new DotConnect();		
		conn.setSQL("select count(*) as count from contentlet where inode in (select inode from inode where identifier='" +ident+"' );");
		ArrayList<Map<String,String>> list=conn.loadResults();
		int count=Integer.parseInt(list.get(0).get("count"));
		assertEquals("Should have two inodes (2 versions)",2,count);
		
		HibernateUtil.startTransaction();
		//Archive the contentlet
		conAPI.archive(c, user, true);
		//Delete the contentlet
		conAPI.delete(c, user, true);
		HibernateUtil.commitTransaction();
		newContentlets.remove(orgContent);
		
		
		
		 conn=new DotConnect();
			conn.setSQL("select count(*) as count from contentlet where inode in (select inode from inode where identifier='" +ident+"' );");
			list=conn.loadResults();
			count=Integer.parseInt(list.get(0).get("count"));
			assertEquals("Should have no inodes (we deleted them)",0,count);
		
	}
	
	public void testDeleteListMultipleVersions() throws DotDataException, DotSecurityException
	{
		//TESTING DOTCMSEE-279
		User user=ua.getSystemUser();
		
		Contentlet orgContent=newContentlets.get(0);
		
		Contentlet c =createNewVersion(orgContent);
		String ident= c.getIdentifier();
		DotConnect conn=new DotConnect();		
		conn.setSQL("select count(*) as count from contentlet where inode in (select inode from inode where identifier='" +ident+"' );");
		ArrayList<Map<String,String>> list=conn.loadResults();
		int count=Integer.parseInt(list.get(0).get("count"));
		assertEquals("Should have two inodes (2 versions)",2,count);
		
		HibernateUtil.startTransaction();
		//Archive the contentlet
		conAPI.archive(c, user, true);
		//Delete the contentlet
		List<Contentlet> delList=new ArrayList<Contentlet>();
		delList.add(c);
		conAPI.delete(delList, user, true);
		HibernateUtil.commitTransaction();
		newContentlets.remove(orgContent);
		
		
		 conn=new DotConnect();
			conn.setSQL("select count(*) as count from contentlet where inode in (select inode from inode where identifier='" +ident+"' );");
			list=conn.loadResults();
			count=Integer.parseInt(list.get(0).get("count"));
			assertEquals("Should have no inodes (we deleted them)",0,count);
		
	}


	//NO Language => true
	public void testNOLanguage() throws DotDataException
	{
		String float1 = "";
		String integer2 = "";
		String languageId = Long.toString(lai.getDefaultLanguage().getId());
		boolean expectedValue = true;
		boolean greaterThanZero = doSearch(textValue,float1,integer2,languageId);
		assertEquals(expectedValue,greaterThanZero);
	}

	//Default Language => true
	public void testDefaultLanguage() throws DotDataException
	{
		String text1 = language.getCountry() + " Language";
		String float1 = "";
		String integer2 = "";
		String languageId = Long.toString(language.getId());
		boolean expectedValue = true;
		boolean greaterThanZero = doSearch(text1,float1,integer2,languageId);
		assertEquals(expectedValue,greaterThanZero);
	}
	
	public void testAddWithNoRequiredRelationship() throws DotDataException, DotSecurityException, Exception
	{
		boolean returnValue = false;
		//Get the System User
		User user = ua.getSystemUser();	
		
		//Set the relationship info
		boolean required = false;
		Structure structure = new StructureFactory().getStructureByType(structureName);
		Relationship relationship = createRelationShip(structure, required);
		
		//Create the new contentlet
		createJUnitTestBinary();
		Contentlet contentlet = createContentlet(textValue, longTextValue, dateValue, longValue, floatValue, booleanValue, testFile.getIdentifier(), testImage.getIdentifier(), new java.io.File(testBinaryFileName), FolderAPI.SYSTEM_FOLDER, null, APILocator.getUserAPI().getSystemUser());
		//Unpublish the contenlet
		conAPI.unpublish(contentlet, user, true);
		//Archive the contentlet
		conAPI.archive(contentlet, user, true);
		//Delete the contentlet
		conAPI.delete(contentlet, user, true);
		
		//Delete the relationship
		relationship = RelationshipFactory.getRelationshipByRelationTypeValue(relationship.getRelationTypeValue());		
		TreeFactory.deleteTreesByRelationType(relationship.getRelationTypeValue());
		RelationshipFactory.deleteRelationship(relationship);
		
		//If arrive here is OK
		returnValue = true;
		assertTrue(returnValue);
	}
	
	public void testAddWithRequiredRelationship() throws DotSecurityException, DotDataException, Exception
	{		
		boolean returnValue = false;
		//Get the System User
		User user = ua.getSystemUser();

		//Set the relationship info
		boolean required = true;
		Structure structure = new StructureFactory().getStructureByType(structureName);
		Relationship relationship = createRelationShip(structure, required);

		//Create the new contentlet
		try
		{
			createJUnitTestBinary();
			Contentlet contentlet = createContentlet(textValue, longTextValue, dateValue, longValue, floatValue, booleanValue, testFile.getIdentifier(), testImage.getIdentifier(), new java.io.File(testBinaryFileName), FolderAPI.SYSTEM_FOLDER, null, APILocator.getUserAPI().getSystemUser());
			//Unpublish the contenlet
			conAPI.unpublish(contentlet, user, true);
			//Archive the contentlet
			conAPI.archive(contentlet, user, true);
			//Delete the contentlet
			conAPI.delete(contentlet, user, true);
			//Delete the relatioship
		}
		catch(Exception ex)
		{
			returnValue = true;
		}
		//Delete the relationship
		TreeFactory.deleteTreesByRelationType(relationship.getRelationTypeValue());
		RelationshipFactory.deleteRelationship(relationship);
		
		assertTrue(returnValue);
	}	
	


	private boolean doSearch(String text1,String float1,String integer2,String languageId) throws DotDataException
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
		fields.add("text1");
		fields.add(text1);
		fields.add("float1");
		fields.add(float1);
		fields.add("integer2");
		fields.add(integer2);
		fields.add("languageId");
		fields.add(languageId);
		ArrayList<String> categories = new ArrayList<String>();
		ContentletAjax ca = new ContentletAjax();
		List list = ca.searchContentletsByUser(structure.getInode(),fields,categories,showDeleted, false, page,  orderBy,perPage,currentUser,sess,modDateFrom,modDateTo);
		long size = (Long) ((HashMap) list.get(0)).get("total"); 
		boolean greaterThanZero = (size > 0 ? true : false);
		return greaterThanZero;
	}
	
	private Contentlet createNewVersion (Contentlet orgContent) throws DotDataException, DotContentletStateException, DotSecurityException {
		
		User user=ua.getSystemUser();
		
		Contentlet c = conAPI.checkout(orgContent.getInode(), user, false);
		List<Field> fields=FieldsCache.getFieldsByStructureInode(c.getStructureInode());
		for (Field field: fields) {
			if(field.getFieldType().equals(Field.FieldType.WYSIWYG.toString()))	{					
				conAPI.setContentletProperty(c, field, "NEW VALUE"); 					
			}
		}
		
		//Get the News Item Parent Category 
		CategoryAPI ca = APILocator.getCategoryAPI();
		Category parentCategory = ca.findByKey("nptype", user, true);				
		//Get the News Item Child Categories
		List<Category> categories = ca.getChildren(parentCategory, user, true);
		
		List<Permission> structurePermissions = pa.getPermissions(c);
		conAPI.validateContentlet(c,categories);
		Contentlet ret = conAPI.checkin(c,categories, structurePermissions, user, true);
		return ret;
	}
	
	public void testCleanField() throws DotSecurityException, DotDataException {
		boolean textWithDefaultValues = true;
		boolean dateWithDefaultValues = true;
		boolean integerWithDefaultValues = true;
		boolean floatWithDefaultValues = true;
		boolean booleanWithDefaultValues = true;
		Structure jUnitTestStructure = StructureCache.getStructureByName(structureName);
		List<Field> fields = FieldsCache.getFieldsByStructureVariableName(structureVelocityVarName);
		User user = ua.getSystemUser();
		Object value;
		
		Contentlet contentlet;
		
		for (int i = 0; i < newContentlets.size(); ++i) {
			contentlet = newContentlets.get(i);
			for (Field field: fields) {
				conAPI.cleanField(jUnitTestStructure, field, user, false);
			}
			
			contentlet = conAPI.findContentletByIdentifier(contentlet.getIdentifier(), contentlet.isLive(), contentlet.getLanguageId(), user, false);
			newContentlets.set(i, contentlet);
		}
		
		for (int i = 0; i < newContentlets.size(); ++i) {
			contentlet = newContentlets.get(i);
			for (Field field: fields) {
				value = conAPI.getFieldValue(contentlet, field);
				
				if (field.getFieldType().equals(Field.FieldType.TEXT.toString())) {
					if (field.getFieldContentlet().startsWith("text") && textValue.equals((String) value)) {
						textWithDefaultValues = false;
					} else if (field.getFieldContentlet().startsWith("integer") && (longValue == ((Long) value))) {
						integerWithDefaultValues = false;
					} else if (field.getFieldContentlet().startsWith("float") && (floatValue == ((Float) value))) {
						floatWithDefaultValues = false;
					}
				} else if ((field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) && longTextValue.equals((String) value)) {					
					textWithDefaultValues = false;					 					
				} else if ((field.getFieldType().equals(Field.FieldType.DATE.toString()) ||
						    field.getFieldType().equals(Field.FieldType.TIME.toString()) ||
						    field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) &&
						   dateValue.equals((Date) value)) {
					dateWithDefaultValues = false;
				}
				else if (field.getFieldContentlet().startsWith("bool") && ((Boolean) value)) {
					booleanWithDefaultValues = false;
				}
			}
		}
		
		if (!textWithDefaultValues)
			assertTrue("Text field not cleaned", textWithDefaultValues);
		
		if (!dateWithDefaultValues)
			assertTrue("Date field not cleaned", dateWithDefaultValues);
		
		if (!integerWithDefaultValues)
			assertTrue("Integer field not cleaned", integerWithDefaultValues);
		
		if (!floatWithDefaultValues)
			assertTrue("Float field not cleaned", floatWithDefaultValues);
		
		if (!booleanWithDefaultValues)
			assertTrue("Boolean field not cleaned", booleanWithDefaultValues);
	}
	
	private boolean checkPermission(Contentlet content) throws Exception {
		List<Permission> permissions = pa.getPermissions(content);
		
		if (permissions.size() < permissionList.size())
			return false;

		for (Permission permission1: permissionList) {
			Boolean found=false;
			for (Permission permission2: permissions){
				if ((permission1.getPermission() == permission2.getPermission()) && permission1.getRoleId().equals(permission2.getRoleId())) {
					found=true;
				}
			}
			if(!found){
				return false;
			}
		}
		return true;
	}
	
	public void testCopy() throws Exception {
		User user = ua.getSystemUser();
		Contentlet testContent = newContentlets.get(0);
		Contentlet testContentCopy1 = conAPI.copyContentlet(testContent, testHost1, user, false);
		Contentlet testContentCopy2 = conAPI.copyContentlet(testContent, testHost1, user, false);
		
		try {
			assertFalse("Invalid \"identifier\" in first copy content.", testContentCopy1.getIdentifier().equals(testContent.getIdentifier()));
			assertFalse("Invalid \"inode\" in first copy content.", testContentCopy1.getInode().equals(testContent.getInode()));
			assertTrue("Invalid \"folder\" in first copy content.", testContentCopy1.getFolder().equals(FolderAPI.SYSTEM_FOLDER));
			assertEquals("Invalid \"host\" in first copy content.", testContent.getHost(), testContentCopy1.getHost());
			assertEquals("Invalid \"structureInode\" in first copy content.", testContent.getStructureInode(), testContentCopy1.getStructureInode());
			
			Map<String, Object> testContentMap = testContent.getMap();
			Map<String, Object> testContentCopyMap = testContentCopy1.getMap();
			List<Field> fields = FieldsCache.getFieldsByStructureInode(testContent.getStructureInode());
			Field field;
			for (int pos = 0; pos < fields.size(); ++pos) {
				field = fields.get(pos);
				if (pos == 0) {
					if (field.getFieldContentlet().startsWith("text")) {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", testContentCopyMap.get(field.getVelocityVarName()), testContentMap.get(field.getVelocityVarName()));
					} else {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", conAPI.getName(testContent, user, false), testContentCopyMap.get(field.getVelocityVarName()));
					}
				} else {
					assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", testContentMap.get(field.getVelocityVarName()), testContentCopyMap.get(field.getVelocityVarName()));
				}
			}
			assertTrue("Invalid permissions assigned to first copy content.", checkPermission(testContentCopy1));
			
			java.io.File binary1 = conAPI.getBinaryFile(testContentCopy1.getInode(), UtilMethods.toCamelCase(binaryFieldName), user);
			assertFalse("Null \"binary file\" in first copy content.", binary1 == null);
			assertTrue("Invalid \"binary file name\" in first copy content.", binary1.getName().equals(testBinaryFileName));
			assertTrue("Invalid \"binary file\" in first copy content.", 0 < binary1.length());
			
			java.io.File binary = conAPI.getBinaryFile(testContent.getInode(), UtilMethods.toCamelCase(binaryFieldName), user);
			assertFalse("\"binary file\" in first copy content is using the same file as the original content.", binary.getPath().equals(binary1.getPath()));
			
			
			assertFalse("Invalid \"identifier\" in second copy content.", testContentCopy2.getIdentifier().equals(testContent.getIdentifier()));
			assertFalse("Invalid \"inode\" in second copy content.", testContentCopy2.getInode().equals(testContent.getInode()));
			assertTrue("Invalid \"folder\" in second copy content.", testContentCopy2.getFolder().equals(FolderAPI.SYSTEM_FOLDER));
			assertEquals("Invalid \"host\" in second copy content.", testContent.getHost(), testContentCopy2.getHost());
			assertEquals("Invalid \"structureInode\" in second copy content.", testContent.getStructureInode(), testContentCopy2.getStructureInode());
			
			testContentCopyMap = testContentCopy2.getMap();
			fields = FieldsCache.getFieldsByStructureInode(testContent.getStructureInode());
			for (int pos = 0; pos < fields.size(); ++pos) {
				field = fields.get(pos);
				if (pos == 0) {
					if (field.getFieldContentlet().startsWith("text")) {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in second copy content.", testContentCopyMap.get(field.getVelocityVarName()), testContentMap.get(field.getVelocityVarName()));
					} else {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in second copy content.", conAPI.getName(testContent, user, false), testContentCopyMap.get(field.getVelocityVarName()));
					}
				} else {
					assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in second copy content.", testContentMap.get(field.getVelocityVarName()), testContentCopyMap.get(field.getVelocityVarName()));
				}
			}
			assertTrue("Invalid permissions assigned to second copy content.", checkPermission(testContentCopy2));
			
			java.io.File binary2 = conAPI.getBinaryFile(testContentCopy2.getInode(), UtilMethods.toCamelCase(binaryFieldName), user);
			assertFalse("Null \"binary file\" in second copy content.", binary2 == null);
			assertTrue("Invalid \"binary file name\" in second copy content.", binary2.getName().equals(testBinaryFileName));
			assertTrue("Invalid \"binary file\" in second copy content.", 0 < binary2.length());
			
			assertFalse("\"binary file\" in second copy content is using the same file as the original content.", binary.getPath().equals(binary2.getPath()));
			assertFalse("\"binary file\" in first copy content is using the same file as the second copy content.", binary1.getPath().equals(binary2.getPath()));
			
			assertFalse("The second copy content has overwrote the first copy content.", testContentCopy1.getIdentifier().equals(testContentCopy2.getIdentifier()));
			assertFalse("The second copy content has overwrote the first copy content.", testContentCopy1.getInode().equals(testContentCopy2.getInode()));
		} finally {
			conAPI.delete(testContentCopy1, user, false);
			conAPI.delete(testContentCopy2, user, false);
		}
	}
	
	public void testCopyAnotherHost() throws Exception {
		User user = ua.getSystemUser();
		Contentlet testContent = newContentlets.get(0);
		Contentlet testContentCopy1 = conAPI.copyContentlet(testContent, testHost2, user, false);
		Contentlet testContentCopy2 = conAPI.copyContentlet(testContent, testHost2, user, false);
		
		try {
			assertFalse("Invalid \"identifier\" in first copy content.", testContentCopy1.getIdentifier().equals(testContent.getIdentifier()));
			assertFalse("Invalid \"inode\" in first copy content.", testContentCopy1.getInode().equals(testContent.getInode()));
			assertTrue("Invalid \"folder\" in first copy content.", testContentCopy1.getFolder().equals(FolderAPI.SYSTEM_FOLDER));
			assertFalse("Invalid \"host\" in first copy content.", testContent.getHost().equals( testContentCopy1.getHost()));
			assertEquals("Invalid \"structureInode\" in first copy content.", testContent.getStructureInode(), testContentCopy1.getStructureInode());
			
			Map<String, Object> testContentMap = testContent.getMap();
			Map<String, Object> testContentCopyMap = testContentCopy1.getMap();
			List<Field> fields = FieldsCache.getFieldsByStructureInode(testContent.getStructureInode());
			Field field;
			for (int pos = 0; pos < fields.size(); ++pos) {
				field = fields.get(pos);
				if (pos == 0) {
					if (field.getFieldContentlet().startsWith("text")) {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", testContentCopyMap.get(field.getVelocityVarName()), testContentMap.get(field.getVelocityVarName()));
					} else {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", conAPI.getName(testContent, user, false), testContentCopyMap.get(field.getVelocityVarName()));
					}
				} else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
					String hostId = (String) testContentCopyMap.get(field.getVelocityVarName());
					assertFalse("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", testContentMap.get(field.getVelocityVarName()).equals(hostId));
					if (hostId != null) {
						assertFalse("Invalid \"" + field.getVelocityVarName() + "\" in first copy content. Not a Host.", hostAPI.find(hostId, user, false) == null);
					}
				} else {
					assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", testContentMap.get(field.getVelocityVarName()), testContentCopyMap.get(field.getVelocityVarName()));
				}
			}
			assertTrue("Invalid permissions assigned to first copy content.", checkPermission(testContentCopy1));
			
			java.io.File binary1 = conAPI.getBinaryFile(testContentCopy1.getInode(), UtilMethods.toCamelCase(binaryFieldName), user);
			assertFalse("Null \"binary file\" in first copy content.", binary1 == null);
			assertTrue("Invalid \"binary file name\" in first copy content.", binary1.getName().equals(testBinaryFileName));
			assertTrue("Invalid \"binary file\" in first copy content.", 0 < binary1.length());
			
			java.io.File binary = conAPI.getBinaryFile(testContent.getInode(), UtilMethods.toCamelCase(binaryFieldName), user);
			assertFalse("\"binary file\" in first copy content is using the same file as the original content.", binary.getPath().equals(binary1.getPath()));
			
			
			assertFalse("Invalid \"identifier\" in second copy content.", testContentCopy2.getIdentifier().equals(testContent.getIdentifier()));
			assertFalse("Invalid \"inode\" in second copy content.", testContentCopy2.getInode().equals(testContent.getInode()));
			assertTrue("Invalid \"folder\" in second copy content.", testContentCopy2.getFolder().equals(FolderAPI.SYSTEM_FOLDER));
			assertFalse("Invalid \"host\" in second copy content.", testContent.getHost().equals( testContentCopy2.getHost()));
			assertEquals("Invalid \"structureInode\" in second copy content.", testContent.getStructureInode(), testContentCopy2.getStructureInode());
			
			testContentCopyMap = testContentCopy2.getMap();
			fields = FieldsCache.getFieldsByStructureInode(testContent.getStructureInode());
			for (int pos = 0; pos < fields.size(); ++pos) {
				field = fields.get(pos);
				if (pos == 0) {
					if (field.getFieldContentlet().startsWith("text")) {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in second copy content.", testContentCopyMap.get(field.getVelocityVarName()), testContentMap.get(field.getVelocityVarName()));
					} else {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in second copy content.", conAPI.getName(testContent, user, false), testContentCopyMap.get(field.getVelocityVarName()));
					}
				} else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
					String hostId = (String) testContentCopyMap.get(field.getVelocityVarName());
					assertFalse("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", testContentMap.get(field.getVelocityVarName()).equals(hostId));
					if (hostId != null) {
						assertFalse("Invalid \"" + field.getVelocityVarName() + "\" in first copy content. Not a Host.", hostAPI.find(hostId, user, false) == null);
					}
				} else {
					assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in second copy content.", testContentMap.get(field.getVelocityVarName()), testContentCopyMap.get(field.getVelocityVarName()));
				}
			}
			assertTrue("Invalid permissions assigned to second copy content.", checkPermission(testContentCopy2));
			
			java.io.File binary2 = conAPI.getBinaryFile(testContentCopy2.getInode(), UtilMethods.toCamelCase(binaryFieldName), user);
			assertFalse("Null \"binary file\" in second copy content.", binary2 == null);
			assertTrue("Invalid \"binary file name\" in second copy content.", binary2.getName().equals(testBinaryFileName));
			assertTrue("Invalid \"binary file\" in second copy content.", 0 < binary2.length());
			
			assertFalse("\"binary file\" in second copy content is using the same file as the original content.", binary.getPath().equals(binary2.getPath()));
			assertFalse("\"binary file\" in first copy content is using the same file as the second copy content.", binary1.getPath().equals(binary2.getPath()));
			
			assertFalse("The second copy content has overwrote the first copy content.", testContentCopy1.getIdentifier().equals(testContentCopy2.getIdentifier()));
			assertFalse("The second copy content has overwrote the first copy content.", testContentCopy1.getInode().equals(testContentCopy2.getInode()));
		} finally {
			conAPI.delete(testContentCopy1, user, false);
			conAPI.delete(testContentCopy2, user, false);
		}
	}
	
	public void testCopyAnotherFolder() throws Exception {
		User user = ua.getSystemUser();
		Contentlet testContent = newContentlets.get(0);
		Contentlet testContentCopy1 = conAPI.copyContentlet(testContent, testFolder, user, false);
		Contentlet testContentCopy2 = conAPI.copyContentlet(testContent, testFolder, user, false);
		
		try {
			assertFalse("Invalid \"identifier\" in first copy content.", testContentCopy1.getIdentifier().equals(testContent.getIdentifier()));
			assertFalse("Invalid \"inode\" in first copy content.", testContentCopy1.getInode().equals(testContent.getInode()));
			assertFalse("Invalid \"folder\" in first copy content.", testContentCopy1.getFolder().equals(FolderAPI.SYSTEM_FOLDER));
			assertEquals("Invalid \"structureInode\" in first copy content.", testContent.getStructureInode(), testContentCopy1.getStructureInode());
			
			Map<String, Object> testContentMap = testContent.getMap();
			Map<String, Object> testContentCopyMap = testContentCopy1.getMap();
			List<Field> fields = FieldsCache.getFieldsByStructureInode(testContent.getStructureInode());
			Field field;
			for (int pos = 0; pos < fields.size(); ++pos) {
				field = fields.get(pos);
				if (pos == 0) {
					if (field.getFieldContentlet().startsWith("text")) {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", testContentCopyMap.get(field.getVelocityVarName()), testContentMap.get(field.getVelocityVarName()));
					} else {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", conAPI.getName(testContent, user, false), testContentCopyMap.get(field.getVelocityVarName()));
					}
				} else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
					String folderId = (String) testContentCopyMap.get(field.getVelocityVarName());
					assertFalse("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", testContentMap.get(field.getVelocityVarName()).equals(folderId));
					if (folderId != null) {
						assertFalse("Invalid \"" + field.getVelocityVarName() + "\" in first copy content. Not a Folder.", folderAPI.find(folderId,user,false) == null);
					}
				} else {
					assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", testContentMap.get(field.getVelocityVarName()), testContentCopyMap.get(field.getVelocityVarName()));
				}
			}
			assertTrue("Invalid permissions assigned to first copy content.", checkPermission(testContentCopy1));
			
			java.io.File binary1 = conAPI.getBinaryFile(testContentCopy1.getInode(), UtilMethods.toCamelCase(binaryFieldName), user);
			assertFalse("Null \"binary file\" in first copy content.", binary1 == null);
			assertTrue("Invalid \"binary file name\" in first copy content.", binary1.getName().equals(testBinaryFileName));
			assertTrue("Invalid \"binary file\" in first copy content.", 0 < binary1.length());
			
			java.io.File binary = conAPI.getBinaryFile(testContent.getInode(), UtilMethods.toCamelCase(binaryFieldName), user);
			assertFalse("\"binary file\" in first copy content is using the same file as the original content.", binary.getPath().equals(binary1.getPath()));
			
			
			assertFalse("Invalid \"identifier\" in second copy content.", testContentCopy2.getIdentifier().equals(testContent.getIdentifier()));
			assertFalse("Invalid \"inode\" in second copy content.", testContentCopy2.getInode().equals(testContent.getInode()));
			assertFalse("Invalid \"folder\" in second copy content.", testContentCopy2.getFolder().equals(FolderAPI.SYSTEM_FOLDER));
			assertEquals("Invalid \"structureInode\" in second copy content.", testContent.getStructureInode(), testContentCopy2.getStructureInode());
			
			testContentCopyMap = testContentCopy2.getMap();
			fields = FieldsCache.getFieldsByStructureInode(testContent.getStructureInode());
			for (int pos = 0; pos < fields.size(); ++pos) {
				field = fields.get(pos);
				if (pos == 0) {
					if (field.getFieldContentlet().startsWith("text")) {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in second copy content.", testContentCopyMap.get(field.getVelocityVarName()), testContentMap.get(field.getVelocityVarName()));
					} else {
						assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in second copy content.", conAPI.getName(testContent, user, false), testContentCopyMap.get(field.getVelocityVarName()));
					}
				} else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
					String folderId = (String) testContentCopyMap.get(field.getVelocityVarName());
					assertFalse("Invalid \"" + field.getVelocityVarName() + "\" in first copy content.", testContentMap.get(field.getVelocityVarName()).equals(folderId));
					if (folderId != null) {
						assertFalse("Invalid \"" + field.getVelocityVarName() + "\" in first copy content. Not a Folder.", folderAPI.find(folderId,user,false) == null);
					}
				} else {
					assertEquals("Invalid \"" + field.getVelocityVarName() + "\" in second copy content.", testContentMap.get(field.getVelocityVarName()), testContentCopyMap.get(field.getVelocityVarName()));
				}
			}
			assertTrue("Invalid permissions assigned to second copy content.", checkPermission(testContentCopy2));
			
			java.io.File binary2 = conAPI.getBinaryFile(testContentCopy2.getInode(), UtilMethods.toCamelCase(binaryFieldName), user);
			assertFalse("Null \"binary file\" in second copy content.", binary2 == null);
			assertTrue("Invalid \"binary file name\" in second copy content.", binary2.getName().equals(testBinaryFileName));
			assertTrue("Invalid \"binary file\" in second copy content.", 0 < binary2.length());
			
			assertFalse("\"binary file\" in second copy content is using the same file as the original content.", binary.getPath().equals(binary2.getPath()));
			assertFalse("\"binary file\" in first copy content is using the same file as the second copy content.", binary1.getPath().equals(binary2.getPath()));
			
			assertFalse("The second copy content has overwrote the first copy content.", testContentCopy1.getIdentifier().equals(testContentCopy2.getIdentifier()));
			assertFalse("The second copy content has overwrote the first copy content.", testContentCopy1.getInode().equals(testContentCopy2.getInode()));
		} finally {
			conAPI.delete(testContentCopy1, user, false);
			conAPI.delete(testContentCopy1, user, false);
		}
	}
	
	

	public void testEditContent() throws DotDataException, DotSecurityException, ParseException {
		//Create content with test user
		APILocator.getUserAPI().getSystemUser();
		User u=APILocator.getUserAPI().loadByUserByEmail("test@dotcms.org",APILocator.getUserAPI().getSystemUser(),false);
		Contentlet contentlet =  createContentlet("test text", "<b>Test wysiwyg</b>", new Date(),50,0f,false,null, null, null,null, null, u );
		//Check if reindexation of original contentlet is working
		String query="+inode:" + contentlet.getInode();
		List<Contentlet> list=conAPI.search(query, 1, 0, "inode", u, false);
		assertTrue("First Conte should be in index",list.size()==1);
		//Edit content with test user
		Contentlet newCont=editContentletWithDifferentUser(contentlet.getInode(), language, u);
		//Check if reindexation of edited contentlet is working
		query="+inode:" + newCont.getInode();
		list=conAPI.search(query, 1, 0, "inode",u, false);
		assertTrue("Should have the inode in the index",list.size()==1);
	}
	
	public Contentlet editContentletWithDifferentUser(String inode, Language language, User user) throws DotDataException,
	DotSecurityException {
		
		//Get the News Item Stucture
		Structure structure = new StructureFactory().getStructureByType(structureName);
		
		//Search Contentlet according to a given inode
		Contentlet newCont = null;
		newCont = conAPI.find(inode, user, false);
		//newCont = conAPI.findContentletByIdentifier(identifier, true, language.getId(), user, false);
		newCont.setInode(null);
		newCont.setLive(true);
		newCont.setWorking(true);
		newCont.setStructureInode(structure.getInode());
		List<Field> fields = structure.getFields();
		fields = structure.getFields();

		//edit at least one Contentlet Field
		for (Field field : fields) 
		{ 
			Object value = null;
			if(field.getFieldType().equals(Field.FieldType.TEXT.toString()))
			{
				if(field.getFieldContentlet().startsWith("text"))
				{
					value = "new";
				}
				else if (field.getFieldContentlet().startsWith("float"))
				{
					value = 15;
				} 					
			}
			else if(field.getFieldType().equals(Field.FieldType.WYSIWYG.toString()))
			{					
				value = "<b>Testing</b>";		 					
			}
			else if(field.getFieldType().equals(Field.FieldType.TAG.toString()))
			{
				value = "Test Tag";					
			}
		}
		
		/*Save edited content*/

		//Get the News Item Parent Category 
		CategoryAPI ca = APILocator.getCategoryAPI();
		Category parentCategory = ca.findByKey("nptype", user, true);
		
		//Get the News Item Child Categories
		List<Category> categories = ca.getChildren(parentCategory, user, true);
		
		//Get The permissions of the structure
		List<Permission> structurePermissions = pa.getPermissions(structure);						

		//Validate if the contenlet is OK
		conAPI.validateContentlet(newCont, categories);

		//Save the contentlet and update the global variable for the tearDown
		newCont = conAPI.checkin(newCont, categories, structurePermissions, user, true);
		return newCont;

	} 
}
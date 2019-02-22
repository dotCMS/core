package com.dotmarketing.portlets.structure.business;

import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class URLMapTest {

	private Folder testFolder;
	private Template template;
	private Container container;
	private Structure testSt;
	private Contentlet widget;
	private User user;
	
	final String salt=Long.toString(System.currentTimeMillis());


	@Before
	public void createAssets() throws Exception {
		try {

			HibernateUtil.setAsyncCommitListenersFinalization(false);

			user = APILocator.getUserAPI().getSystemUser();
			Host demoHost = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);


			HibernateUtil.startTransaction();

			// CONTAINER
			container = APILocator.getContainerAPI().getLiveContainerById("a050073a-a31e-4aab-9307-86bfb248096a", user, false);

			Structure simpleWidgetSt = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("SimpleWidget");

			template = APILocator.getTemplateAPI().find("941a3f59-0d87-4084-8d9c-aca29a26ec8c",user,false);


			// FOLDER
			testFolder = new Folder();

			testFolder.setFilesMasks( "" );
			testFolder.setIDate( new Date() );
			testFolder.setName( "news_test_folder_" + String.valueOf( new Date().getTime() ) );
			testFolder.setOwner( user.getUserId() );
			testFolder.setShowOnMenu( false );
			testFolder.setSortOrder( 0 );
			testFolder.setTitle( "news_test_folder_" + String.valueOf( new Date().getTime() ) );
			testFolder.setType( "folder" );
			testFolder.setHostId( demoHost.getIdentifier() );
			//Creates and set an identifier
			Identifier identifier = APILocator.getIdentifierAPI().createNew( testFolder, demoHost );
			testFolder.setIdentifier( identifier.getId() );

			APILocator.getFolderAPI().save( testFolder, user, false );


			// HTMLPAGE
			HTMLPageAsset htmlPage = new HTMLPageDataGen(testFolder, template).nextPersisted();

			PermissionAPI permissionAPI = APILocator.getPermissionAPI();
			RoleAPI roleAPI = APILocator.getRoleAPI();

			Permission newPermission = new Permission( htmlPage.getPermissionId(), roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ, true );
			permissionAPI.save( newPermission, htmlPage, user, false );

			//Make it working and live
			APILocator.getVersionableAPI().setWorking( htmlPage );
			APILocator.getVersionableAPI().setLive( htmlPage );

			// WIDGET


			widget = new Contentlet();
			widget.setReviewInterval("1m");
			widget.setStructureInode(simpleWidgetSt.getInode());
			widget.setHost(demoHost.getIdentifier());

			Field widgetTitle =  simpleWidgetSt.getFieldVar("widgetTitle");
			Field widgetCode = simpleWidgetSt.getFieldVar("code");


			ContentletAPI contentletAPI = APILocator.getContentletAPI();

			contentletAPI.setContentletProperty( widget, widgetTitle, "newsTestWidget" );
			contentletAPI.setContentletProperty( widget, widgetCode, "$URLMapContent.story" );

			widget.setIndexPolicy(IndexPolicy.FORCE);
			widget.setIndexPolicyDependencies(IndexPolicy.FORCE);
			widget.setBoolProperty(Contentlet.IS_TEST_MODE, true);
			widget = contentletAPI.checkin( widget, null, permissionAPI.getPermissions( simpleWidgetSt ), user, false );
			widget.setIndexPolicy(IndexPolicy.FORCE);
            widget.setIndexPolicyDependencies(IndexPolicy.FORCE);
			widget.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.publish(widget, user, false);
			APILocator.getVersionableAPI().setLive(widget);

			// add the widget to the detail page
			APILocator.getMultiTreeAPI().saveMultiTree( new MultiTree( htmlPage.getIdentifier(), container.getIdentifier(), widget.getIdentifier() ) );

			
			// STRUCTURE
			testSt = new Structure();

			testSt.setDefaultStructure( false );
			testSt.setDescription( "News Test" );
			testSt.setFixed( false );
			testSt.setIDate( new Date() );
			testSt.setName( "NewsTest" +salt);
			testSt.setOwner( user.getUserId() );
			testSt.setStructureType( Structure.STRUCTURE_TYPE_CONTENT );
			testSt.setType( "structure" );
			testSt.setVelocityVarName( "NewsTest" +salt );
			testSt.setUrlMapPattern("/newstest"+salt+"/{urlNewsTitle}");
			testSt.setDetailPage( htmlPage.getIdentifier() );

			StructureFactory.saveStructure( testSt );

			//Creating and adding permissions
			Permission permissionRead = new Permission( testSt.getInode(), roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ );
			Permission permissionEdit = new Permission( testSt.getInode(), roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_EDIT );
			Permission permissionWrite = new Permission( testSt.getInode(), roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_WRITE );

			permissionAPI.save( permissionRead, testSt, user, false );
			permissionAPI.save( permissionEdit, testSt, user, false );
			permissionAPI.save( permissionWrite, testSt, user, false );

			// FIELDS

			// headline
			Field headline = new Field();
			headline.setFieldName("Headline");
			headline.setFieldType(FieldType.TEXT.toString());
			headline.setListed(true);
			headline.setRequired(true);
			headline.setSearchable(true);
			headline.setStructureInode(testSt.getInode());
			headline.setType("field");
			headline.setValues("");
			headline.setVelocityVarName("testTitle");
			headline.setIndexed(true);
			headline.setFieldContentlet("text4");
			FieldFactory.saveField( headline );

			// URL Title
			Field urlTitle = new Field();
			urlTitle.setFieldName("URL Title");
			urlTitle.setFieldType(FieldType.CUSTOM_FIELD.toString());
			urlTitle.setRequired(true);
			urlTitle.setStructureInode(testSt.getInode());
			urlTitle.setType("field");
			urlTitle.setValues("#dotParse('//shared/vtl/custom-fields/url-title.vtl')");
			urlTitle.setVelocityVarName("urlNewsTitle");
			urlTitle.setSearchable(true);
			urlTitle.setIndexed(true);
			urlTitle.setFieldContentlet("text_area4");
			FieldFactory.saveField( urlTitle );

			// WYSIWYG
			Field story = new Field();
			story.setFieldName("Story");
			story.setFieldType(FieldType.WYSIWYG.toString());
			story.setRequired(true);
			story.setStructureInode(testSt.getInode());
			story.setType("field");
			story.setValues("");
			story.setVelocityVarName("story");
			story.setFieldContentlet("text_area3");
			FieldFactory.saveField( story );

			// ENGLISH CONTENT
			Contentlet englishContent = new Contentlet();
			englishContent.setReviewInterval( "1m" );
			englishContent.setStructureInode( testSt.getInode() );
			englishContent.setHost( demoHost.getIdentifier() );
			englishContent.setLanguageId(1);


			contentletAPI.setContentletProperty( englishContent, headline, "the-gas-price" );
			contentletAPI.setContentletProperty( englishContent, story, "the-gas-price" );
			contentletAPI.setContentletProperty( englishContent, urlTitle, "the-gas-price" );
			englishContent.setIndexPolicy(IndexPolicy.FORCE);
            englishContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
			englishContent.setBoolProperty(Contentlet.IS_TEST_MODE, true);
			englishContent = contentletAPI.checkin( englishContent, null, permissionAPI.getPermissions( testSt ), user, false );
			englishContent.setIndexPolicy(IndexPolicy.FORCE);
            englishContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
			englishContent.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.publish(englishContent, user, false);
			APILocator.getVersionableAPI().setLive(englishContent);

			// SPANISH CONTENT
			Contentlet spanishContent = new Contentlet();
			spanishContent.setReviewInterval("1m");
			spanishContent.setStructureInode(testSt.getInode());
			spanishContent.setHost(demoHost.getIdentifier());
			spanishContent.setLanguageId(2);
			spanishContent.setIdentifier(englishContent.getIdentifier());

			contentletAPI.setContentletProperty( spanishContent, headline, "el-precio-del-gas" );
			contentletAPI.setContentletProperty( spanishContent, story, "el-precio-del-gas" );
			contentletAPI.setContentletProperty( spanishContent, urlTitle, "el-precio-del-gas" );
			spanishContent.setIndexPolicy(IndexPolicy.FORCE);
            spanishContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
			spanishContent.setBoolProperty(Contentlet.IS_TEST_MODE, true);

			spanishContent = contentletAPI.checkin( spanishContent, null, permissionAPI.getPermissions( testSt ), user, false );
			spanishContent.setIndexPolicy(IndexPolicy.FORCE);
            spanishContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
			spanishContent.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.publish(spanishContent, user, false);
			APILocator.getVersionableAPI().setLive(spanishContent);

			HibernateUtil.closeAndCommitTransaction();

			if(!(contentletAPI.isInodeIndexed(englishContent.getInode(), true) &&
							contentletAPI.isInodeIndexed(spanishContent.getInode(), true) &&
								contentletAPI.isInodeIndexed(widget.getInode(), true))) {
				fail("Content indexing timeout.");

			}

		} catch (Exception e) {
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(URLMapTest.class,e.getMessage(),e1);
			}
			Logger.error(URLMapTest.class,e.getMessage(),e);
			throw e;
		}
	}


	@Test
	public void testURLMaps() throws Exception {
		
		HttpServletRequest request = ServletTestRunner.localRequest.get();
		String serverName = request.getServerName();
		Integer serverPort = request.getServerPort();

		URL urlE = new URL("http://"+serverName+":"+serverPort+"/newstest"+salt+"/the-gas-price/");
		assertTrue(IOUtils.toString(urlE.openStream()).contains("the-gas-price"));

		try {
		    URL urlS = new URL("http://"+serverName+":"+serverPort+"/newstest"+salt+"/el-precio-del-gas/");
		    urlS.openStream();
		    
		    Assert.fail(); // the previus line should throw an exception
		}
		catch(FileNotFoundException ex) {
		    // fine
		}
		
		// for spanish it should load of language_id=2
		URL urlS = new URL("http://"+serverName+":"+serverPort+"/newstest"+salt+"/el-precio-del-gas?language_id=2");
		assertTrue(IOUtils.toString(urlS.openStream()).contains("el-precio-del-gas"));
	}

	@After
	public void deleteAssets() throws Exception {
       try{
        	HibernateUtil.startTransaction();

        	if(testFolder!=null) {

        		APILocator.getFolderAPI().delete(testFolder, user, false);
			}

        	if(testSt!=null) {

        		APILocator.getStructureAPI().delete(testSt, user);
			}

        	if(widget!=null){

				widget.getContentType(); // preloa the contenttype
				APILocator.getContentletAPI().archive(widget, user, false);
        		APILocator.getContentletAPI().delete(widget, user, false);
			}

        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(URLMapTest.class, e.getMessage());
        }

	}


}

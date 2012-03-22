package com.dotmarketing.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
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
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.XMLUtils;
import com.liferay.portal.model.User;

public class SlideShowServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
		java.io.File dir = new java.io.File(Config.CONTEXT.getRealPath("/WEB-INF/velocity/static/xspf/"));
		dir.mkdirs();
	}

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {

		response.setContentType("text/xml");
		PrintWriter out = response.getWriter();
		StringWriter sw = new StringWriter();

		try {
		
			String title = request.getParameter("slideShowTitle");
	
			if (title == null) {
				return;
			}
			
			Structure slideShowSt = StructureCache.getStructureByVelocityVarName("slideShow");
			List<Field> fields = FieldsCache.getFieldsByStructureVariableName("slideShow");
			Field slideShowTitleF = null;
			Field slideShowCreditsF = null;
			Field slideShowAudioTitleF = null;
			Field slideShowAudioFileF = null;
			
			if (slideShowSt == null ||(!InodeUtils.isSet(slideShowSt.getInode())) ) {
				slideShowSt = new Structure ();
				slideShowSt.setDefaultStructure(false);
				slideShowSt.setName("Slide Show");
				slideShowSt.setDescription("Slide Show Base Structure");
				slideShowSt.setFixed(true);
				slideShowSt.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
				//Create the structure
				StructureFactory.saveStructure(slideShowSt);				
			} 
	
			if (fields != null)
				for (Field field : fields) {
					if (field.getFieldName().equals("Title")) {
						slideShowTitleF = field;
					} else if (field.getFieldName().equals("Credits")) {
						slideShowCreditsF = field;
					} else if (field.getFieldName().equals("Audio Title")) {
						slideShowAudioTitleF = field;
					} else if (field.getFieldName().equals("MP3 File")) {
						slideShowAudioFileF = field;
					}
				}
			
			boolean fieldAdded = false;
			if (slideShowTitleF == null) {
				//Creating the field
				slideShowTitleF = new Field("Title", FieldType.TEXT, DataType.TEXT, slideShowSt, true, true, true, 1, true, false,true);
				FieldFactory.saveField(slideShowTitleF);
				fieldAdded = true;
			} 
			if (slideShowCreditsF == null) {
				//Creating the credits field
				slideShowCreditsF = new Field("Credits", FieldType.TEXT_AREA, DataType.LONG_TEXT, slideShowSt, false, false, true, 2, true, false, true);
				FieldFactory.saveField(slideShowCreditsF);
				fieldAdded = true;
			} 
			if (slideShowAudioTitleF == null) {
				//Create the audio title field
				slideShowAudioTitleF = new Field("Audio Title", FieldType.TEXT, DataType.TEXT, slideShowSt, false, false, true, 3, true, false, true);
				FieldFactory.saveField(slideShowAudioTitleF);
				fieldAdded = true;
			} 
			if (slideShowAudioFileF == null) {
				//Create the audio file
				slideShowAudioFileF = new Field("MP3 File", FieldType.FILE, DataType.INTEGER, slideShowSt, false, false, false, 4, true, false, true);
				FieldFactory.saveField(slideShowAudioFileF);
				fieldAdded = true;
			}
			if (fieldAdded) {
				FieldsCache.removeFields(slideShowSt);
				StructureCache.removeStructure(slideShowSt);
				StructureServices.removeStructureFile(slideShowSt);
				StructureFactory.saveStructure(slideShowSt);
			}
			
			
			Structure slideSt = StructureCache.getStructureByVelocityVarName("slideImage");
			fields = FieldsCache.getFieldsByStructureVariableName("slideImage");
			Field slideTitleF = null;
			Field slideImageF = null;
			Field slideTextBodyF = null;
			Field slideTimingF = null;
	
			if (slideSt == null || (!InodeUtils.isSet(slideSt.getInode()))) {
				//Create the structure
				slideSt = new Structure ();
				slideSt.setDefaultStructure(false);
				slideSt.setName("Slide Image");
				slideSt.setVelocityVarName("slideImage");
				slideSt.setDescription("Slide Image");
				slideSt.setFixed(true);
				slideSt.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
				//Create the structure
				StructureFactory.saveStructure(slideSt);
			}
	
			if (fields != null)
				for (Field field : fields) {
					if (field.getFieldName().equals("Title")) {
						slideTitleF = field;
					} else if (field.getFieldName().equals("Image")) {
						slideImageF = field;
					} else if (field.getFieldName().equals("Text Body")) {
						slideTextBodyF = field; 
					} else if (field.getFieldName().equals("Timing")) {
						slideTimingF = field;
					}
				}
			
			fieldAdded = false;
			if (slideTitleF == null) {
				//Creating the field
				slideTitleF = new Field("Title", FieldType.TEXT, DataType.TEXT, slideSt, true, true, true, 1, true, false, true);
				FieldFactory.saveField(slideTitleF);
				fieldAdded = true;
			} 
			if (slideImageF == null) {
				//Creating the field
				slideImageF = new Field("Image", FieldType.IMAGE, DataType.INTEGER, slideSt, true, true, true, 2, true, false, true);
				FieldFactory.saveField(slideImageF);
				fieldAdded = true;
			} 
			if (slideTextBodyF == null) {
				//Creating the field
				slideTextBodyF = new Field("Text Body", FieldType.TEXT_AREA, DataType.LONG_TEXT, slideSt, true, true, true, 3, true, false, true);
				FieldFactory.saveField(slideTextBodyF);
				fieldAdded = true;
			} 
			if (slideTimingF == null) { 
				//Creating the field
				slideTimingF = new Field("Timing", FieldType.TEXT, DataType.INTEGER, slideSt, true, true, true, 4, "", "10", "", true, false, true);
				FieldFactory.saveField(slideTimingF);
				fieldAdded = true;
			}
			if (fieldAdded) {
				FieldsCache.removeFields(slideSt);
				StructureCache.removeStructure(slideSt);
				StructureServices.removeStructureFile(slideSt);
				StructureFactory.saveStructure(slideSt);
			}

			
			Relationship relationship = RelationshipFactory.getRelationshipByRelationTypeValue("Slide_Show-Slide_Image");
			if (!InodeUtils.isSet(relationship.getInode())) {
				//create the relationship
				relationship = new Relationship (slideShowSt, slideSt, "Slide Show", "Slide Image", com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal(), false, false);
				RelationshipFactory.saveRelationship(relationship);
			}
			
			ContentletAPI conAPI = APILocator.getContentletAPI();
			StringBuffer lqBuffy = new StringBuffer();
			lqBuffy.append("+structureInode:" + slideShowSt.getInode() + " +type:content +live:true +deleted:false +" + slideShowTitleF.getFieldContentlet().trim() + ":\"" + title.toLowerCase() + "\"");
			User user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);
			List<Contentlet> results = conAPI.search(lqBuffy.toString(), 0, -1, "inode",user , true);
			
//			List<Contentlet> results = ContentletFactory.getContentletByCondition("live = " + DbConnectionFactory.getDBTrue() +
//					" and deleted = " + DbConnectionFactory.getDBFalse() + " and " + 
//					slideShowTitleF.getFieldContentlet() + " = '" + UtilMethods.sqlify(title) + "'");
	
			if (results.size() == 0) {
				return;
			}
			
			Contentlet slideShow = results.get(0);
			String slideShowTitle = (String)conAPI.getFieldValue(slideShow, slideShowTitleF);
			String slideShowCredits = (String)conAPI.getFieldValue(slideShow, slideShowCreditsF);
			String slideShowAudioTitle = (String)conAPI.getFieldValue(slideShow, slideShowAudioTitleF);
			String slideShowAudioFileInode = (String)conAPI.getFieldValue(slideShow, slideShowAudioFileF);
			File slideShowFile = (File) InodeFactory.getInode(slideShowAudioFileInode, File.class);
			
			List<Contentlet> slideImages = RelationshipFactory.getAllRelationshipRecords(relationship, slideShow, true, true);
			
			sw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

			sw.write("<slidesInfo>\n");
			sw.write("	<baseInfo>\n");
			sw.write("		<intTotalImg>" + slideImages.size() + "</intTotalImg>\n");
			sw.write("		<strTitle>" + XMLUtils.xmlEscape(slideShowTitle) + "</strTitle>\n");
			sw.write("		<strCredits>" + XMLUtils.xmlEscape(slideShowCredits) + "</strCredits>\n");
			sw.write("		<strAudio>" + XMLUtils.xmlEscape(slideShowAudioTitle) + "</strAudio>\n");
			sw.write("	</baseInfo>\n");
			sw.write("	<strSound>\n");
			sw.write("		<strPathSound>" + (slideShowFile.getURI() == null || slideShowFile.getURI().equals("")?"":slideShowFile.getURI()) + "</strPathSound>\n");
			sw.write("	</strSound>\n");

			StringBuffer imagesPathsXML = new StringBuffer ();
			StringBuffer imagesTimingXML = new StringBuffer ();
			StringBuffer imagesTextXML = new StringBuffer ();
			for (Contentlet slide : slideImages) {
				// only show live slides
				if (!slide.isLive()) {
					continue;
				}

				//String slideTitle = (String)ContentletFactory.getFieldValue(slide, slideTitleF);
				String slideTextBody = (String)conAPI.getFieldValue(slide, slideTextBodyF);
				long slideTiming = (Long)conAPI.getFieldValue(slide, slideTimingF);
				String slideImageInode = (String)conAPI.getFieldValue(slide, slideImageF);
				//File slideImage = (File) InodeFactory.getInode(slideImageInode, File.class);
				//Identifier slideImageIdentifier = (Identifier) InodeFactory.getInode(slideImageInode, Identifier.class);
				Identifier slideImageIdentifier = APILocator.getIdentifierAPI().find(slideImageInode);
				File slideImage = (File)APILocator.getVersionableAPI().findWorkingVersion(slideImageIdentifier, user, true);

				imagesPathsXML.append("		<strPath>" + (slideImage.getURI() == null || slideImage.getURI().equals("")?"":slideImage.getURI()) + "</strPath>\n");
				imagesTimingXML.append("		<imgInterval>" + slideTiming + "</imgInterval>\n");
				imagesTextXML.append("		<strOver>" + XMLUtils.xmlEscape(slideTextBody) + "</strOver>\n");
			}

			sw.write("	<strImages>\n");
			sw.write(imagesPathsXML.toString());
			sw.write("	</strImages>\n");

			sw.write("	<strInterval>\n");
			sw.write(imagesTimingXML.toString());
			sw.write("	</strInterval>\n");

			sw.write("	<strOverion>\n");
			sw.write(imagesTextXML.toString());
			sw.write("	</strOverion>\n");

			sw.write("	<strComplete>\n");
			sw.write("		<blnComplete>true</blnComplete>\n");
			sw.write("	</strComplete>\n");
			sw.write("</slidesInfo>\n");
		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		} finally {
			out.print(sw.toString());
			out.close();
			sw.close();
			try {
				HibernateUtil.commitTransaction();
			} catch (DotHibernateException e) {
				Logger.error(SlideShowServlet.class, e.getMessage(), e);
			}
		}
	}
	
	

}

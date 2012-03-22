package com.dotmarketing.quartz.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.lucene.queryParser.ParseException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.EmailUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This job will queries a POP account for email and creates content.
 * @author BayLogic
 * @since 
 * http://jira.dotmarketing.net/browse/DOTCMS-6298
 */
public class ContentFromEmailJob implements Job {

	public ContentFromEmailJob() {
		
	}
	
	public void execute(JobExecutionContext ctx) throws JobExecutionException {

		ContentletAPI conAPI = APILocator.getContentletAPI();
		FieldAPI fieldAPI = APILocator.getFieldAPI();
		PermissionAPI perAPI = APILocator.getPermissionAPI();
	
		List<Structure> allStructures = StructureFactory.getStructures();		
		for(Structure structure:allStructures){
			
			boolean isCongigured = false;
			String host = "";
			String username = "";
			String password = "";
			int port = 0;
			boolean isSSL = true;

			try{
				host = Config.getStringProperty("pop."+structure.getVelocityVarName()+".host");
				username = Config.getStringProperty("pop."+structure.getVelocityVarName()+".login");
				password = Config.getStringProperty("pop."+structure.getVelocityVarName()+".password");
				port = Config.getIntProperty("pop."+structure.getVelocityVarName()+".port");
				isSSL  = Config.getBooleanProperty("pop."+structure.getVelocityVarName()+".ssl");
			}catch (Exception e) {
				
			}

			if(UtilMethods.isSet(host)
					&& UtilMethods.isSet(port)
					&& UtilMethods.isSet(isSSL)
					&& UtilMethods.isSet(username)
					&& UtilMethods.isSet(password)){
				isCongigured = true;
			}

			if(isCongigured){
				
				String messageIdFieldVarNm = "";
				String subjectFieldVarNm = "";
				String bodyFieldVarNm = "";
				String fromFieldVarNm = "";
				String toFieldVarNm = "";
				String ccFieldVarNm = "";
				String bccFieldVarNm = "";
				String dateFieldVarNm = "";
				String referencesFieldVarNm = "";
				String inReplyToFieldVarNm = "";
				String attachment1FieldVarNm = "";
				String attachment2FieldVarNm = "";
				String attachment3FieldVarNm = "";
				String attachment4FieldVarNm = "";
				String attachment5FieldVarNm = "";
				String attachment6FieldVarNm = "";
				String attachment7FieldVarNm = "";
				String attachment8FieldVarNm = "";
				String attachment9FieldVarNm = "";
				String attachment10FieldVarNm = "";
				
				try{
					messageIdFieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".messageId");
					subjectFieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".subject");
					bodyFieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".body");
					fromFieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".from");
					toFieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".to");
					ccFieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".cc");
					bccFieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".bcc");
					dateFieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".date");
					referencesFieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".references");
					inReplyToFieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".inReplyTo");
					attachment1FieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".attachment1");
					attachment2FieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".attachment2");
					attachment3FieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".attachment3");
					attachment4FieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".attachment4");
					attachment5FieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".attachment5");
					attachment6FieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".attachment6");
					attachment7FieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".attachment7");
					attachment8FieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".attachment8");
					attachment9FieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".attachment9");
					attachment10FieldVarNm = Config.getStringProperty("pop."+structure.getVelocityVarName()+".attachment10");
				}catch (Exception e) {
					
				}

				List<Field> fields = FieldsCache.getFieldsByStructureVariableName(structure.getVelocityVarName());

				ContentletRelationships contRel;
				ArrayList<Category> cats;
				User systemUser = null;
				try {
					systemUser = APILocator.getUserAPI().getSystemUser();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage());
				}

				try {
					List<Map<String,Object>> emails = EmailUtils.getEmails(host, port, isSSL, username, password);

					for(Map<String,Object> email:emails){
						Contentlet contentlet = new Contentlet();
						contentlet.setStructureInode(structure.getInode());

						for (Field field : fields){
							if(fieldAPI.isElementConstant(field)){
								continue;
							}
							Object value = null;
							// using the standard headers/names from javax.mail.* to retrieve the email headers and field's values below 
							if(UtilMethods.isSet(messageIdFieldVarNm) && field.getVelocityVarName().equals(messageIdFieldVarNm)){
								if(email.get("Message-ID") != null)
									value = email.get("Message-ID");
							}else if(UtilMethods.isSet(subjectFieldVarNm) && field.getVelocityVarName().equals(subjectFieldVarNm)){
								if(email.get("Subject") != null)
									value = email.get("Subject");
							}else if(UtilMethods.isSet(bodyFieldVarNm) && field.getVelocityVarName().equals(bodyFieldVarNm)){
								if(email.get("Body") != null)
									value = email.get("Body");
							}else if(UtilMethods.isSet(fromFieldVarNm) && field.getVelocityVarName().equals(fromFieldVarNm)){
								if(email.get("From") != null)
									value = email.get("From");
							} else if(UtilMethods.isSet(toFieldVarNm) && field.getVelocityVarName().equals(toFieldVarNm)){
								if(email.get("To") != null)
									value = email.get("To");
							}else if(UtilMethods.isSet(ccFieldVarNm) && field.getVelocityVarName().equals(ccFieldVarNm)){
								if(email.get("Cc") != null)
									value = email.get("Cc");
							}else if(UtilMethods.isSet(bccFieldVarNm) && field.getVelocityVarName().equals(bccFieldVarNm)){
								if(email.get("Bcc") != null)
									value = email.get("Bcc");
							}else if(UtilMethods.isSet(dateFieldVarNm) && field.getVelocityVarName().equals(dateFieldVarNm)){
								if(email.get("Date") != null)
									value = email.get("Date");
							}else if(UtilMethods.isSet(referencesFieldVarNm) && field.getVelocityVarName().equals(referencesFieldVarNm)){
								if(email.get("References") != null)
									value = email.get("References");
							}else if(UtilMethods.isSet(inReplyToFieldVarNm) && field.getVelocityVarName().equals(inReplyToFieldVarNm)){
								if(email.get("In-Reply-To") != null)
									value = email.get("In-Reply-To");
							}else if(UtilMethods.isSet(attachment1FieldVarNm) && field.getVelocityVarName().equals(attachment1FieldVarNm)){
								if(email.get("attachment1") != null)
									value = email.get("attachment1");
							}else if(UtilMethods.isSet(attachment2FieldVarNm) && field.getVelocityVarName().equals(attachment2FieldVarNm)){
								if(email.get("attachment2") != null)
									value = email.get("attachment2");
							}else if(UtilMethods.isSet(attachment3FieldVarNm) && field.getVelocityVarName().equals(attachment3FieldVarNm)){
								if(email.get("attachment3") != null)
									value = email.get("attachment3");
							}else if(UtilMethods.isSet(attachment4FieldVarNm) && field.getVelocityVarName().equals(attachment4FieldVarNm)){
								if(email.get("attachment4") != null)
									value = email.get("attachment4");
							}else if(UtilMethods.isSet(attachment5FieldVarNm) && field.getVelocityVarName().equals(attachment5FieldVarNm)){
								if(email.get("attachment5") != null)
									value = email.get("attachment5");
							}else if(UtilMethods.isSet(attachment6FieldVarNm) && field.getVelocityVarName().equals(attachment6FieldVarNm)){
								if(email.get("attachment6") != null)
									value = email.get("attachment6");
							}else if(UtilMethods.isSet(attachment7FieldVarNm) && field.getVelocityVarName().equals(attachment7FieldVarNm)){
								if(email.get("attachment7") != null)
									value = email.get("attachment7");
							}else if(UtilMethods.isSet(attachment8FieldVarNm) && field.getVelocityVarName().equals(attachment8FieldVarNm)){
								if(email.get("attachment8") != null)
									value = email.get("attachment8");
							}else if(UtilMethods.isSet(attachment9FieldVarNm) && field.getVelocityVarName().equals(attachment9FieldVarNm)){
								if(email.get("attachment9") != null)
									value = email.get("attachment9");
							}else if(UtilMethods.isSet(attachment10FieldVarNm) && field.getVelocityVarName().equals(attachment10FieldVarNm)){
								if(email.get("attachment10") != null)
									value = email.get("attachment10");
							}

							String typeField = field.getFieldType();

							/* Validate if the field is read only, if so then check to see if it's a new contentlet
							 * and set the structure field default value, otherwise do not set the new value.
							 */
							if (!typeField.equals(Field.FieldType.HIDDEN.toString()) && 
									!typeField.equals(Field.FieldType.IMAGE.toString()) && 
									!typeField.equals(Field.FieldType.FILE.toString())) 
							{
								if(field.isReadOnly() && !InodeUtils.isSet(contentlet.getInode()))
									value = field.getDefaultValue();
								if (field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
									//WYSIWYG workaround because the WYSIWYG includes a <br> even if the field was left blank by the user
									//we have to check the value to leave it blank in that case.
									if (value instanceof String && ((String)value).trim().toLowerCase().equals("<br>")) {
										value = "";
									}
								}
							}
							if ((value != null || field.getFieldType().equals(Field.FieldType.BINARY.toString())) && APILocator.getFieldAPI().valueSettable(field) 
									&& !field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString()))
								try{
									conAPI.setContentletProperty(contentlet, field, value);
								}catch (Exception e) {
									Logger.info(this, "Unable to set field " + field.getFieldName() + " to value " + value);
									Logger.debug(this, "Unable to set field " + field.getFieldName() + " to value " + value, e);
								}
						}

						contRel = retrieveRelationshipsData(contentlet,systemUser,email);

						cats = new ArrayList<Category>();

						Contentlet con = conAPI.checkin(contentlet, contRel,
								cats, perAPI.getPermissions(contentlet, false, true), systemUser, false);
						
						// When a large number of new mails with a number of replies come in.
						// To relate the content based on lucene query, the new content created must be indexed.
						conAPI.isInodeIndexed(con.getInode());
					}
				} catch (MessagingException e) {
					Logger.error(this, e.getMessage());
				} catch (IOException e) {
					Logger.error(this, e.getMessage());
				} catch (DotContentletValidationException e) {
					Logger.error(this, e.getMessage());
				} catch (DotContentletStateException e) {
					Logger.error(this, e.getMessage());
				} catch (IllegalArgumentException e) {
					Logger.error(this, e.getMessage());
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage());
				} catch (DotSecurityException e) {
					Logger.error(this, e.getMessage());
				}
			}
		}
	}

	private ContentletRelationships retrieveRelationshipsData(Contentlet contentlet, User systemUser,Map<String,Object> email) {

		Structure contentStructure = contentlet.getStructure();
		String relName = Config.getStringProperty("pop."+contentStructure.getVelocityVarName()+".relationship.name");

		if(!UtilMethods.isSet(relName))
			return new ContentletRelationships(contentlet);

		Relationship relationship = new Relationship();
		relationship = RelationshipFactory.getRelationshipByRelationTypeValue(relName);
		if(relationship == null)
			return new ContentletRelationships(contentlet);

		ContentletRelationships relationshipsData = new ContentletRelationships(contentlet);
		List<ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationshipRecords> ();
		relationshipsData.setRelationshipsRecords(relationshipsRecords);

		Structure parentStr = relationship.getParentStructure();
		Structure childStr = relationship.getChildStructure();

		if(contentStructure.equals(childStr)){

			String messageIdFieldVarNm = Config.getStringProperty("pop."+contentStructure.getVelocityVarName()+".relationship.parent.messageId");

			if(!UtilMethods.isSet(messageIdFieldVarNm) && parentStr.equals(childStr)){
				messageIdFieldVarNm = Config.getStringProperty("pop."+contentStructure.getVelocityVarName()+".messageId");
			}

			if(!UtilMethods.isSet(messageIdFieldVarNm))
				return new ContentletRelationships(contentlet);

			List<Contentlet> searchResults = new ArrayList<Contentlet>();
			String luceneQuery = "";			
			String ref = (String)email.get("References");
			String[] references = parseReferences(ref);
			
			// When relating between two different structures. Original message does not have any references. 
			if(references.length == 0){
				
				String messageId = (String) email.get("Message-ID");
				luceneQuery = "+structureName:"+ parentStr.getVelocityVarName() 
				+ " +" + parentStr.getVelocityVarName() + "." + messageIdFieldVarNm + ":*" + messageId + "*";

				try {
					searchResults = APILocator.getContentletAPI().search(luceneQuery, -1, 0, "modDate desc", systemUser, false);
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage());
				} catch (DotSecurityException e) {
					Logger.error(this, e.getMessage());
				} catch (Exception e) {
					Logger.error(this, e.getMessage());
				}				
			}
				
			
			for (int i = 0; i < references.length; i++) {
				String reference = references[i];

				luceneQuery = "+structureName:"+ parentStr.getVelocityVarName() 
				+ " +" + parentStr.getVelocityVarName() + "." + messageIdFieldVarNm + ":*" + reference + "*";

				try {
					searchResults = APILocator.getContentletAPI().search(luceneQuery, -1, 0, "modDate desc", systemUser, false);
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage());
				} catch (DotSecurityException e) {
					Logger.error(this, e.getMessage());
				} catch (Exception e) {
					Logger.error(this, e.getMessage());
				}
				if(searchResults.size() > 0)
					break;
			}

			boolean hasParent = false;

			ContentletRelationshipRecords records = relationshipsData.new ContentletRelationshipRecords(relationship, hasParent);
			records.setRecords(searchResults);
			relationshipsRecords.add(records);			
		}		
		return relationshipsData;

	}

	private static String[] parseReferences(String ref) {
		
		if(!UtilMethods.isSet(ref))
			return new String[]{};
		
		List<String> refList = new LinkedList<String>();
		char chr;
		StringBuffer strBfr = new StringBuffer();
		for (int i = 0; i < ref.length(); i++) {
			chr = ref.charAt(i);
			if(chr == '<'){
				strBfr.delete(0, strBfr.length());
				continue;
			}
			if(chr == '>'){				
				refList.add(strBfr.toString());
				continue;
			}
			strBfr.append(chr);
		}
		return refList.toArray(new String[]{});
	}
	
}
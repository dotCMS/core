package com.dotmarketing.quartz.job;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.rendering.velocity.services.ContentTypeLoader;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.DataType;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

public class UpdateRatingThread implements StatefulJob {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private CategoryAPI catAPI = APILocator.getCategoryAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

	private static final String  NUMBER_OF_VOTES_VAR_NAME ="numberOfVotes";
	private static final String  AVERAGE_RATING_VAR_NAME ="averageRating";
	public UpdateRatingThread() {
	}




	@SuppressWarnings("unchecked")
	@CloseDBIfOpened
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		Logger.debug(this, "Running Ratings Statistics");
		DotConnect dc = new DotConnect();
		// http://jira.dotmarketing.net/browse/DOTCMS-1418
		dc.setSQL("select content_rating.identifier as identifier, count(content_rating.identifier) as votes_number,  avg(content_rating.rating) as avg_rating  from content_rating group by content_rating.identifier");
		List<HashMap> l =null;
		try {
			l = dc.getResults();
		} catch (DotDataException e2) {
			Logger.error(this, e2.getMessage(), e2);
		}
		Structure struct;
		List<Field> fields;

		for (HashMap map : l) {

			float ctAvg = -1F;
			long ctNumberOfVotes = -1;
			Float dbAvg = 0f;
			Long dbNumberOfVotes = 0L;
			try {
				String iden = (String) map.get("identifier");
				Identifier ident = APILocator.getIdentifierAPI().find(iden);
				// http://jira.dotmarketing.net/browse/DOTCMS-1418
				if(!InodeUtils.isSet(ident.getInode())){
					dc.setSQL("delete from content_rating where identifier = ?");
					dc.addParam(iden);
					dc.getResult();
					continue;
				}
				Contentlet c = conAPI.findContentletByIdentifier(ident.getInode(), false, langAPI.getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);

				if(c.isArchived()){
					Logger.debug(this,"Cannot update ratings on archived content.  Continuing");
					continue;
				}
				if(c.isLocked()){
					Logger.debug(this,"Cannot update ratings on locked content.  Continuing");
					continue;
				}
				dbAvg = Float.parseFloat((String) map.get("avg_rating"));

				dbNumberOfVotes = Long.parseLong((String) map.get("votes_number"));
				Structure s = c.getStructure();
				if(s == null || !InodeUtils.isSet(s.getInode())){
					continue;
				}
				Field avgField = s.getFieldVar(AVERAGE_RATING_VAR_NAME);
				Field numOfVotesField = s.getFieldVar(NUMBER_OF_VOTES_VAR_NAME);
				if( avgField == null){
					avgField = s.getField("Average Rating");
				}
				if(numOfVotesField ==null){
					numOfVotesField = s.getField("Number Of Votes");
				}
				Float ctAvgFloat = new Float(0);
				Long ctNumberOfVotesObj = null;

				if(avgField != null && c != null){
					try{
						Object ctAvgObj = conAPI.getFieldValue(c, avgField);

						if(ctAvgObj instanceof String) {
							ctAvgFloat = Float.valueOf(((String) ctAvgObj));
						} else {
							ctAvgFloat =  (Float) ctAvgObj;
						}
					}
					catch(Exception e){
						Logger.error(UpdateRatingThread.class, e.getMessage(), e);
					}
				}
				if(numOfVotesField != null && c != null){
					ctNumberOfVotesObj = (Long) conAPI.getFieldValue(c, numOfVotesField);
				}
				if (UtilMethods.isSet(ctAvgFloat) && UtilMethods.isSet(ctNumberOfVotesObj)) {
					ctAvg = ctAvgFloat.floatValue();
					ctNumberOfVotes = ctNumberOfVotesObj.longValue();
					if(ctNumberOfVotes == dbNumberOfVotes){
						continue;
					}

				}
				else {

					struct = CacheLocator.getContentTypeCache().getStructureByInode(c.getStructureInode());
					fields = FieldsCache.getFieldsByStructureInode(struct.getInode());

					Field avfield = null;
					Field countfield = null;
					int fieldsSize = fields.size();
					for (int i = 0; i < fieldsSize; ++i) {
						if (fields.get(i).getFieldName().trim().equalsIgnoreCase("Average Rating"))
							avfield = fields.get(i);
						if (fields.get(i).getFieldName().trim().equalsIgnoreCase("Number Of Votes"))
							countfield = fields.get(i);
					}
					if (avfield ==null || countfield == null) {
						fields = FieldFactory.getFieldsByStructure(struct.getInode());
						fieldsSize = fields.size();
						for (int i = 0; i < fieldsSize; ++i) {
							if (fields.get(i).getFieldName().trim().equalsIgnoreCase("Average Rating"))
								avfield = fields.get(i);
							if (fields.get(i).getFieldName().trim().equalsIgnoreCase("Number Of Votes"))
								countfield = fields.get(i);
						}
					}

					if (avfield == null) {
						Field averageRatingField = new Field("Average Rating", FieldType.TEXT, DataType.FLOAT, struct, false, false, true, ++fieldsSize, true, true, false);
						averageRatingField.setVelocityVarName(AVERAGE_RATING_VAR_NAME);
						averageRatingField.setReadOnly(false);
						averageRatingField.setListed(false);
						averageRatingField.setSearchable(false);
						averageRatingField.setIndexed(true);
						try{
						averageRatingField.setDefaultValue(Integer.toString(Config.getIntProperty("RATING_MAX_VALUE", 10)));
						}
						catch(Exception e){
							Logger.error(this.getClass(), "unable to set default value for rating field:" + e);
						}
						FieldFactory.saveField(averageRatingField);
						FieldsCache.removeFields(struct);
						CacheLocator.getContentTypeCache().remove(struct);
						StructureFactory.saveStructure(struct);
					}

					if (countfield == null) {
						Field numberOfVotesField = new Field("Number Of Votes", FieldType.TEXT, DataType.INTEGER, struct, false, false, true, ++fieldsSize, true, true, false);
						numberOfVotesField.setVelocityVarName(NUMBER_OF_VOTES_VAR_NAME);
						numberOfVotesField.setReadOnly(false);
						numberOfVotesField.setListed(false);
						numberOfVotesField.setSearchable(false);
						numberOfVotesField.setIndexed(true);
						FieldFactory.saveField(numberOfVotesField);

						FieldsCache.removeFields(struct);
						CacheLocator.getContentTypeCache().remove(struct);
						new ContentTypeLoader().invalidate(struct);
						StructureFactory.saveStructure(struct);
					}

				}

				DecimalFormat df = new DecimalFormat("#.00");


				if ((!df.format(ctAvg).equals(df.format(dbAvg)) || ctNumberOfVotes != dbNumberOfVotes)) {
					c.setProperty(avgField.getVelocityVarName(), dbAvg);
					c.setProperty(numOfVotesField.getVelocityVarName(), dbNumberOfVotes);

					User user = APILocator.getUserAPI().getSystemUser();
					List<Category> cats = catAPI.getParents(c, user, true);
					ContentletRelationships contentRelationships = conAPI.getAllRelationships(c);

                    conAPI.checkinWithoutVersioning(c, contentRelationships, cats,
                            APILocator.getPermissionAPI().getPermissions(c), user, true);
					HibernateUtil.closeAndCommitTransaction();
				}


			} catch (DotContentletStateException e) {
				Logger.warn(UpdateRatingThread.class,e.getMessage(), e);
				if (e.getMessage().equals("No contenlet found for given identifier")) {
					dc.setSQL("delete from content_rating where identifier = ?");
					dc.addParam((String) map.get("identifier"));
					dc.getResult();
					try {
						HibernateUtil.closeAndCommitTransaction();
					} catch (DotHibernateException e1) {
						Logger.error(this, e.getMessage(), e);
					}
				}
			} catch (Exception e) {
				Logger.warn(UpdateRatingThread.class,e.getMessage(), e);
			}

		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Thread#destroy()
	 */
	public void destroy() {
		try {
			HibernateUtil.closeSession();
		} catch (DotHibernateException e) {
			Logger.error(this, e.getMessage(), e);
		}
	}
}
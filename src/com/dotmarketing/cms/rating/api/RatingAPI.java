package com.dotmarketing.cms.rating.api;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Rating;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.RatingCache;
import com.dotmarketing.factories.RatingsFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class RatingAPI {

	/**
	 * Returns the rating object of an specific user how rated the given contentlet
	 * it returns an empty Rating object if the user hasn't rated the contentlet
	 *
	 * @param longLiveCookie
	 * @param identifier
	 * @return
	 */
	/*public static Rating getRating(String longLiveCookie, String identifier) {
		try {
			return getRating(longLiveCookie, Long.parseLong(identifier));
		} catch (Exception e) {
			return null;
		}
	}*/

	/**
	 * Returns the rating object of an specific user how rated the given contentlet
	 * it returns an empty Rating object if the user hasn't rated the contentlet
	 *
	 * @param longLiveCookie
	 * @param identifier
	 * @return
	 */
	public static Rating getRating(String longLiveCookie, String identifier) {

		/* get rating from cache */
		Rating rating = (Rating) RatingCache.getRatingFromCache(identifier, longLiveCookie);
		return rating;

	}


	/**
	 * Returns the average rating calculated periodically by a recurrent task an set in the content as a variable
	 * @param identifier
	 * @return
	 */
	public static float getAverageRating(String identifier) {
		float average = 0F;

		List<Contentlet> hits = new ArrayList <Contentlet>();
		String query = "+type:content +deleted:false +(+languageId:1* +identifier:" + identifier + "* +live:true)";
		try {
			ContentletAPI conAPI = APILocator.getContentletAPI();

		    hits = conAPI.search(query,  -1, 0, null, APILocator.getUserAPI().getSystemUser(), false);

			if (0 < hits.size()) {
				Contentlet cont = hits.get(0);
				String structInode = (String) cont.getMap().get("stInode");
				List<Field> fields = FieldsCache.getFieldsByStructureInode(structInode);
				Field field = null;
				for(Field f : fields) {
		            if("averageRating".equals(f.getVelocityVarName())){
		                field = f;
		                break;
		            }
		        }
				if(field == null || (!InodeUtils.isSet(field.getInode())) ){
				    return 0;
				}
				String avg = cont.getMap().get(field.getVelocityVarName()).toString();

				try {
					average = Float.parseFloat(avg);
				} catch (Exception e) {
					average = 0F;
				}

			}
		} catch (Exception ex) {
			Logger.error(RatingAPI.class, "getAverageRating: Error Searching Contentlets - lucene query: " + query, ex);
		}

		return average;
	}

	/**
	 * This method return if a content was alreaded rated by a user
	 * @param inode
	 * @return boolean
	 * @author Oswaldo Gallango
	 */
	/*public static boolean wasAlreadyRated(String identifier, String longLiveCookie){

		return wasAlreadyRated(Long.parseLong(identifier), longLiveCookie);

	}*/

	/**
	 * This method return if a content was already rated by a user (based on the user long lived cookie)
	 * @param inode
	 * @return boolean
	 * @author Oswaldo Gallango
	 */
	public static boolean wasAlreadyRated(String identifier, String longLiveCookie){
		Rating rt = RatingCache.getRatingFromCache(identifier, longLiveCookie);
		if (rt == null || !UtilMethods.isSet(rt.getId()))
			return false;
		else
			return true;
	}

	/**
	 * Get the number of vote that rate this content
	 * @param inode
	 * @return String
	 */
	public static String getRatingVotesNumber(String identifier) {
		String votesNumber = "0";

		List<Contentlet> hits = new ArrayList <Contentlet>();

		String query = "+type:content +deleted:false +(+languageId:1* +identifier:'" + identifier + "'* +live:true)";
		try {

			ContentletAPI conAPI = APILocator.getContentletAPI();

			hits = conAPI.search(query,  -1, 0, null, APILocator.getUserAPI().getSystemUser(), false);

			if (0 < hits.size()) {
				Contentlet cont  = hits.get(0);
				String structInode = (String) cont.getMap().get("stInode");

                List<Field> fields = FieldsCache.getFieldsByStructureVariableName(structInode);
                Field field = null;
                for(Field f : fields) {
                    if("averageRating".equals(f.getVelocityVarName())){
                        field = f;
                        break;
                    }
                }
                if(field == null || (!InodeUtils.isSet(field.getInode()))){
                    return "";
                }

				votesNumber = cont.getMap().get(field.getVelocityVarName()).toString();
			}
		} catch (Exception ex) {
			Logger.error(RatingAPI.class, "getRatingVotesNumber: Error Searching Contentlets - lucene query: " + query, ex);
		}

		return votesNumber;
	}

	/**
	 * Retun the Maximun Rating value could be set
	 * @return int
	 * @author Oswaldo Gallango
	 */
	public static int getMaxRatingValue(){
		return Config.getIntProperty("RATING_MAX_VALUE");
	}

	public static void saveRating(Rating rt) {
		RatingsFactory.saveRating(rt);
		RatingCache.removeRating(rt);
		RatingCache.addToRatingCache(rt);

	}
}
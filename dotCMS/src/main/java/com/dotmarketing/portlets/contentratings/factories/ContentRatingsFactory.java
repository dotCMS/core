package com.dotmarketing.portlets.contentratings.factories;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Rating;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;

public class ContentRatingsFactory {
    public static List<Structure> getContentRatingsStructures() {
    	List<Structure> list = null ;
        String query = "select {structure.*} from structure, field, inode as structure_1_ where field.field_name='Average Rating' and structure.inode=field.structure_inode and structure.inode=structure_1_.inode";
        HibernateUtil dh = new HibernateUtil (Structure.class);
        try {
			dh.setSQLQuery(query);
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ContentRatingsFactory.class, e.getMessage(), e);
		}
        return list;
    }
    
    public static List<Rating> getContentRatingsByStructure(String inode) {
    	List<Rating> result = null;
    	
    	try {
    		String query = "select {content_rating.*} from content_rating, contentlet,inode where content_rating.identifier = contentlet.identifier and inode.inode = contentlet.inode and structure_inode=?";
    		HibernateUtil dh = new HibernateUtil (Rating.class);
    		dh.setSQLQuery(query);
    		dh.setParam(inode);
    		result = dh.list();
    	} catch (Exception e) {
    		Logger.error(ContentRatingsFactory.class, "", e);
    		return null;
    	}
    	
    	return result;
    }
}
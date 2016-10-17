package com.dotmarketing.factories;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Trackback;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

/**
 * This Factory manage the search and saveing of trackback records
 * @author Oswaldo Gallango
 * @version 1.0
 * @since 1.5
 */
public class TrackbackFactory {

	/**
	 * Get Trackbak by id
	 * @param id
	 * @return Trackback
	 */
	public static Trackback getTrackBack(long id) {
		if(id ==0){
			return new Trackback();
		}
		Trackback tb = null ;
		try {
			HibernateUtil dh = new HibernateUtil();
			dh.setClass(Trackback.class);
			tb =(Trackback) dh.load(id);
		} catch (DotHibernateException e) {
			Logger.error(TrackbackFactory.class, "getTrackBack failed:" + e, e);
		}
		return tb;
	}
	
	/**
	 * Get a list of trackback by asset inode
	 * @param assetIdentifier
	 * @return List<Trackback>
	 */
	@SuppressWarnings("unchecked")
	public static List<Trackback> getTrackbacksByAssetId(String assetIdentifier) {
		if(assetIdentifier == null ){
			return new ArrayList<Trackback>();
		}
		List<Trackback> tb = null ;
		try {
			HibernateUtil dh = new HibernateUtil();
			dh.setClass(Trackback.class);
			dh.setQuery("from trackback in class com.dotmarketing.beans.Trackback where asset_identifier = '" + assetIdentifier + "'");
			tb = (List<Trackback>) dh.list();
		} catch (DotHibernateException e) {
			Logger.error(TrackbackFactory.class, "getTrackbacksByAssetId failed:" + e, e);
		}
		return tb;
	}
	
	/**
	 * Get a list of trackback by url
	 * @param url
	 * @return List<Trackback>
	 */
	@SuppressWarnings("unchecked")
	public static List<Trackback> getTrackbakByURL(String url) {
		List<Trackback> tb = null;
		try {
			HibernateUtil dh = new HibernateUtil();
			dh.setClass(Trackback.class);
			dh.setQuery("from trackback in class com.dotmarketing.beans.Trackback where url like '%"+ url+"%'");
			tb = (List<Trackback>) dh.list();
		} catch (DotHibernateException e) {
			Logger.error(TrackbackFactory.class, "getTrackbakByURL failed:" + e, e);
		}
		return tb;
	}
	
	/**
	 * Save or update the trackback object 
	 * @param tb
	 */
	public static void save(Trackback tb){
		try {
			HibernateUtil.saveOrUpdate(tb);
		} catch (Exception e) {
			Logger.error(TrackbackFactory.class, "save failed:" + e, e);
		}
	}
	
	
}

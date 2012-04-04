package com.dotcms.publishing.bundlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;

import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;

public class FileObjectBundler implements IBundler {

	private PublisherConfig config;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	User systemUser = null;
	
	@Override
	public void setConfig(PublisherConfig pc) {
		config = pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(FileObjectBundler.class,e.getMessage(),e);
		}
	}

	@Override
	public long generate(File bundleRoot) {
		List<ContentletSearch> cs = new ArrayList<ContentletSearch>();
		try {
			cs = conAPI.searchIndex("+structuretype:" + Structure.STRUCTURE_TYPE_FILEASSET + " ", 0, 0, "moddate", systemUser, true);
		} catch (ParseException e) {
			Logger.error(FileObjectBundler.class,e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(FileObjectBundler.class,e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(FileObjectBundler.class,e.getMessage(),e);
		}
		
		List<List<ContentletSearch>> listsOfCS = Lists.partition(cs, 500);
		for (List<ContentletSearch> l : listsOfCS) {
			List<String> inodes = new ArrayList<String>();
			for (ContentletSearch c : l) {
				inodes.add(c.getInode());
			}
			try {
				List<Contentlet> cons = conAPI.findContentlets(inodes);
			} catch (DotDataException e) {
				Logger.error(FileObjectBundler.class,e.getMessage(),e);
			} catch (DotSecurityException e) {
				Logger.error(FileObjectBundler.class,e.getMessage(),e);
			}
		}
		
		
//		config.
		return 0;
	}

}

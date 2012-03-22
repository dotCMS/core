/**
 * 
 */
package com.dotmarketing.quartz.job;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.ibm.icu.text.SimpleDateFormat;
import com.liferay.portal.model.User;

/**
 * This job seaches all content types for those with fields 
 * sysPublishDate and sysExpireDate.  If we have those, we will automatically
 * publish/ unpublish the content based on those dates.
 * One caveat - the content to be published/unpublished cannot be in a
 * "Drafted" state, meaning, it you have a published piece of content 
 * and have made changes to it without republihsing, then the published
 * version will not get unpublished,. 
 * 
 * 
 */
public class PublishExpireJob implements StatefulJob {


	int batchSize = 10;
	final String publishDateField = "sysPublishDate";
	final String expireDateField = "sysExpireDate";

	public PublishExpireJob() {

	}

	public void execute(JobExecutionContext ctx) throws JobExecutionException {

		ContentletAPI capi = APILocator.getContentletAPI();
		User pubUser = null;
		User expireUSer = null;
		try {
			pubUser = APILocator.getUserAPI().getSystemUser();
			expireUSer = APILocator.getUserAPI().getSystemUser();
		} catch (DotDataException e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new JobExecutionException(e);
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm00");
		String now = sdf.format(new Date());

		/*
		 * do publish first, where publish is in the past and expire is in the
		 * future and live = false
		 */
		for (Structure s : getStructWithPublishField()) {
			StringWriter luceneQuery = new StringWriter();
			luceneQuery.append(" +structureName:" + s.getVelocityVarName());
			luceneQuery.append(" +" + s.getVelocityVarName() + "." + publishDateField + ":[19990101010000 to " + now + "]");
			luceneQuery.append(" +" + s.getVelocityVarName() + "." + expireDateField + ":[" + now + " to 20990101010000]");
			luceneQuery.append(" +live:false ");
			luceneQuery.append(" +working:true ");
			luceneQuery.append(" +deleted:false ");

			try {
				List<Contentlet> cons = capi.search(luceneQuery.toString(), 0, batchSize, null, expireUSer, false);
				while (cons.size() > 0) {

					capi.publish(cons, pubUser, false);
					Thread.sleep(500);

					cons = capi.search(luceneQuery.toString(), 0, batchSize, null, expireUSer, false);
				}
			} catch (Exception e) {
				Logger.error(this.getClass(), e.getMessage(), e);
				throw new JobExecutionException(e);
			}
		}

		/*
		 * do expire second, where expire is in the past and live = true
		 */
		for (Structure s : getStructWithExpireField()) {
			StringWriter luceneQuery = new StringWriter();
			luceneQuery.append(" +structureName:" + s.getVelocityVarName());
			luceneQuery.append(" +" + s.getVelocityVarName() + "." + expireDateField + ":[19990101010000 to " + now + "]");
			luceneQuery.append(" +live:true ");
			luceneQuery.append(" +working:true ");
			luceneQuery.append(" +deleted:false ");

			try {
				List<Contentlet> cons = capi.search(luceneQuery.toString(), 0, batchSize, null, expireUSer, false);
				while (cons.size() > 0) {

					capi.unpublish(cons, expireUSer, false);
					Thread.sleep(500);
					
					cons = capi.search(luceneQuery.toString(), 0, batchSize, null, expireUSer, false);
				}
			} catch (Exception e) {
				Logger.error(this.getClass(), e.getMessage(), e);
				throw new JobExecutionException(e);
			}
		}

	}

	private List<Structure> getStructWithPublishField() {
		List<Structure> structs = StructureFactory.getStructures();
		List<Structure> ret = new ArrayList<Structure>();

		for (Structure s : structs) {
			List<Field> fields = s.getFields();
			for (Field f : fields) {
				if (publishDateField.equals(f.getVelocityVarName()) && f.isIndexed()) {
					ret.add(s);
				}
				else if (publishDateField.equals(f.getVelocityVarName()) &! f.isIndexed()) {
					Logger.warn(this.getClass(), "Found Publish Date field on " +s.getName()+"  but it is not indexed.  This won't work");
				}
			}

		}
		return ret;
	}

	private List<Structure> getStructWithExpireField() {
		List<Structure> structs = StructureFactory.getStructures();
		List<Structure> ret = new ArrayList<Structure>();

		for (Structure s : structs) {
			List<Field> fields = s.getFields();
			for (Field f : fields) {
				if (expireDateField.equals(f.getVelocityVarName())&& f.isIndexed()) {
					ret.add(s);
				}
			}

		}
		return ret;

	}

}

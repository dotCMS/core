package com.dotcms.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.publisher.receiver.BundlePublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/bundlePublisher")
public class BundlePublisherResource extends WebResource {
	public static String MY_TEMP = "";

	@POST
	@Path("/publish")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public void publish(@FormDataParam("bundle") InputStream bundle,
			@FormDataParam("bundle") FormDataContentDisposition fileDetail) {
		
		//Write file on FS
		String bundleName = fileDetail.getFileName();
		String bundlePath = ConfigUtils.getBundlePath()+File.separator+MY_TEMP;
		writeToFile(bundle, bundlePath+bundleName);
		
		//Configure and Invoke the Publisher
		try {
			Logger.info(PublisherQueueJob.class, "Started bundle publish process");
			
			PublisherConfig pconf = new PublisherConfig();
			BundlePublisher bundlePublisher = new BundlePublisher();
			pconf.setId(bundleName);
			bundlePublisher.init(pconf);
			bundlePublisher.process(null);
			
			Logger.info(PublisherQueueJob.class, "Finished bundle publish process");
		} catch (NumberFormatException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (Exception e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		}
	}

	
	// save uploaded file to new location
	private void writeToFile(InputStream uploadedInputStream,
			String uploadedFileLocation) {

		try {
			OutputStream out = new FileOutputStream(new File(
					uploadedFileLocation));
			int read = 0;
			byte[] bytes = new byte[1024];

			out = new FileOutputStream(new File(uploadedFileLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}
}

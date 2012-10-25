package com.dotcms.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/bundlePublisher")
public class BundlePublisherResource extends WebResource {

	@POST
	@Path("/publish")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public void uploadFile(@FormDataParam("bundle") InputStream bundle,
			@FormDataParam("bundle") FormDataContentDisposition fileDetail) {
		
		String bundleName = fileDetail.getFileName();
		String bundleFolder = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
		File folderOut = new File("/home/alberto/Scrivania/GZIP/"+bundleFolder);
		folderOut.mkdir();
		// Handle multipart file
		untar(bundle,
				folderOut.getAbsolutePath()+File.separator+bundleName,
				bundleName);
	}

	public void untar(InputStream bundle, String path, String fileName) {
		TarEntry entry;
		TarInputStream inputStream = null;
		FileOutputStream outputStream = null;

		try {
			// get a stream to tar file
			InputStream gstream = new GZIPInputStream(bundle);
			inputStream = new TarInputStream(gstream);

			// For each entry in the tar, extract and save the entry to the file
			// system
			while (null != (entry = inputStream.getNextEntry())) {
				// for each entry to be extracted
				int bytesRead;

				String pathWithoutName = path.substring(0,
						path.indexOf(fileName));

				// if the entry is a directory, create the directory
				if (entry.isDirectory()) {
					File fileOrDir = new File(pathWithoutName + entry.getName());
					fileOrDir.mkdir();
					continue;
				}

				// write to file
				byte[] buf = new byte[1024];
				outputStream = new FileOutputStream(pathWithoutName
						+ entry.getName());
				while ((bytesRead = inputStream.read(buf, 0, 1024)) > -1)
					outputStream.write(buf, 0, bytesRead);
				try {
					if (null != outputStream)
						outputStream.close();
				} catch (Exception e) {
				}

				System.out.println("Extracted " + entry.getName());
			}// while

		} catch (Exception e) {
			e.printStackTrace();
		} finally { // close your streams
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	
	
//	BufferedInputStream buffIS = new BufferedInputStream(inputStream);
//    BufferedOutputStream buffOS = new BufferedOutputStream(outputStream);
//
//    try {
//        IOUtils.copy(buffIS, buffOS);
//    } finally {
//    	buffOS.close();
//    	buffIS.close();
//    }

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

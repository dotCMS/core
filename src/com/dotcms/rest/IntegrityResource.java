package com.dotcms.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;

import com.csvreader.CsvWriter;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;


@Path("/integrity")
public class IntegrityResource extends WebResource {

	/**
	 * <p>Returns a tar with data from structures, workflow schemes and folders for integrity check
	 *
	 * Usage: /getdata
	 *
	 */

	public enum IntegrityType {
	    FOLDERS("folders"),
	    WORKFLOW_SCHEMES("workflow_schemes"),
	    STRUCTURES("structures");

	    private final String fileName;

	    IntegrityType(String fileName) {
	    	this.fileName = fileName;
	    }

	    public String getFileName() {
	    	return fileName;
	    }
	}

	@GET
	@Path("/getdata/{params:.*}")
	@Produces("application/zip")
	public StreamingOutput getData(@Context HttpServletRequest request, @PathParam("params") String params) throws DotDataException, DotSecurityException,
			DotRuntimeException, PortalException, SystemException, IOException {
		InitDataObject initData = init(params, true, request, true);

        Map<String, String> paramsMap = initData.getParamsMap();
        User user = initData.getUser();


        FileOutputStream fos = new FileOutputStream(ConfigUtils.getBundlePath() + File.separator + "integrity.zip");
		ZipOutputStream zos = new ZipOutputStream(fos);

		// create Folders CSV
        File foldersCsvFile = createCSV(IntegrityType.FOLDERS);


        addToZipFile(foldersCsvFile.getAbsolutePath(), zos, "folders.csv");

		zos.close();
		fos.close();

		foldersCsvFile.delete();

		return new StreamingOutput() {
		        public void write(OutputStream output) throws IOException, WebApplicationException {
		            try {
		            	InputStream is = new FileInputStream(ConfigUtils.getBundlePath() + File.separator + "integrity.zip");

		            	byte[] buffer = new byte[1024];
		                int bytesRead;
		                //read from is to buffer
		                while((bytesRead = is.read(buffer)) !=-1){
		                	output.write(buffer, 0, bytesRead);
		                }
		                is.close();
		                //flush OutputStream to write any buffered data to file
		                output.flush();
		                output.close();

		            } catch (Exception e) {
		                throw new WebApplicationException(e);
		            }
		        }
		    };

	}

	private static void addToZipFile(String fileName, ZipOutputStream zos, String zipEntryName) throws FileNotFoundException, IOException {

		System.out.println("Writing '" + fileName + "' to zip file");

		File file = new File(fileName);
		FileInputStream fis = new FileInputStream(file);
		ZipEntry zipEntry = new ZipEntry(zipEntryName);
		zos.putNextEntry(zipEntry);

		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}

		zos.closeEntry();
		fis.close();
	}

	private static File createCSV(IntegrityType type) {

		String outputFile = ConfigUtils.getBundlePath() + File.separator + type.getFileName() + ".csv";
        File csvFile = new File(outputFile);
		boolean alreadyExists = csvFile.exists();

		try {
			// use FileWriter constructor that specifies open for appending
			CsvWriter csvOutput = new CsvWriter(new FileWriter(csvFile, true), ',');

			IntegrityUtil.writeFoldersCSV(csvOutput);

			// if the file didn't already exist then we need to write out the header line
			if (!alreadyExists)
			{
				csvOutput.write("id");
				csvOutput.write("name");
				csvOutput.endRecord();
			}
			// else assume that the file already has the correct header line

			if(type == IntegrityType.FOLDERS) {

			}

			// write out a few records
			csvOutput.write("1");
			csvOutput.write("Bruce");
			csvOutput.endRecord();

			csvOutput.write("2");
			csvOutput.write("John");
			csvOutput.endRecord();

			csvOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return csvFile;
	}

}
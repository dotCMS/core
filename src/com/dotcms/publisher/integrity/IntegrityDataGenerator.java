package com.dotcms.publisher.integrity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;

import com.csvreader.CsvWriter;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.rest.IntegrityResource;
import com.dotcms.rest.IntegrityResource.IntegrityType;
import com.dotcms.rest.IntegrityResource.ProcessStatus;
import com.dotcms.rest.IntegrityUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

public class IntegrityDataGenerator extends Thread {

	private PublishingEndPoint requesterEndPoint;
	public ServletContext servletContext;

	public IntegrityDataGenerator(PublishingEndPoint mySelf, ServletContext servletContext) {
		this.requesterEndPoint = mySelf;
		this.servletContext = servletContext;
	}

	public void run() {

		File foldersCsvFile = null;
        File structuresCsvFile = null;
        File schemesCsvFile = null;
        File zipFile = null;

		try {

			if(requesterEndPoint==null)
				throw new Exception("Not valid endpoint provided");

			servletContext.setAttribute("integrityDataGenerationStatus", ProcessStatus.PROCESSING);

			File dir = new File(ConfigUtils.getIntegrityPath() + File.separator + requesterEndPoint.getId());

			// if file doesnt exists, then create it
			if (!dir.exists()) {
				dir.mkdir();
			}

			zipFile = new File(ConfigUtils.getIntegrityPath() + File.separator + requesterEndPoint.getId() + File.separator + "integrity.zip");
        	FileOutputStream fos = new FileOutputStream(zipFile);
        	ZipOutputStream zos = new ZipOutputStream(fos);

        	// create Folders CSV
        	foldersCsvFile = createCSV(IntegrityType.FOLDERS);
        	structuresCsvFile = createCSV(IntegrityType.STRUCTURES);
        	schemesCsvFile = createCSV(IntegrityType.WORKFLOW_SCHEMES);

        	addToZipFile(foldersCsvFile.getAbsolutePath(), zos, "folders.csv");
        	addToZipFile(structuresCsvFile.getAbsolutePath(), zos, "structures.csv");
        	addToZipFile(schemesCsvFile.getAbsolutePath(), zos, "workflow_schemes.csv");

        	zos.close();
        	fos.close();
		} catch (Exception e) {
			if(foldersCsvFile!=null && foldersCsvFile.exists())
				zipFile.delete();

			servletContext.setAttribute("integrityDataGenerationStatus", ProcessStatus.ERROR);
			servletContext.setAttribute("integrityDataGenerationError", e.getMessage());


		} finally {
        	if(foldersCsvFile!=null && foldersCsvFile.exists())
        		foldersCsvFile.delete();
        	if(structuresCsvFile!=null && structuresCsvFile.exists())
        		structuresCsvFile.delete();
        	if(schemesCsvFile!=null && schemesCsvFile.exists())
        		schemesCsvFile.delete();

        	servletContext.setAttribute("integrityDataGenerationStatus", ProcessStatus.FINISHED);
        }
	}

	private static File createCSV(IntegrityType type) throws Exception {

		String outputFile = ConfigUtils.getBundlePath() + File.separator + type.getFileName() + ".csv";
        File csvFile = new File(outputFile);

		try {
			// use FileWriter constructor that specifies open for appending
			CsvWriter csvOutput = new CsvWriter(new FileWriter(csvFile, true), '|');

			if(type == IntegrityType.FOLDERS) {
				IntegrityUtil.writeFoldersCSV(csvOutput);
			} else if(type == IntegrityType.STRUCTURES) {
				IntegrityUtil.writeStructuresCSV(csvOutput);
			}  else if(type == IntegrityType.WORKFLOW_SCHEMES) {
				IntegrityUtil.writeFoldersCSV(csvOutput);
			}

			csvOutput.close();
		} catch (IOException e) {
			Logger.error(IntegrityResource.class, "Error writing csv: " + type.name(), e);
			throw new Exception("Error writing csv: " + type.name(), e);
		} catch (DotDataException e) {
			Logger.error(IntegrityResource.class, "Error getting data from DB for: " + type.name(), e);
			throw new Exception("Error getting data from DB for: " + type.name(), e);
		}

		return csvFile;
	}

	private static void addToZipFile(String fileName, ZipOutputStream zos, String zipEntryName) throws Exception  {

		System.out.println("Writing '" + fileName + "' to zip file");

		try {

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

		} catch(FileNotFoundException f){
			Logger.error(IntegrityResource.class, "Could not find file " + fileName, f);
			throw new Exception("Could not find file " + fileName, f);
		} catch (IOException e) {
			Logger.error(IntegrityResource.class, "Error writing file to zip: " + fileName, e);
			throw new Exception("Error writing file to zip: " + fileName, e);
		}
	}
}

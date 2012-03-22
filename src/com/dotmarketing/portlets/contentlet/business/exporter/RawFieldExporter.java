package com.dotmarketing.portlets.contentlet.business.exporter;

import java.io.File;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Logger;

/**
 * 
 * Just returns the raw data of the given field
 * 
 * @author David H Torres
 * 
 */
public class RawFieldExporter implements BinaryContentExporter {

	public BinaryContentExporterData exportContent(File file, Map<String, String[]> parameters) throws BinaryContentExporterException {
		

		BinaryContentExporterData data;


		data = new BinaryContentExporterData(file);
			

		return data;
	}

	public String getName() {
		
		return "Export Field Content";
	}

	public String getPathMapping() {
		return "raw-data";
	}

	public String getDescription() {
		return "Exports the data contained on a given contentlet field as it is";
	}



}

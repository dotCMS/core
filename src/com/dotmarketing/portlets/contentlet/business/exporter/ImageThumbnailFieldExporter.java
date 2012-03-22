package com.dotmarketing.portlets.contentlet.business.exporter;

import java.io.File;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;

/**
 * 
 * 
 * Just a wrapper for the ThumbnailFilter
 * 
 * @author David H Torres
 */
public class ImageThumbnailFieldExporter implements BinaryContentExporter {

	public BinaryContentExporterData exportContent(File file, Map<String, String[]> parameters)
			throws BinaryContentExporterException {



		if (parameters.get("w") != null) {
			parameters.put("thumbnail_w", parameters.get("w"));
			parameters.remove("w");
		}
		if (parameters.get("h") != null) {
			parameters.put("thumbnail_h", parameters.get("h"));
			parameters.remove("h");
		}
		if (parameters.get("bg") != null) {
			parameters.put("thumbnail_bg", parameters.get("bg"));
			parameters.remove("bg");
		}
		parameters.put("filter", new String[] { "Thumbnail" });

		return new ImageFilterExporter().exportContent(file, parameters);
	}

	public String getName() {
		return "Image Thumbnail Field Exporter";
	}

	public String getPathMapping() {
		return "image-thumbnail";
	}

	public String getDescription() {
		return "Generates a thumbnail a given contentlet image field based on the passed width and height dimensions";
	}

}

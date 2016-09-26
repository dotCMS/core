package com.dotmarketing.portlets.contentlet.business.exporter;

import java.io.File;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;

/**
 * 
 * This has become a wrapper for ResizeImageFilter.java
 * com.dotmarketing.portlets.contentlet.business.exporter.filter.ResizeImageFilter.java
 * 
 * 
 * 
 * 
 * 
 */

public class ImageResizeFieldExporter implements BinaryContentExporter {

	public BinaryContentExporterData exportContent(File file, Map<String, String[]> parameters)
			throws BinaryContentExporterException {


		if (parameters.get("w") != null) {
			parameters.put("resize_w", parameters.get("w"));
			parameters.remove("w");
		}
		if (parameters.get("h") != null) {
			parameters.put("resize_h", parameters.get("h"));
			parameters.remove("h");
		}
		parameters.put("filter", new String[] { "Resize" });

		return new ImageFilterExporter().exportContent(file, parameters);


	}

	public String getName() {
		return "Content Image Fields Resizer";
	}

	public String getPathMapping() {
		return "resize-image";
	}

	public String getDescription() {
		return "Resizes a given contentlet image field to the passed width and height";
	}

}

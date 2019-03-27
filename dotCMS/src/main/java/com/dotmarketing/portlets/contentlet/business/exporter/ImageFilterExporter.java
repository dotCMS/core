package com.dotmarketing.portlets.contentlet.business.exporter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.filter.PDFImageFilter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.util.Logger;

/**
 * 
 * A exporter that can take 1 or more filters in a chain
 * 
 * the chain is provided by the "filter=" parameter
 * You can chain filters so that you resize then crop to 
 * produce the resulting image
 * 
 * 
 */

public class ImageFilterExporter implements BinaryContentExporter {

	
	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.BinaryContentExporter#exportContent(java.io.File, java.util.Map)
	 */
	public BinaryContentExporterData exportContent(File file, Map<String, String[]> parameters) throws BinaryContentExporterException {


		BinaryContentExporterData data;
		
		try {

			List<String> filters=new ArrayList<>();

			if(parameters.get("filter") != null){
				filters.addAll( Arrays.asList(parameters.get("filter")[0].split(",")));
			}
			else if(parameters.get("filters") != null){
			  filters.addAll( Arrays.asList(parameters.get("filters")[0].split(",")));
			}


	       if(file.getAbsolutePath().toLowerCase().endsWith(".pdf")){
	         filters.remove("PDF");
	         filters.add(0, "PDF");
           }

	       else if(filters.size()== 0 ){
	         filters.remove("Png");
	         filters.add(0, "Png");
	       }


           parameters.put("filter", filters.toArray(new String[filters.size()]));
           parameters.put("filters", filters.toArray(new String[filters.size()]));
			for(String s : filters){
				String clazz =null;
				try {
					clazz ="com.dotmarketing.image.filter." + s + "ImageFilter";
					Class<ImageFilter> iFilter = (Class<ImageFilter>) Class.forName( clazz );
					ImageFilter i=	iFilter.newInstance();
					file = i.runFilter(file,   parameters);
				} catch (ClassNotFoundException e) {
					Logger.error(ImageFilterExporter.class, "Unable to instanciate : " +  clazz );
				} catch (InstantiationException e) {
					Logger.error(ImageFilterExporter.class, "InstantiationException : " +  clazz );
				} catch (IllegalAccessException e) {
					Logger.error(ImageFilterExporter.class, "IllegalAccessException : " +  clazz );
				}
				catch (Exception e) {
					Logger.error(ImageFilterExporter.class, "Exception in " +  clazz + " :" + e.getMessage() + e.getStackTrace()[0] );
				}
			}


			data = new BinaryContentExporterData(file);
			
		} catch (Exception e) {
			Logger.error(ImageFilterExporter.class, e.getMessage(), e);
			throw new BinaryContentExporterException(e.getMessage(), e);
		}
		
		return data;
	}

	public String getName() {
		return "Image Filter Exporter";
	}

	public String getPathMapping() {
		return "image";
	}

	public String getDescription() {
		return "Specify filters to run a source image through";
	}

}

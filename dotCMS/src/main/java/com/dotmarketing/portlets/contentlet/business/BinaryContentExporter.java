package com.dotmarketing.portlets.contentlet.business;

import java.io.File;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;

/**
 * 
 * A BinaryContentExporter is a class responsible on transform a piece of content field or the entire piece of content on a diferent format. 
 * E.G. A exporter that takes an image binary field and resize it to an specific dimension, Or a exporter that takes the entire content and 
 * transform it to a Word Document.
 * 
 * This is the interface that every content exporter should implement, is responsibility of the exporter to cache the results but also check if
 * the original binary content has change to also refresh the cached transformed object. 
 * 
 * @author David H Torres
 * @version 1.9
 *
 */
public interface BinaryContentExporter {

	public static class BinaryContentExporterData {
		
		private File dataFile;
		
		public BinaryContentExporterData(File dataFile) {
			this.setDataFile(dataFile);
		}

		public void setDataFile(File dataFile) {
			this.dataFile = dataFile;
		}

		public File getDataFile() {
			return dataFile;
		}
		
	}

	/**
	 * 
	 * In this method the exporter operates over the given field of the piece of content to generate a result based on the passed parameters.
	 * The parameter field is optional and for some exporters could apply that is null or ignored because the exporter works over the whole content instead of an
	 * specific field, E.G. A exporter to Word Document that converts the whole content in a Word document is a good example
	 * 	  
	 * @param file - the file that is looking to be exported
	 * @param field The field of the content over the exporter is going to operate
	 * @param parameters
	 * @return
	 * @throws BinaryContentExporterException In case an error as occurred when exporting, either because bad parameters or invalid data
	 */
	public BinaryContentExporterData exportContent(File file, Map<String, String[]> parameters) throws BinaryContentExporterException;

	
	
	
	/**
	 * Retrieves a human readable name of the exporter
	 * @return
	 */
	public String getName();
	
	/**
	 * Retrieves a short description of what the exporter does
	 * @return
	 */
	public String getDescription();
	
	/**
	 * Returns the path mapping from which this exporter will be triggered  
	 * @return
	 */
	public String getPathMapping();

}

package com.dotcms.tika;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;

public class TikaUtils {

	
	/**
	 * Right now the method use the Tika facade directly for parse the document without any kind of restriction about the parser because the 
	 * new Tika().parse method use the AutoDetectParser by default.
	 * 
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * May 31, 2013 - 12:27:19 PM
	 */
	public Map<String, String> getMetaDataMap(String inode, File binFile, String mimeType, boolean forceMemory) {
		Map<String, String> metaMap = new HashMap<String, String>();
		
		// store content metadata on disk
        File contentM=APILocator.getFileAssetAPI().getContentMetadataFile(inode);
        
		Tika t = new Tika();
		Metadata met = new Metadata();
		int maxStringLenght = Config.getIntProperty("TIKA_PARSE_CHARACTER_LIMIT", -1);
		int defaultMaxStringLenght = t.getMaxStringLength();
		t.setMaxStringLength(maxStringLenght);
		Reader fulltext = null;
		InputStream is = null;
		// if the limit is not "unlimited"
		// I can use the faster parseToString
		try {
			if(forceMemory || (maxStringLenght>0 && maxStringLenght<=defaultMaxStringLenght)){
				// no worry about the limit and less time to process.
				String content = t.parseToString(new FileInputStream(binFile), met);
				metaMap = new HashMap<String, String>();
				for (int i = 0; i < met.names().length; i++) {
					String name = met.names()[i];
					if (UtilMethods.isSet(name) && met.get(name) != null) {
						// we will want to normalize our metadata for searching
						String[] x = translateKey(name);
						for (String y : x)
							metaMap.put(y, met.get(name));
					}
				}
				metaMap.put(FileAssetAPI.CONTENT_FIELD, content);
			}
			else { // otherwise I must use the incremental parsing
				is = TikaInputStream.get(binFile);
				fulltext = t.parse(is, met);
				metaMap = new HashMap<String, String>();
				for (int i = 0; i < met.names().length; i++) {
					String name = met.names()[i];
					if (UtilMethods.isSet(name) && met.get(name) != null) {
						// we will want to normalize our metadata for searching
						String[] x = translateKey(name);
						for (String y : x)
							metaMap.put(y, met.get(name));
					}
				}
				
				if(!contentM.exists() && contentM.getParentFile().mkdirs() && contentM.createNewFile()) {
    				OutputStream out=new FileOutputStream(contentM);
    				
    				// compressor config
    				String compressor=Config.getStringProperty("CONTENT_METADATA_COMPRESSOR", "gzip");
    				if(compressor.equals("gzip")) {
    				    out = new GZIPOutputStream(out);
    				}
    				else if(compressor.equals("bzip2")) {
    				    out = new BZip2CompressorOutputStream(out);
    				}
    				
    				try {
    				    IOUtils.copy(fulltext, out);
    				}
    				finally {
        				IOUtils.closeQuietly(out);
        				IOUtils.closeQuietly(fulltext);
    				}
				}
			}
		} catch (Exception e) {
			Logger.error(this.getClass(), "Could not parse file metadata for file : " + binFile.getAbsolutePath() + ". " +e.getMessage());
		} 
		finally {			
			if(null!=fulltext)
				IOUtils.closeQuietly(fulltext);		
			if(null!=is)
				IOUtils.closeQuietly(is);
			try{
				metaMap.put(FileAssetAPI.SIZE_FIELD, String.valueOf(binFile.length()));
			}
			catch(Exception ex){
				Logger.error(this.getClass(), "Could not parse file metadata for file : " + binFile.getAbsolutePath() + ". " +ex.getMessage());
			}
		}
		return metaMap;
	}
	
	/**
	 * This method takes a file and uses tika to parse the metadata from it. It
	 * returns a Map of the metadata
	 * 
	 * @param binFile
	 * @return
	 */
	public Map<String, String> getMetaDataMap(String inode,File binFile, boolean forceMemory) {
		return getMetaDataMap(inode,binFile, null, forceMemory);
	}

//	/**
//	 * 
//	 * @param binFile
//	 * @return
//	 */
//	private Parser getParser(File binFile) {
//		String mimeType = new MimetypesFileTypeMap().getContentType(binFile);
//		return getParser(binFile, mimeType);
//	}

	
//	private Parser getParser(File binFile, String mimeType) {
//		String[] mimeTypes = Config.getStringArrayProperty("CONTENT_PARSERS_MIMETYPES");
//		String[] parsers = Config.getStringArrayProperty("CONTENT_PARSERS");
//		int index = Arrays.binarySearch(mimeTypes, mimeType);
//		if (index > -1 && parsers.length > 0) {
//			String parserClassName = parsers[index];
//			Class<Parser> parserClass;
//			try {
//				parserClass = (Class<Parser>) Class.forName(parserClassName);
//				return parserClass.newInstance();
//			} catch (Exception e) {
//				Logger.warn(this.getClass(), "A content parser for mime type " + mimeType
//						+ " was found but could not be instantiated, using default content parser.");
//			}
//		}
//		return new AutoDetectParser();
//	}
	
	
	
	
	/**
	 * normalize metadata from various filetypes this method will return an
	 * array of metadata keys that we can use to normalize the values in our
	 * fileAsset metadata For example, tiff:ImageLength = "height" for image
	 * files, so we return {"tiff:ImageLength", "height"} and both metadata are
	 * written to our metadata field
	 * 
	 * @param key
	 * @return
	 */
	private String[] translateKey(String key) {
		String[] x = getTranslationMap().get(key);
		if (x == null) {
			x = new String[] { StringUtils.sanitizeCamelCase(key) };
		}
		return x;
	}

	private Map<String, String[]> translateMeta = null;

	private Map<String, String[]> getTranslationMap() {
		if (translateMeta == null) {
			synchronized ("translateMeta".intern()) {
				if (translateMeta == null) {
					translateMeta = new HashMap<String, String[]>();
					translateMeta.put("tiff:ImageWidth", new String[] { "tiff:ImageWidth", "width" });
					translateMeta.put("tiff:ImageLength", new String[] { "tiff:ImageLength", "height" });
				}
			}
		}
		return translateMeta;
	}
}

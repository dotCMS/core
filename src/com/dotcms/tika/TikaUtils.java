package com.dotcms.tika;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;

public class TikaUtils {

	/**
	 * This method takes a file and uses tika to parse the metadata from it. It
	 * returns a Map of the metadata
	 * 
	 * @param binFile
	 * @return
	 */

	public Map<String, String> getMetaDataMap(File binFile) {
		Map<String, String> metaMap = new HashMap<String, String>();
		Parser parser = getParser(binFile);
		Metadata met = new Metadata();
		// set -1 for no limit when parsing text content
		ContentHandler handler = new BodyContentHandler(Config.getIntProperty("TIKA_PARSE_CHARACTER_LIMIT", -1));
		ParseContext context = new ParseContext();
		InputStream fis = null;
		try {
			fis = new FileInputStream(binFile);
			parser.parse(fis, handler, met, context);
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
			if (handler != null && UtilMethods.isSet(handler.toString()))
				metaMap.put(FileAssetAPI.CONTENT_FIELD, handler.toString());
		} catch (Exception e) {
			Logger.error(this.getClass(), "Could not parse file metadata for file : " + binFile.getAbsolutePath());
		} finally {
			metaMap.put(FileAssetAPI.SIZE_FIELD, String.valueOf(binFile.length()));
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}

		return metaMap;
	}

	/**
	 * 
	 * @param binFile
	 * @return
	 */
	private Parser getParser(File binFile) {
		String mimeType = new MimetypesFileTypeMap().getContentType(binFile);
		String[] mimeTypes = Config.getStringArrayProperty("CONTENT_PARSERS_MIMETYPES");
		String[] parsers = Config.getStringArrayProperty("CONTENT_PARSERS");
		int index = Arrays.binarySearch(mimeTypes, mimeType);
		if (index > -1 && parsers.length > 0) {
			String parserClassName = parsers[index];
			Class<Parser> parserClass;
			try {
				parserClass = (Class<Parser>) Class.forName(parserClassName);
				return parserClass.newInstance();
			} catch (Exception e) {
				Logger.warn(this.getClass(), "A content parser for mime type " + mimeType
						+ " was found but could not be instantiated, using default content parser.");
			}
		}
		return new AutoDetectParser();
	}

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

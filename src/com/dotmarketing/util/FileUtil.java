package com.dotmarketing.util;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

import com.dotmarketing.beans.Inode;

public class FileUtil {

	private static Set<String> extensions = new HashSet<String>();

	/**
	 * This method takes a string of a filename or extension and maps it to a
	 * known .png file in the /html/image/icons/directory
	 * 
	 * @param x
	 *            is the filename or extension
	 * @return
	 */

	public static String getIconExtension(String x) {
		if(x==null){
			return "ukn";
		}
		if (x.lastIndexOf(".") > -1) {
			x = x.substring(x.lastIndexOf(".")+1, x.length());
		}

		if (extensions.size() == 0) {
			synchronized (FileUtil.class) {
				if (extensions.size() == 0) {
					String path = Config.CONTEXT.getRealPath("/html/images/icons");

					String[] files = new File(path).list(new PNGFileNameFilter());
					for (String name : files) {
						if (name.indexOf(".png") > -1)
							extensions.add(name.replace(".png", ""));
					}
				}
			}
		}
		// if known extension
		if (extensions.contains(x)) {
			return x;
		} else {
			return "ukn";
		}

	}

	/**
	 * This will return the full path to the file asset as a String
	 * 
	 * @param inode
	 * @return
	 */
	public static String getAbsoluteFileAssetPath(Inode inode) {
		String _inode = inode.getInode();
		return getAbsoluteFileAssetPath(_inode,
				UtilMethods.getFileExtension(((com.dotmarketing.portlets.files.model.File) inode).getFileName()).intern());
	}

	/**
	 * This will return the full path to the file asset as a String
	 * 
	 * @param inode
	 * @param extenstion
	 * @return
	 */
	public static String getAbsoluteFileAssetPath(String inode, String extenstion) {
		String _inode = inode;
		String path = "";
		String realPath = Config.getStringProperty("ASSET_REAL_PATH");
		String assetPath = Config.getStringProperty("ASSET_PATH");
		path = java.io.File.separator + _inode.charAt(0) + java.io.File.separator + _inode.charAt(1) + java.io.File.separator + _inode
				+ "." + extenstion;
		if (UtilMethods.isSet(realPath)) {
			return realPath + path;
		} else {
			return Config.CONTEXT.getRealPath(assetPath + path);
		}

	}

	public static String sanitizeFileName(String fileName){
		if(fileName ==null){
			return fileName;
		}
		fileName = URLDecoder.decode(fileName);
		char[] invalids ={'\\', '/',':', '*', '?' ,'"','\'' ,'<' ,'>' ,'|'};
		
		
		for(int i=0;i< invalids.length;i++){
			while(fileName.indexOf(invalids[i]) > -1){
				fileName = fileName.replace(invalids[i], ' ');
	        }	
		}
		
		while(fileName.indexOf("  ") > -1){
			fileName = fileName.replaceAll("  ", " ");
        }	
		
		return fileName;
	}
	
	
	
	
	
	
}

final class PNGFileNameFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return (name.indexOf(".png") > -1);
	}

}




package com.dotmarketing.image.filter;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.WebKeys;

public abstract class ImageFilter implements ImageFilterIf {
	protected final static String FILE_EXT = "png";

	/**
	 * the value of this field is used to insure that the generated cache files
	 * 1) do not overwrite each other.
	 * 2) are unique based on the parameters and order in the filter
	 * 3) adds the "Rendition" tag to the final image in the filter chain, if so requested
	 * this is so we can reuse the same heavily generated file (resize:5000px) again and again when needed
	 *
	 *
	 * @param fieldName
	 * @param parameters
	 * @return
	 */
	private String getUniqueFileName(File file, Map<String, String[]> parameters, String inode) {
		try {
			StringBuilder sb = new StringBuilder();
			Iterator<Entry<String, String[]>> it = parameters.entrySet().iterator();

			String[] filters = parameters.get("filter")[0].split(",");
			List<String> acceptFilter = new ArrayList<String>();
			String thisFilter="";
			for(int i=0;i<filters.length;i++){
				String x = filters[i];
				acceptFilter.add(x.toLowerCase());
				sb.append(x + ":");
				if (x.toLowerCase().equals(getFilterName())) {
					thisFilter=getFilterName();
					break;
				}
			}

			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				String key = (String) pairs.getKey();
				String val = ((String[]) pairs.getValue())[0];

				for (String x : acceptFilter) {
					if (key.startsWith(x)) {
						sb.append(key + ":" + val);
					}
					if (key.equalsIgnoreCase("fieldVarName")) {//DOTMCS-5674
						sb.append(key + ":" + val);
					}
				}
			}


			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update((inode + sb.toString() + this.getClass()).getBytes());

			StringBuilder ret = new StringBuilder();
			ret.append( WebKeys.GENERATED_FILE);
			ret.append(thisFilter + "_");
			ret.append(convertToHex(digest.digest()));



			Logger.debug(this.getClass(), "");
			Logger.debug(this.getClass(), "------------------------------------------------------------------");
			Logger.debug(this.getClass(), "   for : " + file.getAbsolutePath()+" " + sb);
			Logger.debug(this.getClass(), "   with vars: + " + sb);
			Logger.debug(this.getClass(), "   unique key: " + ret.toString());
			Logger.debug(this.getClass(), "------------------------------------------------------------------");
			Logger.debug(this.getClass(), "");

			return ret.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new DotStateException(this.getClass() + ":" + e);
		}

	}

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	protected String getFilterName() {
		return this.getClass().getSimpleName().replaceAll("ImageFilter", "").toLowerCase();
	}

	protected String getPrefix() {
		return getFilterName() + "_";
	}

	protected boolean overwrite(File resultFile, Map<String, String[]> parameters){
		boolean overwrite = false;
		long test = resultFile.length();
		if (!resultFile.exists())
			overwrite = true;
		else if (test < 50)
			overwrite = true;
		else if (parameters.get("overwrite") != null)
			overwrite = true;

		return overwrite;
	}


	/**
	 * returns the file that can be used to store resutlts.
	 * The heavy lifting is being in the getUniqueFileName() method
	 * @param file
	 * @param parameters
	 * @return
	 * @throws IOException
	 * @throws DotRuntimeException
	 */
	protected File getResultsFile(File file, Map<String, String[]> parameters) throws DotRuntimeException{
		return  getResultsFile(file, parameters, FILE_EXT);
	}


	/**
	 * returns the file that can be used to store resutlts.
	 * The heavy lifting is being in the getUniqueFileName() method
	 * @param file
	 * @param parameters
	 * @return
	 * @throws IOException
	 * @throws DotRuntimeException
	 */
	protected File getResultsFile(File file, Map<String, String[]> parameters, String fileExt) throws DotRuntimeException{
		String fileFolderPath = file.getParent();

		String inode =null;

		try{
			if(file.getName().startsWith(WebKeys.GENERATED_FILE)){
				inode = file.getName();
				String fileNameNoExt = this.getUniqueFileName(file, parameters, inode);
				String resultFilePath =fileFolderPath+ File.separator + fileNameNoExt + "." + fileExt;
				return  new File(resultFilePath);
			}
			else{
				try{
					inode = RegEX.find(file.getCanonicalPath(), "[\\w]{8}(-[\\w]{4}){3}-[\\w]{12}").get(0).getMatch();
				}
				catch (Exception e){
					inode = parameters.get("assetInodeOrIdentifier")[0];
				}
				String realAssetPath = APILocator.getFileAPI().getRealAssetPath();
				File dirs = new File(realAssetPath + File.separator + "dotGenerated" + File.separator + inode.charAt(0) + File.separator + inode.charAt(1));
				dirs.mkdirs();
				String fileNameNoExt = this.getUniqueFileName(file, parameters, inode);
				String resultFilePath = dirs.getCanonicalPath() + File.separator + fileNameNoExt + "." + fileExt;
				return  new File(resultFilePath);
			}



		}
		catch(Exception e){
			throw new DotRuntimeException("Cannot find the inode of the file : " + e.getMessage());
		}
	}

}

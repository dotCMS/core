package com.dotmarketing.image.filter;

import com.dotcms.cost.RequestPrices.Price;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.focalpoint.FocalPoint;
import com.dotmarketing.image.focalpoint.FocalPointAPIImpl;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.WebKeys;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public abstract class ImageFilter implements ImageFilterIf {
	protected static final String FILE_EXT = "png";
	public    static final String CROP     = "crop";


	
	
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
			List<String> acceptFilter = new ArrayList<>();
			String thisFilter="";
			if(parameters.get("filter")!=null && parameters.get("filter").length>0){
				String[] filters = parameters.get("filter");
	
				
				for(int i=0;i<filters.length;i++){
					String x = filters[i];
					acceptFilter.add(x.toLowerCase());
					sb.append(x + ":");
					if (x.toLowerCase().equals(getFilterName())) {
						thisFilter=getFilterName();
						break;
					}
				}
			}

			while (it.hasNext()) {
				Map.Entry<String, String[]> pairs = it.next();
				String key = pairs.getKey();
				String val = pairs.getValue()[0];

				for (String filterName : acceptFilter) {
					if (key.startsWith(filterName)) {
						sb.append(key + ":" + val);
					}
					if (key.equalsIgnoreCase("fieldVarName")) {//DOTMCS-5674
						sb.append(key + ":" + val);
					}
				}
			}
			
            if (CROP.equals(thisFilter)) {
                Optional<FocalPoint> optPoint = new FocalPointAPIImpl().parseFocalPointFromParams(parameters);
                if(optPoint.isEmpty()) {
                    String fieldVar = parameters.get("fieldVarName")[0];
                    optPoint =new FocalPointAPIImpl().readFocalPoint(inode, fieldVar);
                }
                if(optPoint.isPresent()) {
                    sb.append("fp:" + optPoint.get());
                }
            }
			
			


			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update((inode + sb.toString() + this.getClass() + file.getName()).getBytes());

			StringBuilder ret = new StringBuilder();
			ret.append( WebKeys.GENERATED_FILE);
			ret.append(thisFilter + "_");
			ret.append(convertToHex(digest.digest()));



			Logger.debug(this.getClass(), ()->"");
			Logger.debug(this.getClass(), ()->"------------------------------------------------------------------");
			Logger.debug(this.getClass(), ()->"   for : " + file.getAbsolutePath()+" " + sb);
			Logger.debug(this.getClass(), ()->"   with vars: + " + sb);
			Logger.debug(this.getClass(), ()->"   unique key: " + ret.toString());
			Logger.debug(this.getClass(), ()->"------------------------------------------------------------------");
			Logger.debug(this.getClass(), ()->"");

			return ret.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new DotStateException(this.getClass() + ":" + e,e);
		}

	}

	private static String convertToHex(byte[] data) {
		StringBuilder buf = new StringBuilder();
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


    static final Lazy<Method> overwriteMethod = Lazy.of(
            () -> Try.of(() -> ImageFilter.class.getDeclaredMethod("overwrite", File.class, Map.class))
                    .onFailure(e -> Logger.error(ImageFilter.class, e)).getOrNull());


	protected boolean overwrite(File resultFile, Map<String, String[]> parameters){
        boolean overwrite = !resultFile.exists() || resultFile.length() < 50 || parameters.get("overwrite") != null;

        if (overwrite) {
            // Try to increment cost, but don't fail if CDI is not initialized (e.g., in unit tests)
            Try.run(() -> APILocator.getRequestCostAPI().incrementCost(Price.IMAGE_FILTER_TRANSFORM,
                            overwriteMethod.get(), new Object[]{resultFile.toPath()}))
                    .onFailure(e -> Logger.debug(ImageFilter.class,
                            "Unable to increment request cost (CDI may not be initialized): " + e.getMessage()));
        }

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
	public File getResultsFile(File file, Map<String, String[]> parameters) {
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
	protected final File getResultsFile(File file, Map<String, String[]> parameters, String fileExt) {
		String fileFolderPath = file.getParent();

		String inode =null;

        if (file.getName().startsWith(WebKeys.GENERATED_FILE)) {
            inode = file.getName();
            String fileNameNoExt = this.getUniqueFileName(file, parameters, inode);
            String resultFilePath = fileFolderPath + File.separator + fileNameNoExt + "." + fileExt;
            return new File(resultFilePath);
        } else {
            try {
                inode = RegEX.find(file.getCanonicalPath(), "[\\w]{8}(-[\\w]{4}){3}-[\\w]{12}").get(0).getMatch();
            } catch (Exception e) {
                inode = parameters.get("assetInodeOrIdentifier")[0];
            }

            File dirs = new File(ConfigUtils.getDotGeneratedPath() + File.separator + inode.charAt(0) + File.separator
                            + inode.charAt(1));
            if (!dirs.exists()) {
                dirs.mkdirs();
            }
            String fileNameNoExt = this.getUniqueFileName(file, parameters, inode);
            String finalPath = Try.of(dirs::getCanonicalPath).getOrElseThrow(DotRuntimeException::new);
            String resultFilePath = finalPath + File.separator + fileNameNoExt + "." + fileExt;
            return new File(resultFilePath);
        }

	}
	
    public String[] getAcceptedParameters() {
        return new String[] {
        };
    }
	

}

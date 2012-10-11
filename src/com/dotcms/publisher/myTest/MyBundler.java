package com.dotcms.publisher.myTest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.extended.EncodedByteArrayConverter;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class MyBundler implements IBundler {
	private PublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	
	public final static String  XML_EXTENSION = ".xml" ;
	
	@Override
	public String getName() {
		return "My Test bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		
		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(MyBundler.class,e.getMessage(),e);
		}
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<200)
	        throw new RuntimeException("need an enterprise license to run this bundler");
	    
		List<Contentlet> cs = new ArrayList<Contentlet>();
		
		Logger.info(MyBundler.class, config.getLuceneQuery());
		
		try {
			cs = conAPI.search(config.getLuceneQuery(), 0, 0, "moddate", systemUser, false);
		} catch (Exception e) {
			Logger.error(MyBundler.class,e.getMessage(),e);
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()" + e.getMessage() + ": Unable to pull content with query " + config.getLuceneQuery(), e);
		}
		
		status.setTotal(cs.size());
		
		for (Contentlet con : cs) {
			try {
				writeFileToDisk(bundleRoot, con);
				status.addCount();
			} catch (Exception e) {
				Logger.error(MyBundler.class,e.getMessage() + " : Unable to write file",e);
				status.addFailure();
			}
		}
	}
	
	private void writeFileToDisk(File bundleRoot, Contentlet con) throws IOException, DotBundleException, DotDataException, DotSecurityException{
		List<Field> fields=FieldsCache.getFieldsByStructureInode(con.getStructureInode());
		Map<String,Object> contentMap=new HashMap<String, Object>();
		
		for(Field ff : fields) {
			if(ff.getFieldType().toString().equals(Field.FieldType.BINARY.toString())) {
				File file = con.getBinary( ff.getVelocityVarName()); 
				
				byte[] bytes=org.apache.commons.io.FileUtils.readFileToByteArray(file);
				
				Map<String, Object> bufferMap =  new HashMap<String, Object>();
				bufferMap.put(file.getName(), bytes);
				contentMap.put(ff.getFieldName(), bufferMap);
		    } else {
		    	contentMap.put(ff.getFieldName(), con.getStringProperty(ff.getFieldName()));
		    }
			
		}
		
		PushAssetWrapper wrapper=new PushAssetWrapper();
	    wrapper.setContentMap(contentMap);
	    
	    XStream xstream=new XStream(new DomDriver());
	    xstream.registerConverter(new EncodedByteArrayConverter());
	    
	    // note that it also support passing an outputstream. That would be faster
	    String xml=xstream.toXML(wrapper);
	    
	    System.out.println("this is the XML");
	    System.out.println(xml);
			
			
	//			String myFile = APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator 
	//					file.getURI().replace("/", File.separator) + FILE_ASSET_EXTENSION;
	//	File file= conAPI.getBinaryFile(
	//            con.getInode(), ff.getVelocityVarName(), systemUser);
				
	//			// Should we write or is the file already there:
	//			Calendar cal = Calendar.getInstance();
	//			cal.setTime(info.getVersionTs());
	//			cal.set(Calendar.MILLISECOND, 0);
	//			
	//			String dir = myFile.substring(0, myFile.lastIndexOf(File.separator));
	//			new File(dir).mkdirs();
	//			
	//			
	//			//only write if changed
	//			File f = new File(myFile);
	//			if(!f.exists() || f.lastModified() != cal.getTimeInMillis()){
	//				String x  = (String) fileAsset.get("metaData");
	//				fileAsset.setMetaData(x);
	//				BundlerUtil.objectToXML(wrap, f, true);
	//				f.setLastModified(cal.getTimeInMillis());
	//			}
	//			
	//			//only write if changed
	//			f = new File(myFile.replaceAll(FILE_ASSET_EXTENSION,""));
	//			if(!f.exists() || f.lastModified() != cal.getTimeInMillis()){
	//				File oldAsset = new File(APILocator.getFileAssetAPI().getRealAssetPath(fileAsset.getInode(), fileAsset.getFileName()));
	//				FileUtil.copyFile(oldAsset, f, true);
	//				f.setLastModified(cal.getTimeInMillis());
	//			}
	//			// set the time of the file
				
			
			
	}

	@Override
	public FileFilter getFileFilter() {
		// TODO Auto-generated method stub
		return null;
	}

}

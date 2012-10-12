package com.dotcms.publisher.myTest;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

public class PushPublisherBundler implements IBundler {
	private PublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	
	public final static String  XML_EXTENSION = ".xml" ;
	
	@Override
	public String getName() {
		return "Push publisher bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		
		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(PushPublisherBundler.class,e.getMessage(),e);
		}
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<200)
	        throw new RuntimeException("need an enterprise license to run this bundler");
	    
		List<Contentlet> cs = new ArrayList<Contentlet>();
		
		Logger.info(PushPublisherBundler.class, config.getLuceneQuery());
		
		try {
			cs = conAPI.search(config.getLuceneQuery(), 0, 0, "moddate", systemUser, false);
		} catch (Exception e) {
			Logger.error(PushPublisherBundler.class,e.getMessage(),e);
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()" + e.getMessage() + ": Unable to pull content with query " + config.getLuceneQuery(), e);
		}
		
		status.setTotal(cs.size());
		
		for (Contentlet con : cs) {
			try {
				writeFileToDisk(bundleRoot, con);
				status.addCount();
			} catch (Exception e) {
				Logger.error(PushPublisherBundler.class,e.getMessage() + " : Unable to write file",e);
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
				
				if(file != null) {
					byte[] bytes=org.apache.commons.io.FileUtils.readFileToByteArray(file);
					
					Map<String, Object> bufferMap =  new HashMap<String, Object>();
					bufferMap.put(file.getName(), bytes);
					contentMap.put(ff.getVelocityVarName(), bufferMap);
				} else {
					contentMap.put(ff.getVelocityVarName(), null);
				}
		    } else {
		    	contentMap.put(ff.getVelocityVarName(), con.get(ff.getVelocityVarName()));
		    }
			
		}
		
		PushAssetWrapper wrapper=new PushAssetWrapper();
	    wrapper.setContentMap(contentMap);
	    
	    XStream xstream=new XStream(new DomDriver());
	    xstream.registerConverter(new EncodedByteArrayConverter());
	    
	    String myFile = bundleRoot.getPath() + File.separator 
				+ con.getInode()
				+ XML_EXTENSION;
	    
	    //Write to a file in the file system
        try {
            FileOutputStream fs = new FileOutputStream(myFile);
            xstream.toXML(wrapper, fs);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }		
	}

	@Override
	public FileFilter getFileFilter() {
		// TODO Auto-generated method stub
		return null;
	}

}

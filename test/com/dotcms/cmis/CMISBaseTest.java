package com.dotcms.cmis;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.repackage.chemistry_opencmis_commons_api_0_8_0.org.apache.chemistry.opencmis.commons.PropertyIds;
import com.dotcms.repackage.chemistry_opencmis_commons_api_0_8_0.org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import com.dotcms.repackage.chemistry_opencmis_commons_api_0_8_0.org.apache.chemistry.opencmis.commons.data.ObjectList;
import com.dotcms.repackage.chemistry_opencmis_commons_api_0_8_0.org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import com.dotcms.repackage.chemistry_opencmis_commons_api_0_8_0.org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import com.dotcms.repackage.chemistry_opencmis_commons_api_0_8_0.org.apache.chemistry.opencmis.commons.enums.VersioningState;
import com.dotcms.repackage.chemistry_opencmis_commons_impl_0_8_0.org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import com.dotcms.repackage.chemistry_opencmis_commons_impl_0_8_0.org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import com.dotcms.repackage.chemistry_opencmis_commons_impl_0_8_0.org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import com.dotcms.repackage.chemistry_opencmis_commons_impl_0_8_0.org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import com.dotcms.repackage.chemistry_opencmis_commons_api_0_8_0.org.apache.chemistry.opencmis.commons.server.CallContext;
import com.dotcms.repackage.chemistry_opencmis_commons_api_0_8_0.org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import com.dotcms.repackage.commons_io_2_0_1.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.junit_4_8_1.org.junit.AfterClass;
import com.dotcms.repackage.junit_4_8_1.org.junit.BeforeClass;

import com.dotcms.TestBase;
import com.dotcms.enterprise.cmis.server.CMISManager;
import com.dotcms.enterprise.cmis.server.CMISService;
import com.dotcms.enterprise.cmis.utils.CMISUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class CMISBaseTest extends TestBase {

	protected static User user;
	protected static List<Contentlet> contentlets;
	protected static CallContext callContext;
	protected static ObjectInfoHandler objectInfos;
	protected static String repoId;
	protected static String rootPath;
	protected static CMISService dotRepo;
	protected static CMISManager cmisManager;
	
    @BeforeClass
    public static void prepare () throws DotSecurityException, DotDataException {

        //Setting the test user
        user = APILocator.getUserAPI().getSystemUser();
        repoId = CMISUtils.REPOSITORY_ID;
        rootPath = CMISUtils.ROOT_PATH;
        cmisManager = new CMISManager();
        cmisManager.createAndInitRepository();
        
        contentlets = new ArrayList<Contentlet>();
        Map<String, String> map = new HashMap<String, String>();
        dotRepo = new CMISService(map, cmisManager);
        callContext = new DotCallContextObjectHandler();
        objectInfos = new DotCallContextObjectHandler();
        dotRepo.setCallContext(callContext);
    }
    
    protected static String getdefaultHostId() throws Exception {
        return getRootFolderChildren().getObjects().get(0).getObject().getId();
    }
    
    protected static ObjectInFolderList getRootFolderChildren(){
		return dotRepo.getChildren(CMISUtils.REPOSITORY_ID,
				CMISUtils.ROOT_ID, "", "", Boolean.valueOf(false),
				IncludeRelationships.NONE, "", Boolean.valueOf(false),
				BigInteger.valueOf(1000), BigInteger.valueOf(0), null);
    }

    @AfterClass
    public static void afterClass () throws Exception {
        // TODO cleanup tasks if any
    }

    protected static String createFile ( URL resource, String fileName , String folderId ) throws Exception {

        //Creates a temporal folder where to put the content
        final String runId = UUIDGenerator.generateUuid();
        final java.io.File tmpDir = new java.io.File( APILocator.getFileAPI().getRealAssetPathTmpBinary() + java.io.File.separator + runId );
        tmpDir.mkdirs();

        final java.io.File resourceFile = new java.io.File( tmpDir, fileName );
        FileUtils.copyURLToFile( resource, resourceFile );

        //Reading the file
        if ( !resourceFile.exists() ) {
            String message = "File " + fileName + " does not exist.";
            throw new Exception( message );
        }
        
        ContentStreamImpl contentStream = new ContentStreamImpl();
        contentStream.setFileName(fileName + new java.util.Date().getTime());
        contentStream.setLength(BigInteger.valueOf(resourceFile.length()));
        contentStream.setMimeType(APILocator.getFileAPI().getMimeType(fileName));
        contentStream.setStream(new FileInputStream(resourceFile));
                
        PropertiesImpl result = new PropertiesImpl();
        result.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value()));
        result.addProperty(new PropertyStringImpl(PropertyIds.NAME, fileName));

        if(!UtilMethods.isSet(folderId))
        	folderId = getdefaultHostId();
        
		return dotRepo.createDocument(CMISUtils.REPOSITORY_ID, result,
				folderId, contentStream, VersioningState.MAJOR, null, null,
				null, null);
    }
    
    protected static String createFolder( String folderName ) throws Exception {
        PropertiesImpl props = new PropertiesImpl();
        props.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value()));
        props.addProperty(new PropertyStringImpl(PropertyIds.NAME, folderName));
		return dotRepo.createFolder(CMISUtils.REPOSITORY_ID, props,
				getdefaultHostId(), null, null, null, null);
    }
    
    protected static ObjectList doQuery(String query){
		return dotRepo.query(CMISUtils.REPOSITORY_ID, query,
				Boolean.valueOf(false), Boolean.valueOf(false),
				IncludeRelationships.NONE, "", BigInteger.valueOf(1000),
				BigInteger.valueOf(0), null);
    }
}

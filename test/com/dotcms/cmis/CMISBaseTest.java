package com.dotcms.cmis;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.dotcms.TestBase;
import com.dotcms.enterprise.cmis.server.CMISManager;
import com.dotcms.enterprise.cmis.server.CMISService;
import com.dotcms.enterprise.cmis.utils.CMISUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

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

    protected static String createFile ( String fileName , String folderId ) throws Exception {

        String testFilesPath = ".." + java.io.File.separator +
                "test" + java.io.File.separator +
                "com" + java.io.File.separator +
                "dotmarketing" + java.io.File.separator +
                "portlets" + java.io.File.separator +
                "contentlet" + java.io.File.separator +
                "business" + java.io.File.separator +
                "test_files" + java.io.File.separator;
        
        //Reading the file
        String testFilePath = FileUtil.getRealPath( testFilesPath + fileName );
        java.io.File tempTestFile = new java.io.File( testFilePath );
        if ( !tempTestFile.exists() ) {
            String message = "File does not exist: '" + testFilePath + "'";
            throw new Exception( message );
        }
        
        ContentStreamImpl contentStream = new ContentStreamImpl();
        contentStream.setFileName(fileName + new java.util.Date().getTime());
        contentStream.setLength(BigInteger.valueOf(testFilePath.length()));
        contentStream.setMimeType(APILocator.getFileAPI().getMimeType(fileName));
        contentStream.setStream(new FileInputStream(tempTestFile));
                
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

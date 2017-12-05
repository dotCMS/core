package com.dotcms.cmis;

import com.dotcms.IntegrationTestBase;
import com.dotcms.enterprise.cmis.server.CMISManager;
import com.dotcms.enterprise.cmis.server.CMISService;
import com.dotcms.enterprise.cmis.utils.CMISUtils;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.PropertyIds;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.data.ObjectList;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.enums.VersioningState;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.server.CallContext;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @deprecated CMIS is deprecated and slated for removal.
 */
@Deprecated
public class CMISBaseTest extends IntegrationTestBase {

	protected static User user;
	protected static List<Contentlet> contentlets;
	protected static CallContext callContext;
	protected static ObjectInfoHandler objectInfos;
	protected static String repoId;
	protected static String rootPath;
	protected static CMISService dotRepo;
	protected static CMISManager cmisManager;
	
    @BeforeClass
    public static void prepare () throws Exception {
    	
    	//Setting web app environment
        IntegrationTestInitService.getInstance().init();

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
        final java.io.File tmpDir = new java.io.File( APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + java.io.File.separator + runId );
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
        contentStream.setMimeType(APILocator.getFileAssetAPI().getMimeType(fileName));
        contentStream.setStream(Files.newInputStream(resourceFile.toPath()));
                
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

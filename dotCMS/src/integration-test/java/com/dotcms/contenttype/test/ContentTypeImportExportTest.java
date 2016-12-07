package com.dotcms.contenttype.test;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotcms.repackage.com.google.common.io.Files;

public class ContentTypeImportExportTest {

    final ContentTypeFactory factory = new ContentTypeFactoryImpl();

    @BeforeClass
    public static void SetUpTests() throws FileNotFoundException, Exception {
        SuperContentTypeTest.SetUpTests();
    }

    
    
    @Test
    public void testExport() throws Exception {

        File temp = Files.createTempDir();
        
        System.out.println(temp.getCanonicalFile());
        System.out.println(temp.getCanonicalFile());
        System.out.println(temp.getCanonicalFile());
        System.out.println("---------------------------");
        new ContentTypeImportExportUtil().exportContentTypes(temp);

        
        //testImport(temp);
        
    }
    

    public void testImport(File temp) throws Exception {


        
        new ContentTypeImportExportUtil().importContentTypes(temp);

        
        
        
    }
}

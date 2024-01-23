package com.dotcms.contenttype.test;

import java.io.File;
import org.junit.Test;

import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotcms.repackage.com.google.common.io.Files;

public class ContentTypeImportExportTest extends ContentTypeBaseTest {

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

	private void testImport(File temp) throws Exception {
		new ContentTypeImportExportUtil().importContentTypes(temp);
	}
}

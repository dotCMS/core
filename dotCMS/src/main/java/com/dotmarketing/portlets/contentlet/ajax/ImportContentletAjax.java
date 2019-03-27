package com.dotmarketing.portlets.contentlet.ajax;

import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;


public class ImportContentletAjax {
	
	public boolean checkImportStatus(Long id) {
		Boolean result = ImportAuditUtil.isImportfinished(id);
		return result;
	}
	
	public long cancelImport(Long importId) {
		ImportAuditUtil.cancelImport(importId);
		return importId;
	}

}

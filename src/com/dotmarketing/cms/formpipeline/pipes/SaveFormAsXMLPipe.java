package com.dotmarketing.cms.formpipeline.pipes;

import java.io.File;

import com.dotmarketing.cms.formpipeline.business.FormPipe;
import com.dotmarketing.cms.formpipeline.business.FormPipeBean;
import com.dotmarketing.util.Config;

public class SaveFormAsXMLPipe implements FormPipe {

	public void runForm(FormPipeBean bean) {
		File tmpDir = new File( Config.CONTEXT.getRealPath("/form_backups"));
		tmpDir.mkdir();
		String formPipe = bean.getFormPipe();
		File myFile = new File(tmpDir.getAbsolutePath() + File.separator + formPipe +"_" + System.currentTimeMillis() + ".xml");
	}

    
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public String getExampleUsage() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public String getTitle() {
        // TODO Auto-generated method stub
        return null;
    }

}

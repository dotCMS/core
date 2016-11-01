package com.dotmarketing.cms.formpipeline.pipes;

import java.util.Set;
import java.util.Map.Entry;

import com.dotmarketing.cms.formpipeline.business.FormPipe;
import com.dotmarketing.cms.formpipeline.business.FormPipeBean;
import com.dotmarketing.cms.formpipeline.business.FormPipeException;
import com.dotmarketing.util.FormSpamFilter;

public class SimpleAntiSpamPipe implements FormPipe {



    public void runForm(FormPipeBean bean) throws FormPipeException{

		StringBuffer sb = new StringBuffer();		
		Set<Entry<String,Object>> set = bean.entrySet();
		for(Entry<String,Object> e : set)
		{
			String value = bean.get(e.getKey()).toString();
			sb.append(value + " ");
		}
		if(FormSpamFilter.isSpamField(sb.toString())){
			bean.addErrorMessage("Spam Message Detected");
			throw new FormPipeException();
		}
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

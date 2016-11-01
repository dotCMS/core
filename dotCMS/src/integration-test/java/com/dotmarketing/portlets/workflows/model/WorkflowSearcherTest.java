package com.dotmarketing.portlets.workflows.model;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.IntegrationTestInitService;

public class WorkflowSearcherTest {
	
    @BeforeClass
    public static void prepare () throws Exception {
    	
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }
    
    /**
     * See https://github.com/dotCMS/dotCMS/issues/4134 Oracle specific
     * @throws Exception
     */
    @Test
    public void issue4134() throws Exception {
        Map<String,Object> params=new HashMap<String,Object>();
        params.put("open",true);
        params.put("assignedTo", APILocator.getUserAPI().getSystemUser());
        WorkflowSearcher searcher = new WorkflowSearcher(params, APILocator.getUserAPI().getSystemUser());
        
        searcher.findTasks();
    }
}

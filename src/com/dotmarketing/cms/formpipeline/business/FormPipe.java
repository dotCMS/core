package com.dotmarketing.cms.formpipeline.business;


public interface FormPipe {
	
	
public void runForm(FormPipeBean bean) throws FormPipeException;

public String getDescription() ;

public String getExampleUsage() ;

public String getTitle() ;
}

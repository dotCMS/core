package com.dotmarketing.cms.formpipeline.business;

public class FormPipeException extends Exception {
    private boolean rollBack = true;
    private boolean stopProcessing = true;
	public boolean isStopProcessing() {
        return stopProcessing;
    }

    public boolean isRollBack() {
        return rollBack;
    }


    public FormPipeException() {
		super();
		// TODO Auto-generated constructor stub
	}
    
    
    public FormPipeException(boolean stopProcessing, boolean rollBack) {
        super();
        rollBack = rollBack;
        stopProcessing = stopProcessing;
       
        // TODO Auto-generated constructor stub
    }

	public FormPipeException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public FormPipeException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public FormPipeException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}

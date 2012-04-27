package com.dotmarketing.logConsole.form;

import org.apache.struts.validator.ValidatorForm;

public class ConsoleLogForm extends ValidatorForm{
    
    private static final long serialVersionUID = 1L;
    
    private String[] logs;

    public String[] getLogs() {
        return logs;
    }

    public void setLogs(String[] logs) {
        this.logs = logs;
    }

}

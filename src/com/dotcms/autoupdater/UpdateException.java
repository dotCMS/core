package com.dotcms.autoupdater;


public class UpdateException extends Exception {

    private static final long serialVersionUID = 1L;
    private String type;

    public static final String ERROR = "ERROR";
    public static final String SUCCESS = "SUCCESS";
    public static final String CANCEL = "CANCEL";

    public String getType () {
        return type;
    }

    public UpdateException ( String string, String type ) {
        super( string );
        this.type = type;
    }

}
package com.tonicsystems.jarjar;

/**
 * @author Jonathan Gamba
 *         Date: 11/6/13
 */
public class Dependency {

    private boolean generate;
    private boolean renameServices;
    private String path;

    public Dependency () {
    }

    public String getPath () {
        return this.path;
    }

    public void setPath ( String path ) {
        this.path = path;
    }

    public boolean isGenerate () {
        return generate;
    }

    public void setGenerate ( boolean generate ) {
        this.generate = generate;
    }

    public boolean isRenameServices () {
        return renameServices;
    }

    public void setRenameServices ( boolean renameServices ) {
        this.renameServices = renameServices;
    }

}
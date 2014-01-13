package com.tonicsystems.jarjar;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 11/6/13
 */
public class Dependency {

    private boolean generate;
    private boolean renameServices;
    private String path;

    private List<Ignore> toIgnore = new ArrayList<Ignore>();

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

    public List<Ignore> getPackagesToIgnore () {
        return toIgnore;
    }

    public Ignore createIgnore () {
        Ignore ignore = new Ignore();
        toIgnore.add( ignore );

        return ignore;
    }

    public class Ignore {

        private String parentPackage;

        public String getParentPackage () {
            return parentPackage;
        }

        public void setParentPackage ( String parentPackage ) {
            this.parentPackage = parentPackage;
        }

    }

}
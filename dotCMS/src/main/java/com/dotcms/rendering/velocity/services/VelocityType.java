package com.dotcms.rendering.velocity.services;


import com.dotmarketing.util.UtilMethods;

public enum VelocityType {
    CONTAINER("container"), 
    CONTENT("contentlet"), 
    CONTENT_MAP("oldContentMap"), 
    FIELD("fields"), 
    HTMLPAGE("dotHtmlPage"), 
    TEMPLATE("templateLayout"), 
    CONTENT_TYPE("contentType"), 
    SITE("site"),
    VTL("vtl"), 
    VELOCITY_MACROS("vm"),
    VELOCITY_LEGACY_VL("vl"),
    NOT_VELOCITY("not_velocity");
    final public String fileExtension;

    VelocityType(String fileExtension) {
        this.fileExtension = fileExtension.toLowerCase();
    }


    public static VelocityType resolveVelocityType(final String path) {
;        final String extension = UtilMethods.getFileExtension(path);


        for (final VelocityType val : VelocityType.values()) {
            if (extension.equals(val.fileExtension)) {
                return val;
            }
        }
        return VelocityType.NOT_VELOCITY;
    }
}

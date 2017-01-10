package com.dotmarketing.tag.business;

import com.dotmarketing.util.web.WebDotcmsException;

import java.util.ResourceBundle;

/**
 * Created by freddyrodriguez on 17/3/16.
 */
public class InvalidTagNameLengthException extends WebDotcmsException {

    InvalidTagNameLengthException(String tagName){
        super(getKey( tagName ), tagName);
    }

    private static String getKey( String tagName ) {
        if (tagName != null){
            return "tag.save.error.invalid-tag-name.to-long";
        }else{
            return "tag.save.error.invalid-tag-name.mandotary";
        }
    }
}

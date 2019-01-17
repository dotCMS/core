package com.dotcms.rendering.velocity.events;

import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.util.introspection.Info;

/**
 * This is just a wrapper of {@link ParseErrorException} to identified when the parse error happens on edit o preview mode.
 * @author jsanca
 */
public class PreviewEditParseErrorException extends ParseErrorException {

    public PreviewEditParseErrorException(ParseErrorException exception) {
        super(exception.getMessage(),
                new Info(exception.getTemplateName(), exception.getLineNumber(), exception.getColumnNumber()),
                exception.getInvalidSyntax());
    }

    public PreviewEditParseErrorException(String exceptionMessage) {
        super(exceptionMessage);
    }

    public PreviewEditParseErrorException(ParseException pex, String templName) {
        super(pex, templName);
    }

    public PreviewEditParseErrorException(VelocityException pex, String templName) {
        super(pex, templName);
    }

    public PreviewEditParseErrorException(String exceptionMessage, Info info) {
        super(exceptionMessage, info);
    }

    public PreviewEditParseErrorException(String exceptionMessage, Info info, String invalidSyntax) {
        super(exceptionMessage, info, invalidSyntax);
    }
}
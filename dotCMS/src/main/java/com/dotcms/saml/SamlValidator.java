package com.dotcms.saml;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.XMLUtils;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.control.Try;

import java.util.Arrays;

/**
 * This SAML Validator validates the url and the metadata XML.
 * @author jsanca
 */
public class SamlValidator {

    /**
     * Validates an url
     * @param fieldName
     * @param value
     * @param userId
     */
    public static void validateURL(final String fieldName, final String value, final String userId) {

        boolean validateURL = false;

        if (null != value) {

            final String url = value.toLowerCase().startsWith("http")?value: "http://"+value;
            validateURL      = UtilMethods.isValidURL(url);
        }

        if (!validateURL) {

            final String message = Try.of(()->LanguageUtil.get("invalid.url", value, fieldName)).
                    getOrElse("URL "+ value + " is invalid for the field " + fieldName); // todo: add the host name
            final SystemMessage systemMessage = new SystemMessageBuilder().setMessage(message)
                    .setSeverity(MessageSeverity.WARNING).setLife(DateUtil.SEVEN_SECOND_MILLIS).create();
            SystemMessageEventUtil.getInstance().pushMessage(systemMessage, Arrays.asList(userId));
        }
    }

    /**
     * Validates a XML
     * @param fieldName
     * @param sourceXML
     * @param userId
     */
    public static void validateXML(final String fieldName, final String sourceXML, final String userId) {

        if (null == sourceXML || !XMLUtils.isValidXML(sourceXML)) {

            final String message = Try.of(()->LanguageUtil.get("invalid.xml", fieldName)).
                    getOrElse("XML is invalid for the field " + fieldName);
            final SystemMessage systemMessage = new SystemMessageBuilder().setMessage(message)
                    .setSeverity(MessageSeverity.WARNING).setLife(DateUtil.SEVEN_SECOND_MILLIS).create();
            SystemMessageEventUtil.getInstance().pushMessage(systemMessage, Arrays.asList(userId));
        }
    }

}

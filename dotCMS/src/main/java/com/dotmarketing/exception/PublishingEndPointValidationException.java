package com.dotmarketing.exception;

import com.google.common.collect.Lists;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Exception} to be use as Validation of {@link com.dotcms.publisher.endpoint.bean.PublishingEndPoint}
 * implementations.
 */
public class PublishingEndPointValidationException extends Exception {

    private List<Message> i18nmessages;

    public PublishingEndPointValidationException(final String i18nmessage) {
        this.i18nmessages = Lists.newArrayList(new Message(i18nmessage));
    }

    private PublishingEndPointValidationException(final List<Message> i18nmessages) {
        this.i18nmessages = i18nmessages;
    }

    public String getMessage(final User user) {
        final List<String> errorMessages = Lists.newArrayList();

        for (final Message i18nMessage : i18nmessages) {
            try {
                errorMessages.add(LanguageUtil.get(user, i18nMessage.getTemplate(), i18nMessage.getArguments()));
            } catch (LanguageException le) {
                //If we have a problem, at least display the message code.
                errorMessages.add(i18nMessage.getTemplate());
            }
        }
        return String.join(",", errorMessages);
    }

    public static class Builder {
        final List<Message> messages = new ArrayList<>();

        public void addMessage(final String template, final String... arguments){
            messages.add(new Message(template, arguments));
        }

        public boolean isEmpty() {
            return messages.isEmpty();
        }

        public PublishingEndPointValidationException build(){
            return new PublishingEndPointValidationException(messages);
        }
    }

    private static class Message {
        final String template;
        final String[] arguments;

        public Message(String template, String... arguments) {
            this.template = template;
            this.arguments = arguments;
        }

        public String getTemplate() {
            return template;
        }

        public String[] getArguments() {
            return arguments;
        }
    }
}

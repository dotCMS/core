package com.liferay.util.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Curione
 * Class to represent a Session Message to be displayed in a complex Dialog rather than a String message
 * Use this Class to display an error for a List of objects , where each object contains multiple elements or messages to be displayed.
 *
 * I.Example. Attempting to delete a list of Templates.
 * If a template is being used by multiple pages, then this object will help display a Dependencies error, such as:
 * Title: General title for the Dialog
 * Error: Error describing that the Templates cannot be deleted because they are in use by Pages
 * Map<String, List<String>>. The Key is the Template Name, the list contains the Pages dependant upon that template
 * Footer: any additional information
 *
 */
public class SessionDialogMessage {

    private String title;
    private String error;
    private Map<String, List<String>> messages;
    private String footer;

    public SessionDialogMessage (String title, String error, String footer) {
        this.title = title;
        this.error = error;
        this.messages = new HashMap<>();
        this.footer = footer;
    }

    public void addMessage(String key) {
        this.messages.put(key, new ArrayList<>());
    }
    public void addMessage(String key, String message) {
        if (this.messages.containsKey(key)) {
            this.messages.get(key).add(message);
        } else {
            this.messages.put(key, new ArrayList<>(Arrays.asList(message)));
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, List<String>> getMessages() {
        return messages;
    }

    public void setMessages(Map<String, List<String>> messages) {
        this.messages = messages;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }
}

package com.tonicsystems.jarjar;

/**
 * @author Jonathan Gamba
 *         Date: 12/11/13
 */
public class NamingRule {

    private String pattern;
    private String replacement;

    public NamingRule () {
    }

    public NamingRule ( String pattern, String replacement ) {
        this.pattern = pattern;
        this.replacement = replacement;
    }

    public String getPattern () {
        return pattern;
    }

    public void setPattern ( String pattern ) {
        this.pattern = pattern;
    }

    public String getReplacement () {
        return replacement;
    }

    public void setReplacement ( String replacement ) {
        this.replacement = replacement;
    }

}
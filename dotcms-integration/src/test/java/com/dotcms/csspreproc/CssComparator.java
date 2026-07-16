package com.dotcms.csspreproc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class to compare CSS strings semantically, regardless of formatting differences.
 */
public class CssComparator {

    /**
     * Compares two CSS strings semantically, ignoring formatting differences.
     *
     * @param css1 First CSS string to compare
     * @param css2 Second CSS string to compare
     * @return true if the CSS strings are semantically equivalent
     */
    public static boolean areSemanticallyEqual(String css1, String css2) {
        if (css1 == null && css2 == null) return true;
        if (css1 == null || css2 == null) return false;

        // Option 1: Compare minified versions
        String minified1 = minifyCss(css1);
        String minified2 = minifyCss(css2);
        if (minified1.equals(minified2)) return true;

        // Option 2: Compare structurally
        List<CssRule> rules1 = parseCssRules(css1);
        List<CssRule> rules2 = parseCssRules(css2);

        if (rules1.size() != rules2.size()) return false;

        for (int i = 0; i < rules1.size(); i++) {
            CssRule rule1 = rules1.get(i);
            CssRule rule2 = rules2.get(i);

            if (!rule1.selector.equals(rule2.selector)) return false;
            if (rule1.properties.size() != rule2.properties.size()) return false;

            for (int j = 0; j < rule1.properties.size(); j++) {
                CssProperty prop1 = rule1.properties.get(j);
                CssProperty prop2 = rule2.properties.get(j);

                if (!prop1.name.equals(prop2.name)) return false;
                if (!prop1.value.equals(prop2.value)) return false;
            }
        }

        return true;
    }

    /**
     * Minifies a CSS string by removing unnecessary whitespace and normalizing syntax.
     *
     * @param css CSS string to minify
     * @return Minified CSS string
     */
    private static String minifyCss(String css) {
        // Remove comments
        css = css.replaceAll("/\\*[^*]*\\*+([^/*][^*]*\\*+)*/", "");

        // Remove unnecessary whitespace
        css = css.replaceAll("\\s+", " ");
        css = css.replaceAll("\\s*\\{\\s*", "{");
        css = css.replaceAll("\\s*\\}\\s*", "}");
        css = css.replaceAll("\\s*;\\s*", ";");
        css = css.replaceAll("\\s*:\\s*", ":");
        css = css.replaceAll("\\s*,\\s*", ",");

        // Ensure all blocks end with semicolon
        css = css.replaceAll("\\}\\}", "\\};");

        // Remove semicolon before closing brace
        css = css.replaceAll(";\\}", "}");

        // Remove leading and trailing spaces
        return css.trim();
    }

    /**
     * Parses a CSS string into a structured representation of rules.
     *
     * @param css CSS string to parse
     * @return List of CSS rules
     */
    private static List<CssRule> parseCssRules(String css) {
        List<CssRule> rules = new ArrayList<>();

        // Pattern to extract CSS rules
        Pattern rulePattern = Pattern.compile("([^\\{]+)\\{([^\\}]+)\\}");
        Matcher ruleMatcher = rulePattern.matcher(css);

        while (ruleMatcher.find()) {
            String selector = ruleMatcher.group(1).trim();
            String propertiesText = ruleMatcher.group(2);

            CssRule rule = new CssRule(selector);

            // Pattern to extract properties
            Pattern propPattern = Pattern.compile("([^:]+):([^;]+);?");
            Matcher propMatcher = propPattern.matcher(propertiesText);

            while (propMatcher.find()) {
                String propName = propMatcher.group(1).trim();
                String propValue = propMatcher.group(2).trim();
                rule.addProperty(propName, propValue);
            }

            rules.add(rule);
        }

        return rules;
    }

    /**
     * Represents a CSS rule with a selector and its properties.
     */
    private static class CssRule {
        String selector;
        List<CssProperty> properties = new ArrayList<>();

        CssRule(String selector) {
            this.selector = selector;
        }

        void addProperty(String name, String value) {
            properties.add(new CssProperty(name, value));
        }
    }

    /**
     * Represents a CSS property with name and value.
     */
    private static class CssProperty {
        String name;
        String value;

        CssProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

}
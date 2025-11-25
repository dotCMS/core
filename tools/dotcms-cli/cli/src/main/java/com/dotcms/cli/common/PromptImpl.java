package com.dotcms.cli.common;

import io.quarkus.arc.DefaultBean;
import java.io.Console;
import java.util.Optional;
import jakarta.enterprise.context.Dependent;
import picocli.CommandLine.Help;

@DefaultBean
@Dependent
public class PromptImpl implements Prompt {

    static Help.ColorScheme colorScheme = Help.defaultColorScheme(Help.Ansi.AUTO);

    /**
     * Utility to prompt users for a yes/no answer.
     * The utility will prompt user in a loop until it gets a yes/no or blank response.
     *
     * @param defaultValue The value to return if user provides a blank response.
     * @param prompt The text to display
     * @param args Formatting args for the prompt
     * @return true if user replied with `y` or `yes`, false if user provided `n` or `no`, defaultValue if user provided empty
     *         response.
     */
    public boolean yesOrNo(boolean defaultValue, String prompt, String... args) {
        final String choices = defaultValue ? " (Y/n)" : " (y/N)";
        while (true) {
            try {
                Optional<Console> console = Optional.ofNullable(System.console());
                String response = console
                        .map(c -> c.readLine( withColor( prompt + choices), args).trim().toLowerCase())
                        .orElse(defaultValue ? "y" : "n");
                if (response.isBlank()) {
                    return defaultValue;
                }
                if (response.equals("y") || response.equals("yes")) {
                    return true;
                }
                if (response.equals("n") || response.equals("no")) {
                    return false;
                }
            } catch (Exception ignore) {
                return defaultValue;
            }
        }
    }

    /**
     * Utility to prompt user for input.
     * @param defaultValue The default value to return if user provides a blank response.
     * @param prompt The text to display
     * @param args Formatting args for the prompt
     * @return The string value of the user input or the defaultValue if user provided a blank response.
     */
    public String readInput(String defaultValue, String prompt, String... args) {
        try {
            Optional<Console> console = Optional.ofNullable(System.console());
            String response = console
                    .map(c -> c.readLine( withColor(prompt), args).trim().toLowerCase())
                    .orElse(defaultValue);
            if (response.isBlank()) {
                return defaultValue;
            }
            return response;
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    /**
     * Utility to prompt user for input.
     * @param defaultValue The default value to return if user provides a blank response.
     * @param prompt The text to display
     * @param args Formatting args for the prompt
     * @return The integer value of the user input or the defaultValue if user provided a blank response.
     */
    public int readInput(int defaultValue, String prompt, String... args) {
        while(true) {
            try {
                Optional<Console> console = Optional.ofNullable(System.console());
                String response = console
                        .map(c -> c.readLine(withColor(prompt), args).trim()
                                .toLowerCase())
                        .orElse(String.valueOf(defaultValue));
                if (response.isBlank()) {
                    return defaultValue;
                }
                return Integer.parseInt(response);
            } catch (Exception ignore) {
                //loop until we get a valid response
            }
        }
    }

    /**
     * Reads a password from the user.
     *
     * @param prompt The prompt message displayed to the user.
     * @param args   Additional format arguments for the prompt message.
     * @return The password entered by the user as a char[].
     */
    public char[] readPassword(String prompt, String... args) {
        try {
            Optional<Console> console = Optional.ofNullable(System.console());
            var response = console
                    .map(c -> c.readPassword(withColor(prompt), args))
                    .orElse(null);
            if (response == null || response.length == 0) {
                return new char[0];
            }
            return response;
        } catch (Exception ignore) {
            return new char[0];
        }
    }


    /**
     * Add color to the text
     * @param text The text to colorize
     * @return The colorized text
     */
    static String withColor(final String text) {
        return colorScheme.ansi().new Text(text, colorScheme).toString();
    }


}

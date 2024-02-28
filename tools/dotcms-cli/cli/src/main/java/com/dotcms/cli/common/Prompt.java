package com.dotcms.cli.common;

public interface Prompt {

    /**
     * Utility to prompt users for a yes/no answer.
     * @param defaultValue
     * @param prompt
     * @param args
     * @return
     */
    boolean yesOrNo(boolean defaultValue, String prompt, String... args);

    /**
     * Utility to prompt user for input.
     * @param defaultValue
     * @param prompt
     * @param args
     * @return
     */
    String readInput(String defaultValue, String prompt, String... args);

    /**
     * Utility to prompt user for input.
     * @param defaultValue
     * @param prompt
     * @param args
     * @return
     */
    int readInput(int defaultValue, String prompt, String... args);

}

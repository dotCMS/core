package com.dotcms.cli.common;

/**
 * The ApplyCommandOrder enum represents the order of execution for the subcommands on the global
 * push and pull commands.
 */
public enum ApplyCommandOrder {

    LANGUAGE(0),
    SITE(1),
    CONTENT_TYPE(2),
    FILES(3);

    private final int order;

    ApplyCommandOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }

}

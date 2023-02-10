package com.dotcms.cli.common;

import picocli.CommandLine;

public class FilterOptionsMixin {

        @CommandLine.Option(names = {"-f","--filter"}, order = 41, description = "Specify name to search by. ")
        String name;

        @CommandLine.Option(names = {"-a", "--archived"}, description = "Show archived sites.", defaultValue = "false")
        Boolean archived;

        @CommandLine.Option(names = {"-l", "--live"}, description = "Show live sites.", defaultValue = "true")
        Boolean live;

        @CommandLine.Option(names = {"-p", "--page"}, description = "Page Number.", defaultValue = "1")
        Integer page;

        @CommandLine.Option(names = {"-s", "--pageSize"}, description = "Items per page.", defaultValue = "25")
        Integer pageSize;

        @CommandLine.Option(names = {"-by", "--orderBy"},
                order=43,
                description = "Set an order by param. (variable is default) ", defaultValue = "variable")
        String orderBy;

}

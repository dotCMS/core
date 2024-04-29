package com.dotcms.cli.common;

import com.dotcms.cli.command.EntryCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

/**
 * This class is meant to help organize the subcommands and their results
 * It will process the result of a command execution and return a CommandsChain object
 * The object offers a convenient way to navigate the subcommands and their results and to check if help was requested at any level
 * The EntryCommand is the main command, and it's not considered a subcommand therefore it's excluded from the CommandsChain
 */
public class SubcommandProcessor {

    private SubcommandProcessor() {
        //Utility class
    }

    /**
     * This method will process the result of a command execution and return a CommandsChain object
     * @param subcommand the result of the command execution
     * @return an Optional of CommandsChain
     */
    public static Optional<CommandsChain> process(final ParseResult subcommand){

        //If We're Looking only at EntryCommand this means that we're only displaying The main help screen and no subcommand therefore no need to process
        if(null == subcommand || EntryCommand.NAME.equals(subcommand.commandSpec().name()) && !subcommand.hasSubcommand()){
            return Optional.empty();
        }

        //This is a sub command
        boolean isHelpRequestedAny = false; //This will be true if any subcommand requested help
        boolean isShowErrors = false; //This will be true if any subcommand requested to show errors
        List<ParseResult> subcommands = new ArrayList<>();

        boolean isRemoteURLSet = subcommand.expandedArgs().stream()
                .anyMatch(arg -> arg.contains("--dotcms-url"));
        boolean isTokenSet = subcommand.expandedArgs().stream()
                .anyMatch(arg -> arg.contains("--token"));

        ParseResult current = subcommand;
        while (current != null) {
            isShowErrors = isShowErrors || (current.matchedOption("--errors") != null);
            isHelpRequestedAny = isHelpRequestedAny || current.isUsageHelpRequested();
            //We're only interested in subcommands that are not the main command
            if(!isMainCommand(current)) {
                subcommands.add(current);
            }
            current = current.subcommand();
        }

        final List<String> collect = subcommands.stream().map(ParseResult::commandSpec)
                .map(CommandSpec::name).collect(
                        Collectors.toList());

        return Optional.of(
                CommandsChain.builder().
                        subcommands(subcommands).
                        isShowErrorsAny(isShowErrors).
                        isHelpRequestedAny(isHelpRequestedAny).
                        isRemoteURLSet(isRemoteURLSet).
                        isTokenSet(isTokenSet).
                        command(String.join(" ", collect)).
                        build()
        );

    }

    /**
     * This method will return true if the subcommand is the main command
     * @param subcommand the result of the command execution
     * @return true if the subcommand is the main command
     */
    static boolean isMainCommand(ParseResult subcommand){
        return EntryCommand.NAME.equals(subcommand.commandSpec().name());
    }


}

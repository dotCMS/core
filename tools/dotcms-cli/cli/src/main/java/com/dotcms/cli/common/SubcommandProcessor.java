package com.dotcms.cli.common;

import com.dotcms.api.client.model.AuthenticationParam;
import com.dotcms.api.client.model.RemoteURLParam;
import com.dotcms.cli.command.EntryCommand;
import io.quarkus.arc.Arc;
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

    /**
     * This method will process the result of a command execution and return a CommandsChain object
     * @param subcommand the result of the command execution
     * @return an Optional of CommandsChain
     */
    public Optional<CommandsChain> process(final ParseResult subcommand){

        //If We're Looking only at EntryCommand this means that we're only displaying The main help screen and no subcommand therefore no need to process
        if(null == subcommand || EntryCommand.NAME.equals(subcommand.commandSpec().name()) && !subcommand.hasSubcommand()){
            return Optional.empty();
        }

        //This is a sub command
        boolean isHelpRequestedAny = false; //This will be true if any subcommand requested help
        boolean isShowErrors = false; //This will be true if any subcommand requested to show errors
        boolean watchMode = false; //This will be true if any subcommand requested to watch
        List<ParseResult> subcommands = new ArrayList<>();

        // Get all instances that implement DotPush
        final var remoteURLParam = Arc.container().instance(RemoteURLParam.class).get();
        final var authenticationParam = Arc.container().instance(AuthenticationParam.class).get();

        //This will be true if the dotCMS URL was set
        final var isRemoteURLSet = remoteURLParam.getURL().isPresent();
        //This will be true if the token was set
        final var isTokenSet = authenticationParam.getToken().isPresent();

        ParseResult current = subcommand;
        while (current != null) {
            isShowErrors = isShowErrors || (current.matchedOption("--errors") != null);
            isHelpRequestedAny = isHelpRequestedAny || current.isUsageHelpRequested();
            watchMode = watchMode || (current.matchedOption("--watch") != null);
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
                        isWatchMode(watchMode).
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
    boolean isMainCommand(ParseResult subcommand){
        return EntryCommand.NAME.equals(subcommand.commandSpec().name());
    }


}

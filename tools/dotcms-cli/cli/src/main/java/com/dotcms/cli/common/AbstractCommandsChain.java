package com.dotcms.cli.common;

import com.dotcms.model.annotation.ValueType;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import org.immutables.value.Value.Derived;
import picocli.CommandLine.ParseResult;

@ValueType
@Value.Immutable
public interface AbstractCommandsChain {

    List<ParseResult> subcommands();

    @Derived
    default Optional<ParseResult> lastSubcommand() {
        return subcommands().isEmpty() ? Optional.empty() : Optional.of(subcommands().get(subcommands().size() - 1));
    }

    @Derived
    default Optional<ParseResult> firstSubcommand(){
        return subcommands().isEmpty() ? Optional.empty() : Optional.of(subcommands().get(0));
    }

    boolean isHelpRequestedAny();

    boolean isShowErrorsAny();

    boolean isWatchMode();
    /**
     * Marks if the remote URL was set in the command line
     *
     * @return true if the remote URL was set
     */
    boolean isRemoteURLSet();

    /**
     * Marks if the token was set in the command line
     *
     * @return true if the token was set
     */
    boolean isTokenSet();

    String command();

}

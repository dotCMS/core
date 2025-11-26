package com.dotcms.cli.command.files;

import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.cli.common.AuthenticationMixin;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.WorkspaceManager;
import java.util.HashSet;
import java.util.Set;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import picocli.CommandLine;

public abstract class AbstractFilesCommand {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected AuthenticationMixin authenticationMixin;

    @CommandLine.Mixin
    protected HelpOptionMixin helpOption;

    @Inject
    protected RestClientFactory clientFactory;

    @Inject
    protected WorkspaceManager workspaceManager;

    @Inject
    Logger logger;

    /**
     * Parses the pattern option string into a set of patterns.
     *
     * @param patterns the pattern option string containing patterns separated by commas
     * @return a set of parsed patterns
     */
    protected Set<String> parsePatternOption(String patterns) {

        var patternsSet = new HashSet<String>();

        if (patterns == null) {
            return patternsSet;
        }

        for (String pattern : patterns.split(",")) {
            patternsSet.add(pattern.trim());
        }

        return patternsSet;
    }

}

package com.dotcms.cli.common;

import com.dotcms.api.client.AuthenticationParam;
import io.quarkus.arc.Arc;
import java.util.Stack;
import picocli.CommandLine;
import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * This class is used to pass the token from the CLI to the API client.
 * If the token is present here we use directly
 */
public class AuthenticationMixin {

    @CommandLine.Option(names = {"--token"},
            description = {
                    "A dot CMS token to use for authentication. "}, parameterConsumer = TokenConsumer.class)
    String token;

    /**
     * Here we set the token in the {@link AuthenticationParam} instance.
     */
    static class TokenConsumer implements IParameterConsumer {
        @Override
        public void consumeParameters(Stack<String> args, ArgSpec argSpec, CommandSpec commandSpec) {
            final String token = args.pop();
            if (null != token && !token.isEmpty()) {
                final AuthenticationParam authentication = Arc.container()
                        .instance(AuthenticationParam.class).get();
                authentication.setToken(token.toCharArray());
            }
        }
    }

}

package com.dotcms.cli.common;

import com.dotcms.api.client.ParamAuthentication;
import io.quarkus.arc.Arc;
import java.util.Stack;
import picocli.CommandLine;
import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * This class is used to pass the token from the CLI to the API client. If the
 */
public class AuthenticationMixin {

    @CommandLine.Option(names = {"--token"},
            description = {
                    "A dot CMS token to use for authentication. "}, parameterConsumer = TokenConsumer.class)
    String token;

    static class TokenConsumer implements IParameterConsumer {

        @Override
        public void consumeParameters(Stack<String> args, ArgSpec argSpec,
                CommandSpec commandSpec) {
            final String token = args.pop();
            if (null != token && !token.isEmpty()) {
                ParamAuthentication authentication = Arc.container()
                        .instance(ParamAuthentication.class).get();
                authentication.setToken(token.toCharArray());
            }
        }
    }

}

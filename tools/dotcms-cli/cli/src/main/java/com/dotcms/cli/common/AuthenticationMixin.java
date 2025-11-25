package com.dotcms.cli.common;

import com.dotcms.api.client.model.AuthenticationParam;
import com.dotcms.api.client.model.RemoteURLParam;
import io.quarkus.arc.Arc;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;
import picocli.CommandLine;
import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * This class is used to pass the token and/or dotCMS URL from the CLI to the API client.
 * If the token and/or dotCMS URL are present, we use them directly.
 */
public class AuthenticationMixin {

    @CommandLine.Option(names = {"--token","-tk"},
            description = {
                    "A dotCMS token to use for authentication. "}, parameterConsumer = TokenConsumer.class)
    String token;

    @CommandLine.Option(names = {"--dotcms-url"},
            description = {"The dotCMS URL to connect to. This option must be used "
                    + "along with the token option, which provides the token for "
                    + "the specified dotCMS URL."},
            parameterConsumer = RemoteURLConsumer.class)
    String remoteURL;

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

    /**
     * Here we set the url in the {@link RemoteURLParam} instance.
     */
    static class RemoteURLConsumer implements IParameterConsumer {

        @Override
        public void consumeParameters(Stack<String> args, ArgSpec argSpec,
                CommandSpec commandSpec) {

            final String remoteURL = args.pop();
            if (null != remoteURL && !remoteURL.isEmpty()) {

                final var remoteURLParam = Arc.container().instance(RemoteURLParam.class).get();
                try {
                    remoteURLParam.setURL(new URL(remoteURL));
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(
                            String.format("Invalid dotCMS URL [%s]", remoteURL), e);
                }
            }
        }
    }

}

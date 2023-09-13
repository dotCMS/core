package com.dotcms.cli.common;

import java.util.Stack;
import picocli.CommandLine;
import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

public class AuthenticationMixin {

    @CommandLine.Option(names = {"--token"},
            description = {"A dot CMS token to use for authentication. "}, parameterConsumer = TokenConsumer.class)
    String token;

    static class TokenConsumer implements IParameterConsumer {
        @Override
        public void consumeParameters(Stack<String> args, ArgSpec argSpec, CommandSpec commandSpec) {
            String token = args.pop();
            System.out.println("Token: " + token);
            argSpec.setValue(token);
        }
    }

}

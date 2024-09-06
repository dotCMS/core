package com.dotcms.cli.command;


import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import java.io.IOException;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = LoginCommand.NAME,
        header = "@|bold,blue Use this command to login to a dotCMS instance.|@",
        description = {
                "This command is used to either login with a user/password combination",
                "or directly using a token. These options are mutually exclusive and",
                "only one should be provided. If none is provided, interactive mode will be used.",
                "Note: For security reasons, it's recommended to use interactive mode",
                "for entering passwords. Input through command line may be stored in console history.",
                "" // empty line left here on purpose to make room at the end
        },
        usageHelpAutoWidth = true,
        synopsisHeading = "",
        customSynopsis = {
                "",
                "Usage: login",
                "       login [-u <user> -p <password>]",
                "       login [-tk <token>]",
                ""
        },
        footer = {
                "",
                "Examples:",
                "  login                           # Interactive mode",
                "  login -tk=token                 # Login using token",
                "  login -tk                       # Interactive token",
                "  login -u=username -p=password   # Login using user/password",
                "  login -u=username               # Login using username, interactive password",
                "  login -u -p                     # Interactive username and password"
        }
)
public class LoginCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "login";

    public static final String PROMPT_USERNAME = "Username: ";
    public static final String PROMPT_PASSWORD = "Password: ";
    public static final String PROMPT_TOKEN = "Token: ";
    public static final String FORMAT_TOKEN_LOGGED_IN =
            "@|bold,green Successfully logged-in with token|@";
    public static final String FORMAT_USER_LOGGED_IN =
            "@|bold,green Successfully logged-in as |@[@|bold,blue %s|@]";

    /**
     * Here we encapsulate the password options
     */
    static class PasswordOptions {

        @CommandLine.Option(names = {"-u", "--user"}, arity = "0..1", description = {
                "Username",
                "If not provided in command line, interactive mode will prompt for it."
        }, interactive = true, echo = true, prompt = PROMPT_USERNAME)
        String user;

        @CommandLine.Option(names = {"-p", "--password"}, arity = "0..1", description = {
                "Passphrase",
                "If not provided in command line, interactive mode will prompt for it."
        }, interactive = true, echo = false, prompt = PROMPT_PASSWORD)
        char[] password;
    }

    static class TokenOptions {

        @CommandLine.Option(names = {"-tk", "--token"}, arity = "0..1", description = {
                "dotCMS Token",
                "A token can be used directly to authenticate with the dotCMS instance",
        }, interactive = true, echo = false, prompt = PROMPT_TOKEN)
        char[] token;
    }

    /**
     * Here we encapsulate the login options
     */
    static class LoginOptions {

        @CommandLine.ArgGroup(heading = "\n@|bold,blue Password Login Options. |@\n",
                exclusive = false)
        PasswordOptions passwordOptions;

        @CommandLine.ArgGroup(heading = "\n@|bold,blue Token login Options. |@\n")
        TokenOptions tokenOptions;
    }

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @CommandLine.ArgGroup(exclusive = true)
    LoginOptions loginOptions;

    @Inject
    AuthenticationContext authenticationContext;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Inject
    Prompt prompt;

    @Override
    public Integer call() throws IOException {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        // Login with token if provided
        if (isTokenSet()) {
            output.info("Logging in with token");
            authenticationContext.login(loginOptions.tokenOptions.token);
            output.info(FORMAT_TOKEN_LOGGED_IN);
        } else {

            String userName;
            char[] password;

            // Request the username
            if (isUserNameSet()) {
                userName = loginOptions.passwordOptions.user;
            } else {
                userName = prompt.readInput(null, PROMPT_USERNAME);
            }

            // Request the password
            if (isPasswordSet()) {
                password = loginOptions.passwordOptions.password;
            } else {
                password = prompt.readPassword(PROMPT_PASSWORD);
            }

            // Validating the username and password
            if (areCredentialsInvalid(userName, password)) {
                output.error(
                        "Missing required options. Please provide a valid username and password"
                                + " or token."
                );
                return ExitCode.USAGE;
            }

            output.info(String.format("Logging in as [@|bold,cyan %s|@]", userName));
            authenticationContext.login(
                    userName,
                    password
            );
            output.info(String.format(FORMAT_USER_LOGGED_IN, userName));
        }

        return ExitCode.OK;
    }

    /**
     * Checks if a token is set for logging in.
     *
     * @return true if a token is set, false otherwise.
     */
    private boolean isTokenSet() {
        return loginOptions != null &&
                loginOptions.tokenOptions != null &&
                loginOptions.tokenOptions.token != null &&
                loginOptions.tokenOptions.token.length > 0;
    }

    /**
     * Checks if a password is set for login.
     *
     * @return true if a password is set, false otherwise.
     */
    private boolean isPasswordSet() {
        return loginOptions != null &&
                loginOptions.passwordOptions != null &&
                loginOptions.passwordOptions.password != null &&
                loginOptions.passwordOptions.password.length > 0;
    }

    /**
     * Checks if the username is set for login.
     *
     * @return true if the username is set, false otherwise.
     */
    private boolean isUserNameSet() {
        return loginOptions != null &&
                loginOptions.passwordOptions != null &&
                loginOptions.passwordOptions.user != null &&
                !loginOptions.passwordOptions.user.trim().isEmpty();
    }

    /**
     * Checks whether the provided credentials are invalid.
     *
     * @param userName the username
     * @param password the password
     * @return true if the credentials are invalid, false otherwise
     */
    private boolean areCredentialsInvalid(String userName, char[] password) {
        return userName == null || userName.trim().isEmpty() ||
                password == null || password.length == 0;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }
}

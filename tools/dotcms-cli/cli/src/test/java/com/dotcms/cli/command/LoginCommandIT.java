package com.dotcms.cli.command;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.command.LoginCommand.LoginOptions;
import com.dotcms.cli.command.LoginCommand.PasswordOptions;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class LoginCommandIT extends CommandTest {

    @BeforeEach
    public void setupTest() throws IOException {
        resetServiceProfiles();
        MockitoAnnotations.openMocks(this);
    }

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ServiceManager serviceManager;

    @Mock
    Prompt prompt;

    @Spy
    @InjectMocks
    LoginCommand loginCommand;

    @Mock
    OutputOptionMixin output;

    @Mock
    CommandSpec spec;

    @Mock
    AuthenticationContext authenticationContext;

    /**
     * Scenario: No parameters passed, interactive mode should be used
     */
    @Test
    @Order(1)
    void Test_Command_Login_No_Params() throws Exception {

        final String user = "admin@dotCMS.com";
        final String password = "admin";

        // When username is asked in interactive mode, return the username
        when(prompt.readInput(null, LoginCommand.PROMPT_USERNAME)).
                thenReturn(user);
        // When password is asked in interactive mode, return the password
        when(prompt.readPassword(LoginCommand.PROMPT_PASSWORD)).
                thenReturn(password.toCharArray());

        final Integer status = loginCommand.call();
        Assertions.assertEquals(ExitCode.OK, status);

        verify(loginCommand, times(1)).call();
        verify(prompt, times(1)).readInput(null, LoginCommand.PROMPT_USERNAME);
        verify(prompt, times(1)).readPassword(LoginCommand.PROMPT_PASSWORD);
        verify(output).info(String.format(LoginCommand.FORMAT_USER_LOGGED_IN, user));
    }

    /**
     * Scenario: Pass valid credentials
     * Expect: A Success message and exit code 0
     */
    @Test
    @Order(2)
    void Test_Command_Login_With_Params_Expect_Successful_Login()  {
        final String user = "admin@dotCMS.com";

        final String [][] options = {
                {"--user=admin@dotCMS.com","--password=admin"},
                {"-u=admin@dotCMS.com","-p=admin"}
        };

        for (final String [] option:options) {
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try(PrintWriter out = new PrintWriter(writer)){
                commandLine.setOut(out);
                final int status = commandLine.execute(LoginCommand.NAME,option[0],option[1]);
                Assertions.assertEquals(ExitCode.OK, status);
                final String output = writer.toString();
                Assertions.assertTrue(output.contains(String.format("Successfully logged-in as [%s]",user)));
            }
        }
    }

    /**
     * Scenario: Pass invalid credentials
     * Expect: Should fail inform about that and exit code 1
     */
    @Test
    @Order(3)
    void Test_Command_Login_With_Params_Expect_Login_Reject()  {

        final String [][] options = {
                {"--user=admin@dotCMS.com","--password=lol"},
                {"-u=admin@dotCMS.com","-p=lol"}
        };

        for (final String [] option:options) {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try(PrintWriter out = new PrintWriter(writer)){
                commandLine.setErr(out);
                final int status = commandLine.execute(LoginCommand.NAME,option[0],option[1]);
                Assertions.assertEquals(ExitCode.SOFTWARE, status);
                final String output = writer.toString();
                Assertions.assertTrue(output.contains("[ERROR]"));
                Assertions.assertTrue(output.contains(
                        "Forbidden: You don't have permission to access this resource."));
            }
        }

    }

    /**
     * Scenario: Pass invalid token
     * Expect: Should fail inform about that and exit code 1
     */
    @Test
    @Order(4)
    void Test_Command_Login_With_Invalid_Token() {
        final String token = RandomStringUtils.randomAlphabetic(100);
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try(PrintWriter out = new PrintWriter(writer)){
            commandLine.setErr(out);
            final int status = commandLine.execute(LoginCommand.NAME, "--token", token);
            Assertions.assertEquals(ExitCode.SOFTWARE, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains("[ERROR]"));
            Assertions.assertTrue(output.contains(
                    "Forbidden: You don't have permission to access this resource."));
        }
    }


    /**
     * Scenario: Pass valid token
     * Expect: Should log in successfully and exit code 0 finally we validate the token is stored
     */
    @Test
    @Order(5)
    void Test_Command_Login_With_Valid_Token() throws IOException{
        final String token = requestToken();
        //Now that we have the token, let's use it to login
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try(PrintWriter out = new PrintWriter(writer)){
            commandLine.setOut(out);
            final int status = commandLine.execute(LoginCommand.NAME, "--token", token);
            Assertions.assertEquals(ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains("Successfully logged-in with token"));
        }

        final Optional<ServiceBean> selected = serviceManager.selected();
        Assertions.assertTrue(selected.isPresent());
        final ServiceBean serviceBean = selected.get();
        Assertions.assertNotNull(serviceBean.credentials());

        final Optional<char[]> optionalLoadedToken = serviceBean.credentials().loadToken();
        Assertions.assertNotNull(optionalLoadedToken);
        Assertions.assertTrue(optionalLoadedToken.isPresent());
        Assertions.assertEquals(token, new String(optionalLoadedToken.get()));

        final Optional<char[]> optionalSavedToken = serviceBean.credentials().token();
        Assertions.assertNotNull(optionalSavedToken);
        Assertions.assertTrue(optionalSavedToken.isPresent());
        Assertions.assertEquals(token, new String(optionalSavedToken.get()));
    }

    /**
     * Scenario: No password passed, interactive mode should be used
     */
    @Test
    @Order(6)
    void Test_Command_Login_User_No_Password() throws Exception {

        final String user = "admin@dotCMS.com";
        final String password = "admin";

        // Set login options to your command
        PasswordOptions passwordOptions = new PasswordOptions();
        passwordOptions.user = user;
        LoginOptions loginOptions = new LoginOptions();
        loginOptions.passwordOptions = passwordOptions;
        loginCommand.loginOptions = loginOptions;

        // When password is asked in interactive mode, return password
        when(prompt.readPassword(LoginCommand.PROMPT_PASSWORD)).
                thenReturn(password.toCharArray());

        final Integer status = loginCommand.call();
        Assertions.assertEquals(ExitCode.OK, status);

        verify(loginCommand, times(1)).call();
        verify(prompt, times(0)).readInput(null, LoginCommand.PROMPT_USERNAME);
        verify(prompt, times(1)).readPassword(LoginCommand.PROMPT_PASSWORD);
        verify(output).info(String.format(LoginCommand.FORMAT_USER_LOGGED_IN, user));
    }

}

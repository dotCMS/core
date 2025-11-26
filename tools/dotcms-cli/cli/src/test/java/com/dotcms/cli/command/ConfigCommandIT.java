package com.dotcms.cli.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class ConfigCommandIT {

    public static final String LOCALHOST_8080 = "http://localhost:8080";

    @BeforeEach
    public void setupTest() throws IOException {
        MockitoAnnotations.openMocks(this);
    }

    @Mock
    Prompt prompt;

    @Mock
    OutputOptionMixin output;

    @Mock
    CommandSpec spec;

    @Mock
    ServiceManager serviceManager;

    @Spy
    @InjectMocks
    ConfigCommand configCommand;

    /**
     * Given scenario: Test that we call the command with no configuration file found and the delete option passed.
     * Expected result: The command should return OK and print a message to the user. No other interaction should happen.
     * @throws Exception
     */
    @Test
    void testNoConfigurationFoundDeleteOptionPassed() throws Exception {
        when(serviceManager.services()).thenReturn(List.of());
        configCommand.delete = true;
        final Integer status = configCommand.call();
        Assertions.assertEquals(ExitCode.OK, status);
        verify(output).info("No configuration file found. Nothing to delete. Re-run the command without the -d option.");
    }

    /**
     * Given scenario: Test that we call the command with no configuration file found. The user only specifies one dotCMS instance.
     * Expected result: The command should prompt the user for the dotCMS instance information and then automatically select the only instance as the active one.
     * @throws Exception
     */
    @Test
    void testNoConfigurationFoundAddOneThenMakeActive() throws Exception {
        when(serviceManager.services()).thenReturn(List.of());
        when(prompt.readInput("local", "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ","local")).thenReturn("local");
        when(prompt.readInput(LOCALHOST_8080, "Enter the dotCMS base URL (must be a valid URL starting protocol http or https) [%s] ", LOCALHOST_8080)).thenReturn(
                LOCALHOST_8080);
       when(prompt.yesOrNo(true, "Are these values OK? (Enter to confirm or N to cancel) ")).thenReturn(true);
       when(prompt.yesOrNo(true, "Do you want to continue adding another dotCMS instance? ")).thenReturn(false);

       final Integer status = configCommand.call();
       Assertions.assertEquals(ExitCode.OK, status);
       verify(configCommand,times(1)).freshInit();
       verify(output).info("The current active profile is now [local]");
    }

    /**
     * Given scenario: Test that we call the command with no configuration file found. The user specifies three dotCMS instances and then selects the last one as the active one.
     * Expected result: The command should prompt the user for the dotCMS instance information and then Prompt the user again to select one of the 3 instances as the active one. The last one should be selected.
     * @throws Exception
     */
    @Test
    void testNoConfigurationFoundAddThreeThenMakeLastOneActive() throws Exception {
        when(serviceManager.services()).thenReturn(List.of());
        when(prompt.readInput("local", "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ","local")).thenReturn("local");
        when(prompt.readInput("local#1", "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ","local#1")).thenReturn("local#1");
        when(prompt.readInput("local#2", "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ","local#2")).thenReturn("local#2");

        when(prompt.readInput(LOCALHOST_8080, "Enter the dotCMS base URL (must be a valid URL starting protocol http or https) [%s] ", LOCALHOST_8080)).thenReturn(
                LOCALHOST_8080);
        when(prompt.yesOrNo(true, "Are these values OK? (Enter to confirm or N to cancel) ")).thenReturn(true);

        //After two calls we want to return false, so we can break the loop
        final AtomicInteger count = new AtomicInteger(1);
        when(prompt.yesOrNo(true, "Do you want to continue adding another dotCMS instance? ")).thenAnswer(invocation -> count.getAndIncrement() <= 2);
        when(prompt.readInput(-1, // Index starts at 0, and we want to select the last one therefore we need to select 2
                "Enter the number of the profile to be made default or press enter to exit. ")).thenReturn(2);

        final Integer status = configCommand.call();
        Assertions.assertEquals(ExitCode.OK, status);
        verify(configCommand,times(1)).freshInit();
        verify(output).info("One of these profiles needs to be made the current active one. Please select the number of the profile you want to activate.");
        verify(output).info("The current active profile is now [local#2]");
    }

    /**
     * Given scenario: Test that we call the command with no configuration file found. The user specifies three dotCMS instances and during the process introduces a duplicate name.
     * Expected result: The command should prompt the user for the dotCMS instance information when saving a dupe name and error should be reported to the user.
     * @throws Exception
     */
    @Test
    void testNoConfigurationFoundAddThreeIntroduceDupeNameExpectError() throws Exception {
        when(serviceManager.services()).thenReturn(List.of());
        when(prompt.readInput("local", "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ","local")).thenReturn("local");
        //Here's where we're going to introduce the dupe name
        when(prompt.readInput("local#1", "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ","local#1")).thenReturn("local");
        when(prompt.readInput(LOCALHOST_8080, "Enter the dotCMS base URL (must be a valid URL starting protocol http or https) [%s] ", LOCALHOST_8080)).thenReturn(
                LOCALHOST_8080);
        when(prompt.yesOrNo(true, "Are these values OK? (Enter to confirm or N to cancel) ")).thenReturn(true);
        final AtomicInteger count = new AtomicInteger(1);
        when(prompt.yesOrNo(true, "Do you want to continue adding another dotCMS instance? ")).thenAnswer(invocation -> count.getAndIncrement() <= 1);
        //We want to return 2, so we can break the loop
        when(prompt.readInput(-1, "Enter the number of the profile to be made default or press enter to exit. ")).thenReturn(2);
        when(configCommand.maxCaptureAttempts()).thenReturn(2);

        final Integer status = configCommand.call();
        Assertions.assertEquals(ExitCode.OK, status);
        verify(configCommand,times(1)).freshInit();
        verify(configCommand,never()).persistAndMakeActive(anyList());
        //The name [local] is already in use.
        verify(output,atLeastOnce()).error("There are errors in the captured values : The name [local] is already in use.");

    }


    /**
     * Given scenario: Test that we call the command with no configuration file found. The user specifies three dotCMS instances and during the process introduces an invalid URL.
     * Expected result: The command should prompt the user for the dotCMS instance information when saving an invalid URL and error should be reported to the user.
     * @throws Exception
     */
    @Test
    void testNoConfigurationFoundAddThreeIntroduceInvalidURLExpectError() throws Exception {
        final String invalidURL = "http:\\demo.dotcms.com";
        when(serviceManager.services()).thenReturn(List.of());
        when(prompt.readInput("local", "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ","local")).thenReturn("local");
        when(prompt.readInput(LOCALHOST_8080, "Enter the dotCMS base URL (must be a valid URL starting protocol http or https) [%s] ", LOCALHOST_8080)).thenReturn(invalidURL);
        when(prompt.yesOrNo(true, "Are these values OK? (Enter to confirm or N to cancel) ")).thenReturn(true);
        when(prompt.yesOrNo(true, "Do you want to continue adding another dotCMS instance? ")).thenReturn(false);
        when(configCommand.maxCaptureAttempts()).thenReturn(1);
        final Integer status = configCommand.call();
        Assertions.assertEquals(ExitCode.OK, status);
        verify(configCommand,times(1)).freshInit();
        verify(configCommand,never()).persistAndMakeActive(anyList());
        //The name [local] is already in use.
        verify(output,atLeastOnce()).error("There are errors in the captured values : Invalid URL: http:\\demo.dotcms.com");
    }

    /**
     * Given scenario: Test that we call the command when a configuration file is already in place.
     * Expected result: The command should prompt the user for a profile index. The user selects the 2nd profile and then the command should return OK.
     * We verify an update has taken place by checking the output message.
     */
    @Test
    void testUpdateConfiguration() throws Exception{

        final List<ServiceBean> initialValues = List.of(
                ServiceBean.builder().name("local").url(new URL("http://localhost.com"))
                        .active(true).build(),
                ServiceBean.builder().name("demo")
                        .url(new URL("https://demo.dotcms.com")).active(false).build()
        );

        final List<ServiceBean> updatedValues = List.of(
                ServiceBean.builder().name("local").url(new URL("http://localhost.com"))
                        .active(true).build(),
                ServiceBean.builder().name("demo-updated")
                        .url(new URL("https://demo.updated.dotcms.com")).active(false).build()
        );

        //We configure the mock to return the initial values, and then after the update, the updated values
        when(serviceManager.services()).thenReturn(initialValues);
        when(serviceManager.persist(any())).then(invocation -> {
            when(serviceManager.services()).thenReturn(updatedValues);
            return null;
        });

        //Keep track of the number of times the prompt is called, so we can exit the loop after the first time
        final AtomicInteger count = new AtomicInteger(1);
        when(prompt.readInput(-1,
                "Select the number of the profile you want to @|bold edit|@ or press enter. ")).thenAnswer(invocation -> count.getAndIncrement() <= 1 ? 1 : -1);
        // return the new name of the profile
        when(prompt.readInput("demo", "Enter the new name for the profile [%s]. ", "demo")).thenReturn("demo-updated");
        when(prompt.readInput("https://demo.dotcms.com", "Enter the new URL for the profile [%s]. ", "https://demo.dotcms.com")).thenReturn("https://demo.updated.dotcms.com");
        when(prompt.yesOrNo(true, "Are these values OK? (Enter to confirm or N to cancel) ")).thenReturn(true);
        //Make sure we can also cover the case when the user wants to update the active profile
        when(prompt.yesOrNo(false, "Do you want to update the @|bold current active|@ instance? ")).thenReturn(true);
        when(prompt.readInput(-1, "Enter the number of the profile to be made default or press enter to exit. ")).thenReturn(1);

        when(prompt.yesOrNo(false, "Do you want to add a new dotCMS instance? (Or press enter to exit)")).thenReturn(true);
        when(prompt.readInput("local", "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ","local")).thenReturn("local2");
        when(prompt.readInput(LOCALHOST_8080, "Enter the dotCMS base URL (must be a valid URL starting protocol http or https) [%s] ", LOCALHOST_8080)).thenReturn(LOCALHOST_8080);
        when(prompt.yesOrNo(true, "Are these values OK? (Enter to confirm or N to cancel) ")).thenReturn(true);
        when(prompt.yesOrNo(true, "Do you want to continue adding another dotCMS instance? ")).thenReturn(false);
        when(configCommand.maxCaptureAttempts()).thenReturn(1);

        final Integer status = configCommand.call();
        Assertions.assertEquals(ExitCode.OK, status);
        verify(configCommand,times(1)).performUpdate(anyList());
        verify(configCommand,times(1)).addNewInstances(anyList());
        verify(configCommand,times(2)).persistAndMakeActive(anyList());
        verify(output).info("The profile [demo-updated] has been updated.");
        verify(output,times(2)).info("One of these profiles needs to be made the current active one. Please select the number of the profile you want to activate.");
        verify(output,times(2)).info("The current active profile is now [demo-updated]");
    }


}

package com.dotcms.cli.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.util.List;
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
class InitCommandIT extends CommandTest {

    @BeforeEach
    public void setupTest() throws IOException {
        MockitoAnnotations.openMocks(this);
        resetServiceProfiles();
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
    InitCommand initCommand;

    @Test
    void testNoConfigurationFoundDeleteOptionPassed() throws Exception {
        when(serviceManager.services()).thenReturn(List.of());
        initCommand.delete = true;
        final Integer status = initCommand.call();
        Assertions.assertEquals(ExitCode.OK, status);
        verify(output).info("No configuration file found. Nothing to delete. Re-run the command without the -d option.");
    }

    @Test
    void testNoConfigurationFound() throws Exception {
       when(serviceManager.services()).thenReturn(List.of());
       when(prompt.readInput("local", "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ","local")).thenReturn("local");
       when(prompt.readInput("http://localhost:8080", "Enter the URL of the dotCMS instance [%s].  ","http://localhost:8080")).thenReturn("http://localhost:8080");

       when(prompt.yesOrNo(true, "Are these values OK? (Enter to confirm or N to cancel) ")).thenReturn(true);
       when(prompt.yesOrNo(true, "Do you want to continue adding another dotCMS instance? ")).thenReturn(false);

       initCommand.delete = false;
       final Integer status = initCommand.call();
       Assertions.assertEquals(ExitCode.OK, status);
       verify(initCommand,times(1)).freshInit();

    }

}

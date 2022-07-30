package com.dotcms.cli.command;

import com.dotcms.api.client.ServiceManager;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import io.quarkus.test.junit.QuarkusTest;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;


@QuarkusTest
public class StatusCommandTest {

    @Inject
    PicocliCommandLineFactory factory;

    @Inject
    ServiceManager serviceManager;

    @Test
    @Order(1)
    public void Test_Command_Status_No_Profiles()  {

        serviceManager.removeAll();

        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try(PrintWriter out = new PrintWriter(writer)){
          commandLine.setOut(out);
          Assertions.assertNotNull(commandLine);
          final int status = commandLine.execute(StatusCommand.NAME);
          Assertions.assertEquals(0, status);
          Assertions.assertTrue(writer.toString().contains("No active profile is configured Please use instance Command."));
        }
    }

}

package com.dotcms.cli.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.command.DotPush;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.net.URL;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.ParseResult;

@QuarkusTest
class DotExecutionStrategyTest {

    @Mock
    DotExecutionStrategy dotExecutionStrategy;
    @Mock
    IExecutionStrategy mockUnderlyingStrategy;

    @Mock
    SubcommandProcessor mockSubcommandProcessor;

    @Mock
    DirectoryWatcherService mockWatchService;

    @Mock
    ServiceManager mockServiceManager;

    @Mock
    LinkedBlockingQueue<WatchEvent<?>> watchEvents;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        dotExecutionStrategy = spy(new DotExecutionStrategy(
                mockUnderlyingStrategy, mockSubcommandProcessor, mockWatchService, mockServiceManager
        ));
    }

    /**
     * Given scenario:  We have a DotPush command with watch mode enabled we should be able to execute the command
     * Expected Result: The command should be executed and the watch service should be started Upon receiving a watch event the push command should be executed
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    void testExecute() throws IOException, InterruptedException {

        OutputOptionMixin outputOptionMixin = mock(OutputOptionMixin.class);
        when(outputOptionMixin.isShowErrors()).thenReturn(false);

        PushMixin pushMixin = mock(PushMixin.class);
        pushMixin.interval = 2;
        when(pushMixin.isWatchMode()).thenReturn(true);

        DotPush dotPush = mock(DotPush.class);
        when(dotPush.getOutput()).thenAnswer(invocationOnMock -> outputOptionMixin);
        when(dotPush.getPushMixin()).thenAnswer(invocationOnMock -> pushMixin);

        ParseResult parseResult = mock(ParseResult.class);
        CommandLine.Model.CommandSpec commandSpec = mock(CommandLine.Model.CommandSpec.class);
        when(commandSpec.userObject()).thenAnswer(invocationOnMock -> dotPush);
        when(commandSpec.name()).thenReturn("push");
        when(parseResult.commandSpec()).thenReturn(commandSpec);

        CommandsChain mockCommandsChain = CommandsChain.builder()
                .command("test")
                .isRemoteURLSet(false)
                .isTokenSet(false)
                .isHelpRequestedAny(false)
                .isShowErrorsAny(false)
                .isWatchMode(true)
                .subcommands(List.of(parseResult))
                .build();

        when(mockSubcommandProcessor.process(parseResult)).then(invocationOnMock -> Optional.of(mockCommandsChain));
        WatchEvent mockWatchEvent = mock(WatchEvent.class);
        when(mockWatchEvent.kind()).thenReturn(StandardWatchEventKinds.ENTRY_CREATE);

        // Mock the service manager to return a list of services so that the push command can be executed
        when(mockServiceManager.services()).thenAnswer(invocationOnMock -> {
            List<ServiceBean> services = new ArrayList<>();
            services.add(ServiceBean.builder()
                    .name("demo")
                    .active(true)
                    .url(new URL("https://demo.dotcms.com"))
                    .build());
            return services;
        });

        when(watchEvents.take()).thenReturn(mockWatchEvent);
        when(mockWatchService.watch(any(), anyLong())).thenReturn(watchEvents);
        when(mockWatchService.isRunning()).thenReturn(true).thenReturn(false);
        when(mockWatchService.isSuspended()).thenReturn(false);
        when(mockUnderlyingStrategy.execute(parseResult)).thenReturn(0);

        // Execute the command
        int exitCode = dotExecutionStrategy.execute(parseResult);

        //Verify that the command was executed and the watch service was started
        verify(mockUnderlyingStrategy, atMostOnce()).execute(parseResult);
        verify(dotExecutionStrategy, atLeastOnce()).internalExecute(any(),any(),any());
        verify(dotExecutionStrategy, atLeastOnce()).processCommandExecution(any(),any(),any());
        verify(dotExecutionStrategy, atLeastOnce()).handleWatchPush(any(), any(), any());

        verify(mockSubcommandProcessor).process(parseResult);
        verify(mockWatchService).watch(any(), anyLong());
        verify(watchEvents, atLeastOnce()).take();
        //Ensure the command was executed successfully
        assertEquals(0, exitCode);
    }
}

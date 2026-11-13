package com.dotcms.enterprise.cluster;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.cluster.business.ServerAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Config;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** Verifies returned participants and issued SQL, not database-side row selection. */
public class ServerFactoryImplReindexingServersTest {

    @Test
    public void selectedServersDoNotAppendExcludedLocalNodeAndRequestDefaultLimit() throws Exception {
        final ServerAPI serverAPI = mock(ServerAPI.class);
        when(serverAPI.readServerId()).thenReturn("local");
        try (MockedStatic<Config> config = mockStatic(Config.class);
                MockedStatic<APILocator> api = mockStatic(APILocator.class);
                MockedConstruction<DotConnect> queries = mockConstruction(DotConnect.class,
                        (query, context) -> when(query.loadObjectResults()).thenReturn(List.of(
                                Map.of("server_id", "peer-a"), Map.of("server_id", "peer-b"))))) {
            config.when(() -> Config.getIntProperty("MAX_REINDEXING_SERVERS", 2)).thenReturn(2);
            api.when(APILocator::getServerAPI).thenReturn(serverAPI);

            assertEquals(List.of("peer-a", "peer-b"),
                    new ServerFactoryImpl().getReindexingServers());

            config.verify(() -> Config.getIntProperty("MAX_REINDEXING_SERVERS", 2));
            verify(queries.constructed().get(0)).setSQL(
                    endsWith("order by startup_time, server_id limit ?"));
            verify(queries.constructed().get(0)).addParam(2);
            api.verifyNoInteractions();
        }
    }

    @Test
    public void configuredLimitOneKeepsReturnedParticipantAlone() throws Exception {
        final ServerAPI serverAPI = mock(ServerAPI.class);
        when(serverAPI.readServerId()).thenReturn("local");
        try (MockedStatic<Config> config = mockStatic(Config.class);
                MockedStatic<APILocator> api = mockStatic(APILocator.class);
                MockedConstruction<DotConnect> queries = mockConstruction(DotConnect.class,
                        (query, context) -> when(query.loadObjectResults()).thenReturn(
                                List.of(Map.of("server_id", "peer-a"))))) {
            config.when(() -> Config.getIntProperty("MAX_REINDEXING_SERVERS", 2)).thenReturn(1);
            api.when(APILocator::getServerAPI).thenReturn(serverAPI);

            assertEquals(List.of("peer-a"), new ServerFactoryImpl().getReindexingServers());

            config.verify(() -> Config.getIntProperty("MAX_REINDEXING_SERVERS", 2));
            verify(queries.constructed().get(0)).setSQL(
                    endsWith("order by startup_time, server_id limit ?"));
            verify(queries.constructed().get(0)).addParam(1);
            api.verifyNoInteractions();
        }
    }

    @Test
    public void nonpositiveConfiguredLimitsUseOneAndDoNotAppendExcludedLocalNode() throws Exception {
        for (final int configuredLimit : new int[] {0, -1}) {
            final ServerAPI serverAPI = mock(ServerAPI.class);
            when(serverAPI.readServerId()).thenReturn("local");
            try (MockedStatic<Config> config = mockStatic(Config.class);
                    MockedStatic<APILocator> api = mockStatic(APILocator.class);
                    MockedConstruction<DotConnect> queries = mockConstruction(DotConnect.class,
                            (query, context) -> when(query.loadObjectResults()).thenReturn(
                                    List.of(Map.of("server_id", "peer-a"))))) {
                config.when(() -> Config.getIntProperty("MAX_REINDEXING_SERVERS", 2))
                        .thenReturn(configuredLimit);
                api.when(APILocator::getServerAPI).thenReturn(serverAPI);

                assertEquals(List.of("peer-a"), new ServerFactoryImpl().getReindexingServers());

                config.verify(() -> Config.getIntProperty("MAX_REINDEXING_SERVERS", 2));
                verify(queries.constructed().get(0)).setSQL(
                        endsWith("order by startup_time, server_id limit ?"));
                verify(queries.constructed().get(0)).addParam(1);
                api.verifyNoInteractions();
            }
        }
    }

    @Test
    public void emptyQueryFallsBackToLocalServer() throws Exception {
        final ServerAPI serverAPI = mock(ServerAPI.class);
        when(serverAPI.readServerId()).thenReturn("local");
        try (MockedStatic<Config> config = mockStatic(Config.class);
                MockedStatic<APILocator> api = mockStatic(APILocator.class);
                MockedConstruction<DotConnect> queries = mockConstruction(DotConnect.class,
                        (query, context) -> when(query.loadObjectResults()).thenReturn(
                                Collections.emptyList()))) {
            config.when(() -> Config.getIntProperty("MAX_REINDEXING_SERVERS", 2)).thenReturn(2);
            api.when(APILocator::getServerAPI).thenReturn(serverAPI);

            assertEquals(List.of("local"), new ServerFactoryImpl().getReindexingServers());

            config.verify(() -> Config.getIntProperty("MAX_REINDEXING_SERVERS", 2));
            verify(serverAPI).readServerId();
        }
    }

    @Test
    public void nullQueryFallsBackToLocalServer() throws Exception {
        final ServerAPI serverAPI = mock(ServerAPI.class);
        when(serverAPI.readServerId()).thenReturn("local");
        try (MockedStatic<Config> config = mockStatic(Config.class);
                MockedStatic<APILocator> api = mockStatic(APILocator.class);
                MockedConstruction<DotConnect> queries = mockConstruction(DotConnect.class,
                        (query, context) -> when(query.loadObjectResults()).thenReturn(null))) {
            config.when(() -> Config.getIntProperty("MAX_REINDEXING_SERVERS", 2)).thenReturn(2);
            api.when(APILocator::getServerAPI).thenReturn(serverAPI);

            assertEquals(List.of("local"), new ServerFactoryImpl().getReindexingServers());

            config.verify(() -> Config.getIntProperty("MAX_REINDEXING_SERVERS", 2));
            verify(serverAPI).readServerId();
        }
    }
}

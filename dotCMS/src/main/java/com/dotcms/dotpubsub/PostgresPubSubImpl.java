package com.dotcms.dotpubsub;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.impossibl.postgres.api.jdbc.PGConnection;
import com.impossibl.postgres.api.jdbc.PGNotificationListener;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class PostgresPubSubImpl implements DotPubSubProvider {

    private enum RUNSTATE {
       STOPPED, STARTED, REBUILD
    }


    
    final String serverId;
    private long messagesSent = 0;
    private long messagesRecieved = 0;

    private Lazy<DataSourceAttributes> attributes = Lazy.of(()->getDatasourceAttributes());
    private AtomicReference<RUNSTATE> state = new AtomicReference<>(RUNSTATE.STOPPED);
    private PGConnection connection;

    private Map<Comparable<String>,DotPubSubTopic> topicMap = new ConcurrentHashMap<>();

    @VisibleForTesting
    protected static DotPubSubEvent lastEventIn,lastEventOut;


    @Override
    public DotPubSubProvider init() {

        listen();
        return this;
    }


    public PostgresPubSubImpl() {
        this(APILocator.getServerAPI().readServerId());


    }

    public PostgresPubSubImpl(String serverId) {
        this.serverId = APILocator.getShortyAPI().shortify(serverId);

    }

    private PGNotificationListener listener = new PGNotificationListener() {

        @Override
        public void notification(final int processId, final String channelName, final String payload) {


            List<DotPubSubTopic> matchingTopics =
                            topicMap.values().stream().filter(t -> t.getKey().toString().compareToIgnoreCase(channelName) == 0)
                                            .collect(Collectors.toList());

            if (matchingTopics.isEmpty()) {
                return;
            }
            messagesRecieved++;
            final DotPubSubEvent event = Try.of(() -> new DotPubSubEvent(payload))
                            .onFailure(e -> Logger.warn(PostgresPubSubImpl.class, e.getMessage(), e)).getOrNull();
            if (event == null) {
                return;
            }
            Logger.info(PostgresPubSubImpl.class,
                            () -> "recieved event: " + processId + ", " + channelName + ", " + payload);

            lastEventIn=event;

            topicMap.values().forEach(t -> t.notify(event));


        }

        @Override
        public void closed() {
            Logger.warn(this.getClass(), "Listener connection closed, reconnecting");


            if (state.get() != RUNSTATE.STOPPED) {
                listen();
            }
        }
    };


    public void listen() {
        if(connection!=null) {
            return;
        }

        Logger.info(this.getClass(), "Listener connecting...");
        try {

            connection = DriverManager.getConnection(attributes.get().getDbUrl()).unwrap(PGConnection.class);
            connection.addNotificationListener(listener);

            for (DotPubSubTopic topic : topicMap.values()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("LISTEN " + topic.getKey().toString().toLowerCase());
                }
            }

        } catch (Exception e) {
            Logger.warnAndDebug(getClass(), e);
            if (state.get() != RUNSTATE.STOPPED) {
                restart();
            }
        }

    }


    /**
     * This will automatically restart the connections
     */
    public void restart() {
        this.state.set(RUNSTATE.STOPPED);
        Try.run(() -> connection.close());
        connection = null;
        Logger.warn(getClass(), "waiting 1 second to retry postgres pub/sub connection");
        Try.run(() -> Thread.sleep(1000));
        listen();
    }


    public void shutdown() {
        this.state.set(RUNSTATE.STOPPED);
        Try.run(() -> connection.close());
        connection = null;
    }


    private DataSourceAttributes getDatasourceAttributes() {
        HikariDataSource hds = (HikariDataSource) DbConnectionFactory.getDataSource();
        return new DataSourceAttributes(hds.getUsername(), hds.getPassword(), hds.getJdbcUrl());

    }


    @Override
    public DotPubSubProvider subscribe(DotPubSubTopic topic) {
        this.topicMap.putIfAbsent(topic.getKey(),topic);
        restart();
        return this;
    }



    @Override
    public boolean publish(DotPubSubTopic topic, DotPubSubEvent eventIn) {

        final DotPubSubEvent eventOut = new DotPubSubEvent.Builder(eventIn).withOrigin(serverId).build();

        Logger.info(getClass(), "sending  event:" + eventOut);
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            // postgres pubsub cannot send more than 8000 bytes
            if (eventOut.toString().getBytes().length > 8000) {
                throw new DotRuntimeException("Payload too large, must be under 8000b:" + eventOut.toString());
            }
            new DotConnect().setSQL("SELECT pg_notify(?,?)").addParam(topic.getKey()).addParam(eventOut.toString())
                            .loadResult(conn);
            messagesSent++;
            lastEventOut=eventOut;
            return true;
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "Unable to send pubsub:" + e.getMessage(), e);
            return false;
        }

    }



    public long getMessagesSent() {
        return messagesSent;
    }


    public long getMessagesRecieved() {
        return messagesRecieved;
    }



}

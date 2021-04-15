package com.dotcms.dotpubsub;

import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import com.impossibl.postgres.api.jdbc.PGConnection;
import com.impossibl.postgres.api.jdbc.PGNotificationListener;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.control.Try;

public class PostgresPubSubImpl implements DotPubSubProvider {

    private DataSourceAttributes attributes;
    private boolean shutdown = false;
    private PGConnection connection;

    private List<DotPubSubTopic> topics = new CopyOnWriteArrayList<DotPubSubTopic>();

    @Override
    public DotPubSubProvider init() {
        attributes = getDatasourceAttributes();
        shutdown = false;
        listen();
        return this;
    }


    public PostgresPubSubImpl() {



    }


    public void listen() {
        Logger.info(this.getClass(), "Listener connecting");
        try {

            connection = DriverManager.getConnection(attributes.getDbUrl()).unwrap(PGConnection.class);
            connection.addNotificationListener("channel", new PGNotificationListener() {

                @Override
                public void notification(final int processId, final String channelName, final String payload) {

                    Logger.debug(this.getClass(),
                                    () -> "Received Notification: " + processId + ", " + channelName + ", " + payload);
                    List<DotPubSubTopic> matchingTopics = topics.stream()
                                    .filter(t -> t.getKey().compareTo(channelName) == 0).collect(Collectors.toList());

                    if (matchingTopics.isEmpty()) {
                        return;
                    }
                    final DotPubSubEvent event = Try
                                    .of(() -> DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                                                    .readValue(payload, DotPubSubEvent.class))
                                    .onFailure(e -> Logger.warn(PostgresPubSubImpl.class, e.getMessage(), e))
                                    .getOrNull();
                    if (event == null) {
                        return;
                    }

                    topics.forEach(t -> t.notify(event));


                }

                @Override
                public void closed() {
                    Logger.warn(this.getClass(), "Listener connection closed, reconnecting");

                    PGNotificationListener.super.closed();
                    if (!shutdown) {
                        listen();
                    }
                }
            });

            connection.createStatement().executeUpdate("LISTEN channel");


        } catch (Exception e) {
            Logger.warnAndDebug(getClass(), e);
            if (!shutdown) {
                Logger.warn(getClass(), "waiting 1 second to retry postgres pub/sub connection");
                Try.run(() -> Thread.sleep(1000));
                listen();
            }
        }


    }


    public void shutdown() {
        this.shutdown = true;

        Try.run(() -> connection.close());

    }

    private DataSourceAttributes getDatasourceAttributes() {
        HikariDataSource hds = (HikariDataSource) DbConnectionFactory.getDataSource();
        return new DataSourceAttributes(hds.getUsername(), hds.getPassword(), hds.getJdbcUrl());

    }


    @Override
    public void subscribe(DotPubSubTopic topic) {
        topics.add(topic);
    }




    @Override
    public boolean publish(DotPubSubEvent event) {

        return true;

    }



}

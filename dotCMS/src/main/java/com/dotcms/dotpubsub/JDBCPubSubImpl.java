package com.dotcms.dotpubsub;

import com.dotcms.util.CloseUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import javax.validation.constraints.NotNull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


/**
 * Provides notifications for the postgres pub/sub connection.
 * The secret sauce is that it borrows 1 DB connection and keeps it open forever.
 * With this one long term connection, you "listen" to the topics on
 * postgres and continually do a "SELECT 1" in that connection to be notified
 * of any new messages to those topics.
 */
public class JDBCPubSubImpl implements DotPubSubProvider {

    private enum RUNSTATE {
        STOPPED, STARTED
    }

    private static final String PG_NOTIFY_SQL = "SELECT pg_notify(?,?)";
    private final String serverId;

    private static final int KILL_ON_FAILURES = Config.getIntProperty("PGLISTENER_KILL_ON_FAILURES", 100);
    private static final int SLEEP_BETWEEN_RUNS = Config.getIntProperty("PGLISTENER_SLEEP_BETWEEN_RUNS", 500);

    private PGListener internalListener;

    /**
     * creates a long term listener that holds the connection open
     * and constantly polls for new incoming messages.  If the listener
     * errors out, it will reconnect and resubscribe to those messages
     * @return
     */
    private PGListener listener() {

        if (internalListener != null && internalListener.isListening()) {
            return internalListener;
        }
        synchronized (JDBCPubSubImpl.PGListener.class) {
            if (internalListener != null && internalListener.isListening()) {
                return internalListener;
            }
            internalListener = new PGListener();
            for (Comparable<String> t : topicMap.keySet()) {
                internalListener.subscribeTopic(t.toString());
            }
            internalListener.setName("PGListener Pub/Sub Thread");
            internalListener.setDaemon(true);
            internalListener.start();
            return internalListener;
        }

    }

    /**
     * This is the list of topics that are subscribed to by the postgres pub/sub connection
     */
    private final Map<Comparable<String>, DotPubSubTopic> topicMap = new ConcurrentHashMap<>();

    @VisibleForTesting
    private static DotPubSubEvent lastEventIn;
    @VisibleForTesting
    private static DotPubSubEvent lastEventOut;

    @Override
    public DotPubSubProvider start() {

        listener();
        Logger.info(JDBCPubSubImpl.class, () -> "Starting to listen for Postgres notifications.");


        for (DotPubSubTopic topic : topicMap.values()) {
            subscribeToTopicSQL(topic.getKey().toString());
        }


        return this;
    }

    @Override
    public void stop() {
        listener().stopListening();
    }

    public JDBCPubSubImpl() {
        this(APILocator.getServerAPI().readServerId());

    }

    public JDBCPubSubImpl(String serverId) {
        this.serverId = StringUtils.shortify(serverId, 10);


    }


    class PGListener extends Thread {

        private RUNSTATE runstate = RUNSTATE.STARTED;
        private final Set<String> topics = ConcurrentHashMap.newKeySet();
        private final Lazy<Connection> connection = Lazy.of(() -> Try.of(() -> DbConnectionFactory.getDataSource().getConnection()).getOrElseThrow(DotRuntimeException::new));
        private final Lazy<PGConnection> pgConnection = Lazy.of(() -> Try.of(() -> connection.get().unwrap(PGConnection.class)).getOrElseThrow(DotRuntimeException::new));
        private final Pattern validTopicRegEx = Pattern.compile("[a-z0-9_]");

        PGListener() {
            // init our db connection.  The pgConnection opens the underlying
            // db connection
            pgConnection.get();
        }



        private long failures = 0;

        boolean subscribeTopic(String topic) {
            if (UtilMethods.isEmpty(topic)) {
                return false;
            }

            // topic is checked against an alphanumeric
            // regex to prevent SQL Injection
            if (!validTopicRegEx.matcher(topic).find()) {
                throw new DotRuntimeException("Invalid Topic Name:" + topic + ". Must match pattern" + validTopicRegEx);
            }
            if (this.topics.contains(topic)) {
                return true;
            }
            this.topics.add(topic);
            try (Statement statment = connection.get().createStatement()) {

                statment.execute("LISTEN " + topic); //NOSONAR
                Logger.info(JDBCPubSubImpl.class, "PGListener listening : " + topic);
            } catch (Exception e) {
                Logger.error(JDBCPubSubImpl.class, "PGListener failed to connect:" + e.getMessage(), e);
                stopListening();
                throw new DotRuntimeException(e);

            }
            return true;
        }


        boolean connectionAlive() {
            return Try.of(() -> !connection.get().isClosed()).getOrElse(false) && isListening();
        }

        void stopListening() {
            this.runstate = RUNSTATE.STOPPED;
            CloseUtils.closeQuietly(connection.get());
        }

        boolean isListening() {
            return this.runstate == RUNSTATE.STARTED;
        }


        @Override
        public void run() {
            try {
                runInternal();
            } finally {
                stopListening();
            }
        }


        public void runInternal() {
            Logger.info(JDBCPubSubImpl.class, "Running Listener Loop every " + SLEEP_BETWEEN_RUNS + "ms");


            while (runstate == RUNSTATE.STARTED) {
                if (!connectionAlive()) {
                    return;
                }
                try {

                    try (Statement stmt = connection.get().createStatement()) {
                        stmt.executeQuery("SELECT 1");
                    }

                    int notify = notify(pgConnection.get().getNotifications());

                    failures = 0;


                    // Notifications come in bunches
                    // Only sleep between runs if there are no notifications.
                    // Otherwise, keep listening
                    if (notify > 0) {
                        Try.run(() -> Thread.sleep(20));
                        continue;
                    }

                } catch (Throwable e) { //NOSONAR
                    logFailure(e);
                }
                Try.run(() -> Thread.sleep(SLEEP_BETWEEN_RUNS));
            }
        }

        // throws an error if failures grow > KILL_ON_FAILURES
        private void logFailure(Throwable e) {
            Logger.warn(JDBCPubSubImpl.class, e.getMessage());
            if (++failures > KILL_ON_FAILURES) {
                Logger.fatal(JDBCPubSubImpl.class, "PGListener failed " + KILL_ON_FAILURES + " times.  Dying", e);
                throw new DotRuntimeException(e);
            }
        }

        /**
         * decodes and dispatches incoming events
         */
        private int notify(PGNotification[] notifications) {
            if (notifications == null) {
                return 0;
            }
            Logger.debug(JDBCPubSubImpl.class, "Got Notifications:" + notifications.length);

            for (PGNotification notification : notifications) {

                String channelName = notification.getName();
                String payload = notification.getParameter();
                int processId = notification.getPID();
                Logger.debug(this.getClass(), () -> "received event: " + processId + ", " + channelName + ", " + payload);

                final DotPubSubEvent event = Try.of(() -> new DotPubSubEvent(payload)).onFailure(e -> Logger.warn(JDBCPubSubImpl.class, "event failed: " + processId + ", " + channelName + ", " + payload, e)).getOrNull();
                if (event == null) {
                    continue;
                }

                topicMap.values().stream()
                        .filter(t -> t.getKey().toString().compareToIgnoreCase(channelName) == 0)
                        .forEach(t -> {
                            t.incrementReceivedCounters(event);
                            t.notify(event);
                        });
            }
            return notifications.length;

        }


    }


    private void subscribeToTopicSQL(@NotNull String topic) {
        listener().subscribeTopic(topic.toLowerCase());
    }



    @Override
    public DotPubSubProvider subscribe(DotPubSubTopic topic) {
        this.topicMap.put(topic.getKey().toString().toLowerCase(), topic);
        subscribeToTopicSQL(topic.getKey().toString().toLowerCase());

        return this;
    }

    @Override
    public DotPubSubProvider unsubscribe(DotPubSubTopic topic) {
        this.topicMap.remove(topic.getKey().toString().toLowerCase());
        // will restart the listener without the unsubbed topic
        listener().stopListening();
        listener();
        return this;
    }

    @Override
    public boolean publish(final DotPubSubEvent eventIn) {
        listener();
        final DotPubSubEvent eventOut = new DotPubSubEvent.Builder(eventIn).withOrigin(serverId).build();

        Logger.debug(getClass(), () -> "sending  event:" + eventOut);
        try (final Connection conn = DbConnectionFactory.getDataSource().getConnection();
             final PreparedStatement statment = conn.prepareStatement(PG_NOTIFY_SQL)) {

            statment.setString(1, eventIn.getTopic());
            statment.setString(2, eventOut.toString());
            if (statment.execute()) {

                Try.run(() -> topicMap.get(eventIn.getTopic()).incrementSentCounters(eventOut));
                JDBCPubSubImpl.lastEventOut = eventOut; //NOSONAR
                return true;
            }

            return false;
        } catch (Exception e) {
            Logger.warn(this.getClass(), "Unable to send pubsub : " + eventIn);

            Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
            return false;
        }

    }

    @Override
    public DotPubSubEvent lastEventIn() {
        return lastEventIn;
    }

    @Override
    public DotPubSubEvent lastEventOut() {
        return lastEventOut;
    }

}

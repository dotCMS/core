package com.dotcms.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rest.api.v1.page.PageResource;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class TimeMachineUtil {

    private TimeMachineUtil(){}
    private static final Lazy<Integer> FTM_GRACE_WINDOW_LIMIT =
            Lazy.of(() -> Config.getIntProperty("FTM_GRACE_WINDOW_LIMIT", 5));
    /**
     * If Time Machine is running return the timestamp of the Time Machine date
     * Running Time Machine is determined by the presence of the attribute PageResource.TM_DATE in the request or session
     * @return Optional<String>
     */
    public static Optional<String> getTimeMachineDate() {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (null == request) {
            return Optional.empty();
        }
        final HttpSession session = request.getSession(false);
        Object timeMachineObject = session != null ? session.getAttribute(PageResource.TM_DATE) : null;
        if(null != timeMachineObject){
            return Optional.of(timeMachineObject.toString());
        }
        timeMachineObject = request.getAttribute(PageResource.TM_DATE);
        return Optional.ofNullable(timeMachineObject != null ? timeMachineObject.toString() : null);
    }

    /**
     * If Time Machine is running return the timestamp of the Time Machine date as a Date
     * @return Optional<Date>
     */
    public static Optional<Date> getTimeMachineDateAsDate() {
        final Optional<String> timeMachine = getTimeMachineDate();
        if (timeMachine.isPresent()) {
            final String tmDate = timeMachine.get();
            return Optional.of(new Date(Long.parseLong(tmDate)));
        }
        return Optional.empty();
    }

    /**
     * Return true if Time Machine is running, otherwise return false
     * @return boolean
     */
    public static boolean isRunning(){
        final Optional<String> timeMachine = getTimeMachineDate();
        return timeMachine.isPresent();
    }

    /**
     * Return true if Time Machine is not running, otherwise return false
     * @return boolean
     */
    public static boolean isNotRunning(){
        return !isRunning();
    }

    /**
     * Parses and validates the given date string in ISO 8601 format.
     *
     * @param dateAsISO8601 The date string in ISO 8601 format. If null, an empty {@link Optional} is returned.
     * @return An {@link Optional} containing a valid {@link Instant} if parsing is successful and the date meets the validation criteria.
     *         Returns an empty {@link Optional} if the date is invalid or does not meet the validation criteria.
     * @throws IllegalArgumentException If the date string cannot be parsed.
     */
    public static Optional<Instant> parseTimeMachineDate(final String dateAsISO8601){
        return parseTimeMachineDate(dateAsISO8601, true);
    }

    /**
     * Parses and validates the given date string in ISO 8601 format.
     * @param dateAsISO8601 The date string in ISO 8601 format. If null, an empty {@link Optional} is returned.
     * @param applyGraceWindow If true, the grace window logic is applied to the date.
     * @return
     */
    @VisibleForTesting
    public static Optional<Instant> parseTimeMachineDate(final String dateAsISO8601,final boolean applyGraceWindow) {
        // Early return for null input
        if (Objects.isNull(dateAsISO8601)) {
            return Optional.empty();
        }

        // Handle possible null from the Try operation
        Try<Date> dateTry = Try.of(() -> DateUtil.convertDate(dateAsISO8601));

        // If the date conversion fails or returns null, return empty
        if (dateTry.isFailure() || dateTry.get() == null) {
            throw new IllegalArgumentException(
                    String.format("Error Parsing date: %s", dateAsISO8601));
        }

        // Proceed with converting to Instant

        Instant instant = dateTry.map(date ->
                        date.toInstant().atZone(ZoneId.of("UTC")).toInstant())
                .getOrElseThrow(e ->
                        new IllegalArgumentException(
                                String.format("Error Parsing date: %s", dateAsISO8601), e)
                );

        // If the grace window is not to be applied, return the instant
        if (!applyGraceWindow) {
            return Optional.of(instant);
        }
        return isOlderThanGraceWindow(instant) ? Optional.of(instant) : Optional.empty();

    }


    /**
     * Determines if the FTM logic should be applied based on the given timeMachineDate.
     * It checks if the date is older than the grace window (not too recent),
     * using a configurable time limit.
     *
     * @param timeMachineDate The Time Machine date from the request.
     * @return true if the timeMachineDate is older than the grace window, meaning FTM logic should be applied,
     *         false otherwise (if within the grace window).
     */
    public static boolean isOlderThanGraceWindow(final Instant timeMachineDate) {
        final Instant graceWindowTime = Instant.now().plus(Duration.ofMinutes(FTM_GRACE_WINDOW_LIMIT.get()));
        return timeMachineDate.isAfter(graceWindowTime);
    }
}

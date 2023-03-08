package com.dotcms.experiments.business.result;

import com.dotcms.experiments.business.result.VariantResults.ResultResumeItem;

import com.dotcms.experiments.model.ExperimentVariant;
import com.dotmarketing.util.UtilMethods;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;

/**
 * Builder of {@link VariantResults}
 */
public class VariantResultsBuilder {
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            .withZone(ZoneId.systemDefault());

    private final DateTimeFormatter PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.n")
            .withZone(ZoneId.systemDefault());
    final ExperimentVariant experimentVariant;
    private Map<String, List<Event>> eventsByLookBackWindow = new HashMap<>();
    private long totalSessions;
    private long totalVariantSessions;

    VariantResultsBuilder(final ExperimentVariant experimentVariant) {
        this.experimentVariant = experimentVariant;
    }

    private ArrayList<Event> createEventsList(final String lookBackWindow) {
        final ArrayList<Event> list = new ArrayList<>();
        eventsByLookBackWindow.put(lookBackWindow, list);
        return list;
    }


    public void success(final String lookBackWindow, final Event event) {
        final List<Event> sessionEvents = UtilMethods.isSet(eventsByLookBackWindow.get(lookBackWindow)) ?
                eventsByLookBackWindow.get(lookBackWindow) : createEventsList(lookBackWindow);

        sessionEvents.add(event);
    }

    private int totalMultiBySession(final Map<String, List<Event>> events) {
        return events.values().stream()
                .map(sessionEvents -> sessionEvents.size())
                .mapToInt(Integer::intValue)
                .sum();
    }


    private int totalUniqueBySession(final Map<String, List<Event>> events) {
        return events.size();
    }

    public VariantResults build() {
        final VariantResults.UniqueBySessionResume uniqueBySessionResume = new VariantResults.UniqueBySessionResume(
                totalUniqueBySession(eventsByLookBackWindow),
                totalVariantSessions,
                totalSessions);

        final Map<String, ResultResumeItem> details = getDetails();

        return new VariantResults(experimentVariant.id(),
                totalMultiBySession(eventsByLookBackWindow),
                uniqueBySessionResume,
                details);
    }

    private Map<String, ResultResumeItem> getDetails() {
        final Map<String, Map<String, List<Event>>> eventsOrderByDate = orderEventsByDate();
        final Map<String, ResultResumeItem> result = new HashMap<>();

        for (Entry<String, Map<String, List<Event>>> entry : eventsOrderByDate.entrySet()) {
            final String dateAsString = entry.getKey();
            final Map<String, List<Event>> events = entry.getValue();

            result.put(dateAsString, new ResultResumeItem(totalMultiBySession(events),
                    totalUniqueBySession(events)
            ));
        }

        return result;
    }

    @NotNull
    private Map<String, Map<String, List<Event>>> orderEventsByDate() {
        final Map<String, Map<String, List<Event>>> eventsOrderByDate = new HashMap<>();

        for (Entry<String, List<Event>> entry : eventsByLookBackWindow.entrySet()) {
            final String lookBackWindow = entry.getKey();

            for (Event event : entry.getValue()) {

                final String eventDateFormatted = event.get("utcTime")
                        .map(dateAsObject -> dateAsObject.toString())
                        .map(dateAsString -> PARSER.parse(dateAsString))
                        .map(eventDate -> FORMATTER.format(eventDate))
                        .orElseThrow();

                final Map<String, List<Event>> detailsByDate = eventsOrderByDate.getOrDefault(eventDateFormatted,
                        new HashMap<>());
                eventsOrderByDate.put(eventDateFormatted, detailsByDate);

                final List<Event> events = detailsByDate.getOrDefault(lookBackWindow, new ArrayList<>());
                events.add(event);
                detailsByDate.put(lookBackWindow, events);
            }
        }
        return eventsOrderByDate;
    }

    public void setTotalSession(long total) {
        this.totalSessions = total;
    }

    public void setTotalSessionToVariant(final long total) {
        this.totalVariantSessions = total;
    }
}

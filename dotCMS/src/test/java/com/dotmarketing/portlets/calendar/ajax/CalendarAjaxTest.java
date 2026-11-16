package com.dotmarketing.portlets.calendar.ajax;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * The remote methods the calendar screens use. Events are saved by the edit content screen, so
 * this class offers no save of its own.
 */
class CalendarAjaxTest {

    @Test
    void remoteMethods_doNotIncludeSaveEvent() {
        // Given the public methods of the class (every one of them is callable remotely)
        final boolean hasSaveEvent = Arrays.stream(CalendarAjax.class.getMethods())
                .map(Method::getName)
                .anyMatch("saveEvent"::equals);

        // Then saveEvent is not one of them
        assertFalse(hasSaveEvent, "events are saved by the edit content screen");
    }
}

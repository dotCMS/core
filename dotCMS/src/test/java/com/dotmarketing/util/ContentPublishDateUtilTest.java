package com.dotmarketing.util;

import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.concurrent.Debouncer;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Identifier;
import com.liferay.portal.language.LanguageUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ContentPublishDateUtil}.
 * <p>
 * Verifies behavior related to future publish date checks, system message handling,
 * and the use of debouncing for user notifications.
 */
class ContentPublishDateUtilTest {

    private ContentType contentType;
    private Identifier identifier;
    private Debouncer originalDebouncer;
    private Debouncer debouncerMock;

    /**
     * Initializes mocks and injects a mock Debouncer into the utility class before each test.
     */
    @BeforeEach
    void setUp() {
        contentType = mock(ContentType.class);
        identifier = mock(Identifier.class);
        originalDebouncer = ContentPublishDateUtil.getDebouncer();
        debouncerMock = mock(Debouncer.class);
        ContentPublishDateUtil.setDebouncer(debouncerMock);
    }

    @AfterEach
    void tearDown() {
        ContentPublishDateUtil.setDebouncer(originalDebouncer);
    }

    /**
     * Verifies that when the publish date is in the future and the content type has a publish field,
     * the method returns true and attempts to send a user message.
     */
    @Test
    void test_notifyIfFuturePublishDate_shouldReturnTrueAndSendMessage() {
        when(contentType.publishDateVar()).thenReturn("publishDate");

        Calendar future = Calendar.getInstance();
        future.add(Calendar.HOUR, 1);
        when(identifier.getSysPublishDate()).thenReturn(future.getTime());

        try (
                MockedStatic<LanguageUtil> langMock = Mockito.mockStatic(LanguageUtil.class);
                MockedStatic<SystemMessageEventUtil> msgMock = Mockito.mockStatic(SystemMessageEventUtil.class);
                MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)
        ) {
            langMock.when(() -> LanguageUtil.get("message.contentlet.publish.future.date"))
                    .thenReturn("Future publish message");

            SystemMessageEventUtil mockUtil = mock(SystemMessageEventUtil.class);
            msgMock.when(SystemMessageEventUtil::getInstance).thenReturn(mockUtil);

            boolean result = ContentPublishDateUtil.notifyIfFuturePublishDate(contentType, identifier, "user123");

            assertTrue(result);
        }
    }

    /**
     * Ensures that the method returns false when the content type does not define a publish date field.
     */
    @Test
    void test_notifyIfFuturePublishDate_shouldReturnFalseIfNoPublishDate() {
        when(contentType.publishDateVar()).thenReturn(null);

        boolean result = ContentPublishDateUtil.notifyIfFuturePublishDate(contentType, identifier, "user123");

        assertFalse(result);
    }

    /**
     * Ensures that the method returns false when the publish date is in the past.
     */
    @Test
    void test_notifyIfFuturePublishDate_shouldReturnFalseIfPublishDateNotInFuture() {
        when(contentType.publishDateVar()).thenReturn("publishDate");

        Calendar past = Calendar.getInstance();
        past.add(Calendar.HOUR, -1);
        when(identifier.getSysPublishDate()).thenReturn(past.getTime());

        boolean result = ContentPublishDateUtil.notifyIfFuturePublishDate(contentType, identifier, "user123");

        assertFalse(result);
    }

    /**
     * Verifies that the debouncer is invoked when a future publish date is detected.
     */
    @Test
    void test_debouncerCalled_whenFutureDate() {
        when(contentType.publishDateVar()).thenReturn("publishDate");

        Calendar future = Calendar.getInstance();
        future.add(Calendar.MINUTE, 10);
        when(identifier.getSysPublishDate()).thenReturn(future.getTime());

        boolean result = ContentPublishDateUtil.notifyIfFuturePublishDate(contentType, identifier, "userX");

        assertTrue(result);

        verify(debouncerMock).debounce(
                eq("contentPublishDateErroruserX"),
                any(Runnable.class),
                eq(5000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    /**
     * Verifies that the debouncer is not invoked when the publish date is not in the future.
     */
    @Test
    void test_debouncerNotCalled_whenPublishDateIsPast() {
        when(contentType.publishDateVar()).thenReturn("publishDate");

        Calendar past = Calendar.getInstance();
        past.add(Calendar.MINUTE, -10);
        when(identifier.getSysPublishDate()).thenReturn(past.getTime());

        boolean result = ContentPublishDateUtil.notifyIfFuturePublishDate(contentType, identifier, "userX");

        assertFalse(result);
        verifyNoInteractions(debouncerMock);
    }

    /**
     * Verifies that the Runnable passed to the debouncer sends the correct user message
     * when manually executed.
     */
    @Test
    void test_debouncerRunnable_executesSystemMessage() {
        Runnable[] capturedRunnable = new Runnable[1];

        when(contentType.publishDateVar()).thenReturn("publishDate");

        Calendar future = Calendar.getInstance();
        future.add(Calendar.MINUTE, 5);
        when(identifier.getSysPublishDate()).thenReturn(future.getTime());

        doAnswer(invocation -> {
            capturedRunnable[0] = invocation.getArgument(1);
            return null;
        }).when(debouncerMock).debounce(any(), any(), anyLong(), any());

        try (
                MockedStatic<LanguageUtil> langMock = Mockito.mockStatic(LanguageUtil.class);
                MockedStatic<SystemMessageEventUtil> msgMock = Mockito.mockStatic(SystemMessageEventUtil.class);
                MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)
        ) {
            langMock.when(() -> LanguageUtil.get("message.contentlet.publish.future.date"))
                    .thenReturn("Scheduled in future");

            SystemMessageEventUtil mockUtil = mock(SystemMessageEventUtil.class);
            msgMock.when(SystemMessageEventUtil::getInstance).thenReturn(mockUtil);

            ContentPublishDateUtil.notifyIfFuturePublishDate(contentType, identifier, "user123");

            assertNotNull(capturedRunnable[0]);
            capturedRunnable[0].run();

            verify(mockUtil).pushMessage(any(), eq(java.util.List.of("user123")));
            loggerMock.verify(() -> Logger.debug(eq(ContentPublishDateUtil.class), anyString()));
        }
    }
}

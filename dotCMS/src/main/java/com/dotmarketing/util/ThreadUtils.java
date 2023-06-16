package com.dotmarketing.util;

import java.util.Optional;
import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;

public final class ThreadUtils {

    private ThreadUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void sleep(final long sleepTime) {

        Try.run(() -> Thread.sleep(sleepTime)).onFailure(DotRuntimeException::new);

    }


    public static Thread getThread(final String name) {

        if (name == null) {
            return null;
        }


        Optional<Thread> thread = Thread.getAllStackTraces().keySet().parallelStream()
                        .filter(t -> t.getName().equalsIgnoreCase(name)).findFirst();


        return thread.isPresent() ? thread.get() : null;
    }



}

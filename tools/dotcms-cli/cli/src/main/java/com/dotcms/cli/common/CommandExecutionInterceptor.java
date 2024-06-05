package com.dotcms.cli.common;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.dotcms.api.client.util.DirectoryWatcherService;
import com.dotcms.cli.command.DotPush;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.concurrent.BlockingQueue;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.jboss.logging.Logger;

@CommandInterceptor
@Interceptor
public class CommandExecutionInterceptor {

    @Inject
    Logger logger;

    @Inject
    DirectoryWatcherService service;

    // ThreadLocal to track the recursion depth for the interceptor
    private static final ThreadLocal<Integer> callDepth = ThreadLocal.withInitial(() -> 0);

    @AroundInvoke
    public Object invoke(final InvocationContext context) throws Exception {

        try {
            // Increment the call depth
            callDepth.set(callDepth.get() + 1);
            if (callDepth.get() > 1) {
                // If the call depth is greater than 1, we are in a recursive call and should not intercept
                return context.proceed();
            }
            // otherwise, we are in the first call and should intercept
            final Object target = context.getTarget();
            logger.debug("Executing command: " + context.getTarget());
            if (target instanceof DotPush) {
                final DotPush push = (DotPush) target;
                //Otherwise, we are in the first call and should intercept
                final PushMixin filesPushMixin = push.getPushMixin();
                if (filesPushMixin.isWatchMode()) {
                    push.getOutput().info("Running in Watch Mode on " + filesPushMixin.path());
                    Object result = null;
                    final Path path = filesPushMixin.path();
                    final BlockingQueue<WatchEvent<?>> eventQueue = service.watch(path, filesPushMixin.interval);
                    while (service.isRunning()) {
                        final WatchEvent<?> event = eventQueue.take();
                        if (event.kind() == OVERFLOW) {
                            continue;
                        }
                        try{
                            //Disengage the watch service to avoid recursion issues
                            //The command itself might trigger a file change
                            service.suspend();
                            result = context.proceed();
                        }finally {
                            //Re-engage the watch mode
                            service.resume();
                        }
                    }
                    return result;
                }
            }
            return context.proceed();
        } finally {
            // Decrement the call depth
            callDepth.set(callDepth.get() - 1);
            // Clean up ThreadLocal if it's no longer needed
            if (callDepth.get() == 0) {
                callDepth.remove();
            }
        }
    }


}
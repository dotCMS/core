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

    @AroundInvoke
    public Object invoke(final InvocationContext context) throws Exception {
        final Object target = context.getTarget();
        logger.debug("Executing command: " + context.getTarget());
        if (target instanceof DotPush) {
            final DotPush push = (DotPush) target;
            final PushMixin filesPushMixin = push.getPushMixin();
            if (filesPushMixin.isWatchMode()) {
                Object result = null;
                final Path path = filesPushMixin.path();
                final BlockingQueue<WatchEvent<?>> eventQueue = service.watch(path, filesPushMixin.interval);
                while (service.isRunning()) {
                    final WatchEvent<?> event = eventQueue.take();
                    if (event.kind() == OVERFLOW) {
                        continue;
                    }
                   result = context.proceed();
                }
                return result;
            }
        }
        return context.proceed();
    }


}
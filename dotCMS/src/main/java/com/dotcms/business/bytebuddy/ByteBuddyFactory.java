package com.dotcms.business.bytebuddy;

import com.dotcms.business.CloseDB;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.LogTime;
import com.dotmarketing.util.Logger;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Initializes ByteBuddy to handle transactional annotations. This replaces AspectJ functionality
 * and injects at runtime. This should be initialized as early as possible and before any methods
 * using the annotations are called.
 *
 * @author sbolton
 */
public class ByteBuddyFactory {

    private static final AtomicBoolean agentLoaded = new AtomicBoolean(false);

    static {
        init();
    }

    public static void init() {
        if (!agentLoaded.get()) {
            try {
                premain(null, ByteBuddyAgent.install());
                Logger.info(ByteBuddyFactory.class, "Loaded ByteBuddy Advice");
            } catch (Exception e) {
                Logger.error(ByteBuddyFactory.class, "Cannot install ByteBuddy Advice");
            }
        }
    }


    public static void premain(final String arg, final Instrumentation inst) throws Exception {
        Logger.info(ByteBuddyFactory.class, "Starting ByteBuddy Agent");
        if (!agentLoaded.compareAndSet(false, true)) {
            return;
        }
        AgentBuilder.LocationStrategy bootFallbackLocationStrategy = new AgentBuilder.LocationStrategy() {
            @Override
            public ClassFileLocator classFileLocator(final ClassLoader classLoader, final JavaModule module) {
                return new ClassFileLocator.Compound(ClassFileLocator.ForClassLoader.of(classLoader),
                        ClassFileLocator.ForClassLoader.of(this.getClass().getClassLoader()));
            }
        };

        try {
            AgentBuilder.RedefinitionListenable.WithoutBatchStrategy builder = new AgentBuilder.Default()
                    .with(bootFallbackLocationStrategy)
                    // Filtering by package name is quicker than by annotation
                    .ignore(nameStartsWith("net.bytebuddy."))
                    .ignore(nameStartsWith("com.dotcms.repackage."))
                    .ignore(nameStartsWith("com.dotcms.business.bytebuddy"))
                    .disableClassFormatChanges()
                    .with(AgentBuilder.InitializationStrategy.Minimal.INSTANCE)
                    .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);

            /*
             * We do not use Config class for this as we do not want to load classes that may need
             * transformation or require annotation processing.
             */
            if (Boolean.parseBoolean(System.getenv("DOTCMS_BYTEBUDDY_DEBUG"))) {
                builder.with(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemError())
                        .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
                        .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError());
            }

            builder.type(ElementMatchers.declaresMethod(
                            isAnnotatedWith(WrapInTransaction.class)
                                    .or(isAnnotatedWith(CloseDB.class))
                                    .or(isAnnotatedWith(CloseDBIfOpened.class))
                                    .or(isAnnotatedWith(LogTime.class))
                    ))

                    .transform(getAdvice(WrapInTransaction.class, WrapInTransactionAdvice.class))
                    .transform(getAdvice(CloseDB.class, CloseDBAdvice.class))
                    .transform(getAdvice(CloseDBIfOpened.class, CloseDBIfOpenedAdvice.class))
                    .transform(getAdvice(LogTime.class, LogTimeAdvice.class))

                    .installOn(inst);
            Logger.info(ByteBuddyFactory.class, "ByteBuddy Initialized");
        } catch (Exception e) {
            Logger.error(ByteBuddyFactory.class, "Error Initializing ByteBuddy", e);
        }


    }

    private static AgentBuilder.Transformer.ForAdvice getAdvice(Class<? extends Annotation> annotation, Class<?> advice) {
        return new AgentBuilder.Transformer.ForAdvice()
                .withExceptionHandler(Advice.ExceptionHandler.Default.RETHROWING)
                .advice(isMethod().and(isAnnotatedWith(annotation)),
                        advice.getName());
    }
}

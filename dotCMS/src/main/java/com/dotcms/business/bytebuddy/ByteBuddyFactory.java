package com.dotcms.business.bytebuddy;

import com.dotcms.business.CloseDB;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.test.TransactionalTester;
import com.dotcms.util.LogTime;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.logging.log4j.Level;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Initializes ByteBuddy to handle transactional annotations.  This replaces
 * AspectJ functionality and injects at runtime.  This should be initialized
 * as early as possible and before any methods using the annotations are called.
 *
 * @author sbolton
 */
public class ByteBuddyFactory {

    private static final ByteBuddyFactory INSTANCE = new ByteBuddyFactory();

    private ByteBuddyFactory() {
        init();
    }

    private void init() {
        try {
            premain(null, ByteBuddyAgent.install());
            Logger.info(ByteBuddyFactory.class, "Loaded ByteBuddy Advice");

        } catch (Exception e) {
            Logger.error(ByteBuddyFactory.class, "Cannot install ByteBuddy Advice");
        }
    }


    public static ByteBuddyFactory getInstance() {
        return INSTANCE;
    }


    public void premain(final String arg, final Instrumentation inst)  throws Exception  {

        AgentBuilder.LocationStrategy bootFallbackLocationStrategy = new AgentBuilder.LocationStrategy() {
            @Override
            public ClassFileLocator classFileLocator(final ClassLoader classLoader, final JavaModule module) {
                return new ClassFileLocator.Compound(ClassFileLocator.ForClassLoader.of(classLoader), ClassFileLocator.ForClassLoader.of(this.getClass().getClassLoader()));
            }
        };

    try {
        AgentBuilder.RedefinitionListenable.WithoutBatchStrategy builder = new AgentBuilder.Default()

                .ignore(nameEndsWith("LocalTransactionAndCloseDBIfOpenedFactoryTest"))
                .or(nameStartsWith("net.bytebuddy.")
                        .or(nameStartsWith("sun.reflect."))
                        .or(isSynthetic()), any(), any())
                .or(nameStartsWith("com.dotcms.repackage."))
                .with(AgentBuilder.InitializationStrategy.Minimal.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
                /*
                We do not use Config class for this as we do not want to load classes
                that may need transformation or require annotation processing.
                */
                if (Boolean.parseBoolean(System.getenv("DOTCMS_BYTEBUDDY_DEBUG")))
                {
                    builder.with(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemError())
                        .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
                        .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError());
                }
                builder.with(bootFallbackLocationStrategy)
                .type(ElementMatchers.nameStartsWith("com.dotcms"))
                .transform(
                        new AgentBuilder.Transformer.ForAdvice()
                                .withExceptionHandler(Advice.ExceptionHandler.Default.RETHROWING)
                                .advice(isMethod().and(isAnnotatedWith(CloseDB.class))
                                        , CloseDBAdvice.class.getName())
                )

                .transform(
                        new AgentBuilder.Transformer.ForAdvice()
                                .withExceptionHandler(Advice.ExceptionHandler.Default.RETHROWING)
                                .advice(isMethod().and(isAnnotatedWith(CloseDBIfOpened.class))
                                        , CloseDBIfOpenedAdvice.class.getName())
                )

                .transform(
                        new AgentBuilder.Transformer.ForAdvice()

                                .withExceptionHandler(Advice.ExceptionHandler.Default.RETHROWING)
                                .advice(isMethod().and(isAnnotatedWith(WrapInTransaction.class))
                                        , WrapInTransactionAdvice.class.getName())
                )


                .transform(
                        new AgentBuilder.Transformer.ForAdvice()
                                .advice(isMethod().and(this::checkLogLevelRequired)
                                        , LogTimeAdvice.class.getName())
                )

                .installOn(inst);
        Logger.info(ByteBuddyFactory.class,"ByteBuddy Initialized");
    } catch (Exception e)
    {
        Logger.error(ByteBuddyFactory.class, "Error Initializing ByteBuddy",e);
    }

    }

    private boolean checkLogLevelRequired(final MethodDescription target) {
        boolean result = false;
        AnnotationDescription.Loadable<LogTime> time = target.getDeclaredAnnotations().ofType(LogTime.class);
        if (time != null) {
            String level = time.getValue("loggingLevel").resolve(String.class);
            String declaringType = target.getDeclaringType().toString();
            result = (Logger.isDebugEnabled(declaringType) || Level.INFO.toString().equals(level));
        }
        return result;
    }
}
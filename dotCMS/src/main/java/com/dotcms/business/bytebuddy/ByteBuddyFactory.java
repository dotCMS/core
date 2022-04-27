package com.dotcms.business.bytebuddy;

import com.dotcms.business.CloseDB;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.LogTime;
import com.dotmarketing.util.Logger;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.logging.log4j.Level;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Initializes ByteBuddy to handle transactional annotations.  This replaces
 * AspectJ functionality and injects at runtime.  This should be initialized
 * as early as possible and before any methods using the annotations are called.
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


    public static void premain(final String arg, final Instrumentation inst)  throws Exception  {
        System.out.println("In ByteBuddyAgent premain");
        if (!agentLoaded.compareAndSet(false,true))
            return;
        AgentBuilder.LocationStrategy bootFallbackLocationStrategy = new AgentBuilder.LocationStrategy() {
            @Override
            public ClassFileLocator classFileLocator(final ClassLoader classLoader, final JavaModule module) {
                return new ClassFileLocator.Compound(ClassFileLocator.ForClassLoader.of(classLoader), ClassFileLocator.ForClassLoader.of(this.getClass().getClassLoader()));
            }
        };

    try {
                AgentBuilder builder = new AgentBuilder.Default()
                .ignore(nameStartsWith("net.bytebuddy."))
                .ignore(nameStartsWith("com.dotcms.repackage."))
                        .ignore(nameStartsWith("com.dotcms.business.bytebuddy"))
                        .ignore(nameEndsWith("LocalTransactionAndCloseDBIfOpenedFactoryTest"))
                        .ignore(nameEndsWith("FolderAPITest"))
                .disableClassFormatChanges()
                .with(AgentBuilder.InitializationStrategy.Minimal.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);

                /*
                We do not use Config class for this as we do not want to load classes
                that may need transformation or require annotation processing.
                */
                if (Boolean.parseBoolean(System.getenv("DOTCMS_BYTEBUDDY_DEBUG")))
                {
                   builder = ((AgentBuilder.RedefinitionListenable)builder).with(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemError())
                            .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
                            .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError());
                }

                 builder.with(bootFallbackLocationStrategy)
                .type(ElementMatchers.nameStartsWith("com.dotcms"))
                         .or(ElementMatchers.nameStartsWith("com.dotmarketing"))
                         .or(ElementMatchers.nameStartsWith("com.liferay"))
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
                                .advice(isMethod().and(ByteBuddyFactory::checkLogLevelRequired)
                                        , LogTimeAdvice.class.getName())
                )

                .installOn(inst);
        Logger.info(ByteBuddyFactory.class,"ByteBuddy Initialized");
    } catch (Exception e)
    {
        Logger.error(ByteBuddyFactory.class, "Error Initializing ByteBuddy",e);
    }

    }

    private static boolean checkLogLevelRequired(final MethodDescription target) {
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
package com.dotcms.business.bytebuddy;

import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

import com.dotcms.business.CloseDB;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.ExternalTransaction;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestCostAdvice;
import com.dotcms.util.EnterpriseFeature;
import com.dotcms.util.LogTime;
import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Initializes ByteBuddy to handle transactional annotations. This replaces AspectJ functionality
 * and injects at runtime. This should be initialized as early as possible and before any methods
 * using the annotations are called.
 *
 * @author sbolton
 */
public class ByteBuddyFactory {

    // Use base log4j logger here to prevent recursive issues with internal Logger class
    private static final Logger LOGGER = LogManager.getLogger(ByteBuddyFactory.class);

    private static final AtomicBoolean agentLoaded = new AtomicBoolean(false);
    private static final Map<Class<? extends Annotation>, Class<?>> adviceMap = Map.of(
            WrapInTransaction.class, WrapInTransactionAdvice.class,
            CloseDB.class, CloseDBAdvice.class,
            CloseDBIfOpened.class, CloseDBIfOpenedAdvice.class,
            LogTime.class, LogTimeAdvice.class,
            EnterpriseFeature.class, EnterpriseFeatureAdvice.class,
            ExternalTransaction.class, ExternalTransactionAdvice.class,
            RequestCost.class, RequestCostAdvice.class
    );


    private static final Set<String> packageIgnore = Set.of(
            "com.dotcms.repackage.",
            "net.bytebuddy.",
            "sun.reflect."
    );

    private static final Set<String> packageWhitelist = Set.of(
            "com.dotcms",
            "com.dotmarketing",
            "com.liferay"
    );

    static {
        init();
    }

    public static void init() {
        if (!agentLoaded.get()) {
            try {
                premain(null, ByteBuddyAgent.install());
                LOGGER.info("Loaded ByteBuddy Advice");
            } catch (Exception e) {
                LOGGER.error("Cannot install ByteBuddy Advice");
            }
        }
    }


    public static void premain(final String arg, final Instrumentation inst) throws Exception {
        LOGGER.info("Starting ByteBuddy Agent");
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

        ElementMatcher.Junction<NamedElement> selectByName = ElementMatchers.none();
        for (String packageElement : packageWhitelist)
            selectByName = selectByName.or(nameStartsWith(packageElement));

        ElementMatcher.Junction<NamedElement> ignoresByName = ElementMatchers.none();
        for (String packageElement : packageIgnore)
            ignoresByName = ignoresByName.or(nameStartsWith(packageElement));

        // will filter by name first and then by annotation
        ElementMatcher.Junction<TypeDefinition> classMatcher = selectByName.and(hasAnnotatedMethods());

        try {
            new AgentBuilder.Default()
                    .with(bootFallbackLocationStrategy)
                    // Filtering by package name is quicker than by annotation
                    .ignore(ignoresByName)
                    .or(isSynthetic())
                    .disableClassFormatChanges()
                    .with(new ByteBuddyLogListener())
                    .with(AgentBuilder.InitializationStrategy.Minimal.INSTANCE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(classMatcher)
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                        DynamicType.Builder<?> newBuilder = builder;
                        for (Map.Entry<Class<? extends Annotation>, Class<?>> entry : adviceMap.entrySet()) {
                            newBuilder = getAdvice(entry.getKey(), entry.getValue()).transform(newBuilder,
                                    typeDescription, classLoader, module, protectionDomain);
                        }
                        return newBuilder;
                    })

                    .installOn(inst);

            // Explicitly retransform VelocityUtil if it's already loaded
            try {
                Class<?> velocityUtilClass = Class.forName("com.dotcms.rendering.velocity.util.VelocityUtil", false,
                        Thread.currentThread().getContextClassLoader());
                if (velocityUtilClass != null) {
                    LOGGER.info("VelocityUtil was already loaded, retransforming it now");
                    inst.retransformClasses(velocityUtilClass);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.debug("VelocityUtil not yet loaded, will be transformed when loaded");
            } catch (Exception e) {
                LOGGER.warn("Could not retransform VelocityUtil", e);
            }





            LOGGER.info("ByteBuddy Initialized");
        } catch (Exception e) {
            LOGGER.error("Error Initializing ByteBuddy", e);
        }


    }

    private static ElementMatcher.Junction<TypeDefinition> hasAnnotatedMethods() {
        ElementMatcher.Junction<AnnotationSource> methodMatcher = ElementMatchers.none();
        for (Class<? extends Annotation> annotation : adviceMap.keySet()) {
            methodMatcher = methodMatcher.or(isAnnotatedWith(annotation));
        }
        return declaresMethod(methodMatcher);
    }


    private static AgentBuilder.Transformer getAdvice(Class<? extends Annotation> annotation, Class<?> advice) {
        return new AgentBuilder.Transformer.ForAdvice()
                .withExceptionHandler(Advice.ExceptionHandler.Default.RETHROWING)
                .advice(isMethod().and(isAnnotatedWith(annotation)),
                        advice.getName());
    }
}

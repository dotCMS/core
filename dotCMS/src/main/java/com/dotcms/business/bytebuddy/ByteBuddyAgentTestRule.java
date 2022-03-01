package com.dotcms.business.bytebuddy;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.instrument.Instrumentation;

public class ByteBuddyAgentTestRule implements TestRule {

    private final boolean available;

    public ByteBuddyAgentTestRule() {
        available = ByteBuddyAgent.AttachmentProvider.DEFAULT.attempt().isAvailable();
    }


    public Statement apply(Statement base, Description description) {
        if (available)
        {
            Instrumentation instrumentation = ByteBuddyAgent.install(ByteBuddyAgent.AttachmentProvider.DEFAULT);
            System.out.println("instrumentations redefine=" + instrumentation.isRedefineClassesSupported() + ": retransform=" + instrumentation.isRetransformClassesSupported() + ": nativeMethodPrefix=" + instrumentation.isNativeMethodPrefixSupported());
        }
        return base;
    }
}

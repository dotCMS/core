package com.dotmarketing.util;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

/**
 * Created by nollymar on 9/16/16.
 */
public class TestInitialContextFactory implements InitialContextFactoryBuilder {

    @Override
    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment)
        throws NamingException {
        return new InitialContextFactory() {

            @Override
            public Context getInitialContext(Hashtable<?, ?> environment)
                throws NamingException {
                return TestInitialContext.getInstance();
            }
        };
    }
}

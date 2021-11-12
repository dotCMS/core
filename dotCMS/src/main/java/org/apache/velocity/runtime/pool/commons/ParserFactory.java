package org.apache.velocity.util;

import com.dotmarketing.util.Logger;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.CharStream;
import org.apache.velocity.runtime.parser.Parser;

public class ParserFactory extends BasePooledObjectFactory<Parser> {
    private final RuntimeServices rsvc;

    long totalParsers = 0;
    long totalParserCreationTime = 0;

    public ParserFactory(RuntimeServices rsvc) {
        this.rsvc = rsvc;
    }
    @Override
    public Parser create() throws Exception {
        long start = System.nanoTime();
        try {
            return rsvc.createNewParser();
        } finally {
            totalParserCreationTime += (System.nanoTime() - start);
            totalParsers++;
        }
    }

    @Override
    public PooledObject<Parser> wrap(Parser parser) {
        return new DefaultPooledObject<>(parser);
    }

    public boolean validateObject(PooledObject<Parser> p) {
        // can check parser is still good here
        return true;
    }

    public void activateObject(PooledObject<Parser> p) throws Exception {
        //Parser parser = p.getObject();
        Logger.info(getClass(), "Activating parser from pool idleTime="+p.getIdleTimeMillis());

    }


    public void passivateObject(PooledObject<Parser> p) throws Exception {
        Parser parser = p.getObject();
        parser.ReInit((CharStream) null);
        Logger.info(getClass(), "Deactivating parser from pool idleTime="+p.getIdleTimeMillis());
    }
}

package com.liferay.util;

import com.dotcms.UnitTestBase;
import org.junit.Test;

import static com.liferay.util.StringUtil.replaceAll;
import static com.liferay.util.StringUtil.replaceOnce;
import static org.junit.Assert.assertEquals;

public class StringUtilTest  extends UnitTestBase {

    @Test
    public void testReplaceOnceNull(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange$1");

        replaceOnce(builder, "$1", null);

        assertEquals( "somethingtochange$1", builder.toString() );

        replaceOnce(builder, null, "this");

        assertEquals( "somethingtochange$1", builder.toString() );

        replaceOnce(null, "$1", "this");

        assertEquals( "somethingtochange$1", builder.toString() );
    }

    @Test
    public void testReplaceOnce(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange$1");

        replaceOnce(builder, "$1", "this");

        assertEquals( "somethingtochangethis", builder.toString() );
    }

    @Test
    public void testReplaceOnce2(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange$2$1");

        replaceOnce(builder, "$1", "this");
        replaceOnce(builder, "$2", "these");

        assertEquals( "somethingtochangethesethis", builder.toString() );
    }

    @Test
    public void testReplaceOnceNothing(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange");

        replaceOnce(builder, "$1", "this");

        assertEquals( "somethingtochange", builder.toString() );
    }



    @Test
    public void testReplaceAllNull(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange$1");

        replaceAll(builder, new String [] {"$1"}, null);

        assertEquals( "somethingtochange$1", builder.toString() );

        replaceAll(builder, null, new String [] {"this"});

        assertEquals( "somethingtochange$1", builder.toString() );

        replaceAll(null, new String [] {"$1"}, new String [] {"this"});

        assertEquals( "somethingtochange$1", builder.toString() );
    }

    @Test
    public void testReplaceAll(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange$1");

        replaceAll(builder, new String [] {"$1"}, new String [] {"this"});

        assertEquals( "somethingtochangethis", builder.toString() );
    }

    @Test
    public void testReplaceAll2(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange$2$1");

        replaceAll(builder, new String [] {"$1","$2"}, new String [] {"this", "these"});

        assertEquals( "somethingtochangethesethis", builder.toString() );
    }

    @Test
    public void testReplaceAllNothing(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange");

        replaceAll(builder, new String [] {"$1","$2"}, new String [] {"this", "these"});

        assertEquals( "somethingtochange", builder.toString() );
    }


    @Test
    public void testReplaceAllInvalidLength(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange$1$2");

        replaceAll(builder, new String [] {"$1","$2"}, new String [] {"this", "these","those"});

        assertEquals( "somethingtochange$1$2", builder.toString() );
    }

    @Test
    public void testReplaceAllInvalidLength2(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange$1$2");

        replaceAll(builder, new String [] {"$1","$2","$3"}, new String [] {"this", "these"});

        assertEquals( "somethingtochange$1$2", builder.toString() );
    }

    @Test
    public void testReplaceAllExtraNotMatchReplacements(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange$1$2");

        replaceAll(builder, new String [] {"$1","$2","$3"}, new String [] {"this", "these","those"});

        assertEquals( "somethingtochangethisthese", builder.toString() );
    }

}

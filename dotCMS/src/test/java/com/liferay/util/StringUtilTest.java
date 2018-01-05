package com.liferay.util;

import com.dotcms.UnitTestBase;
import com.dotcms.enterprise.cmis.query.CMISQueryProcessor;
import org.junit.Test;

import static com.liferay.util.StringUtil.replaceAll;
import static com.liferay.util.StringUtil.replaceAllGroups;
import static com.liferay.util.StringUtil.replaceOnce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

        assertTrue(new CMISQueryProcessor().canSeeIt());
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
    public void testReplaceAllSingle(){

        final StringBuilder builder =
                new StringBuilder("$1$1 is a test, and $1 is another test, $1 this is a great test");

        replaceAll(builder, "$1", "this");

        assertEquals( "thisthis is a test, and this is another test, this this is a great test", builder.toString() );
    }

    @Test
    public void testReplaceAll3(){

        final StringBuilder builder =
                new StringBuilder("$1 is a test, and $1 is another test, $2 are great, are not $2");

        replaceAll(builder, new String []{"$1", "$2"}, new String[] {"this", "these"});

        assertEquals( "this is a test, and this is another test, these are great, are not these", builder.toString() );
    }

    @Test
    public void testReplaceAllExtraNotMatchReplacements(){

        final StringBuilder builder =
                new StringBuilder("somethingtochange$1$2");

        replaceAll(builder, new String [] {"$1","$2","$3"}, new String [] {"this", "these","those"});

        assertEquals( "somethingtochangethisthese", builder.toString() );
    }

    @Test
    public void testReplaceAllGroup(){

        final StringBuilder builder =
                new StringBuilder("$1 is a test, and $1 is another test, $2 are great, are not $2");

        replaceAllGroups(builder, new String[] {"this", "these"});

        assertEquals( "this is a test, and this is another test, these are great, are not these", builder.toString() );
    }

}

package com.dotcms.util;

import com.dotcms.UnitTestBase;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class VanityUrlUtilTest extends UnitTestBase {

    @Test
    public void processExpressionsNullTest()  {

        final VanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setForwardTo("http://www.dotcms.com/helloworld?a=$1&b=$2");
        vanityUrl.setURI("/test/[a-z]{4}/(.*).dot");
        vanityUrl.setAction(1);
        vanityUrl.setOrder(0);
        final CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);

        final CachedVanityUrl newCachedVanityUrl =
                VanityUrlUtil.processExpressions(cachedVanityUrl, null);

        assertNotNull(newCachedVanityUrl);
        assertTrue(cachedVanityUrl == newCachedVanityUrl);
    }

    @Test
    public void processExpressionsZeroLengthTest()  {

        final VanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setForwardTo("http://www.dotcms.com/helloworld?a=$1&b=$2");
        vanityUrl.setURI("/test/[a-z]{4}/(.*).dot");
        vanityUrl.setAction(1);
        vanityUrl.setOrder(0);
        final CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        final String [] matches = new String[] {};

        final CachedVanityUrl newCachedVanityUrl =
                VanityUrlUtil.processExpressions(cachedVanityUrl, matches);

        assertNotNull(newCachedVanityUrl);
        assertTrue(cachedVanityUrl == newCachedVanityUrl);
    }

    @Test
    public void processExpressionsNotExpressionTest()  {

        final VanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setForwardTo("http://www.dotcms.com/helloworld?a=1&b=2");
        vanityUrl.setURI("/test/[a-z]{4}/(.*).dot");
        vanityUrl.setAction(1);
        vanityUrl.setOrder(0);
        final CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        final String [] matches = new String[] { "hello", "world" };

        final CachedVanityUrl newCachedVanityUrl =
                VanityUrlUtil.processExpressions(cachedVanityUrl, matches);

        assertNotNull(newCachedVanityUrl);
        assertTrue(cachedVanityUrl == newCachedVanityUrl);
    }

    @Test
    public void processExpressionsTest()  {

        final VanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setForwardTo("http://www.dotcms.com/helloworld?a=$1");
        vanityUrl.setURI("/test/[a-z]{4}/(.*).dot");
        vanityUrl.setAction(1);
        vanityUrl.setOrder(0);
        final CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        final String [] matches = new String[] { "hello"};

        final CachedVanityUrl newCachedVanityUrl =
                VanityUrlUtil.processExpressions(cachedVanityUrl, matches);

        assertNotNull(newCachedVanityUrl);
        assertTrue(cachedVanityUrl != newCachedVanityUrl);
        assertEquals("http://www.dotcms.com/helloworld?a=hello", newCachedVanityUrl.getForwardTo());
    }

    @Test
    public void processExpressions2Test()  {

        final VanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setForwardTo("http://www.dotcms.com/helloworld?a=$1&b=$2");
        vanityUrl.setURI("/test/[a-z]{4}/(.*).dot");
        vanityUrl.setAction(1);
        vanityUrl.setOrder(0);
        final CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        final String [] matches = new String[] { "hello", "world"};

        final CachedVanityUrl newCachedVanityUrl =
                VanityUrlUtil.processExpressions(cachedVanityUrl, matches);

        assertNotNull(newCachedVanityUrl);
        assertTrue(cachedVanityUrl != newCachedVanityUrl);
        assertEquals("http://www.dotcms.com/helloworld?a=hello&b=world", newCachedVanityUrl.getForwardTo());
    }

    @Test
    public void processExpressions3Test()  {

        final VanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setForwardTo("http://www.dotcms.com/helloworld?a=$1&b=$2&c=$3");
        vanityUrl.setURI("/test/[a-z]{4}/(.*).dot");
        vanityUrl.setAction(1);
        vanityUrl.setOrder(0);
        final CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        final String [] matches = new String[] { "hello", "world", "dot"};

        final CachedVanityUrl newCachedVanityUrl =
                VanityUrlUtil.processExpressions(cachedVanityUrl, matches);

        assertNotNull(newCachedVanityUrl);
        assertTrue(cachedVanityUrl != newCachedVanityUrl);
        assertEquals("http://www.dotcms.com/helloworld?a=hello&b=world&c=dot", newCachedVanityUrl.getForwardTo());
    }

    @Test
    public void processExpressions4Test()  {

        final VanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setForwardTo("http://www.dotcms.$4/helloworld?a=$1&b=$2&c=$3");
        vanityUrl.setURI("/test/[a-z]{4}/(.*).dot");
        vanityUrl.setAction(1);
        vanityUrl.setOrder(0);
        final CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        final String [] matches = new String[] { "hello", "world", "dot","com"};

        final CachedVanityUrl newCachedVanityUrl =
                VanityUrlUtil.processExpressions(cachedVanityUrl, matches);

        assertNotNull(newCachedVanityUrl);
        assertTrue(cachedVanityUrl != newCachedVanityUrl);
        assertEquals("http://www.dotcms.com/helloworld?a=hello&b=world&c=dot", newCachedVanityUrl.getForwardTo());
    }

    @Test
    public void processExpressions5Test()  {

        final VanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setForwardTo("http://$5.dotcms.$4/helloworld?a=$1&b=$2&c=$3");
        vanityUrl.setURI("/test/[a-z]{4}/(.*).dot");
        vanityUrl.setAction(1);
        vanityUrl.setOrder(0);
        final CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        final String [] matches = new String[] { "hello", "world", "dot","com", "www"};

        final CachedVanityUrl newCachedVanityUrl =
                VanityUrlUtil.processExpressions(cachedVanityUrl, matches);

        assertNotNull(newCachedVanityUrl);
        assertTrue(cachedVanityUrl != newCachedVanityUrl);
        assertEquals("http://www.dotcms.com/helloworld?a=hello&b=world&c=dot", newCachedVanityUrl.getForwardTo());
    }

    @Test
    public void processExpressions6Test()  {

        final VanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setForwardTo("http://$5.$6.$4/helloworld?a=$1&b=$2&c=$3");
        vanityUrl.setURI("/test/[a-z]{4}/(.*).dot");
        vanityUrl.setAction(1);
        vanityUrl.setOrder(0);
        final CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        final String [] matches = new String[] { "hello", "world", "dot","com", "www", "dotcms"};

        final CachedVanityUrl newCachedVanityUrl =
                VanityUrlUtil.processExpressions(cachedVanityUrl, matches);

        assertNotNull(newCachedVanityUrl);
        assertTrue(cachedVanityUrl != newCachedVanityUrl);
        assertEquals("http://www.dotcms.com/helloworld?a=hello&b=world&c=dot", newCachedVanityUrl.getForwardTo());
    }

    @Test
    public void processExpressions7Test()  {

        final VanityUrl vanityUrl = new DefaultVanityUrl();
        vanityUrl.setForwardTo("$7://$5.$6.$4/helloworld?a=$1&b=$2&c=$3");
        vanityUrl.setURI("/test/[a-z]{4}/(.*).dot");
        vanityUrl.setAction(1);
        vanityUrl.setOrder(0);
        final CachedVanityUrl cachedVanityUrl = new CachedVanityUrl(vanityUrl);
        final String [] matches = new String[] { "hello", "world", "dot","com", "www", "dotcms","http"};

        final CachedVanityUrl newCachedVanityUrl =
                VanityUrlUtil.processExpressions(cachedVanityUrl, matches);

        assertNotNull(newCachedVanityUrl);
        assertTrue(cachedVanityUrl != newCachedVanityUrl);
        assertEquals("http://www.dotcms.com/helloworld?a=hello&b=world&c=dot", newCachedVanityUrl.getForwardTo());
    }

    @Test
    public void processExpressions8Test()  {

        final Pattern pattern = Pattern.compile("^/am/([0-9]+)/([a-c]+)/([d-f]+)$");
        final Matcher matcher = pattern.matcher("/am/123/abc/def");

        if (matcher.matches() && matcher.groupCount() > 0) {

            System.out.println(matcher.group(1));
            System.out.println(matcher.group(2));
            System.out.println(matcher.group(3));
        }

    }
}

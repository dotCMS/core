package com.dotcms.regex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.awaitility.Awaitility;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.regex.TimeLimitedMatcherFactory.RegExpTimeoutException;
import com.dotmarketing.util.Config;


public class TimeLimitedMatcherFactoryTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // prime regex engine - takes about 800ms
        test_regex_performance();
        Config.setProperty("VANITY_URL_QUARANTINE_MS", 6000L);

    }
    
    static final String[][] evilPatternAndMatchingString = {
            {
                "([a-z0-9\\-]*)*?$",
                "abababababababababababab a"
            }
    };

    static final String[] testPatterns = {
            "([a-z0-9\\-]*)*?$",
            "^((ab)*)+$",
            "(a|aa)+",
            "^(([a-z])+.)+[A-Z]([a-z])+$",
            "^((((https?|ftps?|gopher|telnet|nntp)://)|(mailto:|news:))(%[0-9A-Fa-f]{2}|[-()_.!~*';/?:@&=+$,A-Za-z0-9])+)([).!';/?:,][[:blank:|:blank:]])?$"};



    static final String[] testStrings = {
            "/aaaaaaaaaaaaaaaaaaaaaaaa",
            "a",
            "/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa a",
            "/aaaaaaaaaaaaaaaaaaaaaaaa!",
            "https://www.dotcms.com/aaaaaaaaaaaaaaaaaaaaaa.html",
            "/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa !",
            "abababababababababababab a",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab"


    };


    

    static void test_regex_performance() {
        TimeLimitedMatcherFactory.SLOW_REGEX_CACHE.invalidateAll();
        for (String patternStr : testPatterns) {
            Pattern pattern = Pattern.compile(patternStr);
            System.out.println("");
            System.out.println(" testing   : " + pattern);
            System.out.println(" ------------------------------ ");
            for (String str : testStrings) {
                System.out.println(" string    : " + str);

                long startTime = System.currentTimeMillis();
                boolean matches = false;
                try {
                    matches = TimeLimitedMatcherFactory.matcher(pattern, str).matches();
                } catch (RegExpTimeoutException ret) {
                    System.err.println(" FAILED : " + str);
                }
                long took = (System.currentTimeMillis() - startTime);
                if (took > 0) {
                    System.err.println(" regex took: " + (System.currentTimeMillis() - startTime));
                } else {
                    System.out.println(" regex took: " + (System.currentTimeMillis() - startTime));
                }

                System.out.println(" matches   : " + matches);
            }
        }

    }

    @Test
    public void test_regex_timeout() {
        TimeLimitedMatcherFactory.SLOW_REGEX_CACHE.invalidateAll();

        for (String[] evil : evilPatternAndMatchingString) {
            Pattern pattern = Pattern.compile(evil[0]);
            long startTime = System.currentTimeMillis();
            TimeLimitedMatcherFactory.RegExpTimeoutException exception = assertThrows(TimeLimitedMatcherFactory.RegExpTimeoutException.class,()->
                TimeLimitedMatcherFactory.matcher(pattern, evil[1]).matches()
            );
            long endTime = System.currentTimeMillis();
            
            // the regex returned after the expected timeout time.
            assertTrue(endTime >= startTime + TimeLimitedMatcherFactory.VANITY_URL_REGEX_TIMEOUT);
            
            // the regex returned within 100ms of the expected timeout time
            assertTrue(endTime < startTime + TimeLimitedMatcherFactory.VANITY_URL_REGEX_TIMEOUT + 100);
            
            //we got a good exception message
            assertTrue(exception.getMessage().contains(pattern.toString()));

        }
    }

    @Test
    public void test_regex_quarantine() {
        TimeLimitedMatcherFactory.SLOW_REGEX_CACHE.invalidateAll();

        for (String[] evil : evilPatternAndMatchingString) {
            Pattern pattern = Pattern.compile(evil[0]);
            long quarantineTime =  TimeLimitedMatcherFactory.VANITY_URL_QUARANTINE_MS;
            long runUntil = System.currentTimeMillis() + quarantineTime;
            
            
            // this should throw a timeout and add pattern to quarantine
            assertThrows(TimeLimitedMatcherFactory.RegExpTimeoutException.class,()->
                TimeLimitedMatcherFactory.matcher(pattern, evil[1]).matches()
            );
            
            // while the regex is in quarantine, we should get the NO_MATCH_PATTERN result
            while (System.currentTimeMillis() < runUntil) {
                long startTime = System.currentTimeMillis();
                Matcher matcher = TimeLimitedMatcherFactory.matcher(pattern, evil[1]);
                // we have the NO_MATCH_PATTERN
                assertEquals(TimeLimitedMatcherFactory.NO_MATCH_PATTERN, matcher.pattern());
                
                //asert that we are now returning a no match regex in under 100ms
                assertTrue(System.currentTimeMillis() < startTime + 100);
            }
            
            // wait until the regex is released from quarantine
            Awaitility.await()
                .atMost(quarantineTime,TimeUnit.MILLISECONDS)
                .until(()->TimeLimitedMatcherFactory.SLOW_REGEX_CACHE.getIfPresent(pattern.toString())==null);
            
            
            // this should throw a timeout and add pattern to quarantine AGAIN
            assertThrows(TimeLimitedMatcherFactory.RegExpTimeoutException.class,()->TimeLimitedMatcherFactory.matcher(pattern, evil[1]).matches());
            
            // AGAIN, we have the NO_MATCH_PATTERN
            assertEquals(TimeLimitedMatcherFactory.NO_MATCH_PATTERN, TimeLimitedMatcherFactory.matcher(pattern, evil[1]).pattern());

        }
    }
    
    @Test
    public void test_no_matcher_regex() {
        
        for (String str : testStrings) {
           assertFalse(TimeLimitedMatcherFactory.NO_MATCH_PATTERN.matcher(str).matches());
        }

    }

    

}

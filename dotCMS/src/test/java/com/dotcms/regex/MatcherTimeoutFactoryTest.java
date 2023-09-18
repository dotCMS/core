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
import com.dotcms.regex.MatcherTimeoutFactory.RegExpTimeoutException;
import com.dotcms.regex.MatcherTimeoutFactory.TimeLimitedCharSequence;
import io.vavr.control.Try;


public class MatcherTimeoutFactoryTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // prime regex engine - takes about 800ms
        //test_regex_performance();


    }
    
    static final String[][] evilPatternAndMatchingString = {
            {
                MatcherTimeoutFactory.MATCHER_TIMEOUT_PATTERN,
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
        MatcherTimeoutFactory.SLOW_REGEX_CACHE.invalidateAll();
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
                    matches = MatcherTimeoutFactory.matcher(pattern, str).matches();
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
        MatcherTimeoutFactory.SLOW_REGEX_CACHE.invalidateAll();

        for (String[] evil : evilPatternAndMatchingString) {
            Pattern pattern = Pattern.compile(evil[0]);
            long startTime = System.currentTimeMillis();
            MatcherTimeoutFactory.RegExpTimeoutException exception = assertThrows(MatcherTimeoutFactory.RegExpTimeoutException.class,()->
                MatcherTimeoutFactory.matcher(pattern, evil[1]).matches()
            );
            long endTime = System.currentTimeMillis();
            
            // the regex returned after the expected timeout time.
            assertTrue(endTime >= startTime + MatcherTimeoutFactory.VANITY_URL_REGEX_TIMEOUT);
            
            // the regex returned within 100ms of the expected timeout time
            assertTrue(endTime < startTime + MatcherTimeoutFactory.VANITY_URL_REGEX_TIMEOUT + 100);
            
            //we got a good exception message
            assertTrue(exception.getMessage().contains(pattern.toString()));

        }
    }

    @Test
    public void test_regex_quarantine() {
        MatcherTimeoutFactory.SLOW_REGEX_CACHE.invalidateAll();

        for (String[] evil : evilPatternAndMatchingString) {
            Pattern pattern = Pattern.compile(evil[0]);
            long quarantineTime =  MatcherTimeoutFactory.VANITY_URL_QUARANTINE_MS;
            long runUntil = System.currentTimeMillis() + quarantineTime;
            
            
            // this should throw a timeout and add pattern to quarantine
            assertThrows(MatcherTimeoutFactory.RegExpTimeoutException.class,()->
                MatcherTimeoutFactory.matcher(pattern, evil[1]).matches()
            );
            
            // while the regex is in quarantine, we should get the NO_MATCH_PATTERN result
            while (System.currentTimeMillis() < runUntil) {
                long startTime = System.currentTimeMillis();
                Matcher matcher = MatcherTimeoutFactory.matcher(pattern, evil[1]);
                // we have the NO_MATCH_PATTERN
                assertEquals(MatcherTimeoutFactory.NO_MATCH_PATTERN, matcher.pattern());
                
                //asert that we are now returning a no match regex in under 100ms
                assertTrue(System.currentTimeMillis() < startTime + 100);
            }
            
            // wait until the regex is released from quarantine
            Awaitility.await()
                .atMost(quarantineTime,TimeUnit.MILLISECONDS)
                .until(()->MatcherTimeoutFactory.SLOW_REGEX_CACHE.getIfPresent(pattern.toString())==null);
            
            
            // this should throw a timeout and add pattern to quarantine AGAIN
            assertThrows(MatcherTimeoutFactory.RegExpTimeoutException.class,()->MatcherTimeoutFactory.matcher(pattern, evil[1]).matches());
            
            // AGAIN, we have the NO_MATCH_PATTERN
            assertEquals(MatcherTimeoutFactory.NO_MATCH_PATTERN, MatcherTimeoutFactory.matcher(pattern, evil[1]).pattern());

        }
    }
    
    @Test
    public void test_no_matcher_regex() {
        
        for (String str : testStrings) {
           assertFalse(MatcherTimeoutFactory.NO_MATCH_PATTERN.matcher(str).matches());
        }

    }

    
    @Test
    public void test_timelimited_char_sequence() {
        CharSequence charSequence = "This is a big CharSequence";

        TimeLimitedCharSequence timedChar = new TimeLimitedCharSequence(charSequence, MatcherTimeoutFactory.VANITY_URL_REGEX_TIMEOUT, MatcherTimeoutFactory.NO_MATCH_PATTERN, charSequence);
        

        

        assertEquals(timedChar.length(),charSequence.length());
        assertEquals(timedChar.toString(),charSequence.toString());
        assertEquals(timedChar.subSequence(0,1).charAt(0),charSequence.subSequence(0,1).charAt(0));

        
        Try.run(()->Thread.sleep(MatcherTimeoutFactory.VANITY_URL_REGEX_TIMEOUT));

        
        assertThrows(MatcherTimeoutFactory.RegExpTimeoutException.class,()->timedChar.subSequence(0,1).charAt(0));
        
        assertThrows(MatcherTimeoutFactory.RegExpTimeoutException.class,()->timedChar.length());
        
        assertThrows(MatcherTimeoutFactory.RegExpTimeoutException.class,()->timedChar.toString());
        

        

        
        
        
        
    }
    

}

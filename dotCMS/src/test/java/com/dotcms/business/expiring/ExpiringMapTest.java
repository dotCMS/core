package com.dotcms.business.expiring;

import com.dotcms.UnitTestBase;
import com.dotmarketing.util.DateUtil;
import io.vavr.Tuple;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CollectionsUtils unit test.
 * @author jsanca
 */
public class ExpiringMapTest extends UnitTestBase {

    public static final String ONE    = "one";
    public static final String TWO    = "two";
    public static final String THREE  = "three";

    @Test
    public void put_4000_millis_ttl_messages_in_timeout_interval_should_be_ignore_after_should_not_contains() {

        // put a message with 10 seconds, for more messages repeat
        final ExpiringMap<String, String> map = new ExpiringMapBuilder<>()
                .size(10).ttl(DateUtil.MINUTE_MILLIS).build();

        final String one = ONE;
        try {
            map.put(one, one, DateUtil.FOUR_SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(one)));
            DateUtil.sleep(DateUtil.FIVE_SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertFalse(map.containsKey(one)));
        } finally {
            map.remove(one);
        }
    }


    @Test
    public void put_4_seconds_timeunit_messages_in_timeout_interval_should_be_ignore_after_should_not_contains() {

        // put a message with 10 seconds, for more messages repeat
        final ExpiringMap<String, String> map = new ExpiringMapBuilder<>()
                .size(10).ttl(DateUtil.MINUTE_MILLIS).build();

        final String one = ONE;
        try {
            map.put(one, one, 4, TimeUnit.SECONDS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(one)));
            DateUtil.sleep(DateUtil.FIVE_SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertFalse(map.containsKey(one)));
        } finally {
            map.remove(one);
        }
    }

    @Test
    public void put_4_seconds_duration_messages_in_timeout_interval_should_be_ignore_after_should_not_contains() {

        // put a message with 10 seconds, for more messages repeat
        final ExpiringMap<String, String> map = new ExpiringMapBuilder<>()
                .size(10).ttl(DateUtil.MINUTE_MILLIS).build();


        final String one = ONE;
        try {
            map.put(one, one, Duration.ofMillis(DateUtil.FOUR_SECOND_MILLIS));
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(one)));
            DateUtil.sleep(DateUtil.FIVE_SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertFalse(map.containsKey(one)));
        } finally {
            map.remove(one);
        }
    }

    @Test
    public void put_3_seconds_default_strategy_messages_in_timeout_interval_should_be_ignore_after_should_not_contains() {

        // put a message with 10 seconds, for more messages repeat
        final ExpiringMap<String, String> map = new ExpiringMapBuilder<>()
                .size(10).ttl(DateUtil.MINUTE_MILLIS).build();

        final String one = ONE;
        try {
            map.put(one, one);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(one)));
            DateUtil.sleep(DateUtil.FIVE_SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertFalse(map.containsKey(one)));
        } finally {
            map.remove(one);
        }
    }


    @Test
    public void put_with_custom_strategy_messages_in_timeout_interval_should_be_ignore_after_should_not_contains() {

        // put a message with 10 seconds, for more messages repeat
        final ExpiringMap<String, String> map = new ExpiringMapBuilder<>()
                .size(10).ttl(DateUtil.MINUTE_MILLIS)
                .expiringEntryStrategy((key, value)-> {
                    long seconds = 3;
                    final TimeUnit unit = TimeUnit.SECONDS;

                    if (ONE.equals(key)) {
                        seconds = 2;
                    } else if (TWO.equals(key)) {
                        seconds = 4;
                    } else if (THREE.equals(key)) {
                        seconds = 6;
                    }

                    return Tuple.of(seconds, unit);
                })
                .build();

        final String one   = ONE;
        final String two   = TWO;
        final String three = THREE;
        try {
            map.put(one, one);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(one)));
            DateUtil.sleep(DateUtil.THREE_SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertFalse(map.containsKey(one)));

            map.put(two, two);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(two)));
            DateUtil.sleep(DateUtil.SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(two)));
            DateUtil.sleep(DateUtil.FOUR_SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertFalse(map.containsKey(two)));

            map.put(three, three);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(three)));
            DateUtil.sleep(DateUtil.TWO_SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(three)));
            DateUtil.sleep(DateUtil.TWO_SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(three)));
            DateUtil.sleep(DateUtil.SECOND_MILLIS * 6);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertFalse(map.containsKey(three)));
        } finally {
            map.remove(one);
            map.remove(two);
            map.remove(three);
        }
    }


    @Test
    public void Put_Using_Cache_TTL_No_Expiring_Strategy() {

        // put a message with 10 seconds, for more messages repeat
        final ExpiringMap<String, String> map = new ExpiringMapBuilder<>()
                .size(10).ttl(DateUtil.FIVE_SECOND_MILLIS).build();

        final String one = ONE;
        try {
            map.put(one, one, true);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertTrue(map.containsKey(one)));
            DateUtil.sleep(DateUtil.FIVE_SECOND_MILLIS);
            IntStream.of(1, 2, 3).forEach(i -> Assert.assertFalse(map.containsKey(one)));
        } finally {
            map.remove(one);
        }
    }


}
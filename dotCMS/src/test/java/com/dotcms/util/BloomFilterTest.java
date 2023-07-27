package com.dotcms.util;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.liferay.util.StringPool;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

public class BloomFilterTest {

    @Test
    public void test_bloom_filter() {


        BloomFilter<Integer> filter = BloomFilter.create(
                Funnels.integerFunnel(),
                100_000,
                0.01);

        IntStream.range(0, 100_000).forEach(filter::put);

        assert(filter.mightContain(1));
        assert(filter.mightContain(2));
        assert(filter.mightContain(3));
        assert(filter.mightContain(1_000_000) ==false);




    }






}

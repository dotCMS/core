package com.dotcms.util;

import com.dotcms.UnitTestBase;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FunctionUtilsTest extends UnitTestBase {

    @Test
    public void ifTrueTest()  {

        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        assertTrue( FunctionUtils.ifTrue(true, ()-> atomicBoolean.set(true)));
        assertTrue( atomicBoolean.get() );

        assertTrue( FunctionUtils.ifTrue(()-> true, ()-> atomicBoolean.set(true)));
        assertTrue( atomicBoolean.get() );
    }

    @Test
    public void ifElseTest()  {

        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        assertTrue( FunctionUtils.ifOrElse(true, ()-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertTrue( atomicBoolean.get() );

        assertFalse( FunctionUtils.ifOrElse(()-> false, ()-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertFalse( atomicBoolean.get() );
    }

    @Test
    public void ifElseTestWithEmptyOptional()  {

        final Optional<Contentlet> optionalContentlet = Optional.empty();
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        assertFalse( FunctionUtils.ifOrElse(optionalContentlet, v-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertFalse( atomicBoolean.get() );
    }

    @Test
    public void ifElseTestWithNullableOptional()  {

        final Optional<Contentlet> optionalContentlet = Optional.ofNullable(null);
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        assertFalse( FunctionUtils.ifOrElse(optionalContentlet, v-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertFalse( atomicBoolean.get() );
    }

    @Test
    public void ifElseTestWithValidOptional()  {

        final Optional<Contentlet> optionalContentlet = Optional.ofNullable(new Contentlet());
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        assertTrue( FunctionUtils.ifOrElse(optionalContentlet, v-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertTrue( atomicBoolean.get() );
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link FunctionUtils#getOrDefault(Supplier, Supplier)}</li>
     *     <li><b>Given Scenario: </b>Returns the expected {@code Supplier} based on the result
     *     of the boolean condition.</li>
     *     <li><b>Expected Result: </b>The condition evaluates to {@code true}, so the {@code
     *     trueSupplier} is returned.</li>
     * </ul>
     */
    @Test
    public void getOrDefaultReturnsTrueSupplier() {
        final Supplier<Boolean> trueSupplier = () -> true;
        final Supplier<Boolean> falseSupplier = () -> false;
        assertEquals("The value of 'time' should be returned", trueSupplier.get(),
                FunctionUtils.getOrDefault(trueSupplier.get(), trueSupplier, falseSupplier));
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link FunctionUtils#getOrDefault(Supplier, Supplier)}</li>
     *     <li><b>Given Scenario: </b>Returns the expected {@code Supplier} based on the result
     *     of the boolean condition.</li>
     *     <li><b>Expected Result: </b>The condition evaluates to {@code true}, so the {@code
     *     trueSupplier} is returned.</li>
     * </ul>
     */
    @Test
    public void getOrDefaultReturnsFalseSupplier() {
        final Supplier<Boolean> trueSupplier = () -> true;
        final Supplier<Boolean> falseSupplier = () -> false;
        assertEquals("The value of 'time' should be returned", falseSupplier.get(),
                FunctionUtils.getOrDefault(falseSupplier.get(), trueSupplier, falseSupplier));
    }

}

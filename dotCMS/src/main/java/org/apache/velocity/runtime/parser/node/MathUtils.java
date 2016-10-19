package org.apache.velocity.runtime.parser.node;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Utility-class for all arithmetic-operations.<br><br>
 *
 * All operations (+ - / *) return a Number which type is the type of the bigger argument.<br>
 * Example:<br>
 * <code>add ( new Integer(10), new Integer(1))</code> will return an <code>Integer</code>-Object with the value 11<br>
 * <code>add ( new Long(10), new Integer(1))</code> will return an <code>Long</code>-Object with the value 11<br>
 * <code>add ( new Integer(10), new Float(1))</code> will return an <code>Float</code>-Object with the value 11<br><br>
 *
 * Overflow checking:<br>
 * For integral values (byte, short, int) there is an implicit overflow correction (the next "bigger"
 * type will be returned). For example, if you call <code>add (new Integer (Integer.MAX_VALUE), 1)</code> a
 * <code>Long</code>-object will be returned with the correct value of <code>Integer.MAX_VALUE+1</code>.<br>
 * In addition to that the methods <code>multiply</code>,<code>add</code> and <code>substract</code> implement overflow
 * checks for <code>long</code>-values. That means that if an overflow occurs while working with long values a BigInteger
 * will be returned.<br>
 * For all other operations and types (such as Float and Double) there is no overflow checking.
 *
 * @author <a href="mailto:pero@antaramusic.de">Peter Romianowski</a>
 * @since 1.5
 */
public abstract class MathUtils
{

    /**
     * A BigDecimal representing the number 0
     */
    protected static final BigDecimal DECIMAL_ZERO    = new BigDecimal ( BigInteger.ZERO );

    /**
     * The constants are used to determine in which context we have to calculate.
     */
    protected static final int BASE_LONG          = 0;
    protected static final int BASE_FLOAT         = 1;
    protected static final int BASE_DOUBLE        = 2;
    protected static final int BASE_BIGINTEGER    = 3;
    protected static final int BASE_BIGDECIMAL    = 4;

    /**
     * The <code>Class</code>-object is key, the maximum-value is the value
     */
    protected static final Map ints = new HashMap();
    static
    {
        ints.put (Byte.class, BigDecimal.valueOf (Byte.MAX_VALUE));
        ints.put (Short.class, BigDecimal.valueOf (Short.MAX_VALUE));
        ints.put (Integer.class, BigDecimal.valueOf (Integer.MAX_VALUE));
        ints.put (Long.class, BigDecimal.valueOf (Long.MAX_VALUE));
        ints.put (BigInteger.class, BigDecimal.valueOf (-1));
    }

    /**
     * The "size" of the number-types - ascending.
     */
    protected static final List typesBySize = new ArrayList();
    static
    {
        typesBySize.add (Byte.class);
        typesBySize.add (Short.class);
        typesBySize.add (Integer.class);
        typesBySize.add (Long.class);
        typesBySize.add (Float.class);
        typesBySize.add (Double.class);
    }

    /**
     * Convert the given Number to a BigDecimal
     * @param n
     * @return The number as BigDecimal
     */
    public static BigDecimal toBigDecimal (Number n)
    {

        if (n instanceof BigDecimal)
        {
            return (BigDecimal)n;
        }

        if (n instanceof BigInteger)
        {
            return new BigDecimal ( (BigInteger)n );
        }

        return new BigDecimal (n.doubleValue());

    }

    /**
     * Convert the given Number to a BigInteger
     * @param n
     * @return The number as BigInteger
     */
    public static BigInteger toBigInteger (Number n)
    {

        if (n instanceof BigInteger)
        {
            return (BigInteger)n;
        }

        return BigInteger.valueOf (n.longValue());

    }

    /**
     * Compare the given Number to 0.
     * @param n
     * @return True if number is 0.
     */
    public static boolean isZero (Number n)
    {
        if (isInteger( n ) )
        {
            if (n instanceof BigInteger)
            {
                return ((BigInteger)n).compareTo (BigInteger.ZERO) == 0;
            }
            return n.doubleValue() == 0;
        }
        if (n instanceof Float)
        {
            return n.floatValue() == 0f;
        }
        if (n instanceof Double)
        {
            return n.doubleValue() == 0d;
        }
        return toBigDecimal( n ).compareTo( DECIMAL_ZERO) == 0;
    }

    /**
     * Test, whether the given object is an integer value
     * (Byte, Short, Integer, Long, BigInteger)
     * @param n
     * @return True if n is an integer.
     */
    public static boolean isInteger (Number n)
    {
        return ints.containsKey (n.getClass());
    }

    /**
     * Wrap the given primitive into the given class if the value is in the
     * range of the destination type. If not the next bigger type will be chosen.
     * @param value
     * @param type
     * @return Number object representing the primitive.
     */
    public static Number wrapPrimitive (long value, Class type)
    {
        if (type == Byte.class)
        {
            if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE)
            {
                type = Short.class;
            }
            else
            {
                // TODO: JDK 1.4+ -> valueOf()
                return new Byte ((byte)value);
            }
        }
        if (type == Short.class)
        {
            if (value > Short.MAX_VALUE || value < Short.MIN_VALUE)
            {
                type = Integer.class;
            }
            else
            {
                // TODO: JDK 1.4+ -> valueOf()
                return new Short((short)value);
            }
        }
        if (type == Integer.class)
        {
            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)
            {
                type = Long.class;
            }
            else
            {
                // TODO: JDK 1.4+ -> valueOf()
                return new Integer ((int)value);
            }
        }
        if (type == Long.class)
        {
            // TODO: JDK 1.4+ -> valueOf()
            return new Long (value);
        }
        return BigInteger.valueOf( value);
    }

    /**
     * Wrap the result in the object of the bigger type.
     * 
     * @param value result of operation (as a long) - used to check size
     * @param op1 first operand of binary operation
     * @param op2 second operand of binary operation
     * @return Number object of appropriate size to fit the value and operators
     */
    private static Number wrapPrimitive (long value, Number op1, Number op2)
    {
        if ( typesBySize.indexOf( op1.getClass()) > typesBySize.indexOf( op2.getClass()))
        {
            return wrapPrimitive( value, op1.getClass());
        }
        return wrapPrimitive( value, op2.getClass());
    }

    /**
     * Find the common Number-type to be used in calculations.
     * 
     * @param op1 first operand of binary operation
     * @param op2 second operand of binary operation
     * @return constant indicating type of Number to use in calculations
     */
    private static int findCalculationBase (Number op1, Number op2)
    {

        boolean op1Int = isInteger(op1);
        boolean op2Int = isInteger(op2);

        if ( (op1 instanceof BigDecimal || op2 instanceof BigDecimal) ||
             ( (!op1Int || !op2Int) && (op1 instanceof BigInteger || op2 instanceof BigInteger)) )
        {
            return BASE_BIGDECIMAL;
        }

        if (op1Int && op2Int) {
            if (op1 instanceof BigInteger || op2 instanceof BigInteger)
            {
                return BASE_BIGINTEGER;
            }
            return BASE_LONG;
        }

        if ((op1 instanceof Double) || (op2 instanceof Double))
        {
            return BASE_DOUBLE;
        }
        return BASE_FLOAT;
    }

    /**
     * Add two numbers and return the correct value / type.
     * Overflow detection is done for integer values (byte, short, int, long) only!
     * @param op1
     * @param op2
     * @return Addition result.
     */
    public static Number add (Number op1, Number op2)
    {

        int calcBase = findCalculationBase( op1, op2);
        switch (calcBase)
        {
            case BASE_BIGINTEGER:
                return toBigInteger( op1 ).add( toBigInteger( op2 ));
            case BASE_LONG:
                long l1 = op1.longValue();
                long l2 = op2.longValue();
                long result = l1+l2;

                // Overflow check
                if ((result ^ l1) < 0 && (result ^ l2) < 0)
                {
                    return toBigInteger( op1).add( toBigInteger( op2));
                }
                return wrapPrimitive( result, op1, op2);
            case BASE_FLOAT:
                return new Float (op1.floatValue()+op2.floatValue());
            case BASE_DOUBLE:
                return new Double (op1.doubleValue()+op2.doubleValue());

            // Default is BigDecimal operation
            default:
                return toBigDecimal( op1 ).add( toBigDecimal( op2 ));
        }
    }

    /**
     * Subtract two numbers and return the correct value / type.
     * Overflow detection is done for integer values (byte, short, int, long) only!
     * @param op1
     * @param op2
     * @return Subtraction result.
     */
    public static Number subtract (Number op1, Number op2) {

        int calcBase = findCalculationBase( op1, op2);
        switch (calcBase) {
            case BASE_BIGINTEGER:
                return toBigInteger( op1 ).subtract( toBigInteger( op2 ));
            case BASE_LONG:
                long l1 = op1.longValue();
                long l2 = op2.longValue();
                long result = l1-l2;

                // Overflow check
                if ((result ^ l1) < 0 && (result ^ ~l2) < 0) {
                    return toBigInteger( op1).subtract( toBigInteger( op2));
                }
                return wrapPrimitive( result, op1, op2);
            case BASE_FLOAT:
                return new Float (op1.floatValue()-op2.floatValue());
            case BASE_DOUBLE:
                return new Double (op1.doubleValue()-op2.doubleValue());

            // Default is BigDecimal operation
            default:
                return toBigDecimal( op1 ).subtract( toBigDecimal( op2 ));
        }
    }

    /**
     * Multiply two numbers and return the correct value / type.
     * Overflow detection is done for integer values (byte, short, int, long) only!
     * @param op1
     * @param op2
     * @return Multiplication result.
     */
    public static Number multiply (Number op1, Number op2) {

        int calcBase = findCalculationBase( op1, op2);
        switch (calcBase) {
            case BASE_BIGINTEGER:
                return toBigInteger( op1 ).multiply( toBigInteger( op2 ));
            case BASE_LONG:
                long l1 = op1.longValue();
                long l2 = op2.longValue();
                long result = l1*l2;

                // Overflow detection
                if ((l2 != 0) && (result / l2 != l1)) {
                    return toBigInteger( op1).multiply( toBigInteger( op2));
                }
                return wrapPrimitive( result, op1, op2);
            case BASE_FLOAT:
                return new Float (op1.floatValue()*op2.floatValue());
            case BASE_DOUBLE:
                return new Double (op1.doubleValue()*op2.doubleValue());

            // Default is BigDecimal operation
            default:
                return toBigDecimal( op1 ).multiply( toBigDecimal( op2 ));
        }
    }

    /**
     * Divide two numbers. The result will be returned as Integer-type if and only if
     * both sides of the division operator are Integer-types. Otherwise a Float, Double,
     * or BigDecimal will be returned.
     * @param op1
     * @param op2
     * @return Division result.
     */
    public static Number divide (Number op1, Number op2) {

        int calcBase = findCalculationBase( op1, op2);
        switch (calcBase) {
            case BASE_BIGINTEGER:
                BigInteger b1 = toBigInteger( op1 );
                BigInteger b2 = toBigInteger( op2 );
                return b1.divide( b2);

            case BASE_LONG:
                long l1 = op1.longValue();
                long l2 = op2.longValue();
                return wrapPrimitive( l1 / l2, op1, op2);

            case BASE_FLOAT:
                return new Float (op1.floatValue()/op2.floatValue());
            case BASE_DOUBLE:
                return new Double (op1.doubleValue()/op2.doubleValue());

            // Default is BigDecimal operation
            default:
                return toBigDecimal( op1 ).divide( toBigDecimal( op2 ), BigDecimal.ROUND_HALF_DOWN);
        }
    }

    /**
     * Modulo two numbers.
     * @param op1
     * @param op2
     * @return Modulo result.
     *
     * @throws ArithmeticException If at least one parameter is a BigDecimal
     */
    public static Number modulo (Number op1, Number op2) throws ArithmeticException {

        int calcBase = findCalculationBase( op1, op2);
        switch (calcBase) {
            case BASE_BIGINTEGER:
                return toBigInteger( op1 ).mod( toBigInteger( op2 ));
            case BASE_LONG:
                return wrapPrimitive( op1.longValue() % op2.longValue(), op1, op2);
            case BASE_FLOAT:
                return new Float (op1.floatValue() % op2.floatValue());
            case BASE_DOUBLE:
                return new Double (op1.doubleValue() % op2.doubleValue());

            // Default is BigDecimal operation
            default:
                throw new ArithmeticException( "Cannot calculate the modulo of BigDecimals.");
        }
    }

    /**
     * Compare two numbers.
     * @param op1
     * @param op2
     * @return 1 if n1 > n2, -1 if n1 < n2 and 0 if equal.
     */
    public static int compare (Number op1, Number op2) {

        int calcBase = findCalculationBase( op1, op2);
        switch (calcBase) {
            case BASE_BIGINTEGER:
                return toBigInteger( op1 ).compareTo( toBigInteger( op2 ));
            case BASE_LONG:
                long l1 = op1.longValue();
                long l2 = op2.longValue();
                if (l1 < l2) {
                    return -1;
                }
                if (l1 > l2) {
                    return 1;
                }
                return 0;
            case BASE_FLOAT:
                float f1 = op1.floatValue();
                float f2 = op2.floatValue();
                if (f1 < f2) {
                    return -1;
                }
                if (f1 > f2) {
                    return 1;
                }
                return 0;
            case BASE_DOUBLE:
                double d1 = op1.doubleValue();
                double d2 = op2.doubleValue();
                if (d1 < d2) {
                    return -1;
                }
                if (d1 > d2) {
                    return 1;
                }
                return 0;

            // Default is BigDecimal operation
            default:
                return toBigDecimal( op1 ).compareTo( toBigDecimal ( op2 ));
        }
    }
}

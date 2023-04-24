/*
* The MIT License (MIT)
*
* Copyright (c) 2015 QAware GmbH
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
package com.dotcms.enterprise.de.qaware.heimdall;

import java.util.Arrays;

/**
 * Secure handling of char arrays.
 * <p/>
 * Attention: The given char array is cleared when the {@link #close()} method is called or this instance is collected
 * from the garbage collector.
 */
public class SecureCharArray implements AutoCloseable {
    /**
     * Char array.
     */
    private final char[] chars;

    /**
     * True if the instance has been disposed.
     */
    private boolean disposed = false;

    /**
     * Creates a new secure char array with the given chars.
     * <p/>
     * Attention: The given char array is cleared when the {@link #close()} method is called or this instance is collected
     * from the garbage collector.
     *
     * @param chars Char array.
     */
    public SecureCharArray(char[] chars) {
        this.chars = chars;
    }

    /**
     * The char array.
     *
     * @return The char array.
     */
    public char[] getChars() {
        if (disposed) {
            throw new IllegalStateException("Instance has been disposed.");
        }

        return chars;
    }

    @Override
    protected void finalize() throws Throwable {
        if (!disposed) {
            close();
        }

        super.finalize();
    }

    @Override
    public void close() {
        if (disposed) {
            throw new IllegalStateException("Instance has been disposed.");
        }

        Arrays.fill(chars, '0');

        disposed = true;
    }
}

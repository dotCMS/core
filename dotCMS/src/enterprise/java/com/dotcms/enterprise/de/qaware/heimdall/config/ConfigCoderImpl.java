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
package com.dotcms.enterprise.de.qaware.heimdall.config;

import com.dotcms.enterprise.de.qaware.heimdall.util.Preconditions;

import java.util.Map.Entry;

/**
 * Encodes or decodes a config to/from a string.
 */
public class ConfigCoderImpl implements ConfigCoder {
    /**
     * Delimiter for config entries.
     */
    private static final String CONFIG_DELIMITER = ";";
    /**
     * Delimiter for key and value.
     */
    private static final String CONFIG_VALUE_DELIMITER = "=";

    @Override
    public String encode(HashAlgorithmConfig config) {
        Preconditions.checkNotNull(config, "config");

        StringBuilder encoded = new StringBuilder();

        for (Entry<String, String> entry : config.entrySet()) {
            encoded.append(entry.getKey());
            encoded.append(CONFIG_VALUE_DELIMITER);
            encoded.append(entry.getValue());
            encoded.append(CONFIG_DELIMITER);
        }

        // Remove last delimiter
        if (encoded.length() > 0) {
            encoded.deleteCharAt(encoded.length() - 1);
        }

        return encoded.toString();
    }

    /**
     * Decodes complete, unique key/value entries without discarding malformed input.
     *
     * @param encoded the semicolon-separated hash configuration
     * @return the decoded {@link HashAlgorithmConfig}
     * @throws IllegalArgumentException if an entry is incomplete, empty, or duplicated
     */
    @Override
    public HashAlgorithmConfig decode(String encoded) {
        Preconditions.checkNotNull(encoded, "encoded");

        String[] parts = encoded.split(CONFIG_DELIMITER, -1);
        HashAlgorithmConfig config = new HashAlgorithmConfig();

        for (String part : parts) {
            String[] keyAndValue = part.split(CONFIG_VALUE_DELIMITER, -1);
            if (keyAndValue.length != 2 || keyAndValue[0].isEmpty() || keyAndValue[1].isEmpty()) {
                throw new IllegalArgumentException("Invalid hash configuration entry");
            }
            if (config.putIfAbsent(keyAndValue[0], keyAndValue[1]) != null) {
                throw new IllegalArgumentException("Duplicate hash configuration entry");
            }
        }

        return config;
    }
}

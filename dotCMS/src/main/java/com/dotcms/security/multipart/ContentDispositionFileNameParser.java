package com.dotcms.security.multipart;

import com.dotmarketing.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Parses "filename" parameter of the Content-Disposition HTTP header as defined in RFC 6266.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 */
final class ContentDispositionFileNameParser { // todo: unit test

    private static final String INVALID_HEADER_FIELD_PARAMETER_FORMAT = "Invalid header field parameter format (as defined in RFC 5987)";

    /**
     * Parse a {@literal Content-Disposition} header value as defined in RFC 2183.
     *
     * @param contentDisposition the {@literal Content-Disposition} header value
     * @return Return the value of the {@literal filename} parameter (or the value of the
     * {@literal filename*} one decoded as defined in the RFC 5987), or {@code null} if not defined.
     */
    public static String parse(final String contentDisposition) {

        final List<String> parts = tokenize(contentDisposition);
        String filename = null;
        Charset charset;
        for (int i = 1; i < parts.size(); i++) {

            final String part = parts.get(i);
            final int eqIndex = part.indexOf('=');

            if (eqIndex != -1) {

                final String attribute = part.substring(0, eqIndex);
                final String value = (part.startsWith("\"", eqIndex + 1) && part.endsWith("\"") ?
                        part.substring(eqIndex + 2, part.length() - 1) :
                        part.substring(eqIndex + 1));

                if (attribute.equals("filename*")) {

                    final int idx1 = value.indexOf('\'');
                    final int idx2 = value.indexOf('\'', idx1 + 1);

                    if (idx1 != -1 && idx2 != -1) {

                        charset = Charset.forName(value.substring(0, idx1).trim());
                        if (!(UTF_8.equals(charset) || ISO_8859_1.equals(charset))) {

                            Logger.error(ContentDispositionFileNameParser.class,
                                    "Charset should be UTF-8 or ISO-8859-1, contentDisposition:" + contentDisposition);
                            throw new IllegalArgumentException("Charset should be UTF-8 or ISO-8859-1");
                        }

                        filename = decodeFilename(value.substring(idx2 + 1), charset);
                    } else {
                        // US ASCII
                        filename = decodeFilename(value, StandardCharsets.US_ASCII);
                    }
                } else if (attribute.equals("filename") && (filename == null)) {

                    filename = value;
                }
            } else {

                Logger.error(ContentDispositionFileNameParser.class,
                        "Invalid content disposition format, contentDisposition:" + contentDisposition);
                throw new IllegalArgumentException("Invalid content disposition format");
            }
        }

        return filename;
    }

    private static List<String> tokenize(final String headerValue) {

        int index = headerValue.indexOf(';');
        final String type = (index >= 0 ? headerValue.substring(0, index) : headerValue).trim();
        if (type.isEmpty()) {

            Logger.error(ContentDispositionFileNameParser.class,
                    "Content-Disposition header must not be empty, headerValue: " + headerValue);
            throw new IllegalArgumentException("Content-Disposition header must not be empty");
        }

        final List<String> parts = new ArrayList<>();
        parts.add(type);

        if (index >= 0) {
            do {

                int nextIndex = index + 1;
                boolean quoted = false;
                boolean escaped = false;
                while (nextIndex < headerValue.length()) {

                    final char ch = headerValue.charAt(nextIndex);
                    if (ch == ';') {
                        if (!quoted) {
                            break;
                        }
                    } else if (!escaped && ch == '"') {
                        quoted = !quoted;
                    }

                    escaped = (!escaped && ch == '\\');
                    nextIndex++;
                }

                final String part = headerValue.substring(index + 1, nextIndex).trim();
                if (!part.isEmpty()) {

                    parts.add(part);
                }

                index = nextIndex;
            }
            while (index < headerValue.length());
        }

        return parts;
    }

    /**
     * Decode the given header field param as described in RFC 5987.
     * <p>Only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported.
     *
     * @param filename the filename
     * @param charset  the charset for the filename
     * @return the encoded header field param
     */
    private static String decodeFilename(final String filename, final Charset charset) {

        if (filename == null) {

            Logger.error(ContentDispositionFileNameParser.class,
                    "'input' String` should not be null, filename: " + filename);
            throw new IllegalArgumentException("'input' String` should not be null");
        }
        if (charset == null) {

            Logger.error(ContentDispositionFileNameParser.class,
                    "'charset' should not be null, filename: " + filename);
            throw new IllegalArgumentException("'charset' should not be null");
        }

        final byte[] value = filename.getBytes(charset);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int index = 0;
        while (index < value.length) {

            final byte b = value[index];
            if (isRFC5987AttrChar(b)) {

                byteArrayOutputStream.write((char) b);
                index++;
            } else if (b == '%' && index < value.length - 2) {

                char[] array = new char[] {(char) value[index + 1], (char) value[index + 2]};
                try {

                    byteArrayOutputStream.write(Integer.parseInt(String.valueOf(array), 16));
                } catch (NumberFormatException ex) {

                    Logger.error(ContentDispositionFileNameParser.class,
                            INVALID_HEADER_FIELD_PARAMETER_FORMAT + ", filename: " + filename
                                    + ", msg:" + ex.getMessage(), ex);
                    throw new IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT, ex);
                }
                index += 3;
            } else {

                Logger.error(ContentDispositionFileNameParser.class,
                        INVALID_HEADER_FIELD_PARAMETER_FORMAT + ", filename: " + filename);
                throw new IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT);
            }
        }

        try {

            return byteArrayOutputStream.toString(charset.name());
        } catch (UnsupportedEncodingException e) {

            Logger.error(ContentDispositionFileNameParser.class,
                    "Failed to copy contents of ByteArrayOutputStream into a String, filename: " + filename
                            + ", msg:" + e.getMessage(), e);
            throw new RuntimeException("Failed to copy contents of ByteArrayOutputStream into a String", e);
        }
    }

    private static boolean isRFC5987AttrChar(final byte c) {

        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' ||
                c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
    }
}

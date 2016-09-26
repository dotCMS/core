/*
 * Copyright 2007 Guy Van den Broeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dotmarketing.util.diff.tag;

/**
 * A TextAtom with an identifier from a limited set of delimiter strings.
 */
public class DelimiterAtom extends TextAtom {

    public DelimiterAtom(char c) {
        super("" + c);
    }

    public static boolean isValidDelimiter(String s) {
        if (s.length() == 1)
            return isValidDelimiter(s.charAt(0));
        return false;
    }

    public static boolean isValidDelimiter(char c) {
        switch (c) {
        // Basic Delimiters
        case '/':
        case '.':
        case '!':
        case ',':
        case ';':
        case '?':
        case ' ':
        case '=':
        case '\'':
        case '"':
        case '\t':
        case '\r':
        case '\n':
            // Extra Delimiters
        case '[':
        case ']':
        case '{':
        case '}':
        case '(':
        case ')':
        case '&':
        case '|':
        case '\\':
        case '-':
        case '_':
        case '+':
        case '*':
        case ':':
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean isValidAtom(String s) {
        return super.isValidAtom(s) && isValidDelimiterAtom(s);
    }

    private boolean isValidDelimiterAtom(String s) {
        return isValidDelimiter(s);
    }

    @Override
    public String toString() {
        return "DelimiterAtom: "
                + getFullText().replaceAll("\n", "\\\\n").replaceAll("\r",
                        "\\\\r").replaceAll("\t", "\\\\t");
    }

    @Override
    public boolean equalsIdentifier(Atom a) {
        return super.equalsIdentifier(a)
        // Handling for automatically inserted newlines
                || ((a.getIdentifier().equals(" ") || a.getIdentifier().equals(
                        "\n")) && (getIdentifier().equals(" ") || getIdentifier()
                        .equals("\n")));
    }

}

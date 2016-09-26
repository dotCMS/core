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
 * An atom that represents a closing or opening tag.
 */
public class TagAtom implements Atom {

    private String identifier;

    private String internalIdentifiers = "";

    public TagAtom(String s) {
        if (!isValidAtom(s))
            throw new IllegalArgumentException(
                    "The given string is not a valid tag");
        s = s.substring(1, s.length() - 1);

        if (s.indexOf(' ') > 0) {
            identifier = s.substring(0, s.indexOf(' '));
            // TODO check if the +1 is ok!
            internalIdentifiers = s.substring(s.indexOf(' ') + 1);
        } else {
            identifier = s;
        }
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getInternalIdentifiers() {
        return internalIdentifiers;
    }

    public boolean hasInternalIdentifiers() {
        return internalIdentifiers.length() > 0;
    }

    public static boolean isValidTag(String s) {
        return s.lastIndexOf('<') == 0 && s.indexOf('>') == s.length() - 1
                && s.length() >= 3;
    }

    public String getFullText() {
        String s = "<" + identifier;
        if (hasInternalIdentifiers())
            s += " " + internalIdentifiers;
        return s += ">";
    }

    public boolean isValidAtom(String s) {
        return isValidTag(s);
    }

    @Override
    public String toString() {
        return "TagAtom: " + getFullText();
    }

    public boolean equalsIdentifier(Atom other) {
        return other.getIdentifier().equals(getIdentifier());
    }

}

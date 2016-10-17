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
 * An Atom that represents a piece of ordinary text.
 */
public class TextAtom implements Atom {

    private String s;

    public TextAtom(String s) {
        if (!isValidAtom(s))
            throw new IllegalArgumentException(
                    "The given String is not a valid Text Atom");
        this.s = s;
    }

    public String getFullText() {
        return s;
    }

    public String getIdentifier() {
        return s;
    }

    public String getInternalIdentifiers() {
        throw new IllegalStateException("This Atom has no internal identifiers");
    }

    public boolean hasInternalIdentifiers() {
        return false;
    }

    public boolean isValidAtom(String s) {
        return s != null && s.length() > 0;
    }

    @Override
    public String toString() {
        return "TextAtom: " + getFullText();
    }

    public boolean equalsIdentifier(Atom other) {
        return other.getIdentifier().equals(getIdentifier());
    }

}

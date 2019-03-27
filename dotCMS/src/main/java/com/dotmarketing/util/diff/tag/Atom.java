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
 * A unit of comparison between html files. An Atom can be equal to another
 * while not having the same text (tag arguments). In that case the Atom will
 * have internal identifiers that can be compared on a second level.
 */
public interface Atom {

    public String getIdentifier();

    public boolean hasInternalIdentifiers();

    public String getInternalIdentifiers();

    public String getFullText();

    public boolean isValidAtom(String s);

    public boolean equalsIdentifier(Atom other);
}

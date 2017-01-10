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
package com.dotmarketing.util.diff.html.ancestor;

public class ChangeText {

    private int maxNbCharsPerLine;

    private StringBuilder txt = new StringBuilder();

    public final static String newLine = "<br/>";

    private int charsThisLine = 0;

    public ChangeText(int maxNbCharsPerLine) {
        this.maxNbCharsPerLine = maxNbCharsPerLine;
    }

    public synchronized void addText(String s) {
        s = clean(s);

        if (s.length() + charsThisLine > maxNbCharsPerLine) {
            addTextCarefully(s);
        } else {
            addToLine(s);
        }
    }

    private void addToLine(String s) {
      txt.append(s);
      charsThisLine += s.length();
    }

    public synchronized void addHtml(String s) {
        txt.append(s);
        if (s.contains("</li>") || s.contains("</ol>") || s.contains("</ul>")) {
            charsThisLine = 0;
        }
    }

    private synchronized void addTextCarefully(String s) {

        int firstSpace = s.indexOf(" ");
        if (firstSpace < 0) { //  Next word is the whole string.
            if (s.length() < maxNbCharsPerLine) {
              //  If the word will fit on a standard output line, by itself, get a new line and put it out.
              if (charsThisLine > 0)
                addNewLine();
              addText(s);
            } else {
              addNewLine();
              addTextBrokenAcrossLines(s);
            }
        } else if (firstSpace + 1 >= maxNbCharsPerLine){
          //  The first word in s won't fit on a line, break it across several lines
          if (charsThisLine > 0)
            addNewLine();
          addTextBrokenAcrossLines(s.substring(0, firstSpace + 1));
          if (firstSpace + 1 < s.length())
            addText(s.substring(firstSpace + 1, s.length()));
        } else if (firstSpace + 1 + charsThisLine > maxNbCharsPerLine) {
          //  The new word won't fit on the current line.
          addNewLine();
          addText(s);
        } else {
            addText(s.substring(0, firstSpace + 1));
            if (firstSpace + 1 < s.length())
                addTextCarefully(s.substring(firstSpace + 1, s.length()));
        }
    }

    private void addTextBrokenAcrossLines(String s) {
      assert (s.indexOf(' ') < 0 && s.length() > maxNbCharsPerLine);
      int firstPart = Math.min(s.length(), maxNbCharsPerLine
              - charsThisLine);

      addText(s.substring(0, firstPart));

      addNewLine();
      addText(s.substring(firstPart, s.length()));
    }

    public synchronized void addNewLine() {
        addHtml(newLine);
        charsThisLine = 0;
    }

    @Override
    public String toString() {
        return txt.toString();
    }

    private String clean(String s) {
        return s.replaceAll("\n", "").replaceAll("\r", "").replaceAll("<",
                "&lt;").replaceAll(">", "&gt;").replaceAll("'", "&#39;")
                .replaceAll("\"", "&#34;");
    }
}

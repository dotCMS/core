package com.dotcms.unittest;

import java.util.List;
import java.util.Random;

/**
 * @author Geoff M. Granum
 */
public class TestUtil {

    public static <T> Object[][] toCaseArray(List<T> list){
        Object[][] ary = new Object[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            ary[i] = new Object[]{list.get(i)};
        }
        return ary;
    }

    public static String upperCaseRandom(final String input, final int n) {
        final int length = input.length();
        final StringBuilder output = new StringBuilder(input);
        final boolean[] alreadyChecked = new boolean[length];
        final Random random = new Random();

        for (int i = 0, checks = 0; i < n && checks < length; i++) {
            // Pick a place
            final int position = random.nextInt(length);

            // Check if lowercase alpha
            if (!alreadyChecked[position]) {
                if (Character.isLowerCase(output.charAt(position))) {
                    output.setCharAt(position, Character.toUpperCase(output.charAt(position)));
                } else {
                    i--;
                }
                checks++;
                alreadyChecked[position] = true;
            } else {
                i--;
            }
        }
        return output.toString();
    }
}
 

package com.dotmarketing.util;

import static com.dotmarketing.util.PasswordGenerator.Builder.LOWER_CASE_LETTERS_CHARS;
import static com.dotmarketing.util.PasswordGenerator.Builder.NUMBER_CHARS;
import static com.dotmarketing.util.PasswordGenerator.Builder.SPECIAL_CHARS;
import static com.dotmarketing.util.PasswordGenerator.Builder.UPPER_CASE_LETTERS_CHARS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.Test;

public class PasswordGeneratorTest {

    static final List<String> ACCEPTABLE_PASSWORD_CHARS =
            ImmutableList.of(
                    SPECIAL_CHARS, UPPER_CASE_LETTERS_CHARS, LOWER_CASE_LETTERS_CHARS, NUMBER_CHARS
            );

    /**
     * Simply test we're getting back chars that match our charsets with the required minimum number of chars
     * Method to test: {@link PasswordGenerator#nextPassword()}
     */
    @Test
    public void Test_Generate_Password_Then_Test_We_Have_At_Least_One_Match_Per_Group() {
        final PasswordGenerator passwordGenerator = new PasswordGenerator.Builder()
                .withDefaultValues().build();

        final Set<String> generatedPasswords = new HashSet<>(100);

        for (final String acceptablePasswordChar : ACCEPTABLE_PASSWORD_CHARS) {
            final String acceptableCharsPattern = "[" + String.join("", acceptablePasswordChar) + "]";
            final Pattern pattern = Pattern.compile(acceptableCharsPattern);
            //Lets do this a few times just to be sure
            for (int i = 0; i <= 100; i++) {
                final String password = passwordGenerator.nextPassword();
                assertTrue(pattern.matcher(password).find());
                assertEquals(password.length(), 16);
                assertTrue(generatedPasswords.add(password));
            }
        }
    }

}

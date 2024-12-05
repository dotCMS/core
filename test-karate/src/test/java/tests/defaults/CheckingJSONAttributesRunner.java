package tests.defaults;

import com.intuit.karate.junit5.Karate;

public class CheckingJSONAttributesRunner {

    @Karate.Test
    Karate testCheckingJSONAttributesRunner() {
        return Karate.run("CheckingJSONAttributes").relativeTo(getClass());
    }

}
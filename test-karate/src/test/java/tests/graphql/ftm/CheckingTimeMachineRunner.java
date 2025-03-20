package tests.graphql.ftm;

import com.intuit.karate.junit5.Karate;

public class CheckingTimeMachineRunner {

    @Karate.Test
    Karate testCheckingTimeMachine() {
        return Karate.run("CheckingTimeMachine").relativeTo(getClass());
    }

}

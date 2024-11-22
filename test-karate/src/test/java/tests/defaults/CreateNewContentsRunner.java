package tests.defaults;

import com.intuit.karate.junit5.Karate;

public class CreateNewContentsRunner {

    @Karate.Test
    Karate testCreateNewContentsRunner() {
        return Karate.run("CreateNewContents").relativeTo(getClass());
    }

}
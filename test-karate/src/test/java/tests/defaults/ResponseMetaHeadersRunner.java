package tests.defaults;

import com.intuit.karate.junit5.Karate;

public class ResponseMetaHeadersRunner {

    @Karate.Test
    Karate testResponseMetaHeaders() {
        return Karate.run("ResponseMetaHeaders").relativeTo(getClass());
    }

}

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.Test;

public class KarateCITests {
    // Note this is a JUnit 5 test compare with @Karate.Test annotated methods.https://stackoverflow.com/questions/65577487/whats-the-purpose-of-karate-junit5-when-you-can-run-tests-without-it

    @Test
    void defaults() {
        Results results = Runner.path(
                "classpath:tests/defaults",
                        "classpath:tests/graphql/ftm"
                ).tags("~@ignore")
                .outputHtmlReport(true)
                .outputJunitXml(true)
                .outputCucumberJson(true)
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }

}

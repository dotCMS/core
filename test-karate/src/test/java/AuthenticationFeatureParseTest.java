import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureSection;
import com.intuit.karate.core.Step;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Guards the API-JWT bootstrap feature, which is not run by the default suite because it is
 * tagged {@code @ignore} and requires a live server. Parsing it with {@code Feature.read} proves
 * the Gherkin stays valid, and the static step scan proves the token request never reuses the
 * email login name that only the session-login action accepts.
 */
public class AuthenticationFeatureParseTest {

    private static final String FEATURE = "classpath:authentication.feature";

    /** The bootstrap feature must parse and define at least one scenario. */
    @Test
    public void authenticationFeatureParses() {
        final Feature feature = Feature.read(FEATURE);
        assertNotNull(feature);
        assertFalse(feature.getSections().isEmpty(), "the bootstrap feature must declare a scenario");
    }

    /**
     * The token request body must talk about {@code expirationSeconds} only. Reusing the email
     * login name as {@code userId} makes {@code POST /api/v1/apitoken} return 404 because that
     * action resolves a canonical user id, so no expirationSeconds step may carry a userId.
     */
    @Test
    public void tokenRequestDoesNotReuseEmailLogin() {
        final List<String> steps = Feature.read(FEATURE).getSections().stream()
                .map(FeatureSection::getScenario)
                .flatMap(scenario -> scenario.getSteps().stream())
                .map(Step::getText)
                .collect(Collectors.toList());

        final List<String> tokenBodySteps = steps.stream()
                .filter(text -> text.contains("expirationSeconds"))
                .collect(Collectors.toList());
        assertFalse(tokenBodySteps.isEmpty(),
                "the feature must declare the token request body with expirationSeconds");
        assertTrue(tokenBodySteps.stream().noneMatch(text -> text.contains("userId")),
                "the token request body must not reuse the email login name as userId: " + tokenBodySteps);
    }
}

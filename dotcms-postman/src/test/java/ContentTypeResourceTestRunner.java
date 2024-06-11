import com.intuit.karate.junit5.Karate;

class ContentTypeResourceTestRunner {

    @Karate.Test
    Karate testAll() {
        return Karate.run("classpath:contentTypeResourceTests.feature");
    }
}
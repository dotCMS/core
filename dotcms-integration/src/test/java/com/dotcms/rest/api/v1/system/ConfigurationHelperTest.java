package com.dotcms.rest.api.v1.system;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Set;
import com.dotmarketing.exception.DotDataException;
import io.vavr.Tuple2;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigurationHelperTest {

    static ConfigurationHelper instance;

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        instance = ConfigurationHelper.INSTANCE;
    }

    /**
     * This method can deal with two possible valid input something like this: "dude Who Sends The Mail <website@dotcms.com>" or a plain and simple e-mail like "website@dotcms.com"
     * Method to test {@link ConfigurationHelper#parseMailAndSender(String)}
     * Given Scenario: sender + email
     * Expected Result: An input like this: "dude Who Sends The Mail <website@dotcms.com>" or "website@dotcms.com" should pass. While any other input should result in failure
     * @throws DotDataException
     */
    @Test
    public void Test_Parse_Well_Formed_Mail_And_Sender(){
       String sender = "dude Who Sends The Mail";
       String email = "website@dotcms.com";
       String senderAndEMail = String.format("%s     <%s>",sender, email);
       final Tuple2<String, String> mailAndSender = instance.parseMailAndSender(senderAndEMail);
       Assert.assertEquals(mailAndSender._1, email);
       Assert.assertEquals(mailAndSender._2, sender);
    }

    /**
     * This method can deal with two possible valid input something like this: "dude Who Sends The Mail <website@dotcms.com>" or a plain and simple e-mail like "website@dotcms.com"
     * Method to test {@link ConfigurationHelper#parseMailAndSender(String)}
     * Given Scenario: simple well formed email
     * Expected Result: An input like this: "website@dotcms.com" should pass.
     * @throws DotDataException
     */
    @Test
    public void Test_Parse_Well_Formed_Mail(){
        String email = "website@dotcms.com";
        final Tuple2<String, String> mailAndSender = instance.parseMailAndSender(email);
        Assert.assertEquals(mailAndSender._1, email);
        Assert.assertNull(mailAndSender._2);
    }

    /**
     * This method can deal with two possible valid input something like this: "dude Who Sends The Mail <website@dotcms.com>" or a plain and simple e-mail like "website@dotcms.com"
     * Method to test {@link ConfigurationHelper#parseMailAndSender(String)}
     * Given Scenario: bad email
     * Expected Result: bad email should result on IllegalArgumentException
     * @throws DotDataException
     */
    @Test(expected = IllegalArgumentException.class)
    public void Test_Parse_Bad_eMail_Mail(){
        String email = "website@,dotcms.com";
        final Tuple2<String, String> mailAndSender = instance.parseMailAndSender(email);
        Assert.assertEquals(mailAndSender._1, email);
        Assert.assertNull(mailAndSender._2);
    }

    /**
     * Method to test {@link ConfigurationHelper#clusterMap(User)}
     * Given Scenario: a back-end user, then an anonymous user
     * Expected Result: the back-end user's cluster section adds heartbeatEnabled, taken from
     * ENABLE_SERVER_HEARTBEAT (on in this environment), next to the keys it had; the anonymous
     * section is unchanged, with only clusterId (issue #37875)
     */
    @Test
    public void Test_Cluster_Map_HeartbeatEnabled_For_Backend_Users_Only() throws Exception {
        final Map<String, Object> backend = instance.clusterMap(APILocator.getUserAPI().loadByUserByEmail(
                "admin@dotcms.com", APILocator.getUserAPI().getSystemUser(), false));
        Assert.assertEquals(Set.of(ConfigurationHelper.CLUSTER_ID, ConfigurationHelper.KEY_DIGEST, "heartbeatEnabled"),
                backend.keySet());
        Assert.assertEquals(Boolean.TRUE, backend.get("heartbeatEnabled"));

        final Map<String, Object> anonymous = instance.clusterMap(APILocator.getUserAPI().getAnonymousUser());
        Assert.assertEquals(Set.of(ConfigurationHelper.CLUSTER_ID), anonymous.keySet());
    }

    /**
     * Method to test {@link ConfigurationHelper#clusterMap(User)}
     * Given Scenario: ENABLE_SERVER_HEARTBEAT is switched off
     * Expected Result: a back-end user's cluster section reports heartbeatEnabled=false (issue #37875)
     */
    @Test
    public void Test_Cluster_Map_HeartbeatEnabled_False_When_Flag_Off() throws Exception {
        final String original = Config.getStringProperty("ENABLE_SERVER_HEARTBEAT", null);
        try {
            Config.setProperty("ENABLE_SERVER_HEARTBEAT", false);
            final Map<String, Object> backend = instance.clusterMap(APILocator.getUserAPI().loadByUserByEmail(
                    "admin@dotcms.com", APILocator.getUserAPI().getSystemUser(), false));
            Assert.assertEquals(Boolean.FALSE, backend.get(ConfigurationHelper.HEARTBEAT_ENABLED));
        } finally {
            Config.setProperty("ENABLE_SERVER_HEARTBEAT", original);
        }
    }
}

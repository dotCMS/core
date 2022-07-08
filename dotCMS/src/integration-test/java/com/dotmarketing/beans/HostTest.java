package com.dotmarketing.beans;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.repackage.org.directwebremoting.json.parse.JsonParseException;
import com.dotcms.rest.api.v1.experiment.ExperimentResource;
import com.dotcms.rest.api.v1.experiment.ExperimentResource.ExperimentResult;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;

public class HostTest {

    @BeforeClass
    public static void initData() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <b>Method to test:</b> {@link Host#isDefault()}<p></p>
     * <b>Test Case:</b> When the method is called on an object without setting the property {@link Host#IS_DEFAULT_KEY}, it should return false<p></p>
     * <b>Expected Results:</b> When the method {@link Host#isDefault()} is invoked, it should return false
     */
    @Test
    public void isDefaultReturnsFalseWhenNotSet(){
        Host host = new Host();
        assertFalse(host.isDefault());
    }

    @Test
    public void isDefaultReturnsFalseWhenNotSet2()
            throws DotDataException, DotSecurityException, JsonParseException, IOException {
        ExperimentResource experimentResource =  new ExperimentResource();
        final Map<String, ExperimentResult> result = experimentResource.result("POC_experiment");
    }

    /**
     * <b>Method to test:</b> {@link Host#isDefault()}<p></p>
     * <b>Test Case:</b> When the property {@link Host#IS_DEFAULT_KEY} is set, it should return a proper value<p></p>
     * <b>Expected Results:</b> When the method {@link Host#isDefault()} is invoked, it should return the value set in {@link Host#IS_DEFAULT_KEY}
     */
    @Test
    public void isDefaultReturnsProperValueWhenSet(){
        Host host = new Host();
        host.setDefault(true);
        assertTrue(host.isDefault());

        host.setDefault(false);
        assertFalse(host.isDefault());
    }

}

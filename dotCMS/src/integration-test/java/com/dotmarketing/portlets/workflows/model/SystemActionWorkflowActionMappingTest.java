package com.dotmarketing.portlets.workflows.model;

import com.dotcms.publisher.pusher.wrapper.WorkflowWrapper;
import com.dotcms.publishing.DotPrettyPrintWriter;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

public class SystemActionWorkflowActionMappingTest {

    /**
     * The idea behind this method is test how XStream performs over the SystemActionWorkflowActionMapping since it is an immutable object
     * this to ensure the communication between the local and remote nodes on the PP
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void test_serialize_deserialize_xstream () throws DotSecurityException, DotDataException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final User    systemUser      = APILocator.systemUser();
        final XStream xstreamWriter   = new XStream(new DomDriver("UTF-8"));
        final StringWriter writer     = new StringWriter();
        final HierarchicalStreamWriter xmlWriter = new DotPrettyPrintWriter(writer);
        final WorkflowScheme workflowScheme      = workflowAPI.findScheme(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID);
        final List<SystemActionWorkflowActionMapping> systemActionMappingsDB = workflowAPI.findSystemActionsByScheme(workflowScheme, systemUser);
        Assert.assertNotNull("The System Workflow must exits",systemActionMappingsDB);
        Assert.assertTrue("The System Workflow must have system actions",systemActionMappingsDB.size()>0);
        xstreamWriter.marshal(systemActionMappingsDB, xmlWriter);

        final XStream xstreamReader = new XStream(new DomDriver());
        final List<SystemActionWorkflowActionMapping> systemActionMappingsRecovery =
                (List<SystemActionWorkflowActionMapping>) xstreamReader.fromXML(writer.toString());

        Assert.assertNotNull(systemActionMappingsRecovery);
        Assert.assertTrue("The System Workflow must have system actions",systemActionMappingsRecovery.size()>0);
        Assert.assertEquals("The system actions recovery by Xstream must be the same on the db", systemActionMappingsDB.size(), systemActionMappingsRecovery.size());
        for (final SystemActionWorkflowActionMapping mappingDB : systemActionMappingsDB) {

            final Optional<SystemActionWorkflowActionMapping> mappingRecoveryOpt = systemActionMappingsRecovery
                    .stream().filter(recoveryMapping -> recoveryMapping.getIdentifier().equals(mappingDB.getIdentifier())).findFirst();

            Assert.assertTrue("The mapping: " + mappingDB.getIdentifier() + " must be on the recoveries mappings from Xstream", mappingRecoveryOpt.isPresent());
            final SystemActionWorkflowActionMapping mappingRecovery = mappingRecoveryOpt.get();
            Assert.assertEquals("The mapping: " + mappingDB.getIdentifier() + " must have the same information of the mapping from Xstream",
                    mappingDB.getSystemAction(), mappingRecovery.getSystemAction());
            Assert.assertEquals("The mapping: " + mappingDB.getIdentifier() + " must have the same information of the mapping from Xstream",
                    mappingDB.getWorkflowAction(), mappingRecovery.getWorkflowAction());
            Assert.assertEquals("The mapping: " + mappingDB.getIdentifier() + " must have the same information of the mapping from Xstream",
                    mappingDB.getOwner(), mappingRecovery.getOwner());
        }
    }
}

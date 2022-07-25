package com.dotmarketing.portlets.workflows.model;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.publisher.pusher.wrapper.ContentTypeWrapper;
import com.dotcms.publisher.pusher.wrapper.WorkflowWrapper;
import com.dotcms.publishing.DotPrettyPrintWriter;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

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

    /**
     * The idea behind this method is test how XStream performs over the SystemActionWorkflowActionMapping since it is an immutable object
     * this to ensure the communication between the local and remote nodes on the PP
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void test_serialize_deserialize_wrapper_xstream () throws DotSecurityException, DotDataException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final User    systemUser      = APILocator.systemUser();
        final XStream xstreamWriter   = new XStream(new DomDriver("UTF-8"));
        final StringWriter writer     = new StringWriter();

        final HierarchicalStreamWriter xmlWriter = new DotPrettyPrintWriter(writer);
        final WorkflowScheme workflowScheme      = workflowAPI.findScheme(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID);
        final List<SystemActionWorkflowActionMapping> systemActionMappingsDB = workflowAPI.findSystemActionsByScheme(workflowScheme, systemUser);
        WorkflowWrapper workflowWrapper = new WorkflowWrapper(workflowScheme, null, null, null, null,
                null, null, null, null);
        Assert.assertNotNull("The System Workflow must exits",systemActionMappingsDB);
        Assert.assertTrue("The System Workflow must have system actions",systemActionMappingsDB.size()>0);
        workflowWrapper.setScheme(workflowScheme);
        workflowWrapper.setSystemActionMappings(systemActionMappingsDB);
        xstreamWriter.marshal(workflowWrapper, xmlWriter);

        final XStream xstreamReader = new XStream(new DomDriver());
        final WorkflowWrapper workflowWrapperRecovery =
                (WorkflowWrapper) xstreamReader.fromXML(writer.toString());

        Assert.assertNotNull(workflowWrapperRecovery);
        Assert.assertNotNull(workflowWrapperRecovery.getScheme());
        Assert.assertEquals("The System Workflow must have system actions",workflowWrapperRecovery.getScheme(), workflowScheme);
        final List<SystemActionWorkflowActionMapping> systemActionMappingsRecovery = workflowWrapperRecovery.getSystemActionMappings();

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

    @Test
    public void test_serialize_deserialize_wrapper_jackson ()
            throws DotSecurityException, DotDataException, IOException {

        final WorkflowAPI workflowAPI   = APILocator.getWorkflowAPI();
        final User    systemUser        = APILocator.systemUser();
        final ObjectMapper mapperWriter = new ObjectMapper();
        final StringWriter writer       = new StringWriter();

        final WorkflowScheme workflowScheme = workflowAPI.findScheme(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID);
        final List<SystemActionWorkflowActionMapping> systemActionMappingsDB = workflowAPI.findSystemActionsByScheme(workflowScheme, systemUser);
        final ContentType contentType = new ContentTypeDataGen().velocityVarName("test"+System.currentTimeMillis()).nextPersisted();
        ContentTypeWrapper contentTypeWrapper = new ContentTypeWrapper();
        Assert.assertNotNull("The System Workflow must exits",systemActionMappingsDB);
        Assert.assertTrue("The System Workflow must have system actions",systemActionMappingsDB.size()>0);
        contentTypeWrapper.setContentType(contentType);
        contentTypeWrapper.setSystemActionMappings(systemActionMappingsDB);
        contentTypeWrapper.setWorkflowSchemaIds(Arrays.asList("1","2","3"));
        contentTypeWrapper.setWorkflowSchemaNames(Arrays.asList("WF1","WF2","WF3"));
        mapperWriter.writeValue(writer, contentTypeWrapper);

        Logger.info(this, "JSON: " + writer.toString());
        final  ObjectMapper mapperReader             = new ObjectMapper();
        final ContentTypeWrapper contentTypeWrapper1 =
                mapperReader.readValue(writer.toString(), ContentTypeWrapper.class);


        Assert.assertNotNull(contentTypeWrapper1);
        Assert.assertNotNull(contentTypeWrapper1.getContentType());
        Assert.assertEquals("The System Workflow must have system actions",contentTypeWrapper1.getContentType().variable(), contentType.variable());
        final List<SystemActionWorkflowActionMapping> systemActionMappingsRecovery = contentTypeWrapper1.getSystemActionMappings();

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

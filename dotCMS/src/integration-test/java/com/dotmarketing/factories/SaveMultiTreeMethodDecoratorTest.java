package com.dotmarketing.factories;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SaveMultiTreeMethodDecoratorTest extends IntegrationTestBase {
    

    @BeforeClass
    public static void initData() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }
    

    @Test
    public  void testDecorate() throws Exception {

        final SaveMultiTreeMethodDecorator decorator =
                new SaveMultiTreeMethodDecorator();
        final String htmlPage  = "123";
        final String container = "//demo.dotcms.com/application/containers/large-column/";
        final String child     = "222";
        final Object [] arguments = new Object[] {
                "123",
                new MultiTree(htmlPage, container, child)
        };

        final Object [] newArguments = decorator.decorate(arguments);

        Assert.assertNotNull(newArguments);
        Assert.assertEquals (arguments.length, newArguments.length);
        Assert.assertEquals (arguments[0], newArguments[0]);

        final MultiTree newMultiTree = (MultiTree) arguments[1];
        Assert.assertNotNull(newMultiTree);

        final Container largeColumnContainer = APILocator.getContainerAPI()
                .getWorkingContainerByFolderPath(container, APILocator.systemUser(), false, ()->APILocator.systemHost());
        Assert.assertEquals (newMultiTree.getContainer(), largeColumnContainer.getIdentifier());
    }
}

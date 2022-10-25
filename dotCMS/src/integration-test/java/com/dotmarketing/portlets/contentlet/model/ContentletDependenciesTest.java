package com.dotmarketing.portlets.contentlet.model;

import com.dotmarketing.business.APILocator;
import org.junit.Assert;
import org.junit.Test;

public class ContentletDependenciesTest {

    @Test
    public void test_copy_contentlet_dependencies(){

        final ContentletDependencies  contentletDependencies = new ContentletDependencies.Builder()
                .indexPolicyDependencies(IndexPolicy.DEFER)
                .indexPolicy(IndexPolicy.WAIT_FOR)
                .modUser(APILocator.systemUser())
                .build();

        final ContentletDependencies  copyContentletDependencies = new ContentletDependencies.Builder()
                .from(contentletDependencies)
                .indexPolicyDependencies(IndexPolicy.WAIT_FOR)
                .build();

        Assert.assertEquals(IndexPolicy.DEFER, contentletDependencies.getIndexPolicyDependencies());
        Assert.assertEquals(IndexPolicy.WAIT_FOR, contentletDependencies.getIndexPolicy());
        Assert.assertEquals(APILocator.systemUser(), contentletDependencies.getModUser());

        Assert.assertEquals(IndexPolicy.WAIT_FOR, copyContentletDependencies.getIndexPolicyDependencies());
        Assert.assertEquals(IndexPolicy.WAIT_FOR, copyContentletDependencies.getIndexPolicy());
        Assert.assertEquals(APILocator.systemUser(), copyContentletDependencies.getModUser());
    }
}

package com.dotcms.datagen;

import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.personas.model.Persona;
import com.liferay.util.StringPool;

public class MultiTreeDataGen extends AbstractDataGen<MultiTree> {
    private Container container;
    private Contentlet contentlet;
    private String instanceID = "1";
    private String personalization = MultiTree.DOT_PERSONALIZATION_DEFAULT;
    private String variantId = VariantAPI.DEFAULT_VARIANT.name();
    private int treeOrder;
    private HTMLPageAsset page;
    private String styleProperties;

    @Override
    public MultiTree next() {
        MultiTree multiTree = new MultiTree();
        multiTree.setHtmlPage(page);
        multiTree.setContainer(container);
        multiTree.setContentlet(contentlet);
        multiTree.setInstanceId(instanceID);
        multiTree.setTreeOrder(treeOrder);
        multiTree.setPersonalization(personalization);
        multiTree.setVariantId(variantId);
        multiTree.setTreeOrder(1);
        multiTree.setStyleProperties(styleProperties);

        return multiTree;
    }

    @Override
    public MultiTree persist(final MultiTree multiTree) {
        try {
            APILocator.getMultiTreeAPI().saveMultiTree(multiTree);
            return multiTree;
        } catch (DotDataException e) {
            throw  new RuntimeException(e);
        }
    }

    public MultiTreeDataGen setPage(final HTMLPageAsset page) {
        this.page = page;
        return this;
    }

    public MultiTreeDataGen setContainer(final Container container) {
        this.container = container;
        return this;
    }

    public MultiTreeDataGen setContentlet(final Contentlet contentlet) {
        this.contentlet = contentlet;
        return this;
    }

    public MultiTreeDataGen setInstanceID(final String instanceID) {
        this.instanceID = instanceID;
        return this;
    }

    public MultiTreeDataGen setPersonalization(final String personalization) {
        this.personalization = personalization;
        return this;
    }

    public MultiTreeDataGen setTreeOrder(final int treeOrder) {
        this.treeOrder = treeOrder;
        return this;
    }

    public MultiTreeDataGen setPersona(Persona persona) {
        personalization = Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + persona.getKeyTag();
        return this;
    }

    public MultiTreeDataGen setVariant(final Variant variant) {
        this.variantId = variant.name();
        return this;
    }

    public MultiTreeDataGen setStyleProperties(final String styleProperties) {
        this.styleProperties = styleProperties;
        return this;
    }
}


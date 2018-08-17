package com.dotmarketing.portlets.structure.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ContentletRelationships transformer
 * @author jsanca
 */
public class ContentletRelationshipsTransformer implements DBTransformer<ContentletRelationships> {

    private final Contentlet contentlet;
    private final Map<Relationship, List<Contentlet>> contentRelationships;

    public ContentletRelationshipsTransformer(final Contentlet contentlet,
            final Map<Relationship, List<Contentlet>> contentRelationships) {

        this.contentlet           = contentlet;
        this.contentRelationships = contentRelationships;
    }

    @Override
    public List<ContentletRelationships> asList() {

        final List<ContentletRelationships> relationshipsList = new ArrayList<>();

        final ContentletRelationships contentletRelationships =
                this.getContentletRelationshipsFromMap(this.contentlet,
                        this.contentRelationships);

        if (null != contentletRelationships) {
            relationshipsList.add(contentletRelationships);
        }

        return relationshipsList;
    }

    private ContentletRelationships getContentletRelationshipsFromMap(Contentlet contentlet,
                                                                      Map<Relationship, List<Contentlet>> contentRelationships) {

        if(contentRelationships == null) {
            return null;
        }

        Structure st = CacheLocator.getContentTypeCache().getStructureByInode(contentlet.getStructureInode());
        ContentletRelationships relationshipsData = new ContentletRelationships(contentlet);
        List<ContentletRelationships.ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationships.ContentletRelationshipRecords>();
        relationshipsData.setRelationshipsRecords(relationshipsRecords);
        for(Map.Entry<Relationship, List<Contentlet>> relEntry : contentRelationships.entrySet()) {
            Relationship relationship = relEntry.getKey();
            boolean hasParent = FactoryLocator.getRelationshipFactory().isParent(relationship, st);
            boolean hasChildren = FactoryLocator.getRelationshipFactory().isChild(relationship, st);

            // self-join (same CT for parent and child) relationships return true to both, so since we can't
            // determine if it's parent or child we always assume child (e.g. Coming from the Content REST API)
            if (hasParent && hasChildren) {
                hasParent = false;
            }
            ContentletRelationships.ContentletRelationshipRecords
                    records = relationshipsData.new ContentletRelationshipRecords(relationship, hasParent);
            records.setRecords(relEntry.getValue());
            relationshipsRecords.add(records);
        }
        return relationshipsData;
    }
}

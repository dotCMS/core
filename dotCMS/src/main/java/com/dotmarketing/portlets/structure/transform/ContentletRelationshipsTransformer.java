package com.dotmarketing.portlets.structure.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

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

    private ContentletRelationships getContentletRelationshipsFromMap(final Contentlet contentlet,
                                                                      final Map<Relationship, List<Contentlet>> contentRelationships) {

        if(contentRelationships == null) {

            return null;
        }

        final Structure structure = CacheLocator.getContentTypeCache()
                .getStructureByInode(contentlet.getStructureInode());
        final ContentletRelationships relationshipsData = new ContentletRelationships(contentlet);
        final List<ContentletRelationships.ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationships.ContentletRelationshipRecords>();
        relationshipsData.setRelationshipsRecords(relationshipsRecords);

        for(final Map.Entry<Relationship, List<Contentlet>> relEntry : contentRelationships.entrySet()) {

            final Relationship relationship = relEntry.getKey();
            boolean hasParent               = APILocator.getRelationshipAPI().isParent(relationship, structure);

            final ContentletRelationships.ContentletRelationshipRecords
                    records = relationshipsData.new ContentletRelationshipRecords(relationship, hasParent);
            records.setRecords(relEntry.getValue());
            relationshipsRecords.add(records);
        }

        return relationshipsData;
    }
}

package com.dotcms.test.util.assertion;

import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Relationship;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * {@link AssertionChecker} concrete class for {@link Relationship}
 */
public class RelationshipAssertionChecker implements AssertionChecker<Relationship> {
    @Override
    public Map<String, Object> getFileArguments(final Relationship relationship, File file) {
        return Map.of(
                "inode", relationship.getInode(),
                "parent_inode", relationship.getParentStructure().id(),
                "child_inode", relationship.getChildStructure().id(),
                "child_name", relationship.getChildRelationName(),
                "relation_type", relationship.getRelationTypeValue(),
                "cardinality", relationship.getCardinality()
        );
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/relationship/relationship.relationship.xml";
    }

    @Override
    public File getFileInner(final Relationship relationship, final File bundleRoot) {
        try {
            return FileBundlerTestUtil.getRelationshipPath(relationship, bundleRoot);
        } catch (DotSecurityException | DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> getRegExToRemove(File file) {
        return list(
                "<iDate>.*</iDate>"
        );
    }
}

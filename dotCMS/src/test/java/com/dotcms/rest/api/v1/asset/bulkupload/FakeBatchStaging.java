package com.dotcms.rest.api.v1.asset.bulkupload;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An in-memory {@link BatchStaging} for the reader's unit tests.
 * <p>
 * Records what was staged and what was reclaimed, so a test can assert the two things that matter
 * and neither of which is visible from the reader's return value: that the read <b>stopped</b>
 * where it should have, and that nothing was <b>left behind</b> when it did.
 */
class FakeBatchStaging implements BatchStaging {

    private final List<String> staged = new ArrayList<>();
    private final Set<String> reclaimed = new LinkedHashSet<>();
    private int failOnPart = -1;
    private int counter = 0;

    /** Makes the given zero-based part throw, standing in for a client that went away mid-read. */
    FakeBatchStaging failOnPart(final int index) {
        this.failOnPart = index;
        return this;
    }

    @Override
    public StagedPart stage(final String fileName, final InputStream content) throws IOException {
        if (counter == failOnPart) {
            counter++;
            throw new IOException("connection reset by peer");
        }
        counter++;
        final byte[] bytes = content.readAllBytes();
        final String id = "temp_" + fileName;
        staged.add(id);
        return new StagedPart(id, fileName, bytes.length, "application/octet-stream");
    }

    @Override
    public void reclaim(final String tempFileId) {
        reclaimed.add(tempFileId);
    }

    /** Ids staged so far, in order — how far the read actually got. */
    List<String> staged() { return staged; }

    /** Ids handed back for cleanup. */
    Set<String> reclaimed() { return reclaimed; }

    /** Staged and never reclaimed: what would be left on disk, where nothing purges on a schedule. */
    List<String> leaked() {
        final List<String> leaked = new ArrayList<>(staged);
        leaked.removeAll(reclaimed);
        return leaked;
    }
}

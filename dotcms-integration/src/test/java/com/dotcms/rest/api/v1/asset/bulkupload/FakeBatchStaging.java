package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.business.DotStateException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An in-memory {@link BatchStaging} for the submission integration tests.
 * <p>
 * <b>Duplicated from the copy in {@code dotcms-core}'s test sources on purpose.</b>
 * {@code dotcms-integration} does not depend on core's {@code test-jar}, so the class is not on
 * this classpath; adding that dependency to reach one 50-line double would pull every core test
 * class into this module. The two copies are independent by design — if one drifts, the tests that
 * use it are the ones that care.
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
        final byte[] bytes = readAsTempFileApiWould(content);
        final String id = "temp_" + fileName;
        staged.add(id);
        return new StagedPart(id, fileName, bytes.length, "application/octet-stream");
    }

    /**
     * Consumes the stream the way {@code TempFileAPI.createTempFile} does — <b>including what it
     * does to an exception raised during the read</b>.
     * <p>
     * This is the part a test double is easy to get wrong and expensive to get wrong. Reading the
     * bytes raw lets whatever the caller's stream throws propagate unchanged, which makes a
     * {@code catch} on that exact type look correct — while the only staging implementation that
     * ships rewraps it, so production never enters that clause. That is not hypothetical: it is
     * precisely how the per-file ceiling came to be unreachable with six green tests over it.
     * <p>
     * So the wrapping is modelled, not the storage:
     * <ul>
     *   <li>{@code IOException} during the read → {@code DotStateException} carrying the size
     *       message, which is why a refusal cannot be told from a dead connection by its type
     *       ({@code TempFileAPI:191});</li>
     *   <li>anything else → {@code DotRuntimeException} with the original as its
     *       <b>cause</b> ({@code TempFileAPI:194}).</li>
     * </ul>
     */
    private byte[] readAsTempFileApiWould(final InputStream content) {
        try {
            return content.readAllBytes();
        } catch (final IOException e) {
            throw new DotStateException("temp.file.max.file.size.error", e);
        } catch (final Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void reclaim(final String tempFileId) {
        reclaimed.add(tempFileId);
    }

    /** Ids staged so far, in order — how far the read actually got. */
    List<String> staged() { return staged; }

    /** Ids handed back for cleanup. */
    Set<String> reclaimed() { return reclaimed; }

    /** Staged and never reclaimed: what a run would leave on the shared assets volume. */
    List<String> leaked() {
        final List<String> leaked = new ArrayList<>(staged);
        leaked.removeAll(reclaimed);
        return leaked;
    }
}

/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

/**
 * Static Push Publishing: renders a site (or a scoped subset) to a static target such as an AWS S3
 * bucket ({@link com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher}) or a local
 * filesystem mirror ({@link com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher}).
 *
 * <h2>Static vs. Dynamic Push Publishing semantics</h2>
 *
 * <p>The two flavors of Push Publishing differ in <i>what</i> they replicate, which changes how the
 * {@code PUBLISH} and {@code UNPUBLISH} (a.k.a. "push remove") operations behave:</p>
 *
 * <ul>
 *   <li><b>Dynamic</b> (push to a remote dotCMS instance) replicates the <i>content object</i> and
 *       <i>all of its versions</i>. The sender bundles both the working and live versions
 *       ({@code ContentBundler}), and the receiver ({@code ContentHandler}) restores them with
 *       their state: live as live, working as working.</li>
 *   <li><b>Static</b> (S3 / filesystem) materializes only the <i>rendered live output</i>. There is
 *       no content object and no working/live distinction on the target — just files. Therefore
 *       static publishing exports live content only, and the only way to "de-publish" is to delete
 *       the rendered artifact.</li>
 * </ul>
 *
 * <h3>Operation matrix</h3>
 * <pre>
 *  Target    Operation             Behavior
 *  --------  --------------------  --------------------------------------------------------------
 *  Dynamic   PUBLISH               Replicate content; receiver makes it live, or unpublishes it
 *                                  when the sender's liveInode == null (keeps the working version).
 *  Dynamic   UNPUBLISH (remove)    Receiver destroys the content object entirely (all versions),
 *                                  independent of the source live/working state.
 *  Static    PUBLISH               Copy/upload the live-rendered artifacts to the target.
 *                                  Working content is never published (there is no working state
 *                                  on a static target).
 *  Static    UNPUBLISH (remove)    Delete the corresponding rendered artifact from the target,
 *                                  independent of the source live/working state.
 * </pre>
 *
 * <p>Key parallel: a dynamic <b>push remove</b> and a static <b>push remove</b> both remove the
 * content's representation from the target <i>regardless of the source content state</i>. The
 * difference in granularity is that dynamic destroys the whole content object while static deletes
 * the rendered file. The static side has no equivalent of the dynamic "soft unpublish that keeps a
 * working version", precisely because a static target has no working representation.</p>
 *
 * <h3>How static un-publish removal is driven (issue #35365)</h3>
 *
 * <p>Because the content bundlers export live content only, an already-unpublished asset leaves
 * nothing under {@code /live/...} for the static publishers to act on. To remove it, the
 * {@link com.dotcms.enterprise.publishing.staticpublishing.StaticDependencyBundler} — the one
 * bundler that sees the actual publish-queue deltas and can resolve an asset's path even with no
 * live version — writes a zero-byte {@code /live/<host>/<langId>/<path>} marker for each
 * un-published asset on a static {@code UNPUBLISH} bundle (see
 * {@link com.dotcms.enterprise.publishing.bundlers.StaticUnpublishMarker}). The behavior is gated on
 * {@link com.dotcms.publishing.PublisherConfig#isStatic()}, so Site Search and dynamic Push
 * Publishing bundles are unaffected.</p>
 *
 * <p><b>Who consumes the markers.</b> The marker lives in the bundle root's {@code /live/} tree, and
 * each consumer removes the artifact from <i>its own</i> persistent target:</p>
 * <ul>
 *   <li>{@link com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher} reads the bundle's
 *       {@code /live/} directly and deletes the matching objects from the S3 bucket — the built-in
 *       static endpoint.</li>
 *   <li>{@link com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher} copies the bundle's
 *       {@code /live/} contents (markers included) into a <i>per-bundle</i> {@code PUBLISH_TO}
 *       staging folder, operation-agnostically. It is not itself a persistent mirror; a transport
 *       listener (e.g. an SFTP/FTP plugin subscribing to {@code SingleStaticPublishEndpointSuccessEvent})
 *       then ships that staging folder onward and, on {@code UNPUBLISH}, removes the corresponding
 *       files from its endpoint. The markers are what populate the staging folder on an un-publish,
 *       so the listener has the paths to remove even though no live version remains.</li>
 * </ul>
 */
package com.dotcms.enterprise.publishing.staticpublishing;

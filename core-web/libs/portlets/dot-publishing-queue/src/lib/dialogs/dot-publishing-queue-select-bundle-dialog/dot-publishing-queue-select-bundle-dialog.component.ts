import { EMPTY, Subject, forkJoin, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    OnInit,
    computed,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import {
    catchError,
    debounceTime,
    distinctUntilChanged,
    finalize,
    map,
    take
} from 'rxjs/operators';

/* eslint-disable @nx/enforce-module-boundaries */
// `DotPushPublishFormComponent` lives in apps/dotcms-ui (not yet promoted to
// shared libs). Same pattern as `dot-publishing-queue-table`. Tracked alongside
// the v1 consolidation work (#36048).

import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import {
    DotContentletEditUrlService,
    DotCurrentUserService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPublishingQueueService
} from '@dotcms/data-access';
import {
    BundleAssetView,
    DotCMSContentlet,
    DotPushPublishData,
    DotPushPublishDialogData,
    PushBundleForm,
    PushBundleOperation
} from '@dotcms/dotcms-models';
import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';
import { getDownloadLink } from '@dotcms/utils';

import {
    DotDownloadBundleFormComponent,
    DotDownloadBundleFormValue
} from '../../components/dot-download-bundle-form/dot-download-bundle-form.component';
import { groupContentletAssetsByType } from '../../util/asset-groups.util';

type LoadStatus = 'init' | 'loading' | 'loaded' | 'error';

interface BundleRow {
    id: string;
    name: string;
}

interface EmptyStateConfig {
    icon: string;
    messageKey: string;
}

/** Map asset `type` string (lowercase, comes from `PusheableAsset.getType()`)
 * to a PrimeIcon. Unknown types fall back to a generic file icon. */
const TYPE_ICONS: Record<string, string> = {
    contentlet: 'pi pi-file',
    contenttype: 'pi pi-box',
    template: 'pi pi-window-maximize',
    containers: 'pi pi-th-large',
    folder: 'pi pi-folder',
    host: 'pi pi-globe',
    category: 'pi pi-tag',
    links: 'pi pi-link',
    workflow: 'pi pi-cog',
    language: 'pi pi-language',
    rule: 'pi pi-shield',
    user: 'pi pi-user',
    osgi: 'pi pi-box',
    relationship: 'pi pi-share-alt',
    experiment: 'pi pi-chart-bar',
    variant: 'pi pi-clone'
};

const BUNDLES_PER_PAGE = 6;
const ASSETS_PER_PAGE = 10;

/**
 * Two-pane dialog: drafts on the left, the active draft's assets on the right.
 * Used by the "Add Bundle → Select Bundle" entry point in the toolbar.
 *
 * Self-contained state — does NOT mutate the unified bundles table store.
 * The "Configure" and "Download" actions delegate to the project-wide dialogs
 * (same path as the table row kebab in `dot-publishing-queue-table`).
 */
@Component({
    selector: 'dot-publishing-queue-select-bundle-dialog',
    imports: [
        FormsModule,
        ButtonModule,
        ConfirmDialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        SkeletonModule,
        TableModule,
        TagModule,
        TooltipModule,
        DotCopyButtonComponent,
        DotDownloadBundleFormComponent,
        DotMessagePipe,
        DotPushPublishFormComponent
    ],
    // `DotPushPublishFiltersService` is `providedIn: 'root'` (libs/data-access)
    // — both the embedded `<dot-push-publish-form>` and the download step's
    // `<dot-download-bundle-form>` inject the root-scoped singleton. No need
    // to component-provide it (the legacy DotPushPublishDialogComponent did,
    // but that's a stateless HttpClient wrapper so the per-instance copy was
    // redundant).
    providers: [ConfirmationService],
    templateUrl: './dot-publishing-queue-select-bundle-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex h-full min-h-0 flex-col' }
})
export class DotPublishingQueueSelectBundleDialogComponent implements OnInit {
    readonly #publishingService = inject(DotPublishingQueueService);
    readonly #currentUserService = inject(DotCurrentUserService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #editUrlService = inject(DotContentletEditUrlService);
    readonly #globalMessage = inject(DotGlobalMessageService);
    readonly #dialogRef = inject(DynamicDialogRef, { optional: true });

    #userId: string | null = null;

    readonly $bundles = signal<BundleRow[]>([]);
    /** Cursor-style "there is a next page" flag. The BE's `numRows` returns the
     * size of the current page, not the total across all pages, so we can't
     * compute a maxPage. Instead, `bundlesHasMore` is true when the current
     * response returned a full page (=== BUNDLES_PER_PAGE items) — as soon as
     * a partial page comes back, we're on the last page. Follow-up: extend the
     * BE to include a real total count so we can go back to numeric pagination. */
    readonly $bundlesHasMore = signal(false);
    readonly $bundlesStatus = signal<LoadStatus>('init');
    readonly $bundlesPage = signal(1);
    readonly $bundleSearch = signal('');
    /** Multi-select for bulk operations (Remove). Independent of `activeBundleId`. */
    readonly $checkedBundleIds = signal<string[]>([]);
    /** Single "active" bundle whose assets are shown on the right pane. */
    readonly $activeBundleId = signal<string | null>(null);

    readonly $assets = signal<BundleAssetView[]>([]);
    readonly $assetsStatus = signal<LoadStatus>('init');
    readonly $assetsPage = signal(1);
    /** Per-asset edit URLs resolved by `DotContentletEditUrlService` after each
     * asset load. Only contentlet rows get an entry — non-contentlet types are
     * rendered as plain text. Resolution is async (one metadata fetch per
     * content type, cached app-wide by the service). */
    readonly $assetEditUrls = signal<Map<string, string>>(new Map());

    readonly assetsPerPage = ASSETS_PER_PAGE;

    /** Placeholder rows for the skeleton state. PrimeNG's `dataKey` resolution
     * needs a defined key per item; using bare `undefined` slots (as
     * `Array.from({length:N})` produces) makes the table skip rendering the
     * skeleton row. Objects with a stable `id`/`asset` field are the pattern
     * from PrimeNG's official loading-skeleton example. */
    readonly bundlesSkeleton = Array.from({ length: 6 }, (_, i) => ({
        id: `__skel_${i}`
    }));
    readonly assetsSkeleton = Array.from({ length: 6 }, (_, i) => ({
        asset: `__skel_${i}`
    }));

    /** `table-layout: fixed` + `width: 100%` so column widths are driven by the
     * `<col>`/header widths instead of by cell content. Without this, a long
     * bundle name or asset title would push the table past the pane width and
     * trigger horizontal scroll. */
    readonly tableStyleFixed = { 'table-layout': 'fixed' as const, width: '100%' };

    readonly $activeBundle = computed(() => {
        const id = this.$activeBundleId();
        return id ? (this.$bundles().find((b) => b.id === id) ?? null) : null;
    });

    readonly $hasChecked = computed(() => this.$checkedBundleIds().length > 0);
    readonly $hasActive = computed(() => this.$activeBundleId() !== null);

    /** Three-step wizard inside this single modal: step 1 picks bundles,
     * step 2 embeds the push-publish form and submits to
     * /api/v1/publishing/push, step 3 embeds the download-bundle form and
     * fires /api/bundle/_generate. */
    readonly $step = signal<'select' | 'configure' | 'download'>('select');
    readonly $isSending = signal(false);

    /** Latest form value emitted by the embedded `<dot-push-publish-form>`. The
     * form re-emits on every keystroke; we just hold the most recent. */
    readonly $configureFormValue = signal<DotPushPublishData | null>(null);
    readonly $configureFormValid = signal(false);

    /** Send is enabled only when the form is valid AND we're not already
     * pushing. Disabled-while-sending prevents double-submit. */
    readonly $canSend = computed(() => this.$configureFormValid() && !this.$isSending());

    /** Latest form value from the embedded `<dot-download-bundle-form>`. */
    readonly $downloadFormValue = signal<DotDownloadBundleFormValue | null>(null);
    readonly $downloadFormValid = signal(false);

    /** True while a `_generate` POST is in flight — disables the Download
     * button and swaps its label so the user can't double-click. */
    readonly $isDownloading = signal(false);

    readonly $canDownload = computed(() => this.$downloadFormValid() && !this.$isDownloading());

    /** BundleId fed into the download form. The step is only reachable with
     * exactly one bundle checked (guarded in `onOpenDownloadStep`). */
    readonly $downloadBundleId = computed(() => this.$checkedBundleIds()[0] ?? '');

    /** Inline warning shown in the footer's left side when the user clicks an
     * action button (Remove / Download / Configure) without a valid selection.
     * Cleared automatically when the selection changes — `onCheckedChange` is
     * the only place a "valid" state can come into being from this dialog.
     * Stored as a translated i18n key, resolved at render time. */
    readonly $validationWarningKey = signal<string | null>(null);

    /** Data fed to the embedded `<dot-push-publish-form>`. `assetIdentifier`
     * keys the env selector's "remember last push" — for multi-bundle we use
     * the first checked id (same form values get applied to all, so they share
     * the same "last push" memory anyway). */
    readonly $configureFormData = computed<DotPushPublishDialogData>(() => {
        const ids = this.$checkedBundleIds();
        const first = ids[0] ?? '';
        const firstName = this.$bundles().find((b) => b.id === first)?.name ?? first;
        const title = ids.length <= 1 ? firstName : `${ids.length} bundles`;

        return {
            assetIdentifier: first,
            title,
            isBundle: true
        };
    });

    /** Bundle rows currently selected via checkbox. p-table's `[selection]`
     * binding wants the row objects (not just ids), so we re-derive them from
     * the visible bundle list each CD. */
    readonly $checkedBundles = computed(() => {
        const ids = new Set(this.$checkedBundleIds());
        return this.$bundles().filter((b) => ids.has(b.id));
    });

    readonly $pagedAssets = computed(() => {
        const all = this.$assets();
        const page = this.$assetsPage();
        const start = (page - 1) * ASSETS_PER_PAGE;
        return all.slice(start, start + ASSETS_PER_PAGE);
    });

    readonly $assetsTotal = computed(() => this.$assets().length);

    /** Empty-state config for the bundles pane. Splits "nothing saved yet" from
     * "search returned nothing" so the icon + copy guide the user's next move. */
    readonly $bundlesEmptyConfig = computed<EmptyStateConfig>(() => {
        const hasSearch = this.$bundleSearch().trim().length > 0;
        return hasSearch
            ? {
                  icon: 'pi-search-minus',
                  messageKey: 'publishing-queue.select-bundle.empty.search'
              }
            : {
                  icon: 'pi-inbox',
                  messageKey: 'publishing-queue.select-bundle.empty'
              };
    });

    /** Empty-state config for the assets pane. Distinguishes "no bundle picked
     * yet" (steer the user to the left list) from "picked bundle is empty". */
    readonly $assetsEmptyConfig = computed<EmptyStateConfig>(() => {
        const hasActive = this.$activeBundleId() !== null;
        return hasActive
            ? {
                  icon: 'pi-box',
                  messageKey: 'publishing-queue.select-bundle.asset-empty'
              }
            : {
                  icon: 'pi-search-minus',
                  messageKey: 'publishing-queue.select-bundle.no-active'
              };
    });

    readonly #bundleSearchSubject = new Subject<string>();

    ngOnInit(): void {
        this.#bundleSearchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => {
                this.$bundleSearch.set(value);
                this.$bundlesPage.set(1);
                this.#loadBundles();
            });

        this.#currentUserService
            .getCurrentUser()
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.$bundlesStatus.set('error');
                    return EMPTY;
                })
            )
            .subscribe((user) => {
                this.#userId = user.userId;
                this.#loadBundles();
            });
    }

    onBundleSearch(value: string): void {
        this.#bundleSearchSubject.next(value);
    }

    onBundlesPagePrev(): void {
        if (this.$bundlesPage() > 1) {
            this.$bundlesPage.update((p) => p - 1);
            this.#resetCheckedForPageChange();
            this.#loadBundles();
        }
    }

    onBundlesPageNext(): void {
        if (this.$bundlesHasMore()) {
            this.$bundlesPage.update((p) => p + 1);
            this.#resetCheckedForPageChange();
            this.#loadBundles();
        }
    }

    /** Cursor pagination replaces the visible bundle list on every page change,
     * so any ids checked on the previous page would silently outlive the rows
     * they came from — the counter in the footer would include invisible ids.
     * Clear the selection to keep the visible state honest. */
    #resetCheckedForPageChange(): void {
        this.$checkedBundleIds.set([]);
        this.$validationWarningKey.set(null);
    }

    onAssetsPagePrev(): void {
        if (this.$assetsPage() > 1) {
            this.$assetsPage.update((p) => p - 1);
        }
    }

    onAssetsPageNext(): void {
        const maxPage = Math.max(1, Math.ceil(this.$assetsTotal() / ASSETS_PER_PAGE));
        if (this.$assetsPage() < maxPage) {
            this.$assetsPage.update((p) => p + 1);
        }
    }

    onSelectBundle(bundle: BundleRow): void {
        if (this.$activeBundleId() === bundle.id) {
            return;
        }
        this.$activeBundleId.set(bundle.id);
        this.$assetsPage.set(1);
        this.#loadAssets(bundle.id);
    }

    onCheckedChange(ids: BundleRow[]): void {
        this.$checkedBundleIds.set(ids.map((b) => b.id));
        // Any selection change is a direct response to a footer warning — clear
        // it so the user gets immediate feedback that their click registered.
        this.$validationWarningKey.set(null);
    }

    typeIcon(type: string): string {
        return TYPE_ICONS[(type ?? '').toLowerCase()] ?? 'pi pi-file';
    }

    onRemoveAsset(asset: BundleAssetView): void {
        const bundleId = this.$activeBundleId();
        if (!bundleId) {
            return;
        }
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get(
                'publishing-queue.asset-list.remove-confirm.header'
            ),
            message: this.#dotMessageService.get(
                'publishing-queue.asset-list.remove-confirm.message',
                asset.title || asset.asset
            ),
            acceptLabel: this.#dotMessageService.get('publishing-queue.remove'),
            rejectLabel: this.#dotMessageService.get('publishing-queue.cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => {
                this.#publishingService
                    .removeAssetsFromBundle(bundleId, [asset.asset])
                    .pipe(
                        take(1),
                        catchError((error) => {
                            this.#httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => this.#loadAssets(bundleId));
            }
        });
    }

    onRemoveBundles(): void {
        const ids = this.$checkedBundleIds();
        if (ids.length === 0) {
            this.$validationWarningKey.set('publishing-queue.select-bundle.warning.select-one');
            return;
        }
        this.$validationWarningKey.set(null);
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get('publishing-queue.delete.confirm.header'),
            message: this.#dotMessageService.get(
                'publishing-queue.select-bundle.remove.confirm.message',
                String(ids.length)
            ),
            acceptLabel: this.#dotMessageService.get('publishing-queue.history.kebab.delete'),
            rejectLabel: this.#dotMessageService.get('publishing-queue.cancel'),
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => {
                this.#publishingService
                    .deleteBundles(ids)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            this.#httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => {
                        this.$checkedBundleIds.set([]);
                        // If the active bundle was deleted, clear the right pane.
                        if (this.$activeBundleId() && ids.includes(this.$activeBundleId() ?? '')) {
                            this.$activeBundleId.set(null);
                            this.$assets.set([]);
                        }
                        this.#loadBundles();
                    });
            }
        });
    }

    /**
     * Transitions to the download step. Pre-validates selection: empty →
     * "select at least one", multi → "single only" (the BE `_generate`
     * endpoint accepts one bundleId per call).
     */
    onOpenDownloadStep(): void {
        const count = this.$checkedBundleIds().length;
        if (count === 0) {
            this.$validationWarningKey.set('publishing-queue.select-bundle.warning.select-one');
            return;
        }
        if (count > 1) {
            this.$validationWarningKey.set('publishing-queue.select-bundle.download.single-only');
            return;
        }
        this.$validationWarningKey.set(null);
        this.$downloadFormValue.set(null);
        this.$downloadFormValid.set(false);
        this.$step.set('download');
    }

    onDownloadFormValue(value: DotDownloadBundleFormValue): void {
        this.$downloadFormValue.set(value);
    }

    onDownloadFormValid(valid: boolean): void {
        this.$downloadFormValid.set(valid);
        if (valid) {
            this.$validationWarningKey.set(null);
        }
    }

    /**
     * Submits the download-bundle form. Replicates the legacy
     * `DotDownloadBundleDialogComponent` submit exactly — same endpoint, same
     * payload, same blob-anchor click — but via the project's `HttpClient`
     * wrapper (`DotPublishingQueueService.generateBundle`) instead of raw
     * `fetch`, so auth and error interceptors apply.
     */
    onDownload(): void {
        const value = this.$downloadFormValue();
        const bundleId = this.$downloadBundleId();
        if (!value || !bundleId || !this.$downloadFormValid() || this.$isDownloading()) {
            return;
        }

        this.$isDownloading.set(true);
        this.#publishingService
            .generateBundle(bundleId, value.operation, value.filterKey)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    return EMPTY;
                }),
                finalize(() => this.$isDownloading.set(false))
            )
            .subscribe(({ blob, filename }) => {
                getDownloadLink(blob, filename).click();
                this.onBackToList();
            });
    }

    onOpenConfigureStep(): void {
        if (!this.$hasChecked()) {
            this.$validationWarningKey.set('publishing-queue.select-bundle.warning.select-one');
            return;
        }
        this.$validationWarningKey.set(null);
        this.$step.set('configure');
    }

    onBackToList(): void {
        this.$step.set('select');
        this.$validationWarningKey.set(null);
    }

    onConfigureFormValue(value: DotPushPublishData): void {
        this.$configureFormValue.set(value);
    }

    onConfigureFormValid(valid: boolean): void {
        this.$configureFormValid.set(valid);
        // As soon as the form becomes valid, drop the "please complete required
        // fields" warning that a prior Send click may have surfaced.
        if (valid) {
            this.$validationWarningKey.set(null);
        }
    }

    /** Closes the dialog via the custom header's X button. */
    closeDialog(): void {
        this.#dialogRef?.close();
    }

    /**
     * Fans out one `POST /api/v1/publishing/push/{bundleId}` per checked bundle
     * with the same form payload. The user filled the form once; the parallel
     * calls are an implementation detail invisible to them.
     */
    onSend(): void {
        const ids = this.$checkedBundleIds();
        const value = this.$configureFormValue();
        // Send stays clickable even when the form is incomplete — clicking with
        // an invalid form surfaces an inline warning instead of doing nothing
        // silently, so the user gets clear feedback about what to fix.
        if (!value || !this.$configureFormValid()) {
            this.$validationWarningKey.set('publishing-queue.select-bundle.warning.form-invalid');
            return;
        }
        if (ids.length === 0) {
            this.$validationWarningKey.set('publishing-queue.select-bundle.warning.select-one');
            return;
        }

        const form = toPushBundleForm(value);

        this.$isSending.set(true);
        forkJoin(
            ids.map((id) =>
                this.#publishingService.pushBundle(id, form).pipe(
                    map((result) => ({ bundleId: id, ok: true as const, result })),
                    catchError((error: HttpErrorResponse) =>
                        of({ bundleId: id, ok: false as const, error })
                    )
                )
            )
        )
            .pipe(
                take(1),
                finalize(() => this.$isSending.set(false))
            )
            .subscribe((outcomes) => {
                const failed = outcomes.filter((o): o is Extract<typeof o, { ok: false }> => !o.ok);

                if (failed.length === 0) {
                    this.#dialogRef?.close();
                    return;
                }

                // Surface the first real error via the standard handler so the
                // user sees a concrete reason (auth, business rule, server) —
                // the count alone was previously the only signal.
                this.#httpErrorManager.handle(failed[0].error);

                // Drop the bundles that already succeeded from the checked set
                // so a follow-up Send can't re-push them (was previously how a
                // partial failure produced double pushes).
                const failedIds = new Set(failed.map((o) => o.bundleId));
                this.$checkedBundleIds.update((prev) => prev.filter((id) => failedIds.has(id)));

                this.#globalMessage.error(
                    this.#dotMessageService.get(
                        'publishing-queue.select-bundle.send-partial-fail',
                        String(failed.length),
                        String(ids.length)
                    )
                );
            });
    }

    #loadBundles(): void {
        if (!this.#userId) {
            return;
        }
        this.$bundlesStatus.set('loading');
        const start = (this.$bundlesPage() - 1) * BUNDLES_PER_PAGE;
        const search = this.$bundleSearch().trim();
        const filter = search ? `*${search}*` : '*';

        this.#publishingService
            .getUnsendBundles(this.#userId, filter, start, BUNDLES_PER_PAGE)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.$bundlesStatus.set('error');
                    return EMPTY;
                })
            )
            .subscribe((response) => {
                // Cursor-style pagination can't tell "full last page" apart from
                // "full non-last page" without asking the next one. If the user
                // clicked Next past the end (total was an exact multiple of
                // BUNDLES_PER_PAGE), the response comes back empty — roll back to
                // the previous page and disable Next, so the empty "No bundles
                // found" screen never renders. Only apply when past page 1;
                // page 1 empty is a legitimate empty-state.
                if (response.items.length === 0 && this.$bundlesPage() > 1) {
                    this.$bundlesPage.update((p) => p - 1);
                    this.$bundlesHasMore.set(false);
                    this.$bundlesStatus.set('loaded');
                    return;
                }
                this.$bundles.set(response.items.map((item) => ({ id: item.id, name: item.name })));
                // Cursor-style: a full page means "possibly more"; a partial page is the last.
                this.$bundlesHasMore.set(response.items.length === BUNDLES_PER_PAGE);
                this.$bundlesStatus.set('loaded');
                // Auto-select the first bundle on initial load so the right pane
                // isn't empty by default (matches the design's "first row active").
                if (!this.$activeBundleId() && response.items.length > 0) {
                    this.onSelectBundle({
                        id: response.items[0].id,
                        name: response.items[0].name
                    });
                }
            });
    }

    #loadAssets(bundleId: string): void {
        this.$assets.set([]);
        this.$assetsStatus.set('loading');
        this.$assetEditUrls.set(new Map());
        this.#publishingService
            .getBundleAssets(bundleId)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.$assetsStatus.set('error');
                    return EMPTY;
                }),
                finalize(() => {
                    if (this.$assetsStatus() === 'loading') {
                        this.$assetsStatus.set('loaded');
                    }
                })
            )
            .subscribe((assets) => {
                this.$assets.set(assets);
                this.$assetsStatus.set('loaded');
                this.#resolveAssetEditUrls(assets);
            });
    }

    /** Resolves the edit URL for each contentlet asset. Non-contentlet types
     * (templates, languages, containers, etc.) are skipped — the row renders as
     * plain text.
     *
     * Assets are grouped by content type so the underlying service is called
     * once per distinct content type instead of once per asset. The service
     * caches the editor flag per type, so the first call primes the cache; the
     * remaining calls in the same group hit the cache and emit synchronously.
     * A bundle with 200 contentlets of 4 types costs 4 HTTP requests, not 200.
     *
     * `baseType` is not available from `/api/bundle/{id}/assets`, so HTML pages
     * won't get the dedicated page editor route — they get the contentlet
     * editor URL instead (acceptable; the user can navigate to the visual
     * editor from there).
     */
    #resolveAssetEditUrls(assets: BundleAssetView[]): void {
        const groups = groupContentletAssetsByType(assets);

        for (const [contentType, group] of groups) {
            // Prime the per-type cache with the first asset. Subsequent calls
            // in the same group hit the cache and emit synchronously — no
            // additional HTTP. `forkJoin` waits for all group resolutions
            // before writing back atomically.
            const perAsset$ = group.map((asset) =>
                this.#editUrlService
                    .resolveEditUrl({ inode: asset.inode, contentType } as DotCMSContentlet)
                    .pipe(
                        take(1),
                        catchError(() => of('')),
                        map((url) => [asset.asset, url] as const)
                    )
            );

            forkJoin(perAsset$).subscribe((entries) => {
                this.$assetEditUrls.update((prev) => {
                    const next = new Map(prev);
                    for (const [key, url] of entries) {
                        if (url) {
                            next.set(key, url);
                        }
                    }
                    return next;
                });
            });
        }
    }

    /** Template helper. Returns the edit URL for an asset if it's a linkable type
     * and the URL has been resolved, otherwise `null` (render as plain text). */
    editUrlFor(asset: BundleAssetView): string | null {
        return this.$assetEditUrls().get(asset.asset) ?? null;
    }

    /** Row click handler for the asset table. Opens the asset's editor in a
     * new tab when an edit URL is resolved (matches the prior anchor's
     * `target="_blank"` so the dialog stays in context). No-op for assets
     * that don't have a resolved URL (e.g. non-contentlet types). */
    onSelectAssetRow(asset: BundleAssetView): void {
        const url = this.editUrlFor(asset);
        if (url) {
            window.open(url, '_blank', 'noopener');
        }
    }
}

/**
 * Translates the embedded form's `DotPushPublishData` (the same shape the
 * legacy AJAX dialog produces) into the v1 REST endpoint's `PushBundleForm`.
 *
 * Field map:
 *   pushActionSelected → operation        (same vocabulary, just renamed)
 *   environment[]      → environments[]   (renamed)
 *   filterKey          → filterKey
 *   publishDate + timezoneId → publishDate (ISO 8601 with TZ offset)
 *   expireDate  + timezoneId → expireDate  (same)
 *
 * Date conversion uses the user-selected `timezoneId` so the BE receives the
 * intended wall-clock time + offset (mirrors `PublishingJobsHelper#parseISO8601Date`
 * server-side).
 */
function toPushBundleForm(value: DotPushPublishData): PushBundleForm {
    const operation = value.pushActionSelected as PushBundleOperation;
    const form: PushBundleForm = {
        operation,
        environments: value.environment,
        filterKey: value.filterKey ?? ''
    };

    if (operation === 'publish' || operation === 'publishexpire') {
        if (value.publishDate) {
            form.publishDate = toIso8601WithOffset(new Date(value.publishDate), value.timezoneId);
        }
    }

    if (operation === 'expire' || operation === 'publishexpire') {
        if (value.expireDate) {
            form.expireDate = toIso8601WithOffset(new Date(value.expireDate), value.timezoneId);
        }
    }

    return form;
}

/**
 * Formats `date` as ISO 8601 with the timezone offset of `timezoneId`.
 *
 * The wall-clock parts (yyyy-MM-ddTHH:mm:ss) come from the browser's local time
 * — this matches what the user picked in the date picker, and what the legacy
 * AJAX path sends. The offset suffix is computed for the user-selected timezone
 * at that instant (handles DST transitions correctly via `Intl.DateTimeFormat`).
 */
function toIso8601WithOffset(date: Date, timezoneId: string): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    const yyyy = date.getFullYear();
    const mm = pad(date.getMonth() + 1);
    const dd = pad(date.getDate());
    const hh = pad(date.getHours());
    const mi = pad(date.getMinutes());
    const ss = pad(date.getSeconds());

    const offsetMinutes = getTimezoneOffsetMinutes(date, timezoneId);
    const sign = offsetMinutes >= 0 ? '+' : '-';
    const abs = Math.abs(offsetMinutes);
    const offHH = pad(Math.floor(abs / 60));
    const offMM = pad(abs % 60);

    return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}${sign}${offHH}:${offMM}`;
}

/** Returns minutes east of UTC for `timezoneId` at the instant `date` — e.g.
 * `-300` for America/New_York during EST, `+60` for Europe/Madrid in winter.
 * Diffs the wall-clock parts emitted by `Intl.DateTimeFormat` in UTC vs the
 * target zone; correct across DST. */
function getTimezoneOffsetMinutes(date: Date, timezoneId: string): number {
    const fields: Intl.DateTimeFormatOptions = {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    };
    const utcParts = partsToEpoch(
        new Intl.DateTimeFormat('en-US', { ...fields, timeZone: 'UTC' }).formatToParts(date)
    );
    const tzParts = partsToEpoch(
        new Intl.DateTimeFormat('en-US', { ...fields, timeZone: timezoneId }).formatToParts(date)
    );

    return Math.round((tzParts - utcParts) / 60000);
}

function partsToEpoch(parts: Intl.DateTimeFormatPart[]): number {
    const get = (type: string) => Number(parts.find((p) => p.type === type)?.value ?? 0);
    let hour = get('hour');
    if (hour === 24) hour = 0;
    return Date.UTC(get('year'), get('month') - 1, get('day'), hour, get('minute'), get('second'));
}

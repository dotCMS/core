import { EMPTY, Subject, forkJoin, of } from 'rxjs';

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

import { catchError, debounceTime, distinctUntilChanged, finalize, take } from 'rxjs/operators';

/* eslint-disable @nx/enforce-module-boundaries */
// Both `DotDownloadBundleDialogService` and `DotPushPublishFormComponent` live
// in apps/dotcms-ui (not yet promoted to shared libs). Same pattern as
// `dot-publishing-queue-table`. Tracked alongside the v1 consolidation work
// (#36048).

import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import {
    DotContentletEditUrlService,
    DotCurrentUserService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPublishingQueueService,
    DotPushPublishFiltersService
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
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

type LoadStatus = 'init' | 'loading' | 'loaded' | 'error';

interface BundleRow {
    id: string;
    name: string;
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

const BUNDLES_PER_PAGE = 10;
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
    standalone: true,
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
        DotMessagePipe,
        DotPushPublishFormComponent
    ],
    // `DotPushPublishFiltersService` is provided here (same as the legacy global
    // `DotPushPublishDialogComponent` does) so the embedded
    // `<dot-push-publish-form>` can inject it without leaking outside this dialog.
    providers: [ConfirmationService, DotPushPublishFiltersService],
    templateUrl: './dot-publishing-queue-select-bundle-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex h-full min-h-0 flex-col' }
})
export class DotPublishingQueueSelectBundleDialogComponent implements OnInit {
    private readonly publishingService = inject(DotPublishingQueueService);
    private readonly currentUserService = inject(DotCurrentUserService);
    private readonly httpErrorManager = inject(DotHttpErrorManagerService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);
    private readonly downloadService = inject(DotDownloadBundleDialogService);
    private readonly editUrlService = inject(DotContentletEditUrlService);
    private readonly globalMessage = inject(DotGlobalMessageService);
    private readonly dialogRef = inject(DynamicDialogRef, { optional: true });

    private userId: string | null = null;

    readonly bundles = signal<BundleRow[]>([]);
    readonly bundlesTotal = signal(0);
    readonly bundlesStatus = signal<LoadStatus>('init');
    readonly bundlesPage = signal(1);
    readonly bundleSearch = signal('');
    /** Multi-select for bulk operations (Remove). Independent of `activeBundleId`. */
    readonly checkedBundleIds = signal<string[]>([]);
    /** Single "active" bundle whose assets are shown on the right pane. */
    readonly activeBundleId = signal<string | null>(null);

    readonly assets = signal<BundleAssetView[]>([]);
    readonly assetsStatus = signal<LoadStatus>('init');
    readonly assetsPage = signal(1);
    /** Per-asset edit URLs resolved by `DotContentletEditUrlService` after each
     * asset load. Only contentlet rows get an entry — non-contentlet types are
     * rendered as plain text. Resolution is async (one metadata fetch per
     * content type, cached app-wide by the service). */
    readonly assetEditUrls = signal<Map<string, string>>(new Map());

    readonly bundlesPerPage = BUNDLES_PER_PAGE;
    readonly assetsPerPage = ASSETS_PER_PAGE;

    readonly bundlesSkeleton = Array.from({ length: 6 });
    readonly assetsSkeleton = Array.from({ length: 6 });

    /** `table-layout: fixed` + `width: 100%` so column widths are driven by the
     * `<col>`/header widths instead of by cell content. Without this, a long
     * bundle name or asset title would push the table past the pane width and
     * trigger horizontal scroll. */
    readonly tableStyleFixed = { 'table-layout': 'fixed' as const, width: '100%' };

    readonly activeBundle = computed(() => {
        const id = this.activeBundleId();
        return id ? (this.bundles().find((b) => b.id === id) ?? null) : null;
    });

    readonly hasChecked = computed(() => this.checkedBundleIds().length > 0);
    readonly hasActive = computed(() => this.activeBundleId() !== null);

    /** Two-step wizard inside this single modal: step 1 picks bundles, step 2
     * embeds the push-publish form and submits to /api/v1/publishing/push. */
    readonly step = signal<'select' | 'configure'>('select');
    readonly isSending = signal(false);

    /** Latest form value emitted by the embedded `<dot-push-publish-form>`. The
     * form re-emits on every keystroke; we just hold the most recent. */
    readonly configureFormValue = signal<DotPushPublishData | null>(null);
    readonly configureFormValid = signal(false);

    /** Send is enabled only when the form is valid AND we're not already
     * pushing. Disabled-while-sending prevents double-submit. */
    readonly canSend = computed(() => this.configureFormValid() && !this.isSending());

    /** Data fed to the embedded `<dot-push-publish-form>`. `assetIdentifier`
     * keys the env selector's "remember last push" — for multi-bundle we use
     * the first checked id (same form values get applied to all, so they share
     * the same "last push" memory anyway). */
    readonly configureFormData = computed<DotPushPublishDialogData>(() => {
        const ids = this.checkedBundleIds();
        const first = ids[0] ?? '';
        const firstName = this.bundles().find((b) => b.id === first)?.name ?? first;
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
    readonly checkedBundles = computed(() => {
        const ids = new Set(this.checkedBundleIds());
        return this.bundles().filter((b) => ids.has(b.id));
    });

    readonly pagedAssets = computed(() => {
        const all = this.assets();
        const page = this.assetsPage();
        const start = (page - 1) * ASSETS_PER_PAGE;
        return all.slice(start, start + ASSETS_PER_PAGE);
    });

    readonly assetsTotal = computed(() => this.assets().length);

    private readonly bundleSearchSubject = new Subject<string>();

    ngOnInit(): void {
        this.bundleSearchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => {
                this.bundleSearch.set(value);
                this.bundlesPage.set(1);
                this.loadBundles();
            });

        this.currentUserService
            .getCurrentUser()
            .pipe(take(1))
            .subscribe((user) => {
                this.userId = user.userId;
                this.loadBundles();
            });
    }

    onBundleSearch(value: string): void {
        this.bundleSearchSubject.next(value);
    }

    onBundlesPagePrev(): void {
        if (this.bundlesPage() > 1) {
            this.bundlesPage.update((p) => p - 1);
            this.loadBundles();
        }
    }

    onBundlesPageNext(): void {
        const maxPage = Math.max(1, Math.ceil(this.bundlesTotal() / BUNDLES_PER_PAGE));
        if (this.bundlesPage() < maxPage) {
            this.bundlesPage.update((p) => p + 1);
            this.loadBundles();
        }
    }

    onAssetsPagePrev(): void {
        if (this.assetsPage() > 1) {
            this.assetsPage.update((p) => p - 1);
        }
    }

    onAssetsPageNext(): void {
        const maxPage = Math.max(1, Math.ceil(this.assetsTotal() / ASSETS_PER_PAGE));
        if (this.assetsPage() < maxPage) {
            this.assetsPage.update((p) => p + 1);
        }
    }

    onSelectBundle(bundle: BundleRow): void {
        if (this.activeBundleId() === bundle.id) {
            return;
        }
        this.activeBundleId.set(bundle.id);
        this.assetsPage.set(1);
        this.loadAssets(bundle.id);
    }

    onCheckedChange(ids: BundleRow[]): void {
        this.checkedBundleIds.set(ids.map((b) => b.id));
    }

    typeIcon(type: string): string {
        return TYPE_ICONS[(type ?? '').toLowerCase()] ?? 'pi pi-file';
    }

    onRemoveAsset(asset: BundleAssetView): void {
        const bundleId = this.activeBundleId();
        if (!bundleId) {
            return;
        }
        this.confirmationService.confirm({
            header: this.dotMessageService.get('publishing-queue.asset-list.remove-confirm.header'),
            message: this.dotMessageService.get(
                'publishing-queue.asset-list.remove-confirm.message',
                asset.title || asset.asset
            ),
            acceptLabel: this.dotMessageService.get('publishing-queue.remove'),
            rejectLabel: this.dotMessageService.get('publishing-queue.cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => {
                this.publishingService
                    .removeAssetsFromBundle(bundleId, [asset.asset])
                    .pipe(
                        take(1),
                        catchError((error) => {
                            this.httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => this.loadAssets(bundleId));
            }
        });
    }

    onRemoveBundles(): void {
        const ids = this.checkedBundleIds();
        if (ids.length === 0) {
            return;
        }
        this.confirmationService.confirm({
            header: this.dotMessageService.get('publishing-queue.delete.confirm.header'),
            message: this.dotMessageService.get(
                'publishing-queue.select-bundle.remove.confirm.message',
                String(ids.length)
            ),
            acceptLabel: this.dotMessageService.get('publishing-queue.history.kebab.delete'),
            rejectLabel: this.dotMessageService.get('publishing-queue.cancel'),
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => {
                this.publishingService
                    .deleteBundles(ids)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            this.httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => {
                        this.checkedBundleIds.set([]);
                        // If the active bundle was deleted, clear the right pane.
                        if (this.activeBundleId() && ids.includes(this.activeBundleId() ?? '')) {
                            this.activeBundleId.set(null);
                            this.assets.set([]);
                        }
                        this.loadBundles();
                    });
            }
        });
    }

    /** Download is single-target by design — `DotDownloadBundleDialogService.open`
     * accepts one bundle id. The button is gated in the template to be enabled
     * only when exactly one bundle is checked. */
    onDownloadChecked(): void {
        const ids = this.checkedBundleIds();
        if (ids.length !== 1) {
            return;
        }
        this.downloadService.open(ids[0]);
    }

    onOpenConfigureStep(): void {
        if (!this.hasChecked()) {
            return;
        }
        this.step.set('configure');
    }

    onBackToList(): void {
        this.step.set('select');
    }

    onConfigureFormValue(value: DotPushPublishData): void {
        this.configureFormValue.set(value);
    }

    onConfigureFormValid(valid: boolean): void {
        this.configureFormValid.set(valid);
    }

    /**
     * Fans out one `POST /api/v1/publishing/push/{bundleId}` per checked bundle
     * with the same form payload. The user filled the form once; the parallel
     * calls are an implementation detail invisible to them.
     */
    onSend(): void {
        const ids = this.checkedBundleIds();
        const value = this.configureFormValue();
        if (!value || ids.length === 0 || !this.configureFormValid()) {
            return;
        }

        const form = toPushBundleForm(value);

        this.isSending.set(true);
        forkJoin(
            ids.map((id) =>
                this.publishingService
                    .pushBundle(id, form)
                    .pipe(catchError(() => of(null)))
            )
        )
            .pipe(
                take(1),
                finalize(() => this.isSending.set(false))
            )
            .subscribe((results) => {
                const failed = results.filter((r) => r === null).length;

                if (failed === 0) {
                    this.dialogRef?.close();
                    return;
                }

                this.globalMessage.error(
                    this.dotMessageService.get(
                        'publishing-queue.select-bundle.send-partial-fail',
                        String(failed),
                        String(ids.length)
                    )
                );
            });
    }

    private loadBundles(): void {
        if (!this.userId) {
            return;
        }
        this.bundlesStatus.set('loading');
        const start = (this.bundlesPage() - 1) * BUNDLES_PER_PAGE;
        const search = this.bundleSearch().trim();
        const filter = search ? `*${search}*` : '*';

        this.publishingService
            .getUnsendBundles(this.userId, filter, start, BUNDLES_PER_PAGE)
            .pipe(
                take(1),
                catchError((error) => {
                    this.httpErrorManager.handle(error);
                    this.bundlesStatus.set('error');
                    return EMPTY;
                })
            )
            .subscribe((response) => {
                this.bundles.set(response.items.map((item) => ({ id: item.id, name: item.name })));
                this.bundlesTotal.set(response.numRows ?? response.items.length);
                this.bundlesStatus.set('loaded');
                // Auto-select the first bundle on initial load so the right pane
                // isn't empty by default (matches the design's "first row active").
                if (!this.activeBundleId() && response.items.length > 0) {
                    this.onSelectBundle({
                        id: response.items[0].id,
                        name: response.items[0].name
                    });
                }
            });
    }

    private loadAssets(bundleId: string): void {
        this.assetsStatus.set('loading');
        this.assetEditUrls.set(new Map());
        this.publishingService
            .getBundleAssets(bundleId)
            .pipe(
                take(1),
                catchError((error) => {
                    this.httpErrorManager.handle(error);
                    this.assetsStatus.set('error');
                    return EMPTY;
                }),
                finalize(() => {
                    if (this.assetsStatus() === 'loading') {
                        this.assetsStatus.set('loaded');
                    }
                })
            )
            .subscribe((assets) => {
                this.assets.set(assets);
                this.assetsStatus.set('loaded');
                this.resolveAssetEditUrls(assets);
            });
    }

    /** Resolves the edit URL for each contentlet asset in parallel. Non-contentlet
     * types (templates, languages, containers, etc.) are skipped — the row renders
     * as plain text.
     *
     * The service caches by content type, so 50 contentlets of the same type
     * trigger one metadata fetch. `baseType` is not available from
     * `/api/bundle/{id}/assets`, so HTML pages won't get the dedicated page editor
     * route — they'll get the contentlet editor URL instead (acceptable; the
     * user can navigate to the visual editor from there).
     */
    private resolveAssetEditUrls(assets: BundleAssetView[]): void {
        const urls = new Map<string, string>();

        for (const asset of assets) {
            if (asset.type !== 'contentlet' || !asset.inode) {
                continue;
            }
            const partial = {
                inode: asset.inode,
                contentType: asset.content_type_name ?? ''
            } as DotCMSContentlet;

            this.editUrlService
                .resolveEditUrl(partial)
                .pipe(take(1))
                .subscribe((url) => {
                    urls.set(asset.asset, url);
                    // Re-emit to trigger CD — Map mutation alone won't notify signal consumers
                    this.assetEditUrls.set(new Map(urls));
                });
        }
    }

    /** Template helper. Returns the edit URL for an asset if it's a linkable type
     * and the URL has been resolved, otherwise `null` (render as plain text). */
    editUrlFor(asset: BundleAssetView): string | null {
        return this.assetEditUrls().get(asset.asset) ?? null;
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
            form.publishDate = toIso8601WithOffset(
                new Date(value.publishDate),
                value.timezoneId
            );
        }
    }

    if (operation === 'expire' || operation === 'publishexpire') {
        if (value.expireDate) {
            form.expireDate = toIso8601WithOffset(
                new Date(value.expireDate),
                value.timezoneId
            );
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

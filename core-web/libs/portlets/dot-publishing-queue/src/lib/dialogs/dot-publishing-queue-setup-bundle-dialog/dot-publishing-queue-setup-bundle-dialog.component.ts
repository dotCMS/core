import { EMPTY, forkJoin, of } from 'rxjs';

import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, finalize, map, take } from 'rxjs/operators';

/* eslint-disable @nx/enforce-module-boundaries */
// `DotPushPublishFormComponent` lives in apps/dotcms-ui (not yet promoted to
// shared libs). Same pattern as `dot-publishing-queue-table`. Tracked alongside
// the v1 consolidation work (#36048).

import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import {
    DotContentletEditUrlService,
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

/** `DynamicDialogConfig.data` the shell opens this dialog with. */
export interface DotSetupBundleDialogData {
    bundleId: string;
    bundleName: string;
}

/** Map asset `type` string (lowercase, comes from `PusheableAsset.getType()`)
 * to a Material Symbols Rounded ligature name. Unknown types fall back to a
 * generic file icon. Rendered via `<span class="material-symbols-rounded">`. */
const TYPE_ICONS: Record<string, string> = {
    contentlet: 'description',
    contenttype: 'inventory_2',
    template: 'open_in_new',
    containers: 'grid_view',
    folder: 'folder',
    host: 'public',
    category: 'sell',
    links: 'link',
    workflow: 'settings',
    language: 'translate',
    rule: 'shield',
    user: 'person',
    osgi: 'inventory_2',
    relationship: 'share',
    experiment: 'bar_chart',
    variant: 'file_copy'
};

const ASSETS_PER_PAGE = 10;

/**
 * Set Up dialog for one New (unsent) bundle, opened from the bundle's "Set up"
 * button or row in the bundles table.
 *
 * Three steps inside one modal: `items` lists the bundle's assets (remove an
 * asset, or Remove / Download / Configure the bundle), `configure` embeds the
 * push-publish form and submits to `POST /api/v1/publishing/push/{bundleId}`,
 * `download` embeds the download-bundle form and fires `/api/bundle/_generate`.
 *
 * Self-contained state — does NOT mutate the bundles table store. The shell
 * refreshes the table when the dialog closes.
 */
@Component({
    selector: 'dot-publishing-queue-setup-bundle-dialog',
    imports: [
        ButtonModule,
        ConfirmDialogModule,
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
    // `<dot-download-bundle-form>` inject the root-scoped singleton.
    providers: [ConfirmationService],
    templateUrl: './dot-publishing-queue-setup-bundle-dialog.component.html',
    host: { class: 'flex h-full min-h-0 flex-col' }
})
export class DotPublishingQueueSetupBundleDialogComponent implements OnInit {
    readonly #publishingService = inject(DotPublishingQueueService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #editUrlService = inject(DotContentletEditUrlService);
    readonly #dialogRef = inject(DynamicDialogRef, { optional: true });
    readonly #data = inject(DynamicDialogConfig).data as DotSetupBundleDialogData;

    readonly bundleId = this.#data.bundleId;
    readonly bundleName = this.#data.bundleName || this.#data.bundleId;

    readonly $assets = signal<BundleAssetView[]>([]);
    readonly $assetsStatus = signal<LoadStatus>('init');
    readonly $assetsPage = signal(1);
    /** Per-asset edit URLs resolved by `DotContentletEditUrlService` after each
     * asset load. Only contentlet rows get an entry — non-contentlet types are
     * rendered as plain text. Resolution is async (one metadata fetch per
     * content type, cached app-wide by the service). */
    readonly $assetEditUrls = signal<Map<string, string>>(new Map());

    readonly assetsPerPage = ASSETS_PER_PAGE;

    /** Stable per-row keys — PrimeNG's `dataKey` skips `undefined` slots. */
    readonly assetsSkeleton = Array.from({ length: 6 }, (_, i) => ({
        asset: `__skel_${i}`
    }));

    /** `table-layout: fixed` + `width: 100%` so column widths are driven by the
     * header widths instead of by cell content. Without this, a long asset title
     * would push the table past the dialog width and trigger horizontal scroll. */
    readonly tableStyleFixed = { 'table-layout': 'fixed' as const, width: '100%' };

    readonly $step = signal<'items' | 'configure' | 'download'>('items');
    readonly $isSending = signal(false);

    /** Latest form value emitted by the embedded `<dot-push-publish-form>`. The
     * form re-emits on every keystroke; we just hold the most recent. */
    readonly $configureFormValue = signal<DotPushPublishData | null>(null);
    readonly $configureFormValid = signal(false);

    /** Latest form value from the embedded `<dot-download-bundle-form>`. */
    readonly $downloadFormValue = signal<DotDownloadBundleFormValue | null>(null);
    readonly $downloadFormValid = signal(false);

    /** True while a `_generate` POST is in flight — disables the Download
     * button and swaps its label so the user can't double-click. */
    readonly $isDownloading = signal(false);

    readonly $canDownload = computed(() => this.$downloadFormValid() && !this.$isDownloading());

    /** Inline warning shown in the configure footer when Send is clicked with an
     * incomplete form. Stored as an i18n key, resolved at render time. */
    readonly $validationWarningKey = signal<string | null>(null);

    /** Data fed to the embedded `<dot-push-publish-form>`. `assetIdentifier`
     * keys the env selector's "remember last push". */
    readonly configureFormData: DotPushPublishDialogData = {
        assetIdentifier: this.bundleId,
        title: this.bundleName,
        isBundle: true
    };

    readonly $pagedAssets = computed(() => {
        const start = (this.$assetsPage() - 1) * ASSETS_PER_PAGE;
        return this.$assets().slice(start, start + ASSETS_PER_PAGE);
    });

    readonly $assetsTotal = computed(() => this.$assets().length);

    ngOnInit(): void {
        this.#loadAssets();
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

    typeIcon(type: string): string {
        return TYPE_ICONS[(type ?? '').toLowerCase()] ?? 'description';
    }

    /**
     * Asks for confirmation, then removes one asset from the bundle and reloads
     * the item list.
     *
     * @param asset the asset row whose delete button was clicked
     */
    onRemoveAsset(asset: BundleAssetView): void {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get(
                'publishing-queue.asset-list.remove-confirm.header'
            ),
            message: this.#dotMessageService.get(
                'publishing-queue.asset-list.remove-confirm.message',
                asset.title || asset.asset
            ),
            acceptLabel: this.#dotMessageService.get('publishing-queue.delete'),
            rejectLabel: this.#dotMessageService.get('publishing-queue.cancel'),
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => {
                this.#publishingService
                    .removeAssetsFromBundle(this.bundleId, [asset.asset])
                    .pipe(
                        take(1),
                        catchError((error) => {
                            this.#httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => this.#loadAssets());
            }
        });
    }

    /** Asks for confirmation, then deletes the whole bundle and closes the dialog. */
    onRemoveBundle(): void {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get('publishing-queue.delete.confirm.header'),
            message: this.#dotMessageService.get(
                'publishing-queue.delete.confirm.message',
                this.bundleName
            ),
            acceptLabel: this.#dotMessageService.get('publishing-queue.remove'),
            rejectLabel: this.#dotMessageService.get('publishing-queue.cancel'),
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => {
                this.#publishingService
                    .deleteBundle(this.bundleId)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            this.#httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => this.#dialogRef?.close());
            }
        });
    }

    onOpenDownloadStep(): void {
        this.$downloadFormValue.set(null);
        this.$downloadFormValid.set(false);
        this.$step.set('download');
    }

    onDownloadFormValue(value: DotDownloadBundleFormValue): void {
        this.$downloadFormValue.set(value);
    }

    onDownloadFormValid(valid: boolean): void {
        this.$downloadFormValid.set(valid);
    }

    /**
     * Submits the download-bundle form. Same endpoint, payload and blob-anchor
     * click as the legacy `DotDownloadBundleDialogComponent`, but through
     * `DotPublishingQueueService.generateBundle` so auth and error interceptors
     * apply.
     */
    onDownload(): void {
        const value = this.$downloadFormValue();
        if (!value || !this.$downloadFormValid() || this.$isDownloading()) {
            return;
        }

        this.$isDownloading.set(true);
        this.#publishingService
            .generateBundle(this.bundleId, value.operation, value.filterKey)
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
                this.onBackToItems();
            });
    }

    onOpenConfigureStep(): void {
        this.$validationWarningKey.set(null);
        this.$step.set('configure');
    }

    onBackToItems(): void {
        this.$step.set('items');
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
     * Pushes the bundle with the configured form and closes the dialog on
     * success. Send stays clickable with an incomplete form so the click can
     * explain what is missing instead of doing nothing.
     */
    onSend(): void {
        const value = this.$configureFormValue();
        if (!value || !this.$configureFormValid()) {
            this.$validationWarningKey.set('publishing-queue.setup-bundle.warning.form-invalid');
            return;
        }

        this.$isSending.set(true);
        this.#publishingService
            .pushBundle(this.bundleId, toPushBundleForm(value))
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    return EMPTY;
                }),
                finalize(() => this.$isSending.set(false))
            )
            .subscribe(() => this.#dialogRef?.close());
    }

    #loadAssets(): void {
        this.$assets.set([]);
        this.$assetsStatus.set('loading');
        this.$assetEditUrls.set(new Map());
        this.#publishingService
            .getBundleAssets(this.bundleId)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.$assetsStatus.set('error');
                    return EMPTY;
                })
            )
            .subscribe((assets) => {
                this.$assets.set(assets);
                // Removing the last asset of the last page would leave an empty page.
                const maxPage = Math.max(1, Math.ceil(assets.length / ASSETS_PER_PAGE));
                if (this.$assetsPage() > maxPage) {
                    this.$assetsPage.set(maxPage);
                }
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
     *
     * `baseType` is not available from `/api/bundle/{id}/assets`, so HTML pages
     * get the contentlet editor URL instead of the page editor route.
     */
    #resolveAssetEditUrls(assets: BundleAssetView[]): void {
        const groups = groupContentletAssetsByType(assets);

        for (const [contentType, group] of groups) {
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

    /** Row click handler for the asset table. Opens the asset's editor in a new
     * tab when an edit URL is resolved, so the dialog stays in context. No-op
     * for assets without one (e.g. non-contentlet types). */
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

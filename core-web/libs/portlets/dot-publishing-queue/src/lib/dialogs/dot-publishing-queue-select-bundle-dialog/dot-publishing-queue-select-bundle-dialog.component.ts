import { EMPTY, Subject } from 'rxjs';

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
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, debounceTime, distinctUntilChanged, finalize, take } from 'rxjs/operators';

/* eslint-disable @nx/enforce-module-boundaries */
// `DotDownloadBundleDialogService` lives in apps/dotcms-ui (not yet promoted to
// a shared lib). Same pattern as `dot-publishing-queue-table`. Tracked
// alongside the v1 consolidation work (#36048).

import {
    DotContentletEditUrlService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPublishingQueueService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { BundleAssetView, DotCMSContentlet } from '@dotcms/dotcms-models';
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
        DotMessagePipe
    ],
    providers: [ConfirmationService],
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
    private readonly pushPublishService = inject(DotPushPublishDialogService);
    private readonly downloadService = inject(DotDownloadBundleDialogService);
    private readonly editUrlService = inject(DotContentletEditUrlService);

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

    onDownloadActive(): void {
        const id = this.activeBundleId();
        if (id) {
            this.downloadService.open(id);
        }
    }

    onConfigureActive(): void {
        const bundle = this.activeBundle();
        if (!bundle) {
            return;
        }
        this.pushPublishService.open({
            assetIdentifier: bundle.id,
            title: bundle.name || bundle.id,
            isBundle: true
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

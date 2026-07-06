import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';

/** Custom header rendered inside PrimeNG's native `.p-dialog-header` via
 * `DynamicDialogConfig.templates.header`. PrimeNG provides the padding, border,
 * and close button; we only contribute the title + items-count pill. */
@Component({
    selector: 'dot-publishing-queue-asset-list-dialog-header',
    imports: [TagModule],
    template: `
        <div class="flex min-w-0 items-center gap-3">
            <h2
                class="m-0 truncate text-xl leading-tight font-bold"
                [title]="bundleName"
                data-testid="pq-asset-list-title">
                {{ displayTitle }}
            </h2>
            <p-tag
                severity="secondary"
                [rounded]="true"
                [value]="itemsCountLabel()"
                styleClass="text-xs font-medium px-2 py-0.5"
                data-testid="pq-asset-list-count" />
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueAssetListDialogHeaderComponent {
    readonly #store = inject(DotPublishingQueueStore);
    readonly #dialogConfig = inject(DynamicDialogConfig);
    readonly #dotMessageService = inject(DotMessageService);

    readonly bundleName = (this.#dialogConfig.data?.bundleName ?? null) as string | null;

    /** Truncate long bundle names so the header stays in one tidy line. The full
     * name is exposed via the `title` attribute for hover discovery. */
    readonly displayTitle = ((): string => {
        const name = this.bundleName;
        if (!name) {
            return this.#dotMessageService.get('publishing-queue.asset-list.title');
        }
        return name.length > 30 ? `${name.slice(0, 30)}…` : name;
    })();

    /** "N items" / "1 item" — singular vs plural so the pill reads naturally. */
    readonly itemsCountLabel = computed(() => {
        const count = this.#store.selectedAssets().length;
        const key =
            count === 1
                ? 'publishing-queue.asset-list.items-count.singular'
                : 'publishing-queue.asset-list.items-count.plural';

        return this.#dotMessageService.get(key, String(count));
    });
}

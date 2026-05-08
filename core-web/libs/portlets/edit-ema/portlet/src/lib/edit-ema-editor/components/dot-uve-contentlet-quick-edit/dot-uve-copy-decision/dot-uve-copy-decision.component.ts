import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    effect,
    inject,
    input,
    output,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DotCopyContentService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotColorIconComponent, DotMessagePipe } from '@dotcms/ui';

import { ContentletPayload } from '../../../../shared/models';
import { UVEStore } from '../../../../store/dot-uve.store';
import { ContentletEditData, CopyMode } from '../types';

/**
 * Asks the user "edit this contentlet on every page where it appears,
 * or only this one?" before opening the side-panel form.
 *
 * Self-contained: holds its own decision state, does its own
 * `copyInPage` call when "this page only" is chosen, then triggers a
 * `pageReload` so the editor's page asset reflects the new contentlet
 * inode. On "all pages" it just emits `decisionMade` and the parent
 * can render the form against the original contentlet.
 *
 * Extracted from `DotUveContentletQuickEditComponent` so the form
 * component stays focused on form lifecycle.
 */
@Component({
    selector: 'dot-uve-copy-decision',
    standalone: true,
    imports: [DotColorIconComponent, DotMessagePipe],
    templateUrl: './dot-uve-copy-decision.component.html',
    host: { class: 'flex flex-1 min-h-0' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveCopyDecisionComponent {
    readonly #uveStore = inject(UVEStore);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dotCopyContentService = inject(DotCopyContentService);
    readonly #dotHttpErrorManagerService = inject(DotHttpErrorManagerService);

    /** Contentlet under decision. */
    readonly data = input.required<ContentletEditData>();

    /**
     * Emitted once the user chooses ALL_PAGES. The THIS_PAGE branch
     * resolves through `pageReload` instead — the page asset updates
     * with the new (forked) contentlet's inode and the parent's data
     * input changes naturally, removing the need for an explicit
     * "this-page decision was made" signal.
     */
    readonly decisionMade = output<void>();

    readonly #selectedCopyMode = signal<CopyMode | null>(null);
    readonly #isCopying = signal(false);
    readonly #lastResetIdentifier = signal<string | undefined>(undefined);

    protected readonly $selectedCopyMode = this.#selectedCopyMode.asReadonly();
    protected readonly $isCopying = this.#isCopying.asReadonly();
    protected readonly CopyMode = CopyMode;

    /** Reset selection when the user navigates to a different contentlet. */
    protected readonly $resetCopyDecisionEffect = effect(() => {
        const identifier = this.data().contentlet?.identifier;
        untracked(() => {
            if (identifier !== undefined && identifier !== this.#lastResetIdentifier()) {
                this.#lastResetIdentifier.set(identifier);
                this.#selectedCopyMode.set(null);
            }
        });
    });

    protected selectCopyMode(mode: CopyMode): void {
        if (this.#isCopying()) {
            return;
        }
        this.#selectedCopyMode.set(mode);
        this.#confirm();
    }

    #confirm(): void {
        const mode = this.#selectedCopyMode();
        if (!mode) {
            return;
        }

        if (mode === CopyMode.ALL_PAGES) {
            this.decisionMade.emit();
            return;
        }

        this.#isCopying.set(true);
        const { container, contentlet } = this.data();
        const treeNode = this.#uveStore.getCurrentTreeNode(container, contentlet);

        this.#dotCopyContentService
            .copyInPage(treeNode)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (copiedContentlet) => {
                    const newContentletPayload: ContentletPayload = {
                        ...contentlet,
                        identifier: copiedContentlet.identifier,
                        inode: copiedContentlet.inode,
                        title: copiedContentlet.title,
                        contentType: copiedContentlet.contentType,
                        onNumberOfPages: 1
                    };

                    const activeContentlet = this.#uveStore.getPageSavePayload({
                        container,
                        contentlet: newContentletPayload
                    });

                    // Patch payload only — bounds are preserved (the
                    // forked contentlet sits in the same on-screen
                    // position). pageReload will re-emit SET_BOUNDS via
                    // auto-bounds and re-anchor against the new inode.
                    this.#uveStore.setSelectedPayload(activeContentlet);
                    this.#uveStore.pageReload();
                    this.#isCopying.set(false);
                },
                error: (error) => {
                    this.#dotHttpErrorManagerService.handle(error).subscribe();
                    this.#isCopying.set(false);
                }
            });
    }
}

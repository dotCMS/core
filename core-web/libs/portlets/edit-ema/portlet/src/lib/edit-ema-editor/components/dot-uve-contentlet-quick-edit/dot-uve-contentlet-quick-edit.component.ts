import { injectDispatch } from '@ngrx/signals/events';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe, DotSpinnerComponent } from '@dotcms/ui';
import { TEMP_EMPTY_CONTENTLET_TYPE } from '@dotcms/uve/internal';

import { DotUveCopyDecisionComponent } from './dot-uve-copy-decision/dot-uve-copy-decision.component';
import { DotUveQuickEditFormComponent } from './dot-uve-quick-edit-form/dot-uve-quick-edit-form.component';
import { ContentletField } from './types';

import { UVEStore } from '../../../store/dot-uve.store';
import { ContentletEditData } from '../../../store/features/editor/models';
import { getQuickEditFields, parseFieldValues } from '../../utils';
import { uveContentEditorEvents } from '../dot-uve-content-editor/dot-uve-content-editor.events';

type QuickEditMode =
    | 'decide'
    | 'loading'
    | 'form'
    | 'empty'
    | 'no-fields'
    | 'no-permission'
    | 'no-selection';

/**
 * Side-panel quick-edit container. Owns mode resolution and
 * content-type loading; delegates the actual form lifecycle to
 * `<dot-uve-quick-edit-form>` and the multi-page copy prompt to
 * `<dot-uve-copy-decision>`.
 *
 * The component is intentionally thin: a single `$mode` computed
 * determines which child to render, and the template is a `@switch`
 * over that mode. Each non-trivial UI state lives in its own child
 * component.
 */
@Component({
    selector: 'dot-uve-contentlet-quick-edit',
    standalone: true,
    imports: [
        ButtonModule,
        DotMessagePipe,
        DotSpinnerComponent,
        DotUveCopyDecisionComponent,
        DotUveQuickEditFormComponent
    ],
    templateUrl: './dot-uve-contentlet-quick-edit.component.html',
    host: { class: 'flex flex-col h-full' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveContentletQuickEditComponent {
    readonly #uveStore = inject(UVEStore);
    readonly #contentEditor = injectDispatch(uveContentEditorEvents);

    readonly data = input.required<ContentletEditData>();
    readonly loading = input<boolean>(false);

    readonly #decisionMade = signal(false);
    readonly #lastIdentifierForDecision = signal<string | undefined>(undefined);

    readonly $isEmptyContainer = computed(
        () => this.data().contentlet?.contentType === TEMP_EMPTY_CONTENTLET_TYPE
    );

    /**
     * Whether the user may edit the selected contentlet. Read from the DOM
     * payload's `canEdit`, which the store's `$editorContentletEditData` carries across the
     * page-asset swap. Fail-open: absent means allowed, since headless pages
     * never supply it.
     */
    readonly $canEditContentlet = computed(() => this.data().contentlet?.canEdit !== false);

    readonly $isLoadingContentType = computed(() => {
        const contentType = this.data().contentlet?.contentType;
        return (
            !!contentType &&
            contentType !== TEMP_EMPTY_CONTENTLET_TYPE &&
            !this.#uveStore.contentTypeCache()[contentType]
        );
    });

    readonly $fields = computed((): ContentletField[] => {
        const contentType = this.data().contentlet?.contentType;
        if (!contentType) {
            return [];
        }

        const cached = this.#uveStore.contentTypeCache()[contentType];
        if (!cached?.layout) {
            return [];
        }

        return getQuickEditFields(cached.layout).map((field) => ({
            ...field,
            options: parseFieldValues(field.values)
        }));
    });

    readonly $needsCopyDecision = computed(
        () => !this.#decisionMade() && Number(this.data().contentlet?.onNumberOfPages ?? 1) > 1
    );

    /**
     * Pick which UI to render. Order matters — earlier branches win
     * when multiple conditions could be true (e.g. a multi-page
     * contentlet whose content type is still loading still shows the
     * copy-decision UI first; we don't show the form until they decide).
     */
    readonly $mode = computed<QuickEditMode>(() => {
        if (this.$isEmptyContainer()) return 'empty';
        // Ranked above 'decide' on purpose: a user who cannot edit the original
        // must not be offered the copy-and-edit prompt from here either.
        if (!this.$canEditContentlet()) return 'no-permission';
        if (this.$needsCopyDecision()) return 'decide';
        if (this.$isLoadingContentType()) return 'loading';
        if (this.$fields().length > 0) return 'form';
        if (this.data().contentlet?.identifier) return 'no-fields';
        return 'no-selection';
    });

    /** Trigger a content-type cache load whenever the contentType changes. */
    protected readonly $loadContentTypeEffect = effect(() => {
        const contentType = this.data().contentlet?.contentType;
        if (contentType && contentType !== TEMP_EMPTY_CONTENTLET_TYPE) {
            untracked(() => this.#uveStore.loadContentType(contentType));
        }
    });

    /** Reset the decision flag when the user moves to a different contentlet. */
    protected readonly $resetDecisionEffect = effect(() => {
        const identifier = this.data().contentlet?.identifier;
        untracked(() => {
            if (identifier !== undefined && identifier !== this.#lastIdentifierForDecision()) {
                this.#lastIdentifierForDecision.set(identifier);
                this.#decisionMade.set(false);
            }
        });
    });

    /** Wired to the copy-decision component's `decisionMade` output. */
    protected handleDecisionMade(): void {
        this.#decisionMade.set(true);
    }

    /**
     * Wired to the form's `openFullEditor` output and the no-fields button. Opens the full editor
     * on the contentlet the panel shows, which comes from the page asset, so it is the latest
     * saved version even after a save changed its inode. No container goes along: this panel has
     * its own copy prompt.
     */
    protected openFullEditor(): void {
        const { contentlet } = this.data();

        if (!contentlet?.inode) {
            return;
        }

        this.#contentEditor.uveEditContentRequested({ contentlet });
    }

    /** Wired to the form component's `closed` output (Cancel button). */
    protected closePanel(): void {
        this.#uveStore.resetSelected();
        this.#uveStore.setEditPanelOpen(false);
    }
}

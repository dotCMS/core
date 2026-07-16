import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    output,
    signal,
    untracked
} from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe, DotSpinnerComponent } from '@dotcms/ui';
import { TEMP_EMPTY_CONTENTLET_TYPE } from '@dotcms/uve/internal';

import { DotUveCopyDecisionComponent } from './dot-uve-copy-decision/dot-uve-copy-decision.component';
import { DotUveQuickEditFormComponent } from './dot-uve-quick-edit-form/dot-uve-quick-edit-form.component';
import { ContentletEditData, ContentletField } from './types';

import { UVEStore } from '../../../store/dot-uve.store';
import { getQuickEditFields, parseFieldValues } from '../../utils';

type QuickEditMode = 'decide' | 'loading' | 'form' | 'empty' | 'no-fields' | 'no-selection';

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

    readonly data = input.required<ContentletEditData>();
    readonly loading = input<boolean>(false);

    readonly openFullEditor = output<void>();

    readonly #decisionMade = signal(false);
    readonly #lastIdentifierForDecision = signal<string | undefined>(undefined);

    readonly $isEmptyContainer = computed(
        () => this.data().contentlet?.contentType === TEMP_EMPTY_CONTENTLET_TYPE
    );

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

    /** Wired to the form component's `closed` output (Cancel button). */
    protected closePanel(): void {
        this.#uveStore.resetSelected();
        this.#uveStore.setEditPanelOpen(false);
    }
}

import { NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ContentChild,
    TemplateRef,
    computed,
    inject,
    input,
    linkedSignal
} from '@angular/core';

import { AccordionModule } from 'primeng/accordion';

import { DotLocalstorageService } from '@dotcms/data-access';

/**
 * Prefix used to persist the collapsed state of each section in localstorage.
 */
const SECTION_STORAGE_PREFIX = 'dot-edit-content.section.';

/**
 *  Component that renders a section with a title and an optional action template.
 *
 *  When a `key` is provided the section can be collapsed/expanded by clicking its
 *  header, and the collapsed state is persisted in localstorage under
 *  `dot-edit-content.section.<key>`. When no `key` is provided the section stays
 *  expanded and no storage writes happen (backward-compatible behaviour).
 *
 *  Internally this wraps a single-panel `p-accordion`: PrimeNG owns the slide motion,
 *  the click/keyboard (Enter/Space) handling and the `aria-expanded` wiring on the
 *  header; this component only adds the localstorage persistence on top of it.
 */
@Component({
    selector: 'dot-edit-content-sidebar-section',
    imports: [NgTemplateOutlet, AccordionModule],
    templateUrl: './dot-edit-content-sidebar-section.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex flex-col'
    }
})
export class DotEditContentSidebarSectionComponent {
    readonly #dotLocalstorageService = inject(DotLocalstorageService);

    /**
     * The title of the section.
     */
    $title = input<string | null>(null, { alias: 'title' });

    /**
     * Unique key used to persist the collapsed state. When empty the section is
     * not collapsible-persistent and stays expanded with no storage writes. Also
     * doubles as the single panel's `value` inside the internal `p-accordion`.
     */
    key = input<string>('');

    /**
     * The internal `p-accordion`'s value: the section's own `key` while expanded,
     * `undefined` while collapsed — the shape a single-panel (non-`multiple`)
     * PrimeNG accordion expects.
     *
     * Initialised reactively once the `key` input is bound: when a key is present
     * it seeds from localstorage (default expanded when absent), otherwise it
     * stays expanded in-memory.
     *
     * NOTE: the only reactive dependency is `key()`. `getItem` is a plain synchronous read,
     * so the stored value is re-seeded only when the key changes — not on every parent
     * change-detection cycle. If DotLocalstorageService ever becomes signal-backed, this would
     * snap back to the stored state on each read and the user's in-session toggle would be lost.
     */
    $accordionValue = linkedSignal<string | undefined>(() => {
        const key = this.key();
        const collapsed = key
            ? !!this.#dotLocalstorageService.getItem<boolean>(SECTION_STORAGE_PREFIX + key)
            : false;

        return collapsed ? undefined : key;
    });

    /**
     * Whether the section is currently collapsed, derived from `$accordionValue` for
     * callers that only care about the boolean state (template bindings, tests).
     */
    $collapsed = computed<boolean>(() => this.$accordionValue() !== this.key());

    /**
     * The action template for the section.
     */
    @ContentChild('sectionAction')
    actionTemplate: TemplateRef<unknown>;

    /**
     * Handles the accordion opening/closing its single panel: mirrors the new value into
     * `$accordionValue` and, when a `key` is present, persists the resulting collapsed
     * state to localstorage.
     *
     * @param value - The accordion's new value: the panel's `key` once it opens, or
     * anything else (PrimeNG sends `undefined`) once it closes.
     */
    onValueChange(value: unknown): void {
        const key = this.key();
        const opened = value === key;

        this.$accordionValue.set(opened ? key : undefined);

        if (key) {
            this.#dotLocalstorageService.setItem<boolean>(SECTION_STORAGE_PREFIX + key, !opened);
        }
    }
}

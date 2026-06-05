import { NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ContentChild,
    TemplateRef,
    inject,
    input,
    linkedSignal
} from '@angular/core';

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
 */
@Component({
    selector: 'dot-edit-content-sidebar-section',
    imports: [NgTemplateOutlet],
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
     * not collapsible-persistent and stays expanded with no storage writes.
     */
    key = input<string>('');

    /**
     * Writable signal holding the collapsed state of the section.
     *
     * Initialised reactively once the `key` input is bound: when a key is present
     * it seeds from localstorage (default expanded when absent), otherwise it
     * stays expanded in-memory.
     */
    $collapsed = linkedSignal<boolean>(() => {
        const key = this.key();

        return key
            ? !!this.#dotLocalstorageService.getItem<boolean>(SECTION_STORAGE_PREFIX + key)
            : false;
    });

    /**
     * The action template for the section.
     */
    @ContentChild('sectionAction')
    actionTemplate: TemplateRef<unknown>;

    /**
     * Toggles the collapsed state of the section. When a `key` is present the new
     * state is persisted to localstorage.
     */
    toggle(): void {
        const collapsed = !this.$collapsed();
        this.$collapsed.set(collapsed);

        const key = this.key();
        if (key) {
            this.#dotLocalstorageService.setItem<boolean>(SECTION_STORAGE_PREFIX + key, collapsed);
        }
    }
}

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ToggleSwitchModule } from 'primeng/toggleswitch';

import {
    DotMessagePipe,
    SHARED_ASSETS_DISABLED_VALUE,
    SHARED_ASSETS_ENABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY
} from '@dotcms/ui';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

/**
 * Says what the listing is showing, and carries the one control that changes it.
 *
 * Only all site content gets a bar. The other two scopes answer the question it asks by being
 * chosen: the site root is this site's root and nothing else, and System Host is shared content and
 * nothing else — so there is nothing left for a sentence to qualify or a toggle to decide.
 *
 * The toggle replaced the `Show System Host` chip that used to sit in the filter row. Same filter,
 * same values; what changes is that the control now sits beside the sentence describing its effect,
 * rather than in a row of chips that answer a different kind of question.
 */
@Component({
    selector: 'dot-content-drive-scope-bar',
    imports: [DotMessagePipe, ToggleSwitchModule, FormsModule],
    templateUrl: './dot-content-drive-scope-bar.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block' },
    styles: [
        `
            /* A "small" size input on p-toggleswitch does nothing here: the dotCMS Lara preset
               defines no small variant, so no p-toggleswitch-sm class is emitted and no
               --p-toggleswitch-sm-* token exists to pick up. The size is set through the tokens the
               theme DOES define, scoped to this bar so no other switch in the app changes.

               Two thirds of the default (3rem / 1.75rem / 1.25rem): this control annotates a
               sentence rather than sitting in a form, so it should read as part of the line. */
            :host {
                --p-toggleswitch-width: 2rem;
                --p-toggleswitch-height: 1.125rem;
                --p-toggleswitch-handle-size: 0.75rem;
            }
        `
    ]
})
export class DotContentDriveScopeBarComponent {
    readonly #store = inject(DotContentDriveStore);

    /**
     * Whether shared content is in the listing.
     *
     * "Off only when explicitly off": the endpoint's own form defaults the flag to true and an
     * absent key reads as on, so anything other than the disabled value means included.
     */
    protected readonly $includesSystemHost = computed(
        () => this.#store.getFilterValue(SHARED_ASSETS_FILTER_KEY) !== SHARED_ASSETS_DISABLED_VALUE
    );

    /** The sentence, which has to agree with the toggle rather than describe a fixed scope. */
    protected readonly $summaryKey = computed(() =>
        this.$includesSystemHost()
            ? 'content-drive.scope-bar.all-site-content.included'
            : 'content-drive.scope-bar.all-site-content.excluded'
    );

    /**
     * Writes the state either way rather than clearing the key, so the applied filter is spelled
     * out in the URL instead of being implied by an absence that happens to read as on.
     */
    protected onToggle(): void {
        this.#store.patchFilters({
            [SHARED_ASSETS_FILTER_KEY]: this.$includesSystemHost()
                ? SHARED_ASSETS_DISABLED_VALUE
                : SHARED_ASSETS_ENABLED_VALUE
        });
    }
}

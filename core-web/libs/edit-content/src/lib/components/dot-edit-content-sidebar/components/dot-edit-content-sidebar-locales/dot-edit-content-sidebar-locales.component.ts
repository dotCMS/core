import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    output,
    signal
} from '@angular/core';

import { ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotIsoCodePipe } from '@dotcms/ui';

enum LOCALE_STATUS {
    BASE = 'p-chip-sm',
    DEFAULT = ' default',
    CURRENT = ' p-chip-filled',
    TRANSLATED = ' p-chip-primary',
    UNTRANSLATED = ' p-chip-gray p-chip-dashed'
}

/**
 * The maximum number of locales to display without truncation.
 */
const MAX_LOCALES = 9;

/**
 * Component representing the locales section in the edit content sidebar.
 *
 * This component displays the available locales and the default locale for the content being edited.
 */
@Component({
    selector: 'dot-edit-content-sidebar-locales',
    imports: [ChipModule, SkeletonModule, DotIsoCodePipe],
    templateUrl: './dot-edit-content-sidebar-locales.component.html',
    styleUrl: './dot-edit-content-sidebar-locales.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarLocalesComponent {
    /**
     * The list of available locales.
     */
    $locales = input.required<DotLanguage[]>({ alias: 'locales' });

    /**
     * The default locale.
     */
    $defaultLocale = input.required<DotLanguage>({ alias: 'defaultLocale' });

    /**
     * The current locale.
     */
    $currentLocale = input.required<DotLanguage>({ alias: 'currentLocale' });

    /**
     * Whether the data is loading.
     */
    $isLoading = input.required<boolean>({ alias: 'isLoading' });

    /**
     * Event emitted when the locale is switched.
     */
    switchLocale = output<DotLanguage>();

    readonly #dotMessageService = inject(DotMessageService);

    $maxLocaleChips = signal(MAX_LOCALES);
    $showAll = signal(false);

    /**
     * Computes the label for the button based on show all or not and specify  the number of locales.
     *
     * @returns {string | null} The label for the button, or null if no label is needed.
     */
    $btnLabel = computed(() => {
        const size = this.$locales().length;
        const max = this.$maxLocaleChips();

        if (this.$showAll()) {
            return this.#dotMessageService.get('edit.content.sidebar.locales.show.less');
        }

        if (size > max) {
            return this.#dotMessageService.get(
                'edit.content.sidebar.locales.show.more',
                `${size - max}`
            );
        }

        return null;
    });

    /**
     * Computes the list of locales to show based on show all or not.
     *
     * @returns {DotLanguage[]} The list of locales to display.
     */
    $localesToShow = computed(() => {
        const locales = this.$locales();
        if (this.$showAll()) {
            return locales;
        }

        return locales?.slice(0, this.$maxLocaleChips());
    });

    /**
     * Determines the appropriate style class for a given locale.
     *
     * @param {DotLanguage} locale - The locale object containing its id and translation status.
     * @returns {string} The computed style class based on the locale's properties.
     */
    getStyleClass({ id, translated }: DotLanguage): string {
        let styleClass: string = LOCALE_STATUS.BASE;

        if (id === this.$currentLocale().id) {
            styleClass += LOCALE_STATUS.CURRENT;
        } else if (translated) {
            styleClass += LOCALE_STATUS.TRANSLATED;
        } else {
            styleClass += LOCALE_STATUS.UNTRANSLATED;
        }

        if (id === this.$defaultLocale().id) {
            styleClass += LOCALE_STATUS.DEFAULT;
        }

        return styleClass;
    }

    toggleShowAll(): void {
        this.$showAll.update((showAll) => !showAll);
    }
}

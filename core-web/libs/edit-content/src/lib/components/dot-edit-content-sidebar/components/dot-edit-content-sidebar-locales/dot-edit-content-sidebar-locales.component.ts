import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';

import { DotLanguage } from '@dotcms/dotcms-models';

enum LOCALE_STATUS {
    BASE = 'p-chip-sm',
    DEFAULT = ' default',
    CURRENT = ' p-chip-filled',
    TRANSLATED = ' p-chip-primary',
    UNTRANSLATED = ' p-chip-gray p-chip-dashed'
}

/**
 * Component representing the locales section in the edit content sidebar.
 *
 * This component displays the available locales and the default locale for the content being edited.
 */
@Component({
    selector: 'dot-edit-content-sidebar-locales',
    standalone: true,
    imports: [CommonModule, ChipModule, SkeletonModule],
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
}

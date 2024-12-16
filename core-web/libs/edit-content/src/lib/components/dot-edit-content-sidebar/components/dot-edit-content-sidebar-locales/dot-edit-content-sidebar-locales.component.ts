import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';

import { DotLanguage } from '@dotcms/dotcms-models';

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
}

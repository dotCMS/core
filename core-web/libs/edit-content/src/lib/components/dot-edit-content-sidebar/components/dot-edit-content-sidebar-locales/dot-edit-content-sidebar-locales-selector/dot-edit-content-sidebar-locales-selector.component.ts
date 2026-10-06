import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    output,
    signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { ListboxModule } from 'primeng/listbox';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';

import { DotCurrentUserService, DotRouterService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotIsoCodePipe, DotMessagePipe } from '@dotcms/ui';

import { LocaleTab } from '../../../../../models/dot-edit-content.model';

type TabDef = Readonly<{ value: LocaleTab; label: string }>;

const SIMPLE_VIEW_MAX_LOCALES = 5;

@Component({
    selector: 'dot-edit-content-sidebar-locales-selector',
    imports: [
        FormsModule,
        SkeletonModule,
        ButtonModule,
        ChipModule,
        InputTextModule,
        SelectButtonModule,
        IconFieldModule,
        InputIconModule,
        ListboxModule,
        DotIsoCodePipe,
        DotMessagePipe,
        BadgeModule,
        TagModule
    ],
    templateUrl: './dot-edit-content-sidebar-locales-selector.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarLocalesSelectorComponent {
    // Nullable: the sidebar renders this component while the locales are still loading, and in
    // that state the store has nothing to give. The `@if ($isLoading())` branch of the template
    // renders a skeleton and reads none of them, so absent data never reaches the views below.
    readonly $locales = input.required<DotLanguage[] | null>({ alias: 'locales' });
    readonly $defaultLocale = input.required<DotLanguage | null>({ alias: 'defaultLocale' });
    readonly $currentLocale = input.required<DotLanguage | null>({ alias: 'currentLocale' });
    readonly $isLoading = input.required<boolean>({ alias: 'isLoading' });
    readonly $activeTab = input<LocaleTab>('all', { alias: 'activeTab' });

    readonly switchLocale = output<DotLanguage>();
    readonly tabChange = output<LocaleTab>();

    readonly #currentUserService = inject(DotCurrentUserService);
    readonly #dotRouterService = inject(DotRouterService);

    readonly $canManageLocales = toSignal(this.#currentUserService.isPortletInMenu('locales'), {
        initialValue: false
    });

    readonly $searchQuery = signal('');

    /** `$locales` before it has loaded is an empty list for every derivation below. */
    readonly $localeList = computed(() => this.$locales() ?? []);

    readonly $translatedLocales = computed(() => this.$localeList().filter((l) => l.translated));
    readonly $pendingLocales = computed(() => this.$localeList().filter((l) => !l.translated));

    readonly $showEnhancedView = computed(
        () => this.$localeList().length > SIMPLE_VIEW_MAX_LOCALES
    );

    readonly $filteredLocales = computed(() => {
        const tab = this.$activeTab();
        const query = this.$searchQuery().toLowerCase().trim();

        const base =
            tab === 'translated'
                ? this.$translatedLocales()
                : tab === 'pending'
                  ? this.$pendingLocales()
                  : this.$localeList();

        if (!query) return base;

        return base.filter(
            (l) =>
                l.language?.toLowerCase().includes(query) ||
                l.isoCode?.toLowerCase().includes(query)
        );
    });

    readonly $tabCounts = computed(() => ({
        all: this.$localeList().length,
        translated: this.$translatedLocales().length,
        pending: this.$pendingLocales().length
    }));

    /**
     * Count for a tab. PrimeNG's `let-item` template context is untyped, so the tab key arrives
     * as `any`; taking it as a `LocaleTab` restores the check at the one place it is lost.
     */
    protected tabCount(tab: LocaleTab): number {
        return this.$tabCounts()[tab];
    }

    readonly tabDefs: TabDef[] = [
        { value: 'all', label: 'edit.content.sidebar.locales.selector.tab.all' },
        { value: 'translated', label: 'edit.content.sidebar.locales.selector.translated' },
        { value: 'pending', label: 'edit.content.sidebar.locales.selector.pending' }
    ];

    setTab(tab: LocaleTab): void {
        this.tabChange.emit(tab);
    }

    onSearchChange(event: Event): void {
        this.$searchQuery.set((event.target as HTMLInputElement).value);
    }

    clearSearch(): void {
        this.$searchQuery.set('');
    }

    goToManageLocales(): void {
        this.#dotRouterService.gotoPortlet('/locales');
    }
}

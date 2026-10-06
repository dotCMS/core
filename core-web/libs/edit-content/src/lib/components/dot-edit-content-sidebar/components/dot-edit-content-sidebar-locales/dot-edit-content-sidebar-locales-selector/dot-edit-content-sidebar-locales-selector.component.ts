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
    readonly $locales = input.required<DotLanguage[]>({ alias: 'locales' });
    readonly $defaultLocale = input.required<DotLanguage>({ alias: 'defaultLocale' });
    readonly $currentLocale = input.required<DotLanguage>({ alias: 'currentLocale' });
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

    readonly $translatedLocales = computed(() => this.$locales().filter((l) => l.translated));
    readonly $pendingLocales = computed(() => this.$locales().filter((l) => !l.translated));

    readonly $showEnhancedView = computed(() => this.$locales().length > SIMPLE_VIEW_MAX_LOCALES);

    readonly $filteredLocales = computed(() => {
        const tab = this.$activeTab();
        const query = this.$searchQuery().toLowerCase().trim();

        const base =
            tab === 'translated'
                ? this.$translatedLocales()
                : tab === 'pending'
                  ? this.$pendingLocales()
                  : this.$locales();

        if (!query) return base;

        return base.filter(
            (l) =>
                l.language?.toLowerCase().includes(query) ||
                l.isoCode?.toLowerCase().includes(query)
        );
    });

    readonly $tabCounts = computed(() => ({
        all: this.$locales().length,
        translated: this.$translatedLocales().length,
        pending: this.$pendingLocales().length
    }));

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

import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';

import { DotCurrentUserService, DotMessageService, DotRouterService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotIsoCodePipe, DotMessagePipe } from '@dotcms/ui';
import { createFakeLanguage, MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentSidebarLocalesSelectorComponent } from './dot-edit-content-sidebar-locales-selector.component';

const MOCK_MESSAGES = {
    'edit.content.sidebar.locales.selector.translated': 'Translated',
    'edit.content.sidebar.locales.selector.pending': 'Pending',
    'edit.content.sidebar.locales.selector.default': 'DEFAULT',
    'edit.content.sidebar.locales.selector.tab.all': 'All',
    'edit.content.sidebar.locales.selector.search': 'Search locales',
    'edit.content.sidebar.locales.selector.selected': 'Selected:',
    'edit.content.sidebar.locales.selector.empty': 'All caught up',
    'edit.content.sidebar.locales.selector.manage': 'Manage locales'
};

const SIMPLE_LOCALES: DotLanguage[] = [
    createFakeLanguage({ id: 1, language: 'English', isoCode: 'en-us', translated: true }),
    createFakeLanguage({ id: 2, language: 'Spanish', isoCode: 'es-es', translated: true }),
    createFakeLanguage({ id: 3, language: 'French', isoCode: 'fr-fr', translated: true }),
    createFakeLanguage({ id: 4, language: 'German', isoCode: 'de-de', translated: false }),
    createFakeLanguage({ id: 5, language: 'Portuguese', isoCode: 'pt-br', translated: false })
];

const ENHANCED_LOCALES: DotLanguage[] = [
    ...SIMPLE_LOCALES,
    createFakeLanguage({ id: 6, language: 'Italian', isoCode: 'it-it', translated: false })
];

const DEFAULT_LOCALE = SIMPLE_LOCALES[0];
const CURRENT_LOCALE = SIMPLE_LOCALES[0];

function makeFactory(canManage = false) {
    return createComponentFactory({
        component: DotEditContentSidebarLocalesSelectorComponent,
        imports: [
            SkeletonModule,
            ButtonModule,
            ChipModule,
            InputTextModule,
            DotIsoCodePipe,
            DotMessagePipe
        ],
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(MOCK_MESSAGES) },
            mockProvider(DotCurrentUserService, {
                isPortletInMenu: jest.fn().mockReturnValue(of(canManage))
            }),
            mockProvider(DotRouterService, { gotoPortlet: jest.fn() })
        ]
    });
}

function typeInSearch(
    spectator: Spectator<DotEditContentSidebarLocalesSelectorComponent>,
    value: string
) {
    const input = spectator.query<HTMLInputElement>(byTestId('search-input'));
    input.value = value;
    spectator.dispatchFakeEvent(input, 'input');
    spectator.detectChanges();
}

describe('DotEditContentSidebarLocalesSelectorComponent', () => {
    let spectator: Spectator<DotEditContentSidebarLocalesSelectorComponent>;

    describe('Loading state', () => {
        const createComponent = makeFactory();

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    locales: SIMPLE_LOCALES,
                    defaultLocale: DEFAULT_LOCALE,
                    currentLocale: CURRENT_LOCALE,
                    isLoading: true
                } as unknown
            });
        });

        it('should show skeleton', () => {
            expect(spectator.query('p-skeleton')).toBeTruthy();
        });

        it('should not show locale list', () => {
            expect(spectator.query(byTestId('locale-item'))).toBeNull();
        });
    });

    describe('Simple view (≤5 locales)', () => {
        const createComponent = makeFactory();

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    locales: SIMPLE_LOCALES,
                    defaultLocale: DEFAULT_LOCALE,
                    currentLocale: CURRENT_LOCALE,
                    isLoading: false
                } as unknown
            });
        });

        it('should not render the tab bar', () => {
            expect(spectator.query(byTestId('locale-tabs'))).toBeNull();
        });

        it('should show translated locales under the "Translated" header', () => {
            const items = spectator.queryAll(byTestId('locale-item'));
            const translated = SIMPLE_LOCALES.filter((l) => l.translated);

            expect(items.length).toBe(SIMPLE_LOCALES.length);
            expect(items[0].textContent).toContain('English');
            expect(items[1].textContent).toContain('Spanish');
            expect(items.length - translated.length).toBe(
                SIMPLE_LOCALES.filter((l) => !l.translated).length
            );
        });

        it('should show pending locales under the "Pending" header', () => {
            const pending = SIMPLE_LOCALES.filter((l) => !l.translated);
            const allItems = spectator.queryAll(byTestId('locale-item'));
            const pendingItems = allItems.slice(3);

            expect(pendingItems.length).toBe(pending.length);
            expect(pendingItems[0].textContent).toContain('German');
            expect(pendingItems[1].textContent).toContain('Portuguese');
        });

        it('should show the DEFAULT badge for the default locale', () => {
            const badge = spectator.query(byTestId('default-badge'));
            expect(badge).toBeTruthy();
            expect(badge.textContent.trim()).toBe('DEFAULT');
        });

        it('should emit switchLocale when clicking a non-current locale', () => {
            const spy = jest.spyOn(spectator.component.switchLocale, 'emit');
            const items = spectator.queryAll(byTestId('locale-item'));
            spectator.click(items[1]);
            expect(spy).toHaveBeenCalledWith(SIMPLE_LOCALES[1]);
        });

        it('should not emit switchLocale when clicking the current locale', () => {
            const spy = jest.spyOn(spectator.component.switchLocale, 'emit');
            const items = spectator.queryAll(byTestId('locale-item'));
            spectator.click(items[0]);
            expect(spy).not.toHaveBeenCalled();
        });
    });

    describe('Enhanced view (>5 locales)', () => {
        const createComponent = makeFactory();

        beforeEach(() => {
            spectator = createComponent({
                props: {
                    locales: ENHANCED_LOCALES,
                    defaultLocale: DEFAULT_LOCALE,
                    currentLocale: CURRENT_LOCALE,
                    isLoading: false
                } as unknown
            });
        });

        it('should render the tab bar with All, Translated and Pending tabs', () => {
            expect(spectator.query(byTestId('tab-all'))).toBeTruthy();
            expect(spectator.query(byTestId('tab-translated'))).toBeTruthy();
            expect(spectator.query(byTestId('tab-pending'))).toBeTruthy();
        });

        it('should default to the All tab', () => {
            expect(spectator.queryAll(byTestId('locale-item')).length).toBe(
                ENHANCED_LOCALES.length
            );
        });

        it('should emit tabChange when clicking the Translated tab', () => {
            const spy = jest.spyOn(spectator.component.tabChange, 'emit');
            spectator.click(byTestId('tab-translated'));
            expect(spy).toHaveBeenCalledWith('translated');
        });

        it('should emit tabChange when clicking the Pending tab', () => {
            const spy = jest.spyOn(spectator.component.tabChange, 'emit');
            spectator.click(byTestId('tab-pending'));
            expect(spy).toHaveBeenCalledWith('pending');
        });

        it('should emit tabChange with "all" when setTab is called', () => {
            const spy = jest.spyOn(spectator.component.tabChange, 'emit');
            spectator.component.setTab('all');
            expect(spy).toHaveBeenCalledWith('all');
        });

        it('should reflect activeTab input value in filtered locales', () => {
            spectator.setInput('activeTab', 'translated');
            spectator.detectChanges();
            expect(spectator.queryAll(byTestId('locale-item')).length).toBe(
                ENHANCED_LOCALES.filter((l) => l.translated).length
            );
        });

        it('should show correct counts in each tab', () => {
            const translated = ENHANCED_LOCALES.filter((l) => l.translated).length;
            const pending = ENHANCED_LOCALES.filter((l) => !l.translated).length;

            expect(spectator.query(byTestId('tab-all')).textContent).toContain(
                `${ENHANCED_LOCALES.length}`
            );
            expect(spectator.query(byTestId('tab-translated')).textContent).toContain(
                `${translated}`
            );
            expect(spectator.query(byTestId('tab-pending')).textContent).toContain(`${pending}`);
        });

        it('should filter to translated locales when activeTab input is "translated"', () => {
            spectator.setInput('activeTab', 'translated');
            spectator.detectChanges();

            const items = spectator.queryAll(byTestId('locale-item'));
            expect(items.length).toBe(ENHANCED_LOCALES.filter((l) => l.translated).length);
        });

        it('should filter to pending locales when activeTab input is "pending"', () => {
            spectator.setInput('activeTab', 'pending');
            spectator.detectChanges();

            const items = spectator.queryAll(byTestId('locale-item'));
            expect(items.length).toBe(ENHANCED_LOCALES.filter((l) => !l.translated).length);
        });

        it('should always show the sticky selected row regardless of active tab', () => {
            expect(spectator.query(byTestId('selected-locale'))).toBeTruthy();

            spectator.setInput('activeTab', 'translated');
            spectator.detectChanges();
            expect(spectator.query(byTestId('selected-locale'))).toBeTruthy();

            spectator.setInput('activeTab', 'pending');
            spectator.detectChanges();
            expect(spectator.query(byTestId('selected-locale'))).toBeTruthy();
        });

        it('should show current locale info in the sticky row', () => {
            const row = spectator.query(byTestId('selected-locale'));
            expect(row.textContent).toContain(CURRENT_LOCALE.language);
        });

        it('should show DEFAULT badge in sticky row when current locale is default', () => {
            const badge = spectator
                .query(byTestId('selected-locale'))
                ?.querySelector('[data-testid="default-badge"]');
            expect(badge).toBeTruthy();
        });

        it('should emit switchLocale when clicking a locale item', () => {
            const spy = jest.spyOn(spectator.component.switchLocale, 'emit');
            const items = spectator.queryAll(byTestId('locale-item'));
            spectator.click(items[1]);
            expect(spy).toHaveBeenCalledWith(ENHANCED_LOCALES[1]);
        });

        it('should not emit switchLocale when clicking the current locale', () => {
            const spy = jest.spyOn(spectator.component.switchLocale, 'emit');
            const items = spectator.queryAll(byTestId('locale-item'));
            spectator.click(items[0]);
            expect(spy).not.toHaveBeenCalled();
        });

        describe('Search', () => {
            it('should show matching locales when typing in the search input', () => {
                typeInSearch(spectator, 'english');
                expect(spectator.queryAll(byTestId('locale-item')).length).toBe(1);
                expect(spectator.query(byTestId('locale-item')).textContent).toContain('English');
            });

            it('should filter by isoCode', () => {
                typeInSearch(spectator, 'de-de');
                expect(spectator.queryAll(byTestId('locale-item')).length).toBe(1);
                expect(spectator.query(byTestId('locale-item')).textContent).toContain('German');
            });

            it('should be case-insensitive', () => {
                typeInSearch(spectator, 'SPANISH');
                expect(spectator.queryAll(byTestId('locale-item')).length).toBe(1);
            });

            it('should show "All caught up" when no locales match', () => {
                typeInSearch(spectator, 'xyznotfound');
                expect(spectator.query(byTestId('no-results'))).toBeTruthy();
                expect(spectator.query(byTestId('no-results')).textContent.trim()).toBe(
                    'All caught up'
                );
            });

            it('should preserve search query when switching tabs', () => {
                typeInSearch(spectator, 'english');
                spectator.setInput('activeTab', 'translated');
                spectator.detectChanges();
                expect(spectator.query<HTMLInputElement>(byTestId('search-input')).value).toBe(
                    'english'
                );
            });

            it('should show the clear (X) button when search has content', () => {
                expect(spectator.query(byTestId('search-clear'))).toBeNull();
                typeInSearch(spectator, 'english');
                expect(spectator.query(byTestId('search-clear'))).toBeTruthy();
            });

            it('should clear search when clicking the X button', () => {
                typeInSearch(spectator, 'english');
                spectator.click(byTestId('search-clear'));
                spectator.detectChanges();
                expect(spectator.query<HTMLInputElement>(byTestId('search-input')).value).toBe('');
                expect(spectator.query(byTestId('search-clear'))).toBeNull();
            });

            it('should restore the full list after clearing search', () => {
                typeInSearch(spectator, 'english');
                spectator.click(byTestId('search-clear'));
                spectator.detectChanges();
                expect(spectator.queryAll(byTestId('locale-item')).length).toBe(
                    ENHANCED_LOCALES.length
                );
            });
        });
    });

    describe('Manage locales button', () => {
        describe('when user has no access', () => {
            const createComponent = makeFactory(false);

            beforeEach(() => {
                spectator = createComponent({
                    props: {
                        locales: SIMPLE_LOCALES,
                        defaultLocale: DEFAULT_LOCALE,
                        currentLocale: CURRENT_LOCALE,
                        isLoading: false
                    } as unknown
                });
            });

            it('should not show the manage locales button', () => {
                expect(spectator.query(byTestId('manage-locales-link'))).toBeNull();
            });
        });

        describe('when user has access', () => {
            const createComponent = makeFactory(true);

            beforeEach(() => {
                spectator = createComponent({
                    props: {
                        locales: SIMPLE_LOCALES,
                        defaultLocale: DEFAULT_LOCALE,
                        currentLocale: CURRENT_LOCALE,
                        isLoading: false
                    } as unknown
                });
            });

            it('should show the manage locales button', () => {
                expect(spectator.query(byTestId('manage-locales-link'))).toBeTruthy();
            });

            it('should call gotoPortlet when clicking manage locales', () => {
                const router = spectator.inject(DotRouterService);
                const btn = spectator
                    .query(byTestId('manage-locales-link'))
                    ?.querySelector('button');
                spectator.click(btn);
                expect(router.gotoPortlet).toHaveBeenCalledWith('/locales');
            });
        });
    });
});

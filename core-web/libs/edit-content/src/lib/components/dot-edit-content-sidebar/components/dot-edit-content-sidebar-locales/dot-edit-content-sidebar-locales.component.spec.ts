import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { Chip, ChipModule } from 'primeng/chip';

import { DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

import { DotEditContentSidebarLocalesComponent } from './dot-edit-content-sidebar-locales.component';

describe('DotEditContentSidebarLocalesComponent', () => {
    let spectator: Spectator<DotEditContentSidebarLocalesComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarLocalesComponent,
        imports: [ChipModule],
        providers: [mockProvider(DotMessageService)]
    });

    const locales: DotLanguage[] = [
        {
            id: 1,
            isoCode: 'en-us',
            translated: true
        },
        {
            id: 2,
            isoCode: 'es-es',
            translated: true
        },
        {
            id: 3,
            isoCode: 'it-it',
            translated: false
        },
        {
            id: 4,
            isoCode: 'it-it',
            translated: false
        },
        {
            id: 5,
            isoCode: 'qwert',
            translated: false
        },
        {
            id: 6,
            isoCode: 'it-es',
            translated: false
        },
        {
            id: 7,
            isoCode: 'en-pa',
            translated: false
        },
        {
            id: 8,
            isoCode: 'es-co',
            translated: false
        },
        {
            id: 9,
            isoCode: 'it-es',
            translated: false
        },
        {
            id: 10,
            isoCode: 'en-co',
            translated: false
        },
        {
            id: 11,
            isoCode: 'en-br',
            translated: false
        }
    ] as DotLanguage[];
    const defaultLocale: DotLanguage = locales[0];

    beforeEach(() => {
        spectator = createComponent({
            props: {
                locales: locales,
                defaultLocale: defaultLocale,
                currentLocale: defaultLocale,
                isLoading: false
            } as unknown
        });
        spectator.detectChanges();
    });

    it('should display the list of locales', () => {
        const chipElements = spectator.queryAll(Chip);
        expect(chipElements.length).toBe(10); // 9 + 1 of the show more.

        expect(chipElements[0].label).toBe('en-US');
        expect(chipElements[1].label).toBe('es-ES');
        expect(chipElements[2].label).toBe('it-IT');

        expect(chipElements[0].styleClass).toBe('p-chip-sm p-chip-filled default');
        expect(chipElements[1].styleClass).toBe('p-chip-sm p-chip-primary');
        expect(chipElements[2].styleClass).toBe('p-chip-sm p-chip-gray p-chip-dashed');
    });

    it('should show all and hide all locales when the show more button is clicked', () => {
        const showMoreButton = spectator.query(byTestId('show-more-button'));
        let chipElements = spectator.queryAll(Chip);

        spectator.click(showMoreButton);
        spectator.detectChanges();
        chipElements = spectator.queryAll(Chip);

        expect(chipElements.length).toBe(12); // 11 + 1 of the show more.

        spectator.click(showMoreButton);
        spectator.detectChanges();

        chipElements = spectator.queryAll(Chip);

        spectator.component.$showAll.set(false);
        spectator.detectChanges();

        expect(chipElements.length).toBe(10); // 9 + 1 of the show less.
    });

    it('should show the skeleton on loading', () => {
        spectator.setInput('isLoading', true);
        spectator.detectChanges();

        const skeleton = spectator.queryAll('p-skeleton');
        expect(skeleton).toExist();
    });

    it('should emit switchLocale event when a locale chip is clicked', () => {
        const chipElement = spectator.query('p-chip');
        const switchLocaleSpy = jest.spyOn(spectator.component.switchLocale, 'emit');

        spectator.click(chipElement);

        expect(switchLocaleSpy).not.toHaveBeenCalled();
    });

    it('should not emit switchLocale event when the current locale chip is clicked', () => {
        const chipElements = spectator.queryAll('p-chip');
        const switchLocaleSpy = jest.spyOn(spectator.component.switchLocale, 'emit');

        spectator.click(chipElements[1]);

        expect(switchLocaleSpy).toHaveBeenCalledWith(locales[1]);
    });
});

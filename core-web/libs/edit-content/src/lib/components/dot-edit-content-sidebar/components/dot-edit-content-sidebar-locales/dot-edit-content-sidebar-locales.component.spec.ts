import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ButtonModule } from 'primeng/button';
import { OverlayBadgeModule } from 'primeng/overlaybadge';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotIsoCodePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentSidebarLocalesComponent } from './dot-edit-content-sidebar-locales.component';

const messageServiceMock = new MockDotMessageService({
    'edit.content.sidebar.locales.show.more': 'Show more',
    'edit.content.sidebar.locales.show.less': 'Show less'
});

describe('DotEditContentSidebarLocalesComponent', () => {
    let spectator: Spectator<DotEditContentSidebarLocalesComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarLocalesComponent,
        imports: [ButtonModule, OverlayBadgeModule, SkeletonModule, DotIsoCodePipe],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
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

    const getLocaleButtons = () => spectator.queryAll('p-button');
    const clickButton = (buttonEl: Element) => {
        const innerButton = buttonEl?.querySelector('button') as HTMLButtonElement;
        if (innerButton) {
            spectator.click(innerButton);
        } else {
            spectator.click(buttonEl);
        }
    };

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
        const localeButtons = getLocaleButtons();
        // 9 locales + 1 show more button
        expect(localeButtons.length).toBe(10);

        expect(localeButtons[0].querySelector('.p-button-label')?.textContent?.trim()).toBe(
            'en-US'
        );
        expect(localeButtons[1].querySelector('.p-button-label')?.textContent?.trim()).toBe(
            'es-ES'
        );
        expect(localeButtons[2].querySelector('.p-button-label')?.textContent?.trim()).toBe(
            'it-IT'
        );
    });

    it('should show all and hide all locales when the show more button is clicked', () => {
        const showMoreButton = spectator.query(byTestId('show-more-button'));
        let localeButtons = getLocaleButtons();

        clickButton(showMoreButton);
        spectator.detectChanges();
        localeButtons = getLocaleButtons();

        // 11 locales + 1 show less button
        expect(localeButtons.length).toBe(12);

        clickButton(showMoreButton);
        spectator.detectChanges();
        localeButtons = getLocaleButtons();

        // Back to 9 locales + 1 show more button
        expect(localeButtons.length).toBe(10);
    });

    it('should show the skeleton on loading', () => {
        spectator.setInput('isLoading', true);
        spectator.detectChanges();

        const skeleton = spectator.query('p-skeleton');
        expect(skeleton).toBeTruthy();
    });

    it('should emit switchLocale event when a non-current locale button is clicked', () => {
        const switchLocaleSpy = jest.spyOn(spectator.component.switchLocale, 'emit');
        const localeButtons = getLocaleButtons();
        // Index 1 is es-ES (not current locale which is en-US at index 0)
        const secondLocaleButton = localeButtons[1];

        clickButton(secondLocaleButton);

        expect(switchLocaleSpy).toHaveBeenCalledWith(locales[1]);
    });

    it('should not emit switchLocale event when the current locale button is clicked', () => {
        const switchLocaleSpy = jest.spyOn(spectator.component.switchLocale, 'emit');
        const localeButtons = getLocaleButtons();
        // Index 0 is current locale (en-US)
        const currentLocaleButton = localeButtons[0];

        clickButton(currentLocaleButton);

        expect(switchLocaleSpy).not.toHaveBeenCalled();
    });
});

import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationBrandingComponent } from './dot-configuration-branding.component';

import { DotConfigurationColorFieldComponent } from '../../components/dot-configuration-color-field/dot-configuration-color-field.component';
import { DotConfigurationStore } from '../../store/dot-configuration.store';
import { createConfigurationStoreSignals } from '../../testing/configuration-store.stub';

const createStoreStub = () => ({ ...createConfigurationStoreSignals(), patchBranding: vi.fn() });

describe('DotConfigurationBrandingComponent', () => {
    let spectator: Spectator<DotConfigurationBrandingComponent>;
    let store: ReturnType<typeof createStoreStub>;

    const createComponent = createComponentFactory({
        component: DotConfigurationBrandingComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'configuration.branding.background.none': 'None',
                    'configuration.branding.navbar-logo.empty': 'No logo selected',
                    'configuration.validation.required': 'This field is required.'
                })
            }
        ],
        detectChanges: false
    });

    const button = (testId: string) =>
        spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement;

    const render = (isEnterprise = true) => {
        spectator.setInput('isEnterprise', isEnterprise);
        spectator.detectChanges();
    };

    const patchDraftBranding = (patch: Record<string, string>) => {
        const draft = store.draft();

        if (draft) {
            store.draft.set({ ...draft, branding: { ...draft.branding, ...patch } });
        }
    };

    beforeEach(() => {
        store = createStoreStub();
        spectator = createComponent({
            providers: [{ provide: DotConfigurationStore, useValue: store }]
        });
    });

    describe('colors', () => {
        it('writes a new primary color to the store', () => {
            render();
            const [primary] = spectator.queryAll(DotConfigurationColorFieldComponent);

            primary.valueChange.emit('#111111');

            expect(store.patchBranding).toHaveBeenCalledWith({ primaryColor: '#111111' });
        });

        it('writes a new secondary color to the store', () => {
            render();
            const [, secondary] = spectator.queryAll(DotConfigurationColorFieldComponent);

            secondary.valueChange.emit('#222222');

            expect(store.patchBranding).toHaveBeenCalledWith({ secondaryColor: '#222222' });
        });

        it('passes the validation message to the color field', () => {
            store.errors.set({ primaryColor: 'configuration.validation.color' });
            render();
            const [primary] = spectator.queryAll(DotConfigurationColorFieldComponent);

            expect(primary.errorKey()).toBe('configuration.validation.color');
        });
    });

    describe('login background', () => {
        it('previews the background and shows its file name', () => {
            render();

            expect(spectator.query(byTestId('configuration-background-preview'))).toExist();
            expect(spectator.query(byTestId('configuration-background-file'))).toHaveText('bg.jpg');
        });

        it('quotes the preview URL so file names with spaces still render', () => {
            patchDraftBranding({ backgroundImage: '/dA/bg-id/asset/my bg (1).jpg' });
            render();

            expect(
                spectator.query<HTMLElement>(byTestId('configuration-background-preview'))?.style
                    .backgroundImage
            ).toBe('url("/dA/bg-id/asset/my bg (1).jpg")');
        });

        it('says None when no background is set', () => {
            patchDraftBranding({ backgroundImage: '' });
            render();

            expect(spectator.query(byTestId('configuration-background-none'))).toExist();
            expect(spectator.query(byTestId('configuration-background-file'))).toHaveText('None');
        });

        it('asks the page to open the background picker', () => {
            render();
            const spy = vi.spyOn(spectator.component.changeBackground, 'emit');

            spectator.click(button('configuration-background-change-btn'));

            expect(spy).toHaveBeenCalled();
        });
    });

    describe('login screen logo', () => {
        it('previews the logo and shows its file name', () => {
            render();

            expect(spectator.query(byTestId('configuration-login-logo-preview'))).toHaveAttribute(
                'src',
                '/dA/logo-id/asset/logo.svg'
            );
            expect(spectator.query(byTestId('configuration-login-logo-file'))).toHaveText(
                'logo.svg'
            );
        });

        it('shows the validation message', () => {
            store.errors.set({ loginScreenLogo: 'configuration.validation.required' });
            render();

            expect(spectator.query(byTestId('configuration-login-logo-error'))).toHaveText(
                'This field is required.'
            );
        });

        it('asks the page to open the logo picker', () => {
            render();
            const spy = vi.spyOn(spectator.component.changeLoginLogo, 'emit');

            spectator.click(button('configuration-login-logo-change-btn'));

            expect(spy).toHaveBeenCalled();
        });
    });

    describe('navbar logo override', () => {
        it('is hidden on a Community license', () => {
            render(false);

            expect(spectator.query(byTestId('configuration-navbar-logo'))).not.toExist();
        });

        it('starts unticked when no navbar logo is set', () => {
            render();

            expect(spectator.query(byTestId('configuration-navbar-logo-tile'))).not.toExist();
        });

        it('starts ticked and previews the logo when one is set', () => {
            patchDraftBranding({ navBarLogo: '/dA/nav-id/asset/nav.png' });
            render();

            expect(spectator.query(byTestId('configuration-navbar-logo-preview'))).toExist();
            expect(spectator.query(byTestId('configuration-navbar-logo-file'))).toHaveText(
                'nav.png'
            );
        });

        it('reveals the logo picker when ticked', () => {
            render();

            spectator.triggerEventHandler('p-checkbox', 'ngModelChange', true);
            spectator.detectChanges();

            expect(spectator.query(byTestId('configuration-navbar-logo-file'))).toHaveText(
                'No logo selected'
            );
        });

        it('clears the navbar logo when unticked', () => {
            patchDraftBranding({ navBarLogo: '/dA/nav-id/asset/nav.png' });
            render();

            spectator.triggerEventHandler('p-checkbox', 'ngModelChange', false);

            expect(store.patchBranding).toHaveBeenCalledWith({ navBarLogo: '' });
        });

        it('asks the page to open the navbar logo picker', () => {
            patchDraftBranding({ navBarLogo: '/dA/nav-id/asset/nav.png' });
            render();
            const spy = vi.spyOn(spectator.component.changeNavBarLogo, 'emit');

            spectator.click(button('configuration-navbar-logo-change-btn'));

            expect(spy).toHaveBeenCalled();
        });
    });

    it('locks the change buttons while saving', () => {
        store.saving.set(true);
        render();

        expect(button('configuration-background-change-btn')).toBeDisabled();
        expect(button('configuration-login-logo-change-btn')).toBeDisabled();
    });
});

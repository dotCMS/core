import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    ALTERNATIVE_LICENSING_URL,
    DotConfigurationLicenseDialogComponent,
    LICENSE_SOURCE_URL
} from './dot-configuration-license-dialog.component';

describe('DotConfigurationLicenseDialogComponent', () => {
    let spectator: Spectator<DotConfigurationLicenseDialogComponent>;

    const createComponent = createComponentFactory({
        component: DotConfigurationLicenseDialogComponent,
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            mockProvider(DynamicDialogRef, { close: vi.fn() })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('links to the license text, opening in a new tab', () => {
        const link = spectator.query(byTestId('configuration-license-source-link'));

        expect(link).toHaveAttribute('href', LICENSE_SOURCE_URL);
        expect(link).toHaveAttribute('target', '_blank');
        expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('links to the alternative licensing arrangements', () => {
        expect(
            spectator.query(byTestId('configuration-license-alternatives-link'))
        ).toHaveAttribute('href', ALTERNATIVE_LICENSING_URL);
    });

    it('closes on Close', () => {
        spectator.click(
            spectator
                .query(byTestId('configuration-license-close-btn'))
                ?.querySelector('button') as HTMLButtonElement
        );

        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalled();
    });
});

import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { Subject, of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotCompanyConfigurationService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotLicenseInfo } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    ALTERNATIVE_LICENSING_URL,
    DotConfigurationLicenseDialogComponent,
    LICENSE_SOURCE_URL
} from './dot-configuration-license-dialog.component';

const LICENSE: DotLicenseInfo = {
    title: 'dotCMS Business Source License 1.1',
    licensor: 'dotCMS LLC',
    changeDate: 'Four years from August 01, 2025',
    changeLicense: 'GNU General Public License (GPL) v3',
    text: 'Licensor:             dotCMS LLC\n\nAdditional Use Grant:\n\n   Individual developer'
};

describe('DotConfigurationLicenseDialogComponent', () => {
    let spectator: Spectator<DotConfigurationLicenseDialogComponent>;
    let config: DynamicDialogConfig;

    const getLicense = vi.fn();

    const createComponent = createComponentFactory({
        component: DotConfigurationLicenseDialogComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'configuration.license.licensor': 'Licensor {0}',
                    'configuration.license.change-date': 'Change Date {0}',
                    'configuration.license.change-license': 'Change License {0}'
                })
            },
            mockProvider(DynamicDialogRef, { close: vi.fn() }),
            mockProvider(DotCompanyConfigurationService, { getLicense }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() })
        ],
        detectChanges: false
    });

    const open = () => {
        config = { header: 'dotCMS Business Source License' };
        spectator = createComponent({
            providers: [{ provide: DynamicDialogConfig, useValue: config }]
        });
        spectator.detectChanges();
    };

    beforeEach(() => {
        vi.clearAllMocks();
        getLicense.mockReturnValue(of(LICENSE));
    });

    describe('when the license loads', () => {
        beforeEach(() => open());

        it('uses the license title, with its version, as the dialog header', () => {
            expect(config.header).toBe('dotCMS Business Source License 1.1');
        });

        it('lists the licensor, change date and change license', () => {
            expect(spectator.query(byTestId('configuration-license-details'))).toHaveText(
                'Licensor dotCMS LLC · Change Date Four years from August 01, 2025 · Change License GNU General Public License (GPL) v3'
            );
        });

        it('shows the full text with its original line breaks', () => {
            const text = spectator.query<HTMLElement>(byTestId('configuration-license-text'));

            expect(text?.textContent).toBe(LICENSE.text);
            expect(text).toHaveClass('whitespace-pre-wrap');
        });

        it('does not show the fallback link', () => {
            expect(spectator.query(byTestId('configuration-license-fallback'))).not.toExist();
        });
    });

    it('leaves out the header values the license does not state', () => {
        getLicense.mockReturnValue(of({ ...LICENSE, licensor: null, changeDate: null }));
        open();

        expect(spectator.query(byTestId('configuration-license-details'))).toHaveText(
            'Change License GNU General Public License (GPL) v3'
        );
    });

    it('shows a loading state until the license arrives', () => {
        const response = new Subject<DotLicenseInfo>();
        getLicense.mockReturnValue(response);
        open();

        expect(spectator.query(byTestId('configuration-license-loading'))).toExist();
        expect(spectator.query(byTestId('configuration-license-text'))).not.toExist();

        response.next(LICENSE);
        spectator.detectChanges();

        expect(spectator.query(byTestId('configuration-license-loading'))).not.toExist();
        expect(spectator.query(byTestId('configuration-license-text'))).toExist();
    });

    describe('when the license cannot be loaded', () => {
        const error = new HttpErrorResponse({ status: 500 });

        beforeEach(() => {
            getLicense.mockReturnValue(throwError(() => error));
            open();
        });

        it('reports the error', () => {
            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
        });

        it('links to the published license, opening in a new tab', () => {
            const link = spectator.query(byTestId('configuration-license-source-link'));

            expect(link).toHaveAttribute('href', LICENSE_SOURCE_URL);
            expect(link).toHaveAttribute('target', '_blank');
            expect(link).toHaveAttribute('rel', 'noopener noreferrer');
        });

        it('keeps the generic header', () => {
            expect(config.header).toBe('dotCMS Business Source License');
        });
    });

    describe('footer', () => {
        beforeEach(() => open());

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
});

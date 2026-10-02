import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationRegenerateKeyDialogComponent } from './dot-configuration-regenerate-key-dialog.component';

describe('DotConfigurationRegenerateKeyDialogComponent', () => {
    let spectator: Spectator<DotConfigurationRegenerateKeyDialogComponent>;

    const createComponent = createComponentFactory({
        component: DotConfigurationRegenerateKeyDialogComponent,
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            mockProvider(DynamicDialogRef, { close: vi.fn() })
        ]
    });

    const confirmButton = () =>
        spectator
            .query(byTestId('configuration-regenerate-key-confirm-btn'))
            ?.querySelector('button') as HTMLButtonElement;

    beforeEach(() => {
        spectator = createComponent();
    });

    it('keeps Regenerate Key disabled until the impact is acknowledged', () => {
        expect(confirmButton()).toBeDisabled();
    });

    it('confirms once the impact is acknowledged', () => {
        spectator.triggerEventHandler('p-checkbox', 'ngModelChange', true);
        spectator.detectChanges();

        spectator.click(confirmButton());

        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(true);
    });

    it('closes without confirming on cancel', () => {
        spectator.click(
            spectator
                .query(byTestId('configuration-regenerate-key-cancel-btn'))
                ?.querySelector('button') as HTMLButtonElement
        );

        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith();
    });
});

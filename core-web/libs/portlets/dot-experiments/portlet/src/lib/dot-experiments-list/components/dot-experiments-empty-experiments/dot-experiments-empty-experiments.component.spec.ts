import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { Button, ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsEmptyExperimentsComponent } from './dot-experiments-empty-experiments.component';

const messageServiceMock = new MockDotMessageService({
    'experimentspage.add.new.experiment': 'Create a new Experiment'
});

describe('DotExperimentsEmptyExperimentsComponent', () => {
    let spectator: Spectator<DotExperimentsEmptyExperimentsComponent>;
    let pButton: Button | null;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, DotMessagePipe],
        component: DotExperimentsEmptyExperimentsComponent,
        providers: [
            mockProvider(CoreWebService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should show icon, title and button', () => {
        const description = 'description message';
        spectator.setInput({
            description,
            showButton: true
        });

        pButton = spectator.query(Button);

        expect(spectator.query(byTestId('description'))).toHaveText(description);
        expect(pButton.label).toBe('Create a new Experiment');
    });

    it('should show icon and title, not button', () => {
        const description = 'description message';
        spectator.setInput({
            description,
            showButton: false
        });

        pButton = spectator.query(Button);

        expect(spectator.query(byTestId('description'))).toHaveText(description);
        expect(pButton).not.toExist();
    });
});

import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';

import { Button, ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotIconComponent, DotIconModule } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

import { DotExperimentsEmptyExperimentsComponent } from './dot-experiments-empty-experiments.component';

const messageServiceMock = new MockDotMessageService({
    'experimentspage.add.new.experiment': 'Create a new Experiment'
});

describe('DotExperimentsEmptyExperimentsComponent', () => {
    let spectator: Spectator<DotExperimentsEmptyExperimentsComponent>;
    let pButton: Button | null;
    let dotIcon: DotIconComponent | null;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, DotMessagePipeModule, DotIconModule],
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

        dotIcon = spectator.query(DotIconComponent);
        pButton = spectator.query(Button);

        expect(dotIcon).toExist();

        expect(spectator.query(byTestId('description'))).toHaveText(description);
        expect(pButton.label).toBe('Create a new Experiment');
    });

    it('should show icon and title, not button', () => {
        const description = 'description message';
        spectator.setInput({
            description,
            showButton: false
        });

        dotIcon = spectator.query(DotIconComponent);
        pButton = spectator.query(Button);

        expect(dotIcon).toExist();

        expect(spectator.query(byTestId('description'))).toHaveText(description);
        expect(pButton).not.toExist();
    });
});

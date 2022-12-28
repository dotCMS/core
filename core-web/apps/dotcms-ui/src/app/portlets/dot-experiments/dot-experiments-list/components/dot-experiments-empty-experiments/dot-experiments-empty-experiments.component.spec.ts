import { DotExperimentsEmptyExperimentsComponent } from './dot-experiments-empty-experiments.component';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { Button, ButtonModule } from 'primeng/button';
import { DotIconComponent, DotIconModule } from '@dotcms/ui';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotMessageService } from '@dotcms/data-access';

const messageServiceMock = new MockDotMessageService({
    'experimentspage.add.new.experiment': 'Add a new experiment'
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
        expect(pButton.label).toBe('Add a new experiment');
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

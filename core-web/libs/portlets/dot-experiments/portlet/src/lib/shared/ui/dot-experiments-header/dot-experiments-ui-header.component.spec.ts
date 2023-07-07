import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Skeleton } from 'primeng/skeleton';
import { Tag, TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsUiHeaderComponent } from './dot-experiments-ui-header.component';

const messageServiceMock = new MockDotMessageService({
    running: 'RUNNING'
});
describe('ExperimentsHeaderComponent', () => {
    let spectator: Spectator<DotExperimentsUiHeaderComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsUiHeaderComponent,
        imports: [TagModule],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should has a title rendered', () => {
        const title = 'My title';
        spectator.setInput('title', title);
        expect(spectator.query(byTestId('title'))).toHaveText(title);
    });

    it('should emit goBack output when icon is clicked. ', () => {
        let output;
        spectator.output('goBack').subscribe((result) => (output = result));
        const goBackButton = spectator.query(byTestId('goback-link')) as HTMLAnchorElement;
        spectator.click(goBackButton);
        expect(output).toEqual(true);
    });

    it('should show the skeleton component if isLoading true ', () => {
        spectator.setInput({
            isLoading: true
        });

        expect(spectator.query(Skeleton)).toExist();
    });

    it('should rendered the status Input', () => {
        const expectedStatus: DotExperimentStatus = DotExperimentStatus.RUNNING;
        spectator.setInput({
            status: DotExperimentStatus.RUNNING
        });

        expect(spectator.query(Tag)).toExist();
        expect(spectator.query(byTestId('status-tag'))).toContainText(expectedStatus);
    });
});

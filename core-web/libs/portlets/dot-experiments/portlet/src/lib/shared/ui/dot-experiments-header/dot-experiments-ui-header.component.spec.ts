import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DatePipe } from '@angular/common';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus, RUNNING_UNTIL_DATE_FORMAT } from '@dotcms/dotcms-models';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsUiHeaderComponent } from './dot-experiments-ui-header.component';

const messageServiceMock = new MockDotMessageService({
    running: 'RUNNING',
    'dot.common.until': 'until'
});

const EXPERIMENT_MOCK = getExperimentMock(0);

describe('ExperimentsHeaderComponent', () => {
    let spectator: Spectator<DotExperimentsUiHeaderComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsUiHeaderComponent,
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
        spectator.setInput({
            title: title,
            isLoading: false
        });
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

        expect(spectator.query('p-skeleton')).toExist();
    });

    it('should render the status tag', () => {
        spectator.setInput({
            experiment: { ...EXPERIMENT_MOCK, status: DotExperimentStatus.SCHEDULED }
        });

        expect(spectator.query('p-tag')).toExist();
        expect(spectator.query(byTestId('status-tag'))).toContainText('Scheduled');
    });

    it('should rendered the RUNNING status tag', () => {
        const expectedStatus =
            'Running until ' +
            new DatePipe('en-US').transform(
                EXPERIMENT_MOCK.scheduling.endDate,
                RUNNING_UNTIL_DATE_FORMAT
            );

        spectator.setInput({
            experiment: { ...EXPERIMENT_MOCK, status: DotExperimentStatus.RUNNING }
        });

        expect(spectator.query('p-tag')).toExist();
        expect(spectator.query(byTestId('status-tag'))).toContainText(expectedStatus);
    });
});

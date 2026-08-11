import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { Tag } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus, ExperimentsStatusIcons } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotExperimentStatusTagComponent,
    experimentStatusSeverity,
    TagSeverity
} from './dot-experiment-status-tag.component';

/** The label key each status is expected to resolve to, per `ExperimentsStatusList`. */
const LABEL_KEY_BY_STATUS: Record<DotExperimentStatus, string> = {
    [DotExperimentStatus.DRAFT]: 'draft',
    [DotExperimentStatus.SCHEDULED]: 'scheduled',
    [DotExperimentStatus.RUNNING]: 'running',
    [DotExperimentStatus.ENDED]: 'ended',
    [DotExperimentStatus.ARCHIVED]: 'archived'
};

/**
 * Mirrors `DotExperimentsUiHeaderComponent.statusSeverityMap`: the list tag and the UVE
 * header must render the same severity for the same status.
 */
const SEVERITY_BY_STATUS: Record<DotExperimentStatus, TagSeverity> = {
    [DotExperimentStatus.RUNNING]: 'success',
    [DotExperimentStatus.SCHEDULED]: 'info',
    [DotExperimentStatus.DRAFT]: 'warn',
    [DotExperimentStatus.ENDED]: 'info',
    [DotExperimentStatus.ARCHIVED]: 'secondary'
};

const messageServiceMock = new MockDotMessageService({
    draft: 'Draft',
    scheduled: 'Scheduled',
    running: 'Running',
    ended: 'Ended',
    archived: 'Archived'
});

describe('DotExperimentStatusTagComponent', () => {
    let spectator: Spectator<DotExperimentStatusTagComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentStatusTagComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        detectChanges: false
    });

    const tag = (): Tag => spectator.query(Tag) as Tag;

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('severity mapping', () => {
        it.each(Object.entries(SEVERITY_BY_STATUS))(
            'should map %s to the %s severity',
            (status, severity) => {
                expect(experimentStatusSeverity(status as DotExperimentStatus)).toBe(severity);
            }
        );

        it.each(Object.entries(SEVERITY_BY_STATUS))(
            'should render %s with the %s severity',
            (status, severity) => {
                spectator.setInput('status', status as DotExperimentStatus);

                expect(tag().severity).toBe(severity);
            }
        );
    });

    it.each(Object.values(DotExperimentStatus))('should render the shared icon of %s', (status) => {
        spectator.setInput('status', status);

        expect(tag().icon).toBe(ExperimentsStatusIcons[status]);
    });

    it.each(Object.entries(LABEL_KEY_BY_STATUS))(
        'should render the translated label of %s',
        (status, labelKey) => {
            spectator.setInput('status', status as DotExperimentStatus);

            expect(tag().value).toBe(messageServiceMock.get(labelKey));
        }
    );

    it('should render nothing when the status is null', () => {
        spectator.setInput('status', null);

        expect(spectator.query(byTestId('experiment-status-tag'))).toBeNull();
    });
});

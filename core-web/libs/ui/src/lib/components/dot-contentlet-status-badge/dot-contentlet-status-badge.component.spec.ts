import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotContentState } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentletStatusBadgeComponent } from './dot-contentlet-status-badge.component';

describe('DotContentletStatusBadgeComponent', () => {
    let spectator: Spectator<DotContentletStatusBadgeComponent>;

    const messageServiceMock = new MockDotMessageService({ New: 'New Translated' });

    const createComponent = createComponentFactory({
        component: DotContentletStatusBadgeComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    const getTag = () => spectator.query(byTestId('status-tag'));

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('Status tag rendering', () => {
        it('should render a "Published" tag with success severity when contentlet is published', () => {
            const state: DotContentState = {
                live: true,
                working: true,
                hasLiveVersion: true,
                archived: false,
                deleted: false
            };

            spectator.setInput('state', state);

            const tag = getTag();
            expect(tag).toBeTruthy();
            expect(tag).toHaveClass('p-tag-success');
            expect(tag).toHaveText('Published');
        });

        it('should render an "Archived" tag with danger severity when contentlet is archived', () => {
            const state: DotContentState = {
                archived: true,
                live: false,
                working: false,
                hasLiveVersion: false
            };

            spectator.setInput('state', state);

            const tag = getTag();
            expect(tag).toBeTruthy();
            expect(tag).toHaveClass('p-tag-danger');
            expect(tag).toHaveText('Archived');
        });

        it('should render an "Archived" tag with danger severity when contentlet is deleted', () => {
            const state: DotContentState = {
                deleted: true,
                live: false,
                working: false,
                hasLiveVersion: false
            };

            spectator.setInput('state', state);

            const tag = getTag();
            expect(tag).toBeTruthy();
            expect(tag).toHaveClass('p-tag-danger');
            expect(tag).toHaveText('Archived');
        });

        it('should render a "Revision" tag with info severity when contentlet has live version but is not live', () => {
            const state: DotContentState = {
                live: false,
                working: false,
                hasLiveVersion: true,
                archived: false,
                deleted: false
            };

            spectator.setInput('state', state);

            const tag = getTag();
            expect(tag).toBeTruthy();
            expect(tag).toHaveClass('p-tag-info');
            expect(tag).toHaveText('Revision');
        });

        it('should render a "Draft" tag with warn severity when contentlet is draft', () => {
            const state: DotContentState = {
                live: false,
                working: false,
                hasLiveVersion: false,
                archived: false,
                deleted: false
            };

            spectator.setInput('state', state);

            const tag = getTag();
            expect(tag).toBeTruthy();
            expect(tag).toHaveClass('p-tag-warn');
            expect(tag).toHaveText('Draft');
        });

        it('should render a translated "New" tag with info severity when state is null', () => {
            spectator.setInput('state', null);

            const tag = getTag();
            expect(tag).toBeTruthy();
            expect(tag).toHaveClass('p-tag-info');
            expect(tag).toHaveText('New Translated');
        });

        it('should never render a p-chip', () => {
            spectator.setInput('state', {
                live: true,
                working: true,
                hasLiveVersion: true,
                archived: false,
                deleted: false
            });

            expect(spectator.query('p-chip')).toBeNull();
        });
    });
});

import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotGravatarDirective } from '@dotcms/ui';

import { Activity } from '../../../../models/dot-edit-content.model';
import { DotEditContentSidebarActivitiesComponent } from './dot-edit-content-sidebar-activities.component';

describe('DotEditContentSidebarActivitiesComponent', () => {
    let spectator: Spectator<DotEditContentSidebarActivitiesComponent>;

    const mockActivities: Activity[] = [
        {
            commentDescription: 'Activity 1 description',
            createdDate: 1620000000000,
            email: 'user1@example.com',
            postedBy: 'User One',
            roleId: '1',
            taskId: 'task1',
            type: 'comment'
        },
        {
            commentDescription: 'Activity 2 description',
            createdDate: 1620000100000,
            email: 'user2@example.com',
            postedBy: 'User Two',
            roleId: '2',
            taskId: 'task2',
            type: 'comment'
        }
    ];

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarActivitiesComponent,
        imports: [DotGravatarDirective],
        shallow: true
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [mockProvider(DotFormatDateService), mockProvider(DotMessageService)]
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Loading state', () => {
        it('should show skeleton when in loading state', () => {
            spectator.setInput('state', ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.query(byTestId('loading-state'))).toExist();
            expect(spectator.query(byTestId('activities-list'))).not.toExist();
            expect(spectator.query(byTestId('empty-state'))).not.toExist();
        });
    });

    describe('Empty state', () => {
        it('should show empty state message when activities array is empty', () => {
            spectator.setInput({
                state: ComponentStatus.LOADED,
                activities: []
            });
            spectator.detectChanges();

            expect(spectator.query(byTestId('empty-state'))).toExist();
            expect(spectator.query(byTestId('activities-list'))).toExist();
            expect(spectator.query(byTestId('activity-item'))).not.toExist();
            expect(spectator.query(byTestId('loading-state'))).not.toExist();
        });
    });

    describe('Activities list', () => {
        beforeEach(() => {
            spectator.setInput({
                state: ComponentStatus.LOADED,
                activities: mockActivities
            });
            spectator.detectChanges();
        });

        it('should show activities list with items when activities are available', () => {
            expect(spectator.query(byTestId('activities-list'))).toExist();
            expect(spectator.query(byTestId('activity-item'))).toExist();
            expect(spectator.query(byTestId('empty-state'))).not.toExist();
            expect(spectator.query(byTestId('loading-state'))).not.toExist();
        });

        it('should render all activity items from the input', () => {
            const activityItems = spectator.queryAll(byTestId('activity-item'));
            expect(activityItems.length).toBe(mockActivities.length);
        });

        it('should display correct activity user information', () => {
            const activityUsers = spectator.queryAll(byTestId('activity-user'));

            expect(activityUsers[0]).toHaveText(mockActivities[0].postedBy);
            expect(activityUsers[1]).toHaveText(mockActivities[1].postedBy);
        });

        it('should display correct activity description', () => {
            const activityDescriptions = spectator.queryAll(byTestId('activity-description'));

            expect(activityDescriptions[0]).toHaveText(mockActivities[0].commentDescription);
            expect(activityDescriptions[1]).toHaveText(mockActivities[1].commentDescription);
        });

        it('should have avatars with correct dotGravatar directive', () => {
            const avatarElements = spectator.queryAll('[dotGravatar]');

            expect(avatarElements.length).toBe(mockActivities.length);

            expect(avatarElements[0]).toHaveAttribute('ng-reflect-email', mockActivities[0].email);
            expect(avatarElements[1]).toHaveAttribute('ng-reflect-email', mockActivities[1].email);

            expect(spectator.queryAll('p-avatar').length).toBe(mockActivities.length);
        });

        it('should display date information correctly', () => {
            const dates = spectator.queryAll(byTestId('activity-date'));

            expect(dates.length).toBe(mockActivities.length);
        });
    });
});

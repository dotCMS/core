import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotFieldValidationMessageComponent, DotGravatarDirective } from '@dotcms/ui';

import { DotEditContentSidebarActivitiesComponent } from './dot-edit-content-sidebar-activities.component';

import { Activity, DotContentletState } from '../../../../models/dot-edit-content.model';

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
        imports: [DotGravatarDirective, ReactiveFormsModule],
        providers: [FormBuilder],
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
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.query(byTestId('loading-state'))).toBeVisible();
            expect(spectator.query(byTestId('activities-list'))).not.toBeVisible();
            expect(spectator.query(byTestId('empty-state'))).not.toBeVisible();
        });
    });

    describe('Empty state', () => {
        it('should show empty state message when activities array is empty', () => {
            spectator.setInput({
                status: ComponentStatus.LOADED,
                activities: []
            });
            spectator.detectChanges();

            expect(spectator.query(byTestId('empty-state'))).toBeVisible();
            expect(spectator.query(byTestId('activities-list'))).toBeVisible();
            expect(spectator.query(byTestId('activity-item'))).not.toBeVisible();
            expect(spectator.query(byTestId('loading-state'))).not.toBeVisible();
        });
    });

    describe('Activities list', () => {
        beforeEach(() => {
            spectator.setInput({
                status: ComponentStatus.LOADED,
                activities: mockActivities
            });
            spectator.detectChanges();
        });

        it('should show activities list with items when activities are available', () => {
            expect(spectator.query(byTestId('activities-list'))).toBeVisible();
            expect(spectator.query(byTestId('activity-item'))).toBeVisible();
            expect(spectator.query(byTestId('empty-state'))).not.toBeVisible();
            expect(spectator.query(byTestId('loading-state'))).not.toBeVisible();
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

    describe('Comment Form', () => {
        beforeEach(() => {
            const state: DotContentletState = 'existing';
            spectator.setInput({
                status: ComponentStatus.LOADED,
                initialContentletState: state
            });
            spectator.detectChanges();
            jest.spyOn(spectator.component.commentSubmitted, 'emit');
        });

        it('should show comment form when initialContentletState is not new', () => {
            expect(spectator.query(byTestId('activities-form'))).toBeVisible();
        });

        it('should have dot-field-validation-message component', () => {
            expect(spectator.query(DotFieldValidationMessageComponent)).toBeTruthy();
        });

        it('should hide comment form when initialContentletState is new', () => {
            const state: DotContentletState = 'new';
            spectator.setInput('initialContentletState', state);
            spectator.detectChanges();
            expect(spectator.query(byTestId('activities-form'))).not.toBeVisible();
        });

        it('should not emit and mark form as invalid when submitting invalid form', () => {
            const commentInput = spectator.query(byTestId('activities-input'));
            const form = spectator.query(byTestId('activities-form'));

            spectator.typeInElement('ab', commentInput);
            spectator.detectChanges();

            spectator.dispatchFakeEvent(form, 'submit');
            spectator.detectChanges();

            expect(spectator.component.commentSubmitted.emit).not.toHaveBeenCalled();
            expect(spectator.component.form.get('comment').errors).toBeTruthy();
            expect(spectator.component.form.get('comment').errors['minlength']).toBeTruthy();
            expect(commentInput).toHaveClass('ng-invalid');
            expect(commentInput).toHaveClass('ng-touched');
        });

        it('should show minlength error when comment is too short', () => {
            const commentInput = spectator.query(byTestId('activities-input'));
            const form = spectator.query(byTestId('activities-form'));

            spectator.typeInElement('ab', commentInput);
            spectator.detectChanges();

            spectator.dispatchFakeEvent(form, 'submit');
            spectator.detectChanges();

            const control = spectator.component.form.get('comment');
            expect(control.errors).toBeTruthy();
            expect(control.errors['minlength']).toBeTruthy();
            expect(commentInput).toHaveClass('ng-invalid');
            expect(commentInput).toHaveClass('ng-touched');
        });

        it('should show maxlength error when comment exceeds max length', () => {
            const commentInput = spectator.query(byTestId('activities-input'));
            const form = spectator.query(byTestId('activities-form'));
            const longComment = 'a'.repeat(501);

            spectator.typeInElement(longComment, commentInput);
            spectator.detectChanges();

            spectator.dispatchFakeEvent(form, 'submit');
            spectator.detectChanges();

            const control = spectator.component.form.get('comment');
            expect(control.errors).toBeTruthy();
            expect(control.errors['maxlength']).toBeTruthy();
            expect(commentInput).toHaveClass('ng-invalid');
            expect(commentInput).toHaveClass('ng-touched');
        });

        it('should show required error when comment is empty', () => {
            const commentInput = spectator.query(byTestId('activities-input'));
            const form = spectator.query(byTestId('activities-form'));

            spectator.typeInElement('', commentInput);
            spectator.detectChanges();

            spectator.dispatchFakeEvent(form, 'submit');
            spectator.detectChanges();

            const control = spectator.component.form.get('comment');
            expect(control.errors).toBeTruthy();
            expect(control.errors['required']).toBeTruthy();
            expect(commentInput).toHaveClass('ng-invalid');
            expect(commentInput).toHaveClass('ng-touched');
        });

        it('should emit comment when form is submitted with valid input', () => {
            const commentText = 'New valid comment';
            const commentInput = spectator.query(byTestId('activities-input'));
            const form = spectator.query(byTestId('activities-form'));

            spectator.typeInElement(commentText, commentInput);
            spectator.detectChanges();

            spectator.dispatchFakeEvent(form, 'submit');
            spectator.detectChanges();

            expect(spectator.component.commentSubmitted.emit).toHaveBeenCalledWith(commentText);
        });

        it('should clear form after successful submission', () => {
            const commentInput = spectator.query(byTestId('activities-input'));
            const form = spectator.query(byTestId('activities-form'));

            spectator.typeInElement('Valid comment', commentInput);
            spectator.detectChanges();

            spectator.dispatchFakeEvent(form, 'submit');
            spectator.detectChanges();

            expect(commentInput).toHaveValue('');
            expect(spectator.component.form.pristine).toBe(true);
        });

        it('should not emit comment when input contains only whitespace', () => {
            const commentInput = spectator.query(byTestId('activities-input'));
            const form = spectator.query(byTestId('activities-form'));

            spectator.typeInElement('   ', commentInput);
            spectator.detectChanges();

            spectator.dispatchFakeEvent(form, 'submit');
            spectator.detectChanges();

            expect(spectator.component.commentSubmitted.emit).not.toHaveBeenCalled();
        });

        it('should trim whitespace from comment before emitting', () => {
            const commentText = '  Valid comment with spaces  ';
            const commentInput = spectator.query(byTestId('activities-input'));
            const form = spectator.query(byTestId('activities-form'));

            spectator.typeInElement(commentText, commentInput);
            spectator.detectChanges();

            spectator.dispatchFakeEvent(form, 'submit');
            spectator.detectChanges();

            expect(spectator.component.commentSubmitted.emit).toHaveBeenCalledWith(
                commentText.trim()
            );
        });

        it('should maintain form state after failed submission', () => {
            const commentText = 'ab';
            const commentInput = spectator.query(byTestId('activities-input'));
            const form = spectator.query(byTestId('activities-form'));

            spectator.typeInElement(commentText, commentInput);
            spectator.detectChanges();

            spectator.dispatchFakeEvent(form, 'submit');
            spectator.detectChanges();

            expect(commentInput).toHaveValue(commentText);
            expect(spectator.component.form.dirty).toBe(true);
            expect(spectator.component.form.touched).toBe(true);
        });

        it('should clear comment when clear button is clicked', () => {
            const commentInput = spectator.query(byTestId('activities-input'));
            spectator.typeInElement('Test comment', commentInput);
            spectator.detectChanges();

            const clearButton = spectator.query(byTestId('activities-clear'));
            spectator.click(clearButton);
            spectator.detectChanges();

            expect(commentInput).toHaveValue('');
            expect(spectator.component.form.pristine).toBe(true);
            expect(spectator.component.form.get('comment').untouched).toBe(true);
        });

        it('should reset form state when clearComment is called', () => {
            const commentInput = spectator.query(byTestId('activities-input'));
            spectator.typeInElement('Test comment', commentInput);
            spectator.detectChanges();

            spectator.component.clearComment();
            spectator.detectChanges();

            expect(commentInput).toHaveValue('');
            expect(spectator.component.form.pristine).toBe(true);
            expect(spectator.component.form.get('comment').untouched).toBe(true);
            expect(spectator.component.form.get('comment').value).toBe(null);
        });
    });

    describe('Computed Properties', () => {
        it('should correctly compute isLoading state', () => {
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();
            expect(spectator.component['$isLoading']()).toBe(true);

            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.component['$isLoading']()).toBe(false);
        });

        it('should correctly compute isSaving state', () => {
            spectator.setInput('status', ComponentStatus.SAVING);
            spectator.detectChanges();
            expect(spectator.component['$isSaving']()).toBe(true);

            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.component['$isSaving']()).toBe(false);
        });

        it('should correctly compute hideForm state', () => {
            spectator.setInput('initialContentletState', 'new');
            spectator.detectChanges();
            expect(spectator.component['$hideForm']()).toBe(true);

            spectator.setInput('initialContentletState', 'existing');
            spectator.detectChanges();
            expect(spectator.component['$hideForm']()).toBe(false);
        });
    });
});

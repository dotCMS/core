import { mockProvider } from '@ngneat/spectator/jest';

import { TestBed } from '@angular/core/testing';
import {
    ActivatedRouteSnapshot,
    RouterStateSnapshot,
    UrlTree,
    type CanDeactivateFn
} from '@angular/router';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotAlertConfirm } from '@dotcms/dotcms-models';

import { unsavedChangesGuard } from './unsaved-changes.guard';

import { DotEditContentLayoutComponent } from '../components/dot-edit-content-layout/dot-edit-content.layout.component';

type GuardResult = boolean | UrlTree;

describe('unsavedChangesGuard', () => {
    let capturedModel: DotAlertConfirm | null;
    let dotAlertConfirmService: DotAlertConfirmService;

    const runGuard = (component: Partial<DotEditContentLayoutComponent>) =>
        TestBed.runInInjectionContext(() =>
            (unsavedChangesGuard as CanDeactivateFn<DotEditContentLayoutComponent>)(
                component as DotEditContentLayoutComponent,
                {} as ActivatedRouteSnapshot,
                {} as RouterStateSnapshot,
                {} as RouterStateSnapshot
            )
        );

    beforeEach(() => {
        capturedModel = null;

        TestBed.configureTestingModule({
            providers: [
                mockProvider(DotAlertConfirmService, {
                    confirm: jest.fn((model: DotAlertConfirm) => {
                        capturedModel = model;
                    })
                }),
                mockProvider(DotMessageService, {
                    get: jest.fn((key: string) => key)
                })
            ]
        });

        dotAlertConfirmService = TestBed.inject(DotAlertConfirmService);
    });

    it('should allow navigation immediately when the form is pristine', () => {
        const component = {
            hasUnsavedChanges: () => false
        } as Partial<DotEditContentLayoutComponent>;

        const result = runGuard(component);

        expect(result).toBe(true);
        expect(dotAlertConfirmService.confirm).not.toHaveBeenCalled();
    });

    it('should cancel navigation when the user clicks "Keep editing" (accept)', (done) => {
        const component = {
            hasUnsavedChanges: () => true
        } as Partial<DotEditContentLayoutComponent>;

        const result = runGuard(component) as Exclude<GuardResult, boolean | UrlTree> & {
            subscribe: (
                next: (value: boolean) => void,
                err?: unknown,
                complete?: () => void
            ) => void;
        };

        let completed = false;
        result.subscribe(
            (value) => {
                expect(value).toBe(false);
            },
            undefined,
            () => {
                completed = true;
            }
        );

        // Sanity: dialog was opened with the expected i18n keys and label mapping
        expect(dotAlertConfirmService.confirm).toHaveBeenCalledTimes(1);
        const model = capturedModel as DotAlertConfirm;
        expect(model.header).toBe('edit.content.unsaved.changes.title');
        expect(model.message).toBe('edit.content.unsaved.changes.message');
        expect(model.footerLabel).toEqual({
            accept: 'edit.content.unsaved.changes.keep',
            reject: 'edit.content.unsaved.changes.discard'
        });

        // Simulate clicking the primary "Keep editing" button
        model.accept?.();
        expect(completed).toBe(true);
        done();
    });

    it('should allow navigation when the user clicks "Discard changes" (reject)', (done) => {
        const component = {
            hasUnsavedChanges: () => true
        } as Partial<DotEditContentLayoutComponent>;

        const result = runGuard(component) as Exclude<GuardResult, boolean | UrlTree> & {
            subscribe: (
                next: (value: boolean) => void,
                err?: unknown,
                complete?: () => void
            ) => void;
        };

        let completed = false;
        result.subscribe(
            (value) => {
                expect(value).toBe(true);
            },
            undefined,
            () => {
                completed = true;
            }
        );

        const model = capturedModel as DotAlertConfirm;

        // Simulate clicking the secondary "Discard changes" button
        model.reject?.();
        expect(completed).toBe(true);
        done();
    });
});

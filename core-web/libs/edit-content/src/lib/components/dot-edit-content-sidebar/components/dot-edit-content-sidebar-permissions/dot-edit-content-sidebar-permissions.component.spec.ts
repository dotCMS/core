import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotPermissionsDialogComponent } from './components/permissions-dialog/permissions-dialog.component';
import { DotEditContentSidebarPermissionsComponent } from './dot-edit-content-sidebar-permissions.component';

describe('DotEditContentSidebarPermissionsComponent', () => {
    let spectator: Spectator<DotEditContentSidebarPermissionsComponent>;
    let dotMessageService: SpyObject<DotMessageService>;
    let dialogOpenSpy: jest.Mock;
    let mockDialogRef: DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarPermissionsComponent,
        providers: [
            mockProvider(DotMessageService, {
                get: jest.fn((key: string) => key)
            })
        ]
    });

    beforeEach(() => {
        mockDialogRef = {
            onClose: { subscribe: jest.fn(() => ({ unsubscribe: jest.fn() })) },
            close: jest.fn()
        } as unknown as DynamicDialogRef;
        dialogOpenSpy = jest.fn().mockReturnValue(mockDialogRef);

        spectator = createComponent({
            props: {
                identifier: 'content-123',
                languageId: 123
            },
            providers: [
                {
                    provide: DialogService,
                    useValue: { open: dialogOpenSpy }
                }
            ]
        });

        dotMessageService = spectator.inject(DotMessageService);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Elements by data-testId', () => {
        it('should render permissions-card when identifier and languageId are set', () => {
            spectator.setInput('identifier', 'content-456');
            spectator.setInput('languageId', 1);
            spectator.detectChanges();

            const card = spectator.query(byTestId('permissions-card'));
            expect(card).toBeTruthy();
        });

        it('should have permissions-card with role="button" and tabindex="0"', () => {
            const card = spectator.query(byTestId('permissions-card'));
            expect(card?.getAttribute('role')).toBe('button');
            expect(card?.getAttribute('tabindex')).toBe('0');
        });
    });

    describe('openPermissionsDialog - Success', () => {
        it('should open permissions dialog when card is clicked with valid identifier and languageId', () => {
            spectator.setInput('identifier', 'content-789');
            spectator.setInput('languageId', 2);
            spectator.detectChanges();

            spectator.click(byTestId('permissions-card'));

            expect(dialogOpenSpy).toHaveBeenCalledWith(DotPermissionsDialogComponent, {
                header: 'edit.content.sidebar.permissions.title',
                width: 'min(92vw, 75rem)',
                contentStyle: { overflow: 'hidden' },
                data: { identifier: 'content-789', languageId: 2 },
                transitionOptions: null,
                modal: true,
                appendTo: 'body',
                closeOnEscape: false,
                draggable: false,
                keepInViewport: false,
                maskStyleClass: 'p-dialog-mask-dynamic',
                resizable: false,
                position: 'center'
            });
        });

        it('should call DotMessageService.get for header when opening dialog', () => {
            spectator.component.openPermissionsDialog();

            expect(dotMessageService.get).toHaveBeenCalledWith(
                'edit.content.sidebar.permissions.title'
            );
        });
    });

    describe('openPermissionsDialog - Failure and Edge Cases', () => {
        it('should NOT open dialog when identifier is empty string', () => {
            spectator.setInput('identifier', '');
            spectator.setInput('languageId', 1);
            spectator.detectChanges();

            spectator.component.openPermissionsDialog();

            expect(dialogOpenSpy).not.toHaveBeenCalled();
        });

        it('should NOT open dialog when languageId is 0', () => {
            spectator.setInput('identifier', 'content-ok');
            spectator.setInput('languageId', 0);
            spectator.detectChanges();

            spectator.component.openPermissionsDialog();

            expect(dialogOpenSpy).not.toHaveBeenCalled();
        });

        it('should NOT open dialog when identifier is undefined', () => {
            spectator.setInput('identifier', undefined as never);
            spectator.setInput('languageId', 1);
            spectator.detectChanges();

            spectator.component.openPermissionsDialog();

            expect(dialogOpenSpy).not.toHaveBeenCalled();
        });

        it('should NOT open dialog when languageId is undefined', () => {
            spectator.setInput('identifier', 'content-ok');
            spectator.setInput('languageId', undefined as never);
            spectator.detectChanges();

            spectator.component.openPermissionsDialog();

            expect(dialogOpenSpy).not.toHaveBeenCalled();
        });
    });

    describe('Keyboard and accessibility', () => {
        it('should open dialog on Enter key on permissions-card', () => {
            spectator.setInput('identifier', 'k1');
            spectator.setInput('languageId', 1);
            spectator.detectChanges();

            spectator.dispatchKeyboardEvent(byTestId('permissions-card'), 'keydown', 'Enter');

            expect(dialogOpenSpy).toHaveBeenCalled();
        });

        it('should open dialog on Space key on permissions-card', () => {
            spectator.setInput('identifier', 'k2');
            spectator.setInput('languageId', 1);
            spectator.detectChanges();

            spectator.dispatchKeyboardEvent(byTestId('permissions-card'), 'keydown', ' ');
            spectator.detectChanges();

            expect(dialogOpenSpy).toHaveBeenCalled();
        });
    });

    describe('ngOnDestroy', () => {
        it('should close dialog ref on destroy when dialog was opened', () => {
            spectator.component.openPermissionsDialog();

            spectator.fixture.destroy();

            expect(mockDialogRef.close).toHaveBeenCalled();
        });

        it('should not throw when destroy and dialog was never opened', () => {
            expect(() => spectator.fixture.destroy()).not.toThrow();
        });
    });
});

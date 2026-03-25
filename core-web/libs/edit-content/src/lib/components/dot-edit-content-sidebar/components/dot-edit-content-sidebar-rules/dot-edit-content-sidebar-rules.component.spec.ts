import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotRulesDialogComponent } from './components/rules-dialog/rules-dialog.component';
import { DotEditContentSidebarRulesComponent } from './dot-edit-content-sidebar-rules.component';

describe('DotEditContentSidebarRulesComponent', () => {
    let spectator: Spectator<DotEditContentSidebarRulesComponent>;
    let dotMessageService: SpyObject<DotMessageService>;
    let dialogOpenSpy: jest.Mock;
    let mockDialogRef: DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarRulesComponent,
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
        it('should render rules-card when identifier is set', () => {
            spectator.setInput('identifier', 'content-456');
            spectator.detectChanges();

            const card = spectator.query(byTestId('rules-card'));
            expect(card).toBeTruthy();
        });

        it('should have rules-card with role="button" and tabindex="0"', () => {
            const card = spectator.query(byTestId('rules-card'));
            expect(card?.getAttribute('role')).toBe('button');
            expect(card?.getAttribute('tabindex')).toBe('0');
        });
    });

    describe('openRulesDialog - Success', () => {
        it('should open rules dialog when card is clicked with valid identifier', () => {
            spectator.setInput('identifier', 'content-789');
            spectator.detectChanges();

            spectator.click(byTestId('rules-card'));

            expect(dialogOpenSpy).toHaveBeenCalledWith(DotRulesDialogComponent, {
                header: 'edit.content.sidebar.rules.title',
                width: 'min(92vw, 75rem)',
                data: { identifier: 'content-789' },
                modal: true,
                appendTo: 'body',
                closeOnEscape: false,
                closable: true,
                draggable: false,
                keepInViewport: false,
                resizable: false,
                position: 'center'
            });
        });

        it('should call DotMessageService.get for header when opening dialog', () => {
            spectator.component.openRulesDialog();

            expect(dotMessageService.get).toHaveBeenCalledWith('edit.content.sidebar.rules.title');
        });
    });

    describe('openRulesDialog - Failure and Edge Cases', () => {
        it('should NOT open dialog when identifier is empty string', () => {
            spectator.setInput('identifier', '');
            spectator.detectChanges();

            spectator.component.openRulesDialog();

            expect(dialogOpenSpy).not.toHaveBeenCalled();
        });

        it('should NOT open dialog when identifier is undefined', () => {
            spectator.setInput('identifier', undefined as never);
            spectator.detectChanges();

            spectator.component.openRulesDialog();

            expect(dialogOpenSpy).not.toHaveBeenCalled();
        });

        it('should NOT open a second dialog when one is already open', () => {
            spectator.setInput('identifier', 'content-123');
            spectator.detectChanges();

            spectator.component.openRulesDialog();
            spectator.component.openRulesDialog();

            expect(dialogOpenSpy).toHaveBeenCalledTimes(1);
        });
    });

    describe('Keyboard and accessibility', () => {
        it('should open dialog on Enter key on rules-card', () => {
            spectator.setInput('identifier', 'k1');
            spectator.detectChanges();

            spectator.dispatchKeyboardEvent(byTestId('rules-card'), 'keydown', 'Enter');

            expect(dialogOpenSpy).toHaveBeenCalled();
        });

        it('should open dialog on Space key on rules-card', () => {
            spectator.setInput('identifier', 'k2');
            spectator.detectChanges();

            spectator.dispatchKeyboardEvent(byTestId('rules-card'), 'keydown', ' ');
            spectator.detectChanges();

            expect(dialogOpenSpy).toHaveBeenCalled();
        });
    });

    describe('ngOnDestroy', () => {
        it('should close dialog ref on destroy when dialog was opened', () => {
            spectator.component.openRulesDialog();

            spectator.fixture.destroy();

            expect(mockDialogRef.close).toHaveBeenCalled();
        });

        it('should not throw when destroy and dialog was never opened', () => {
            expect(() => spectator.fixture.destroy()).not.toThrow();
        });
    });
});

import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { Subject } from 'rxjs';

import { MenuItem } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotPermissionsIframeDialogComponent } from '@dotcms/ui';

import {
    CONTENTLET_PERMISSIONS_IFRAME_PATH,
    DotEditContentCommandBarActionsComponent
} from './dot-edit-content-command-bar-actions.component';

import { DotEditContentSidebarReferencesDialogComponent } from '../../../dot-edit-content-sidebar/components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-references-dialog/dot-edit-content-sidebar-references-dialog.component';
import { DotRulesDialogComponent } from '../../../dot-edit-content-sidebar/components/dot-edit-content-sidebar-rules/components/rules-dialog/rules-dialog.component';

const findItem = (model: MenuItem[], testId: string): MenuItem | undefined =>
    model.find((item) => item.testId === testId);

describe('DotEditContentCommandBarActionsComponent', () => {
    let spectator: Spectator<DotEditContentCommandBarActionsComponent>;
    let dotMessageService: SpyObject<DotMessageService>;
    let dialogOpenSpy: jest.Mock;
    let mockDialogRef: DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotEditContentCommandBarActionsComponent,
        providers: [
            mockProvider(DotMessageService, {
                get: jest.fn((key: string) => key)
            })
        ],
        // DialogService is provided at the component node (providers in the component
        // decorator), so the mock must be supplied via componentProviders to override it.
        // The open mock delegates to the per-test dialogOpenSpy so each test gets a fresh spy.
        componentProviders: [
            {
                provide: DialogService,
                useValue: { open: (...args: unknown[]) => dialogOpenSpy(...args) }
            }
        ]
    });

    beforeEach(() => {
        mockDialogRef = {
            onClose: new Subject<void>(),
            close: jest.fn()
        } as unknown as DynamicDialogRef;
        dialogOpenSpy = jest.fn().mockReturnValue(mockDialogRef);

        spectator = createComponent({
            props: {
                identifier: 'content-123',
                languageId: 123
            }
        });

        dotMessageService = spectator.inject(DotMessageService);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Trigger button', () => {
        it('should render the overflow trigger button', () => {
            expect(spectator.query(byTestId('command-bar-actions-button'))).toBeTruthy();
        });
    });

    describe('Menu model', () => {
        it('should always include the Permissions action', () => {
            const item = findItem(spectator.component.$model(), 'command-bar-action-permissions');
            expect(item).toBeTruthy();
            expect(item?.label).toBe('edit.content.sidebar.permissions.title');
        });

        it('should NOT include the Rules action when isPage is false', () => {
            spectator.setInput('isPage', false);
            spectator.detectChanges();

            expect(
                findItem(spectator.component.$model(), 'command-bar-action-rules')
            ).toBeUndefined();
        });

        it('should include the Rules action only when isPage is true', () => {
            spectator.setInput('isPage', true);
            spectator.detectChanges();

            const item = findItem(spectator.component.$model(), 'command-bar-action-rules');
            expect(item).toBeTruthy();
            expect(item?.label).toBe('edit.content.sidebar.rules.title');
        });

        it('should include a separator', () => {
            expect(spectator.component.$model().some((item) => item.separator === true)).toBe(true);
        });

        it('should include the References action', () => {
            const item = findItem(spectator.component.$model(), 'command-bar-action-references');
            expect(item).toBeTruthy();
            expect(item?.label).toBe('edit.content.sidebar.command-bar.references');
        });

        it('should disable the References action when hasReferences is false', () => {
            spectator.setInput('hasReferences', false);
            spectator.detectChanges();

            const item = findItem(spectator.component.$model(), 'command-bar-action-references');
            expect(item?.disabled).toBe(true);
        });

        it('should enable the References action when hasReferences is true', () => {
            spectator.setInput('hasReferences', true);
            spectator.detectChanges();

            const item = findItem(spectator.component.$model(), 'command-bar-action-references');
            expect(item?.disabled).toBe(false);
        });
    });

    describe('openPermissionsDialog', () => {
        it('should open the permissions dialog with DotPermissionsIframeDialogComponent', () => {
            spectator.setInput('identifier', 'content-789');
            spectator.setInput('languageId', 2);
            spectator.detectChanges();

            findItem(spectator.component.$model(), 'command-bar-action-permissions')?.command?.(
                {} as never
            );

            expect(dialogOpenSpy).toHaveBeenCalledWith(
                DotPermissionsIframeDialogComponent,
                expect.objectContaining({
                    header: 'edit.content.sidebar.permissions.title',
                    width: 'min(92vw, 75rem)',
                    contentStyle: { overflow: 'hidden' },
                    modal: true,
                    appendTo: 'body',
                    closeOnEscape: true,
                    closable: true
                })
            );
        });

        it('should build the url with contentletId, languageId and popup', () => {
            spectator.setInput('identifier', 'content-789');
            spectator.setInput('languageId', 2);
            spectator.detectChanges();

            spectator.component.openPermissionsDialog();

            const callData = dialogOpenSpy.mock.calls[0][1].data;
            expect(callData.url).toContain(CONTENTLET_PERMISSIONS_IFRAME_PATH);
            expect(callData.url).toContain('contentletId=content-789');
            expect(callData.url).toContain('languageId=2');
            expect(callData.url).toContain('popup=true');
        });

        it('should NOT open the dialog when identifier is empty', () => {
            spectator.setInput('identifier', '');
            spectator.setInput('languageId', 1);
            spectator.detectChanges();

            spectator.component.openPermissionsDialog();

            expect(dialogOpenSpy).not.toHaveBeenCalled();
        });

        it('should NOT open the dialog when languageId is 0', () => {
            spectator.setInput('identifier', 'content-ok');
            spectator.setInput('languageId', 0);
            spectator.detectChanges();

            spectator.component.openPermissionsDialog();

            expect(dialogOpenSpy).not.toHaveBeenCalled();
        });

        it('should NOT open a second permissions dialog while one is already open', () => {
            spectator.component.openPermissionsDialog();
            spectator.component.openPermissionsDialog();

            expect(dialogOpenSpy).toHaveBeenCalledTimes(1);
        });
    });

    describe('openRulesDialog', () => {
        it('should open the rules dialog with DotRulesDialogComponent', () => {
            spectator.setInput('identifier', 'page-1');
            spectator.detectChanges();

            spectator.component.openRulesDialog();

            expect(dialogOpenSpy).toHaveBeenCalledWith(
                DotRulesDialogComponent,
                expect.objectContaining({
                    header: 'edit.content.sidebar.rules.title',
                    width: 'min(92vw, 75rem)',
                    data: { identifier: 'page-1' },
                    modal: true,
                    appendTo: 'body'
                })
            );
        });

        it('should NOT open the dialog when identifier is empty', () => {
            spectator.setInput('identifier', '');
            spectator.detectChanges();

            spectator.component.openRulesDialog();

            expect(dialogOpenSpy).not.toHaveBeenCalled();
        });
    });

    describe('openReferencesDialog', () => {
        it('should open the references dialog with DotEditContentSidebarReferencesDialogComponent', () => {
            spectator.setInput('identifier', 'ref-1');
            spectator.setInput('title', 'My Content');
            spectator.detectChanges();

            spectator.component.openReferencesDialog();

            expect(dialogOpenSpy).toHaveBeenCalledWith(
                DotEditContentSidebarReferencesDialogComponent,
                expect.objectContaining({
                    data: { identifier: 'ref-1' },
                    modal: true,
                    appendTo: 'body',
                    closeOnEscape: true,
                    closable: true
                })
            );
        });

        it('should use the title input for the references dialog header', () => {
            spectator.setInput('identifier', 'ref-1');
            spectator.setInput('title', 'My Content');
            spectator.detectChanges();

            spectator.component.openReferencesDialog();

            expect(dotMessageService.get).toHaveBeenCalledWith(
                'edit.content.sidebar.references.dialog.title',
                'My Content'
            );
        });

        it('should fall back to the contentlet title when the title input is empty', () => {
            spectator.setInput('identifier', 'ref-1');
            spectator.setInput('title', '');
            spectator.setInput('contentlet', { title: 'Fallback Title' } as never);
            spectator.detectChanges();

            spectator.component.openReferencesDialog();

            expect(dotMessageService.get).toHaveBeenCalledWith(
                'edit.content.sidebar.references.dialog.title',
                'Fallback Title'
            );
        });

        it('should NOT open the dialog when identifier is empty', () => {
            spectator.setInput('identifier', '');
            spectator.detectChanges();

            spectator.component.openReferencesDialog();

            expect(dialogOpenSpy).not.toHaveBeenCalled();
        });
    });

    describe('destroy', () => {
        it('should close any open dialog on destroy', () => {
            spectator.component.openPermissionsDialog();

            spectator.fixture.destroy();

            expect(mockDialogRef.close).toHaveBeenCalled();
        });

        it('should not throw when destroyed and no dialog was opened', () => {
            expect(() => spectator.fixture.destroy()).not.toThrow();
        });
    });
});

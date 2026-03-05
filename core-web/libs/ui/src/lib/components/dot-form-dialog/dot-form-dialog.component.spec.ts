import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFormDialogComponent } from './dot-form-dialog.component';

function dispatchKeydown(element: HTMLElement, key: string, meta = false, alt = false): void {
    const event = new KeyboardEvent('keydown', {
        key,
        code: key,
        metaKey: meta,
        altKey: alt
    });
    element.dispatchEvent(event);
}

describe('DotFormDialogComponent', () => {
    let spectator: SpectatorHost<DotFormDialogComponent>;
    let dynamicDialogRef: DynamicDialogRef;

    const createHost = createHostFactory({
        component: DotFormDialogComponent,
        template:
            '<dot-form-dialog [saveButtonDisabled]="false" [saveButtonLoading]="false"><form>Hello World</form></dot-form-dialog>',
        imports: [ButtonModule, DotFormDialogComponent],
        providers: [
            {
                provide: DynamicDialogRef,
                useValue: { close: jest.fn() }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ save: 'Save', cancel: 'Cancel' })
            }
        ]
    });

    function getDialogElement(): HTMLElement {
        const el = spectator.fixture.debugElement.query(By.directive(DotFormDialogComponent));
        return el?.nativeElement ?? spectator.element;
    }

    beforeEach(() => {
        jest.spyOn(document, 'querySelector').mockReturnValue(document.createElement('div'));
        spectator = createHost();
        dynamicDialogRef = spectator.inject(DynamicDialogRef);
        spectator.detectChanges();
    });

    it('should bind event to dialog content', () => {
        expect(document.querySelector).toHaveBeenCalledWith('p-dynamicdialog .p-dialog-content');
    });

    it('should project ng-content', () => {
        const content = spectator.query('form');
        expect(content?.textContent?.trim()).toBe('Hello World');
    });

    describe('buttons', () => {
        beforeEach(() => {
            jest.spyOn(spectator.component.save, 'emit');
            jest.spyOn(spectator.component.cancel, 'emit');
        });

        it('should have save button', () => {
            const saveButton = spectator.query(byTestId('dotFormDialogSave'));
            expect(saveButton).toBeTruthy();
            expect(saveButton?.textContent?.trim()).toBe('Save');
        });

        it('should emit save event when save button is clicked', () => {
            const saveButton = spectator.query(byTestId('dotFormDialogSave'));
            const event = new MouseEvent('click');
            saveButton?.dispatchEvent(event);
            spectator.detectChanges();

            expect(spectator.component.save.emit).toHaveBeenCalledWith(event);
        });

        it('should emit save event on CMD + ENTER keys', () => {
            dispatchKeydown(getDialogElement(), 'Enter', true);

            expect(spectator.component.save.emit).toHaveBeenCalledTimes(1);
        });

        it('should not emit save event when status Loading', () => {
            spectator.component.saveButtonLoading = true;
            dispatchKeydown(getDialogElement(), 'Enter', true);
            expect(spectator.component.save.emit).not.toHaveBeenCalled();

            const saveButton = spectator.query(byTestId('dotFormDialogSave'));
            saveButton?.dispatchEvent(new MouseEvent('click'));
            expect(spectator.component.save.emit).not.toHaveBeenCalled();
        });

        it('should have cancel button', () => {
            const cancelButton = spectator.query(byTestId('dotFormDialogCancel'));
            expect(cancelButton).toBeTruthy();
            expect(cancelButton?.textContent?.trim()).toBe('Cancel');
        });

        it('should emit cancel event and close dialog when cancel button is clicked', () => {
            const cancelButton = spectator.query(byTestId('dotFormDialogCancel'));
            const event = new MouseEvent('click');
            cancelButton?.dispatchEvent(event);
            spectator.detectChanges();

            expect(spectator.component.cancel.emit).toHaveBeenCalledWith(event);
            expect(dynamicDialogRef.close).toHaveBeenCalledTimes(1);
        });
    });
});

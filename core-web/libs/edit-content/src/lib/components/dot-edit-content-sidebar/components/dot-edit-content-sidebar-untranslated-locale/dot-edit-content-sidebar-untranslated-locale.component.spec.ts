import { Spectator, createComponentFactory, byTestId } from '@ngneat/spectator/jest';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ButtonDirective } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { RadioButtonModule } from 'primeng/radiobutton';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentSidebarUntranslatedLocaleComponent } from './dot-edit-content-sidebar-untranslated-locale.component';

const messageServiceMock = new MockDotMessageService({
    'edit.content.sidebar.locales.untranslated.populate': 'Populate from Current Locale'
});

describe('DotEditContentSidebarUntranslatedLocaleComponent', () => {
    let spectator: Spectator<DotEditContentSidebarUntranslatedLocaleComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentSidebarUntranslatedLocaleComponent,
        imports: [CommonModule, RadioButtonModule, FormsModule, ButtonDirective, DotMessagePipe],
        providers: [
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
            {
                provide: DynamicDialogConfig,
                useValue: { data: { currentLocale: { isoCode: 'en-us' } } }
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should close dialog with selected option on continue button click', () => {
        const continueButton = spectator.query(byTestId('continue-button'));
        const dialogRefCloseSpy = jest.spyOn(spectator.component.dialogRef, 'close');

        spectator.click(continueButton);

        expect(dialogRefCloseSpy).toHaveBeenCalledWith(spectator.component.selectedOption);
    });

    it('should close dialog on cancel button click', () => {
        const cancelButton = spectator.query(byTestId('cancel-button'));
        const dialogRefCloseSpy = jest.spyOn(spectator.component.dialogRef, 'close');

        spectator.click(cancelButton);

        expect(dialogRefCloseSpy).toHaveBeenCalledWith();
    });

    it('should display the correct label for the populate radio button', () => {
        expect(spectator.query(byTestId('populate-label')).textContent).toContain(
            'Populate from Current Locale en-US'
        );
    });
});

import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotEditContentDialogHeaderComponent } from './dot-edit-content-dialog-header.component';

@Component({ selector: 'dot-test-dialog-body', template: '<p>body</p>' })
class BodyComponent {}

/**
 * The header against the real PrimeNG dynamic dialog, not a stand-in container: the dialog keeps its
 * `aria-labelledby` pointing at the title it no longer prints once a header template is given, and
 * the header has to answer to that id.
 */
describe('DotEditContentDialogHeaderComponent in a PrimeNG dynamic dialog', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DialogService,
                { provide: DotMessageService, useValue: { get: (key: string) => key } }
            ]
        });
    });

    afterEach(() => document.querySelectorAll('.p-dialog-mask').forEach((el) => el.remove()));

    it('should leave the dialog labelled by a title that exists', async () => {
        TestBed.inject(DialogService).open(BodyComponent, {
            header: 'My Blog Post',
            templates: { header: DotEditContentDialogHeaderComponent }
        });
        await new Promise((resolve) => setTimeout(resolve, 50));
        TestBed.tick();
        await new Promise((resolve) => setTimeout(resolve, 50));
        TestBed.tick();

        const dialog = document.querySelector('[role="dialog"]') as HTMLElement;
        const labelledBy = dialog.getAttribute('aria-labelledby') as string;
        const title = document.getElementById(labelledBy);

        expect(labelledBy).toBeTruthy();
        expect(title?.textContent?.trim()).toBe('My Blog Post');
    });
});

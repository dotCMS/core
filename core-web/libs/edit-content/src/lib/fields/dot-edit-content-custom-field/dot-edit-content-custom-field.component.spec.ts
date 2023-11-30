import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotContentTypeService, DotWorkflowActionsFireService } from '@dotcms/data-access';

import { DotEditContentCustomFieldComponent } from './dot-edit-content-custom-field.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { CUSTOM_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

describe('DotEditContentCustomFieldComponent', () => {
    let spectator: Spectator<DotEditContentCustomFieldComponent>;

    const FAKE_FORM_GROUP = new FormGroup({
        custom: new FormControl()
    });

    const createComponent = createComponentFactory({
        component: DotEditContentCustomFieldComponent,
        detectChanges: false,
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock(FAKE_FORM_GROUP) }
        ],
        providers: [
            FormGroupDirective,
            {
                provide: DotEditContentService,
                useValue: {
                    currentContentType: 'test'
                }
            },
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should have a valid iframe src', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        spectator.detectChanges();
        expect(spectator.component.src).toBe(
            `/html/legacy_custom_field/legacy-custom-field.jsp?variable=test&field=${CUSTOM_FIELD_MOCK.variable}`
        );
    });

    it('should set the iframe contentWindow form property correctly on iframe load', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        spectator.component.onIframeLoad();
        spectator.detectChanges();
        expect(spectator.component.iframe.nativeElement.contentWindow['form']).toEqual(
            FAKE_FORM_GROUP
        );
    });

    it('should the component receive iframe turnOnFullscreen info', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        spectator.detectChanges();
        const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));
        const onMessageFromCustomField = jest.spyOn(
            spectator.component,
            'onMessageFromCustomField'
        );

        iframe.nativeElement.contentWindow.parent.dispatchEvent(
            new MessageEvent('message', {
                origin: 'http://localhost:3000',
                data: { type: 'toggleFullscreen' }
            })
        );

        expect(onMessageFromCustomField).toHaveBeenCalled();
        expect(spectator.component.isFullscreen).toBe(true);
    });

    it('should the iframe get the form reference from component', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        spectator.component.onIframeLoad();
        spectator.detectChanges();
        spectator.component.form.get('custom').setValue('A text');

        const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));
        expect(iframe.nativeElement.contentWindow['form'].get('custom').value).toBe('A text');
    });

    it('should the component form be modified from iframe', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        spectator.component.onIframeLoad();
        spectator.detectChanges();

        const iframe = spectator.debugElement.query(By.css('[data-testId="iframe"]'));
        iframe.nativeElement.contentWindow['form'].get('custom').setValue('Other text');

        expect(spectator.component.form.get('custom').value).toBe('Other text');
    });
});

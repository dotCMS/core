import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { DotEditContentCustomFieldComponent } from './dot-edit-content-custom-field.component';

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
        providers: [FormGroupDirective],
        componentProviders: [
            { provide: ActivatedRoute, useValue: { snapshot: { params: { contentType: 'test' } } } }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { params: { contentType: 'test' } } }
                }
            ]
        });
    });

    it('should have a valid iframe src', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        spectator.detectChanges();
        expect(spectator.component.src).toBe(
            `/html/legacy_custom_field/legacy-custom-field.jsp?variable=test&field=${CUSTOM_FIELD_MOCK.variable}`
        );
    });
    it('should set the contentType property correctly', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        spectator.detectChanges();
        expect(spectator.component.contentType).toBe('test');
    });

    it('should set the iframe contentWindow form property correctly on iframe load', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        spectator.component.onIframeLoad();
        spectator.detectChanges();
        expect(spectator.component.iframe.nativeElement.contentWindow['form']).toEqual(
            FAKE_FORM_GROUP
        );
    });
});

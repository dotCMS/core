import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { ElementRef, Sanitizer } from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { DotEditContentCustomFieldComponent } from './dot-edit-content-custom-field.component';

import { CUSTOM_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

describe('DotEditContentCustomFieldComponent', () => {
    let spectator: Spectator<DotEditContentCustomFieldComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentCustomFieldComponent,
        detectChanges: false,
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ],
        providers: [FormGroupDirective, Sanitizer],
        componentProviders: [
            { provide: ActivatedRoute, useValue: { snapshot: { params: { contentType: 'test' } } } }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({});
    });

    it('should have a valid iframe src', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        spectator.detectChanges();
        expect(spectator.component.src).toBe(
            `/html/legacy_custom_field/legacy-custom-field.jsp?variable=Test&field=${CUSTOM_FIELD_MOCK.variable}`
        );
    });

    // it('should have a valid iframe src', () => {
    //     spectator.setInput('field', CUSTOM_FIELD_MOCK);
    //     spectator.detectChanges();
    //     const expectedSrc = spectator.component.sanitizer.bypassSecurityTrustResourceUrl(
    //         `/html/legacy_custom_field/legacy-custom-field.jsp?variable=test&field=${CUSTOM_FIELD_MOCK.variable}`
    //     );
    //     expect(spectator.component.src).toEqual(expectedSrc);
    // });

    it('should set the contentType property correctly', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        spectator.detectChanges();
        expect(spectator.component.contentType).toBe('test');
    });

    it('should set the iframe contentWindow form property correctly on iframe load', () => {
        spectator.setInput('field', CUSTOM_FIELD_MOCK);
        const mockContentWindow = {
            form: null
        };
        spectator.component.iframe = {
            nativeElement: {
                contentWindow: mockContentWindow
            }
        } as ElementRef;
        spectator.component.onIframeLoad();
        spectator.detectChanges();
        expect(mockContentWindow.form).toBe(spectator.component.form);
    });
});

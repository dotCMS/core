/* eslint-disable @typescript-eslint/no-explicit-any */

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, forwardRef, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    ControlValueAccessor,
    FormsModule,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotMessageService, DotEventsService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { MockDotMessageService, CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotTemplateAdvancedComponent } from './dot-template-advanced.component';

@Component({
    selector: 'dot-portlet-base',
    template: '<ng-content></ng-content>',
    standalone: false
})
export class DotPortletBaseMockComponent {}

@Component({
    selector: 'dot-portlet-toolbar',
    template: '<ng-content></ng-content>',
    standalone: false
})
export class DotPortletToolbarMockComponent {
    @Input() actions;
}

@Component({
    selector: 'dot-global-message',
    template: '',
    standalone: false
})
class MockDotGlobalMessageComponent {}

@Component({
    selector: 'dot-container-selector',
    template: '',
    standalone: false
})
export class DotContainerSelectorMockComponent {
    @Output() swap = new EventEmitter<any>();
}

@Component({
    selector: 'dot-textarea-content',
    template: '',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTextareaContentMockComponent)
        }
    ],
    standalone: false
})
export class DotTextareaContentMockComponent implements ControlValueAccessor {
    @Input()
    code;

    @Input()
    height;

    @Input()
    show;

    @Input()
    value;

    @Input()
    width;

    @Output()
    monacoInit = new EventEmitter<any>();

    @Input()
    language;

    writeValue() {
        //
    }
    registerOnChange() {
        //
    }
    registerOnTouched() {
        //
    }
}

const messageServiceMock = new MockDotMessageService({
    save: 'Save'
});

describe('DotTemplateAdvancedComponent', () => {
    let fixture: ComponentFixture<DotTemplateAdvancedComponent>;
    let de: DebugElement;
    let component: DotTemplateAdvancedComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotTemplateAdvancedComponent, FormsModule, ReactiveFormsModule],
            declarations: [
                DotPortletBaseMockComponent,
                DotPortletToolbarMockComponent,
                DotContainerSelectorMockComponent,
                DotTextareaContentMockComponent,
                MockDotGlobalMessageComponent
            ],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                },
                DotEventsService,
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplateAdvancedComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        fixture.detectChanges();

        const code = de.query(By.css('dot-textarea-content'));
        code.triggerEventHandler('monacoInit', {
            name: 'testEditor',
            editor: { executeEdits: jest.fn(), getSelection: () => 100 }
        });
    });

    describe('HTML', () => {
        it('should have portlet base', () => {
            const portlet = de.query(By.css('dot-portlet-base'));
            const toolbar = portlet.query(By.css('dot-portlet-toolbar'));
            const globalMessage = portlet.query(By.css('dot-global-message[right]'));

            expect(portlet).not.toBeNull();
            expect(toolbar).not.toBeNull();
            expect(globalMessage).not.toBeNull();
            expect(toolbar.componentInstance.actions).toEqual({
                primary: [
                    {
                        label: 'Save',
                        disabled: true,
                        command: expect.any(Function)
                    }
                ],
                cancel: expect.any(Function)
            });
        });

        it('should have form and fields', () => {
            const form = de.query(By.css('form'));

            expect(form).not.toBeNull();
            const container = de.query(By.css('dot-container-selector'));
            const code = de.query(By.css('dot-textarea-content'));

            expect(container).not.toBeNull();
            expect(container.attributes.class).toBeUndefined();

            expect(code).not.toBeNull();
            expect(code.attributes.formControlName).toBe('body');
            expect(code.attributes.height).toBe('100%');
            expect(code.attributes.language).toBe('html');
            const codeComponent = code.componentInstance as DotTextareaContentMockComponent;
            expect(codeComponent.show).toEqual(['code']);
        });
    });

    describe('events', () => {
        it('should emit updateTemplate event when the form changes', () => {
            const updateTemplate = jest.spyOn(component.updateTemplate, 'emit');
            component.form.get('body').setValue('<body></body>');

            expect<any>(updateTemplate).toHaveBeenCalledWith({ body: '<body></body>' });
        });

        it('should have form and fields', () => {
            jest.spyOn(Date, 'now').mockReturnValue(1111111);
            const container = de.query(By.css('dot-container-selector'));

            container.triggerEventHandler('swap', {
                identifier: '123',
                hostName: 'demo.com'
            });

            expect(component.editor.executeEdits).toHaveBeenCalledWith('source', [
                {
                    range: 100,
                    // Changing the spacing and new lines in this string breaks the test
                    text: `## Container: undefined
## This is autogenerated code that cannot be changed
#parseContainer('123','1111111')
`,
                    forceMoveMarkers: true
                }
            ]);
        });
    });
});

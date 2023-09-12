/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, DebugElement, forwardRef, Input, OnInit } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {
    ControlValueAccessor,
    FormsModule,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotLayout } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { mockDotLayout } from '@dotcms/utils-testing';

import { DotLayoutDesignerComponent } from './dot-layout-designer.component';

@Component({
    selector: 'dot-edit-layout-grid',
    template: '',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MockDotEditLayoutGridComponent)
        }
    ]
})
export class MockDotEditLayoutGridComponent implements ControlValueAccessor {
    propagateChange = (_: any) => {};

    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

    writeValue(): void {}
}

@Component({
    selector: 'dot-edit-layout-sidebar',
    template: '',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MockDotEditLayoutSidebarComponent)
        }
    ]
})
export class MockDotEditLayoutSidebarComponent implements ControlValueAccessor {
    propagateChange = (_: any) => {};

    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

    writeValue(): void {}
}

@Component({
    template: `
        <form [formGroup]="form">
            <dot-layout-designer formGroupName="layout"></dot-layout-designer>
        </form>
    `
})
class TestHostComponent implements OnInit {
    @Input()
    layout: DotLayout;

    form: UntypedFormGroup;

    constructor(private fb: UntypedFormBuilder) {}

    ngOnInit() {
        this.form = this.fb.group({
            layout: this.fb.group(this.layout)
        });
    }
}

describe('DotLayoutDesignerComponent', () => {
    let hostFixture: ComponentFixture<TestHostComponent>;
    let hostComponent: TestHostComponent;
    let hostDe: DebugElement;
    let component: DotLayoutDesignerComponent;
    let de: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [
                TestHostComponent,
                DotLayoutDesignerComponent,
                MockDotEditLayoutGridComponent,
                MockDotEditLayoutSidebarComponent
            ],
            imports: [DotMessagePipe, FormsModule, ReactiveFormsModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: {
                        get(value) {
                            const map = {
                                'editpage.layout.designer.header': 'HEADER',
                                'editpage.layout.designer.footer': 'FOOTER'
                            };

                            return map[value];
                        }
                    }
                }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = TestBed.createComponent(TestHostComponent);
        hostComponent = hostFixture.componentInstance;
        hostDe = hostFixture.debugElement;

        de = hostDe.query(By.css('dot-layout-designer'));
        component = de.componentInstance;
    });

    describe('default', () => {
        beforeEach(() => {
            hostComponent.layout = {
                ...mockDotLayout(),
                sidebar: {
                    location: '',
                    containers: [],
                    width: 'small'
                }
            };
            hostFixture.detectChanges();
        });

        it('should NOT show header in the template', () => {
            const headerElem: DebugElement = de.query(By.css('.dot-layout-designer__header'));
            expect(headerElem).toBe(null);
        });

        it('should NOT show footer in the template', () => {
            const footerElem: DebugElement = de.query(By.css('.dot-layout-designer__footer'));
            expect(footerElem).toBe(null);
        });

        it('should NOT show a sidebar', () => {
            const sidebar: DebugElement = de.query(
                By.css('[class^="dot-layout-designer__sidebar"]')
            );
            expect(sidebar).toBe(null);
        });

        describe('dot-edit-layout-grid', () => {
            let gridLayout: DebugElement;

            beforeEach(() => {
                gridLayout = de.query(By.css('dot-edit-layout-grid'));
            });

            it('should show dot-edit-layout-grid', () => {
                expect(gridLayout).toBeDefined();
            });

            it('should pass body as form control', () => {
                expect(gridLayout.attributes.formControlName).toBe('body');
            });
        });
    });

    describe('filled', () => {
        describe('header and footer', () => {
            beforeEach(() => {
                hostComponent.layout = {
                    width: '',
                    title: '',
                    header: true,
                    footer: true,
                    body: mockDotLayout().body,
                    sidebar: {
                        location: '',
                        containers: [],
                        width: 'small'
                    }
                };
                hostFixture.detectChanges();
            });

            it('should show header in the template', () => {
                const headerElem: DebugElement = de.query(By.css('.dot-layout-designer__header'));
                expect(headerElem).toBeTruthy();
            });

            it('should show footer in the template', () => {
                const footerElem: DebugElement = de.query(By.css('.dot-layout-designer__footer'));
                expect(footerElem).toBeTruthy();
            });

            it('should have the right label for the Header', () => {
                const headerSelector = de.query(By.css('.dot-layout-designer__header'));
                expect(headerSelector.nativeElement.outerText).toBe('HEADER');
            });

            it('should have the right label for the Footer', () => {
                const headerSelector = de.query(By.css('.dot-layout-designer__footer'));
                expect(headerSelector.nativeElement.outerText).toBe('FOOTER');
            });
        });

        describe('sidebar size and position', () => {
            beforeEach(() => {
                hostComponent.layout = mockDotLayout();
                hostFixture.detectChanges();
            });

            it('should show', () => {
                const sidebar: DebugElement = de.query(
                    By.css('.dot-layout-designer__sidebar--left')
                );
                expect(sidebar).toBeTruthy();
            });

            it('should show sidebar position correctly', () => {
                const positions = ['left', 'right'];
                positions.forEach((position) => {
                    component.group.control.get('sidebar').value.location = position;
                    hostFixture.detectChanges();
                    const sidebar: DebugElement = de.query(
                        By.css(`.dot-layout-designer__sidebar--${position}`)
                    );
                    expect(sidebar).toBeTruthy(position);
                });
            });

            it('it should set sidebar size correctly', () => {
                const sizes = ['small', 'medium', 'large'];

                sizes.forEach((size) => {
                    component.group.control.get('sidebar').value.width = size;
                    hostFixture.detectChanges();
                    const sidebar: DebugElement = de.query(
                        By.css(`.dot-layout-designer__sidebar--${size}`)
                    );
                    expect(sidebar).toBeDefined();
                });
            });
        });
    });
});

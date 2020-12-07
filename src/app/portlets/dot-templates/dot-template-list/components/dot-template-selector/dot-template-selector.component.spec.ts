import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotTemplateSelectorComponent } from './dot-template-selector.component';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';

const messageServiceMock = new MockDotMessageService({
    next: 'Next',
    'templates.template.selector.label.designer': 'Designer',
    'templates.template.selector.label.advanced': 'Advanced',
    'templates.template.selector.design':
        '<b>Template Designer</b> allows you to create templates seamlessly with a set of tools lorem ipsum.',
    'templates.template.selector.advanced':
        '<b>Template Advanced</b> allows you to create templates using HTML code'
});

describe('DotTemplateSelectorComponent', () => {
    let fixture: ComponentFixture<DotTemplateSelectorComponent>;
    let de: DebugElement;
    let dynamicDialogRef: DynamicDialogRef;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTemplateSelectorComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: jasmine.createSpy()
                    }
                }
            ],
            imports: [
                DotMessagePipeModule,
                DotIconModule,
                ReactiveFormsModule,
                FormsModule,
                ButtonModule
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplateSelectorComponent);
        de = fixture.debugElement;
        dynamicDialogRef = TestBed.inject(DynamicDialogRef);
        fixture.detectChanges();
    });

    describe('design', () => {
        let design: DebugElement;

        beforeEach(() => {
            design = de.query(By.css('.wrapper .options [data-testId="designer"]'));
        });

        it('should have radio button', () => {
            const radio = design.query(By.css('input[type="radio"]'));

            const { type, id, name, value } = radio.attributes;
            expect<any>({ type, id, name, value }).toEqual({
                type: 'radio',
                id: 'designer',
                name: 'template',
                value: 'designer'
            });

            expect(radio.attributes['ng-reflect-model']).toEqual('designer');
        });

        it('should have icon', () => {
            const icon = design.query(By.css('dot-icon'));

            expect(icon.attributes.size).toEqual('100');
            expect(icon.attributes.name).toEqual('web');
        });

        it('should have label', () => {
            const label = design.query(By.css('span'));
            expect(label.nativeElement.innerText).toBe('Designer');
        });
    });

    describe('advanced', () => {
        let advanced: DebugElement;

        beforeEach(() => {
            advanced = de.query(By.css('.wrapper .options [data-testId="advanced"]'));
        });

        it('should have radio button', () => {
            const radio = advanced.query(By.css('input[type="radio"]'));

            const { type, id, name, value } = radio.attributes;
            expect<any>({ type, id, name, value }).toEqual({
                type: 'radio',
                id: 'advanced',
                name: 'template',
                value: 'advanced'
            });

            expect(radio.attributes['ng-reflect-model']).toEqual('designer');
        });

        it('should have icon', () => {
            const icon = advanced.query(By.css('dot-icon'));

            expect(icon.attributes.size).toEqual('100');
            expect(icon.attributes.name).toEqual('settings_applications');
        });

        it('should have label', () => {
            const label = advanced.query(By.css('span'));
            expect(label.nativeElement.innerText).toBe('Advanced');
        });
    });

    describe('description', () => {
        it('should have innerText', () => {
            const description = de.query(By.css('.wrapper [data-testId="description"]'));
            expect(description.nativeElement.innerText).toBe(
                'Template Designer allows you to create templates seamlessly with a set of tools lorem ipsum.'
            );

            const advanced = de.query(
                By.css('.wrapper .options [data-testId="advanced"] input[type="radio"]')
            );

            advanced.nativeElement.click();
            fixture.detectChanges();

            expect(description.nativeElement.innerText).toBe(
                'Template Advanced allows you to create templates using HTML code'
            );
        });
    });

    describe('button', () => {
        it('it should close the dialog', () => {
            const button = de.query(By.css('[data-testId="button"]'));
            button.triggerEventHandler('click', {});

            const advanced = de.query(
                By.css('.wrapper .options [data-testId="advanced"] input[type="radio"]')
            );

            advanced.nativeElement.click();
            fixture.detectChanges();
            button.triggerEventHandler('click', {});

            expect(dynamicDialogRef.close).toHaveBeenCalledTimes(2);
            expect(dynamicDialogRef.close).toHaveBeenCalledWith('designer');
            expect(dynamicDialogRef.close).toHaveBeenCalledWith('advanced');
        });
    });
});

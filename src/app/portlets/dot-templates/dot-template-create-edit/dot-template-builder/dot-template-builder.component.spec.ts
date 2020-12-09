import {
    AfterContentInit,
    Component,
    ContentChild,
    DebugElement,
    EventEmitter,
    Input,
    Output,
    TemplateRef
} from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotTemplateBuilderComponent } from './dot-template-builder.component';
import { By } from '@angular/platform-browser';
import { EMPTY_TEMPLATE_ADVANCED, EMPTY_TEMPLATE_DESIGN } from '../store/dot-template.store';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';

@Component({
    selector: 'dot-edit-layout-designer',
    template: ``
})
class DotEditLayoutDesignerMockComponent {
    @Input()
    theme: string;

    @Input()
    layout;

    @Output()
    cancel: EventEmitter<MouseEvent> = new EventEmitter();

    @Output()
    save: EventEmitter<Event> = new EventEmitter();
}

@Component({
    selector: 'dot-template-advanced',
    template: ``
})
class DotTemplateAdvancedMockComponent {
    @Input()
    url;
}

@Component({
    selector: 'dot-iframe',
    template: ''
})
export class IframeMockComponent {
    @Input() src: string;
}

@Component({
    selector: 'p-tabView',
    template: '<ng-content></ng-content>'
})
export class TabViewMockComponent {}

@Component({
    selector: 'p-tabPanel',
    template:
        '<ng-content></ng-content><ng-container *ngTemplateOutlet="contentTemplate"></ng-container>'
})
export class TabPanelMockComponent implements AfterContentInit {
    @Input() header: string;
    @ContentChild(TemplateRef) container;
    contentTemplate;

    ngAfterContentInit() {
        if (this.container.elementRef.nativeElement.textContent === 'container') {
            this.contentTemplate = this.container;
        }
    }
}

describe('DotTemplateBuilderComponent', () => {
    let component: DotTemplateBuilderComponent;
    let fixture: ComponentFixture<DotTemplateBuilderComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotTemplateBuilderComponent,
                DotEditLayoutDesignerMockComponent,
                DotTemplateAdvancedMockComponent,
                IframeMockComponent,
                TabViewMockComponent,
                TabPanelMockComponent
            ],
            imports: [DotMessagePipeModule, DotPortletBoxModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        design: 'Design',
                        code: 'Code'
                    })
                }
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplateBuilderComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;

        spyOn(component.save, 'emit');
        spyOn(component.cancel, 'emit');
    });

    describe('design', () => {
        beforeEach(() => {
            component.item = {
                ...EMPTY_TEMPLATE_DESIGN,
                theme: '123'
            };
            fixture.detectChanges();
        });

        it('should have tab title "Design"', () => {
            const panel = de.query(By.css('[data-testId="builder"]'));
            expect(panel.componentInstance.header).toBe('Design');
        });

        it('should show dot-edit-layout-designer and pass attr', () => {
            const builder = de.query(By.css('dot-edit-layout-designer')).componentInstance;
            expect(builder.theme).toBe('123');
            expect(builder.layout).toEqual({
                header: true,
                footer: true,
                body: {
                    rows: []
                },
                sidebar: null,
                title: '',
                width: null
            });
        });

        it('should not show <dot-template-advanced>', () => {
            const advanced = de.query(By.css('dot-template-advanced'));
            expect(advanced).toBeNull();
        });

        it('should emit events from dot-edit-layout-designer', () => {
            const builder = de.query(By.css('dot-edit-layout-designer'));

            builder.triggerEventHandler('save', EMPTY_TEMPLATE_DESIGN);
            builder.triggerEventHandler('cancel', {});

            expect(component.save.emit).toHaveBeenCalledWith(EMPTY_TEMPLATE_DESIGN);
            expect(component.cancel.emit).toHaveBeenCalledTimes(1);
        });
    });

    describe('advanced', () => {
        beforeEach(() => {
            component.item = EMPTY_TEMPLATE_ADVANCED;
            fixture.detectChanges();
        });

        it('should have tab title "Design"', () => {
            const panel = de.query(By.css('[data-testId="builder"]'));
            expect(panel.componentInstance.header).toBe('Code');
        });

        it('should not show <dot-edit-layout-designer>', () => {
            const designer = de.query(By.css('dot-edit-layout-designer'));
            expect(designer).toBeNull();
        });

        it('should emit events from dot-template-advanced', () => {
            const builder = de.query(By.css('dot-template-advanced'));

            builder.triggerEventHandler('save', EMPTY_TEMPLATE_ADVANCED);
            builder.triggerEventHandler('cancel', {});

            expect(component.save.emit).toHaveBeenCalledWith(EMPTY_TEMPLATE_ADVANCED);
            expect(component.cancel.emit).toHaveBeenCalledTimes(1);
        });
    });

    describe('permissions', () => {
        beforeEach(() => {
            component.item = {
                ...EMPTY_TEMPLATE_ADVANCED,
                identifier: '123'
            };
            fixture.detectChanges();
        });

        it('should set iframe url', () => {
            const permissions = de.query(By.css('[data-testId="permissionsIframe"]'));
            expect(permissions.componentInstance.src).toBe(
                '/html/templates/permissions.jsp?templateId=123&popup=true'
            );
        });
    });
});

import { of } from 'rxjs';

import {
    AfterContentInit,
    Component,
    ContentChild,
    DebugElement,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    TemplateRef,
    ViewChild
} from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';

import { DotGlobalMessageComponent } from '@components/_common/dot-global-message/dot-global-message.component';
import { IframeComponent } from '@components/_common/iframe/iframe-component';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';
import { DotShowHideFeatureDirective } from '@dotcms/app/shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { DotEventsService, DotMessageService, DotPropertiesService } from '@dotcms/data-access';
import { DotLayout, DotTemplateDesigner } from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTemplateBuilderComponent } from './dot-template-builder.component';

import {
    DotTemplateItem,
    EMPTY_TEMPLATE_ADVANCED,
    EMPTY_TEMPLATE_DESIGN
} from '../store/dot-template.store';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dotcms-template-builder',
    template: ` <ng-content select="[toolbar-left]"></ng-content>
        <ng-content select="[toolbar-actions-right]"></ng-content>`
})
class TemplateBuilderMockComponent {
    @Input() layout: DotLayout;
    @Input() themeId: string;
    @Output() templateChange: EventEmitter<DotTemplateDesigner> = new EventEmitter();
}

@Component({
    selector: 'dot-edit-layout-designer',
    template: ``
})
class DotEditLayoutDesignerMockComponent {
    @Input() theme: string;

    @Input() layout;

    @Input() disablePublish: boolean;

    @Output() cancel: EventEmitter<MouseEvent> = new EventEmitter();

    @Output() save: EventEmitter<Event> = new EventEmitter();

    @Output() updateTemplate: EventEmitter<Event> = new EventEmitter();

    @Output() saveAndPublish: EventEmitter<Event> = new EventEmitter();
}

@Component({
    selector: 'dot-template-advanced',
    template: ``
})
class DotTemplateAdvancedMockComponent {
    @Input() url;

    @Input() body;

    @Input() didTemplateChanged: boolean;

    @Output() cancel: EventEmitter<MouseEvent> = new EventEmitter();

    @Output() save: EventEmitter<Event> = new EventEmitter();

    @Output() updateTemplate: EventEmitter<Event> = new EventEmitter();
}

@Component({
    selector: 'dot-iframe',
    template: ''
})
export class IframeMockComponent {
    @Input() src: string;
    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();
    @ViewChild('iframeElement') iframeElement: ElementRef;
}

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'p-tabView',
    template: '<ng-content></ng-content>'
})
export class TabViewMockComponent {
    @Input() styleClass: string;
}

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
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

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-template-builder #builder [item]="item"></dot-template-builder> '
})
class DotTestHostComponent {
    @ViewChild('builder') builder: DotTemplateBuilderComponent;
    item: DotTemplateItem;
}

const ITEM_FOR_NEW_TEMPLATE_BUILDER = {
    ...EMPTY_TEMPLATE_DESIGN,
    theme: '123',
    live: true
};

describe('DotTemplateBuilderComponent', () => {
    let component: DotTemplateBuilderComponent;
    let fixture: ComponentFixture<DotTemplateBuilderComponent>;
    let de: DebugElement;
    let dotTestHostComponent: DotTestHostComponent;
    let hostFixture: ComponentFixture<DotTestHostComponent>;
    let dotPropertiesService: DotPropertiesService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotTemplateBuilderComponent,
                DotEditLayoutDesignerMockComponent,
                DotTemplateAdvancedMockComponent,
                IframeMockComponent,
                TabViewMockComponent,
                TabPanelMockComponent,
                DotTestHostComponent,
                TemplateBuilderMockComponent,
                DotGlobalMessageComponent
            ],
            imports: [
                DotMessagePipe,
                DotPortletBoxModule,
                DotShowHideFeatureDirective,
                ButtonModule,
                DotIconModule
            ],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        design: 'Design',
                        code: 'Code'
                    })
                },
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getKey: () => of('false')
                    }
                },
                DotEventsService
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplateBuilderComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;

        dotPropertiesService = TestBed.inject(DotPropertiesService);
        spyOn(component.save, 'emit');
        spyOn(component.updateTemplate, 'emit');
        spyOn(component.cancel, 'emit');
    });

    describe('design', () => {
        beforeEach(() => {
            component.item = ITEM_FOR_NEW_TEMPLATE_BUILDER;
            fixture.detectChanges();
        });

        it('should have tab title "Design"', () => {
            const panel = de.query(By.css('[data-testId="builder"]'));
            expect(panel.componentInstance.header).toBe('Design');
        });

        it('should show dot-edit-layout-designer and pass attr', () => {
            const builder = de.query(By.css('dot-edit-layout-designer')).componentInstance;
            expect(builder.theme).toBe('123');
            expect(builder.disablePublish).toBe(true);
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

        it('should emit save events from dot-edit-layout-designer', () => {
            const builder = de.query(By.css('dot-edit-layout-designer'));

            builder.triggerEventHandler('save', EMPTY_TEMPLATE_DESIGN);
            builder.triggerEventHandler('updateTemplate', EMPTY_TEMPLATE_DESIGN);

            expect(component.save.emit).toHaveBeenCalledWith(EMPTY_TEMPLATE_DESIGN);
            expect(component.updateTemplate.emit).toHaveBeenCalledWith(EMPTY_TEMPLATE_DESIGN);
        });

        it('should emit save and publish event from dot-edit-layout-designer', () => {
            spyOn(component.saveAndPublish, 'emit');
            const builder = de.query(By.css('dot-edit-layout-designer'));
            builder.triggerEventHandler('saveAndPublish', EMPTY_TEMPLATE_DESIGN);
            expect(component.saveAndPublish.emit).toHaveBeenCalledWith(EMPTY_TEMPLATE_DESIGN);
        });
    });

    describe('New template design', () => {
        beforeEach(() => {
            component.item = {
                ...EMPTY_TEMPLATE_DESIGN,
                theme: '123',
                live: true
            };
            spyOn(dotPropertiesService, 'getKey').and.returnValue(of('true'));
            fixture.detectChanges();
        });

        it('should show new template builder component', () => {
            const component: DebugElement = fixture.debugElement.query(
                By.css('[data-testId="new-template-builder"]')
            );

            expect(component).toBeTruthy();
        });

        it('should set the themeId @Input correctly', () => {
            const templateBuilder = de.query(By.css('[data-testId="new-template-builder"]'));
            expect(templateBuilder.componentInstance.themeId).toBe('123');
        });

        it('should emit events from new-template-builder when the layout is changed', () => {
            const templateBuilder = de.query(By.css('[data-testId="new-template-builder"]'));
            const template = {
                layout: EMPTY_TEMPLATE_DESIGN.layout,
                themeId: '123'
            } as DotTemplateDesigner;

            templateBuilder.triggerEventHandler('templateChange', template);
            expect(component.updateTemplate.emit).toHaveBeenCalled();
        });

        it('should emit events from new-template-builder when click on save button', () => {
            const btnsave = de.query(By.css('[data-testId="save-and-publish-btn"]'));
            spyOn(component.saveAndPublish, 'emit');
            btnsave.triggerEventHandler('click', component.item);

            (btnsave.nativeElement as HTMLElement).click();
            expect(component.saveAndPublish.emit).toHaveBeenCalled();
        });

        it('should add style classes if new template builder feature flag is on', () => {
            fixture = TestBed.createComponent(DotTemplateBuilderComponent); // new fixture as async pipe was running before function was replaced
            fixture.componentInstance.item = ITEM_FOR_NEW_TEMPLATE_BUILDER;
            fixture.detectChanges();
            const tabView = fixture.debugElement.query(By.css('p-tabView'));
            const tabViewComponent: TabViewMockComponent = tabView.componentInstance;
            expect(tabViewComponent.styleClass).toEqual(
                'dot-template-builder__new-template-builder'
            );
        });
    });

    describe('advanced', () => {
        beforeEach(() => {
            component.item = EMPTY_TEMPLATE_ADVANCED;
            component.didTemplateChanged = false;

            fixture.detectChanges();
        });

        it('should have tab title "Design"', () => {
            const panel = de.query(By.css('[data-testId="builder"]'));
            expect(panel.componentInstance.header).toBe('Code');
        });

        it('should show dot-template-advanced and pass attr', () => {
            const builder = de.query(By.css('dot-template-advanced')).componentInstance;
            expect(builder.body).toBe('');
            expect(builder.didTemplateChanged).toBe(false);
        });

        it('should not show <dot-edit-layout-designer>', () => {
            const designer = de.query(By.css('dot-edit-layout-designer'));
            expect(designer).toBeNull();
        });

        it('should emit events from dot-template-advanced', () => {
            const builder = de.query(By.css('dot-template-advanced'));

            builder.triggerEventHandler('save', EMPTY_TEMPLATE_ADVANCED);
            builder.triggerEventHandler('updateTemplate', EMPTY_TEMPLATE_ADVANCED);
            builder.triggerEventHandler('cancel', {});

            expect(component.save.emit).toHaveBeenCalledWith(EMPTY_TEMPLATE_ADVANCED);
            expect(component.updateTemplate.emit).toHaveBeenCalledWith(EMPTY_TEMPLATE_ADVANCED);
            expect(component.cancel.emit).toHaveBeenCalledTimes(1);
        });
    });

    describe('permissions and history', () => {
        beforeEach(() => {
            component.item = {
                ...EMPTY_TEMPLATE_ADVANCED,
                identifier: '123'
            };
            fixture.detectChanges();
        });

        it('should set iframe permissions url', () => {
            const permissions = de.query(By.css('[data-testId="permissionsIframe"]'));
            expect(permissions.componentInstance.src).toBe(
                '/html/templates/permissions.jsp?templateId=123&popup=true'
            );
        });

        it('should set iframe history url', () => {
            const historyIframe = de.query(By.css('[data-testId="historyIframe"]'));
            expect(historyIframe.componentInstance.src).toBe(
                '/html/templates/push_history.jsp?templateId=123&popup=true'
            );
        });

        it('should reload iframe when changes in the template happens', () => {
            hostFixture = TestBed.createComponent(DotTestHostComponent);
            dotTestHostComponent = hostFixture.componentInstance;
            dotTestHostComponent.item = {
                ...EMPTY_TEMPLATE_DESIGN,
                theme: '123'
            };
            hostFixture.detectChanges();
            const builder = hostFixture.debugElement.query(
                By.css('dot-edit-layout-designer')
            ).componentInstance;
            dotTestHostComponent.builder.historyIframe = {
                iframeElement: {
                    nativeElement: {
                        contentWindow: {
                            location: {
                                reload: jasmine.createSpy('reload')
                            }
                        }
                    }
                }
            } as IframeComponent;
            dotTestHostComponent.item = {
                ...EMPTY_TEMPLATE_DESIGN,
                theme: 'dotcms-123'
            };

            builder.updateTemplate.emit(dotTestHostComponent.item);

            hostFixture.detectChanges();
            expect(
                dotTestHostComponent.builder.historyIframe.iframeElement.nativeElement.contentWindow
                    .location.reload
            ).toHaveBeenCalledTimes(1);
        });

        it('should handle custom event', () => {
            spyOn(component.custom, 'emit');
            const permissions: IframeMockComponent = de.query(
                By.css('[data-testId="historyIframe"]')
            ).componentInstance;
            const customEvent = document.createEvent('CustomEvent');
            customEvent.initCustomEvent('ng-event', false, false, {
                name: 'edit-template',
                data: {
                    id: 'id',
                    inode: 'inode'
                }
            });
            permissions.custom.emit(customEvent);
            expect(component.custom.emit).toHaveBeenCalledWith(customEvent);
        });
    });
});

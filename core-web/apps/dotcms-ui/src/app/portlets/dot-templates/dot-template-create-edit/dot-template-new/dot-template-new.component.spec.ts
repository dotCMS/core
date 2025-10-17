/* eslint-disable @typescript-eslint/no-explicit-any */

import { Subject } from 'rxjs';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTemplateNewComponent } from './dot-template-new.component';

import { DotTemplateCreateEditResolver } from '../resolvers/dot-template-create-edit.resolver';

describe('DotTemplateNewComponent', () => {
    let fixture: ComponentFixture<DotTemplateNewComponent>;
    let dialogService: DialogService;
    let dotRouterService: DotRouterService;

    const dialogRefClose = new Subject();

    beforeEach(async () => {
        const mockDialogService = {
            open: jest.fn().mockReturnValue({
                onClose: dialogRefClose
            })
        };

        await TestBed.configureTestingModule({
            imports: [DotTemplateNewComponent],
            providers: [
                provideNoopAnimations(),
                {
                    provide: DotRouterService,
                    useValue: {
                        gotoPortlet: jest.fn(),
                        goToURL: jest.fn()
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'templates.select.template.title': 'Create a template'
                    })
                }
            ]
        })
            .overrideComponent(DotTemplateNewComponent, {
                set: {
                    providers: [
                        { provide: DialogService, useValue: mockDialogService },
                        { provide: DotTemplateCreateEditResolver, useValue: {} }
                    ]
                }
            })
            .compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplateNewComponent);

        // Get the services from the component's injector
        dialogService = fixture.debugElement.injector.get(DialogService);
        dotRouterService = TestBed.inject(DotRouterService);

        // Initialize the component - this will trigger ngOnInit
        fixture.detectChanges();
    });

    it('should open template type selector', () => {
        expect(dialogService.open).toHaveBeenCalledWith(
            expect.any(Function), // DotBinaryOptionSelectorComponent
            {
                header: 'Create a template',
                width: '37rem',
                contentStyle: { padding: '0px' },
                data: {
                    options: {
                        option1: {
                            value: 'designer',
                            message: 'templates.template.selector.design',
                            icon: 'web',
                            label: 'templates.template.selector.label.designer'
                        },
                        option2: {
                            value: 'advanced',
                            message: 'templates.template.selector.advanced',
                            icon: 'settings_applications',
                            label: 'templates.template.selector.label.advanced'
                        }
                    }
                }
            }
        );
    });

    it('should go to create design template', () => {
        // Trigger the dialog close with 'design' value
        dialogRefClose.next('design');

        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/templates/new/design');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
    });

    it('should go to listing if close the dialog', () => {
        // Trigger the dialog close with undefined value
        dialogRefClose.next(undefined);

        expect(dotRouterService.goToURL).toHaveBeenCalledWith('/templates');
        expect(dotRouterService.goToURL).toHaveBeenCalledTimes(1);
    });
});

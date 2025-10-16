/* eslint-disable @typescript-eslint/no-explicit-any */

import { Subject } from 'rxjs';

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTemplateNewComponent } from './dot-template-new.component';

describe('DotTemplateNewComponent', () => {
    let fixture: ComponentFixture<DotTemplateNewComponent>;
    let dialogService: DialogService;
    let dotRouterService: DotRouterService;

    const dialogRefClose = new Subject();

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotTemplateNewComponent],
            providers: [
                DialogService,
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
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplateNewComponent);

        dotRouterService = TestBed.inject(DotRouterService);

        dialogService = TestBed.inject(DialogService);
        jest.spyOn<any>(dialogService, 'open').mockReturnValue({
            onClose: dialogRefClose
        });

        fixture.detectChanges();
    });

    it('should open template type selector', () => {
        expect(dialogService.open).toHaveBeenCalledWith(expect.any(Function), {
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
        });
    });

    it('should go to create design template', () => {
        dialogRefClose.next('design');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/templates/new/design');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
    });

    it('should go to listing if close the dialog', () => {
        dialogRefClose.next(undefined);
        expect(dotRouterService.goToURL).toHaveBeenCalledWith('/templates');
        expect(dotRouterService.goToURL).toHaveBeenCalledTimes(1);
    });
});

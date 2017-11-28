import { ActivatedRoute } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core/src/debug/debug_node';
import { DotConfirmationService } from '../../../../api/services/dot-confirmation';
import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { DotEditLayoutGridModule } from '../dot-edit-layout-grid/dot-edit-layout-grid.module';
import { FormatDateService } from '../../../../api/services/format-date-service';
import { LoginService, SocketFactory } from 'dotcms-js/dotcms-js';
import { MessageService } from '../../../../api/services/messages-service';
import { MockMessageService } from '../../../../test/message-service.mock';
import { Observable } from 'rxjs/Observable';
import { PageViewService } from '../../../../api/services/page-view/page-view.service';
import { PaginatorService } from '../../../../api/services/paginator';
import { RouterTestingModule } from '@angular/router/testing';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { DotActionButtonModule } from '../../../../view/components/_common/dot-action-button/dot-action-button.module';

describe('DotEditLayoutComponent', () => {
    let component: DotEditLayoutComponent;
    let fixture: ComponentFixture<DotEditLayoutComponent>;

    const fakePageView = {
        pageView: {
            page: {
                identifier: '123',
                title: 'Hello World'
            },
            layout: {
                body: {
                    rows: []
                }
            },
            template: {
                name: 'anonymous_layout_1511798005268'
            }
        }
    };

    const messageServiceMock = new MockMessageService({
        'editpage.layout.toolbar.action.save': 'Save',
        'editpage.layout.toolbar.action.cancel': 'Cancel',
        'editpage.layout.toolbar.template.name': 'Name of the template',
        'editpage.layout.toolbar.save.template': 'Save as template'
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotEditLayoutComponent],
            imports: [DotEditLayoutGridModule, RouterTestingModule, BrowserAnimationsModule, DotActionButtonModule],
            providers: [
                DotConfirmationService,
                FormatDateService,
                LoginService,
                PageViewService,
                PaginatorService,
                SocketFactory,
                DotEditLayoutService,
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: Observable.of(fakePageView)
                    }
                },
                { provide: MessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditLayoutComponent);
        component = fixture.componentInstance;

        fixture.detectChanges();
    });

    it('should have dot-edit-layout-grid', () => {
        const gridLayout: DebugElement = fixture.debugElement.query(By.css('dot-edit-layout-grid'));
        expect(gridLayout).toBeDefined();
    });

    it('should have add box button', () => {
        const addBoxButton: DebugElement = fixture.debugElement.query(By.css('.edit-page__toolbar-add'));
        expect(addBoxButton).toBeDefined();
    });

    it ('should have page title', () => {
        const pageTitle: DebugElement = fixture.debugElement.query(By.css('.edit-page__page-title'));
        expect(pageTitle.nativeElement.textContent).toEqual('Hello World');
    });

    it('should have cancel button', () => {
        const cancelButton: DebugElement = fixture.debugElement.query(By.css('.edit-page__toolbar-action-cancel'));
        expect(cancelButton).toBeDefined();
        expect(cancelButton.nativeElement.textContent).toEqual('Cancel');
    });

    it('should have save button', () => {
        const saveButton: DebugElement = fixture.debugElement.query(By.css('.edit-page__toolbar-action-save'));
        expect(saveButton).toBeDefined();
        expect(saveButton.nativeElement.textContent).toEqual('Save');
    });

    it('should have checkbox to save as template', () => {
        const checkboxSave: DebugElement = fixture.debugElement.query(By.css('.edit-page__toolbar-save-template'));
        expect(checkboxSave).toBeDefined();
        expect(checkboxSave.nativeElement.textContent).toContain('Save as template');
    });

    it('should show template name input and hide page title if save as template is checked', () => {
        component.saveAsTemplate = true;
        fixture.detectChanges();

        const pageTitle: DebugElement = fixture.debugElement.query(By.css('.edit-page__page-title'));
        expect(pageTitle === null).toBe(true);

        const templateNameInput: DebugElement = fixture.debugElement.query(By.css('.edit-page__toolbar-template-name'));
        expect(templateNameInput).toBeDefined();
    });
});

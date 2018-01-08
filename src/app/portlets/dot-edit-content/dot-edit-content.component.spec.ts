import { async, ComponentFixture } from '@angular/core/testing';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DialogModule } from 'primeng/primeng';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { DotConfirmationService } from '../../api/services/dot-confirmation/index';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { DotEditContentHtmlService } from './services/dot-edit-content-html.service';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotMessageService } from '../../api/services/dot-messages-service';
import { DOTTestBed } from '../../test/dot-test-bed';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../test/login-service.mock';
import { MockDotMessageService } from '../../test/dot-message-service.mock';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';
import { DotLoadingIndicatorModule } from '../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

describe('DotEditContentComponent', () => {
    let component: DotEditContentComponent;
    let fixture: ComponentFixture<DotEditContentComponent>;
    let de: DebugElement;
    let dotConfirmationService: DotConfirmationService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'editpage.toolbar.primary.action': 'Save',
            'editpage.toolbar.secondary.action': 'Cancel'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotEditContentComponent],
            imports: [
                DialogModule,
                BrowserAnimationsModule,
                DotEditPageToolbarModule,
                DotLoadingIndicatorModule
            ],
            providers: [
                DotConfirmationService,
                DotContainerContentletService,
                DotEditContentHtmlService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: Observable.of({
                            editPageHTML: ''
                        })
                    }
                },
                DotDragDropAPIHtmlService,
                DotDOMHtmlUtilService,
                DotEditContentToolbarHtmlService
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditContentComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        dotConfirmationService = fixture.debugElement.injector.get(DotConfirmationService);
    });

    it('should be created', () => {
        expect(component).toBeTruthy();
    });

    it('should show dotLoadingIndicatorService on init', () => {
        const spyLoadingIndicator = spyOn(component.dotLoadingIndicatorService, 'show');

        component.ngOnInit();

        expect(spyLoadingIndicator).toHaveBeenCalled();
    });

    it('should hide dotLoadingIndicatorService when the component loads', () => {
        const spyLoadingIndicator = spyOn(component.dotLoadingIndicatorService, 'hide');
        const loadingIndicatorElem: DebugElement = de.query(By.css('dot-loading-indicator'));

        component.onLoad(Event);

        expect(loadingIndicatorElem).not.toBeNull();
        expect(spyLoadingIndicator).toHaveBeenCalled();
    });

    it('should display confirmation dialog and remove contentlet when user accepts', () => {
        component.ngOnInit();
        const mockResEvent = {
            contentletEvents: {},
            dataset: {
                dotIdentifier: '2sfasfk-sd2d-4dxc-sdfnsdkjnajd0',
                dotInode: '26ad1jbj-23xd-4cx3-9cf2-432scc413cc2'
            },
            event: 'remove'
        };
        const dotEditContentHtmlService = fixture.debugElement.injector.get(DotEditContentHtmlService);

        spyOn(dotEditContentHtmlService, 'contentletEvents').and.returnValue(Observable.of(mockResEvent));
        spyOn(dotEditContentHtmlService, 'removeContentlet').and.callFake(res => {});

        spyOn(dotConfirmationService, 'confirm').and.callFake(conf => {
            conf.accept();
        });

        component['removeContentlet'](mockResEvent);

        expect(dotEditContentHtmlService.removeContentlet).toHaveBeenCalledWith(mockResEvent.dataset.dotInode);
    });
});

import { ActivatedRoute, Params, UrlSegment } from '@angular/router';
import { async } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { ContentTypesCreateComponent } from './content-types-create.component';
import { ContentTypesFormComponent} from '../common/content-types-form';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { ContentTypesLayoutComponent } from '../common/content-type-layout/content-types-layout.component';
import { CrudService } from '../../../api/services/crud/crud.service';
import { DebugElement } from '@angular/core';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { FieldValidationMessageModule } from '../../../view/components/_common/field-validation-message/file-validation-message.module';
import { LoginService } from '../../../api/services/login-service';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { MessageService } from '../../../api/services/messages-service';
import { MockMessageService } from '../../../test/message-service.mock';
import { Observable } from 'rxjs/Observable';
import { OverlayPanelModule } from 'primeng/primeng';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { StringUtils } from '../../../api/util/string.utils';

describe('ContentTypesCreateComponent', () => {
    let comp: ContentTypesCreateComponent;
    let fixture: ComponentFixture<ContentTypesCreateComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let route: ActivatedRoute;
    let url: UrlSegment[];

    beforeEach(async(() => {
        let messageServiceMock = new MockMessageService({
            'Content': 'Content',
            'File': 'File',
            'Form': 'Form',
            'Page': 'Page',
            'Persona': 'Persona',
            'Widget': 'Widget'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypesLayoutComponent,
                ContentTypesFormComponent,
                ContentTypesCreateComponent,
            ],
            imports: [
                FieldValidationMessageModule,
                BrowserAnimationsModule,
                ReactiveFormsModule,
                RouterTestingModule.withRoutes([{
                    component: ContentTypesCreateComponent,
                    path: 'test'
                }]),
                OverlayPanelModule
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: MessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: {}
                },
                CrudService,
                ContentTypesInfoService,
                StringUtils
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesCreateComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('#content-type'));
        el = de.nativeElement;
        route = fixture.debugElement.injector.get(ActivatedRoute);
    }));

    it('should have Content Types Layout', () => {
        de = de.query(By.css('content-types-layout'));
        expect(de).toBeDefined();
    });

    it('should have Content Types Form', () => {
        de = de.query(By.css('content-types-form'));
        expect(de).toBeDefined();
    });

    it('should have call content types endpoint with content data', async(() => {
        url = [
            new UrlSegment('create', { name: 'create' }),
            new UrlSegment('content', { name: 'content' })
        ];

        route.url = Observable.of(url);

        fixture.detectChanges();

        let crudService = fixture.debugElement.injector.get(CrudService);
        spyOn(crudService, 'postData').and.returnValue(Observable.of({}));

        comp.handleFormSubmit({
            originalEvent: Event,
            value: {
                host: '12345',
                name: 'Hello World'
            }
        });

        let mockData = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: '12345',
            name: 'Hello World',
            owner: '123',
            system: false
        };

        expect(crudService.postData).toHaveBeenCalledWith('v1/contenttype', mockData);
    }));

    it('should have call content types endpoint with widget data', async(() => {
        url = [
            new UrlSegment('create', { name: 'create' }),
            new UrlSegment('widget', { name: 'widget' })
        ];

        route.url = Observable.of(url);

        fixture.detectChanges();

        let crudService = fixture.debugElement.injector.get(CrudService);
        spyOn(crudService, 'postData').and.returnValue(Observable.of({}));

        comp.handleFormSubmit({
            originalEvent: Event,
            value: {
                host: '12345',
                name: 'Hello World'
            }
        });

        let mockData = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: '12345',
            name: 'Hello World',
            owner: '123',
            system: false
        };

        expect(crudService.postData).toHaveBeenCalledWith('v1/contenttype', mockData);
    }));
});

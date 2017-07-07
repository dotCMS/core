import { ActivatedRoute, Params, UrlSegment } from '@angular/router';
import { async } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { ContentTypesEditComponent } from './content-types-edit.component';
import { ContentTypesFormComponent} from '../common/content-types-form';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { ContentTypesLayoutComponent } from '../common/content-type-layout/content-types-layout.component';
import { CrudService } from '../../../api/services/crud/crud.service';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
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

@Component({
    selector: 'fields-drop-zone',
    template: ''
})
class TestFieldsRowComponent {

}

@Component({
    selector: 'content-type-layout',
    template: '<ng-content></ng-content>'
})
class TestContentTypeLayout {
    @Input() mode: string;
}

@Component({
    selector: 'content-types-form',
    template: ''
})
class TestContentTypesForm {
    @Input() data: any;
    @Input() icon: string;
    @Input() name: string;
    @Input() type: string;
    @Output() onCancel: EventEmitter<any> = new EventEmitter();
    @Output() onSubmit: EventEmitter<any> = new EventEmitter();

    public resetForm(): void {}
}

describe('ContentTypesEditComponent', () => {
    let comp: ContentTypesEditComponent;
    let fixture: ComponentFixture<ContentTypesEditComponent>;
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
                ContentTypesEditComponent,
                TestContentTypesForm,
                TestContentTypeLayout,
                TestFieldsRowComponent
            ],
            imports: [
                RouterTestingModule.withRoutes([{
                    component: ContentTypesEditComponent,
                    path: 'test'
                }])
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

        fixture = DOTTestBed.createComponent(ContentTypesEditComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
        route = fixture.debugElement.injector.get(ActivatedRoute);
    }));

    it('should have Content Types Layout', () => {
        let contentTypeLayout = de.query(By.css('content-type-layout'));
        let contentTypeLayoutComponentInstance = contentTypeLayout.componentInstance;
        expect(contentTypeLayout).not.toBeNull();
    });

    it('should have Content Types Form', () => {
        let contentTypeLayout = de.query(By.css('content-type-layout'));
        let contentTypeForm = contentTypeLayout.query(By.css('content-types-form'));
        expect(contentTypeForm).not.toBeNull();
    });

    it('should get the content type data with the id in the url', async(() => {
        url = [
            new UrlSegment('edit', { name: 'edit' }),
            new UrlSegment('1234-identifier', { name: '1234-identifier' })
        ];

        route.url = Observable.of(url);

        let crudService = fixture.debugElement.injector.get(CrudService);
        spyOn(crudService, 'getDataById').and.returnValue(Observable.of({
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            fields: []
        }));

        fixture.detectChanges();

        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', '1234-identifier');
    }));

    it('should have call content types endpoint with widget data', async(() => {
        url = [
            new UrlSegment('edit', { name: 'edit' }),
            new UrlSegment('1234-identifier', { name: '1234-identifier' })
        ];

        route.url = Observable.of(url);

        let crudService = fixture.debugElement.injector.get(CrudService);

        spyOn(crudService, 'getDataById').and.returnValue(Observable.of({
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            fields: [],
            id: '1234-identifier',
        }));

        spyOn(crudService, 'putData').and.returnValue(Observable.of({}));

        fixture.detectChanges();

        comp.handleFormSubmit({
            originalEvent: Event,
            value: {
                host: '12345',
                name: 'Hello World'
            }
        });

        expect(crudService.putData).toHaveBeenCalledWith('v1/contenttype/id/1234-identifier', {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            fields: [],
            host: '12345',
            id: '1234-identifier',
            name: 'Hello World'
        });
    }));
});

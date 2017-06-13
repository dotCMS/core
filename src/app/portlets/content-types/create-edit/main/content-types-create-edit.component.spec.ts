import { ActivatedRoute, Params, UrlSegment } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { ContentTypesCreateEditPortletComponent } from './';
import { ContentTypesFormComponent} from '../content-types-form';
import { CrudService } from '../../../../api/services/crud/crud.service';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { FieldValidationMessageModule } from '../../../../view/components/_common/field-validation-message/file-validation-message.module';
import { LoginService } from '../../../../api/services/login-service';
import { LoginServiceMock } from '../../../../test/login-service.mock';
import { MessageService } from '../../../../api/services/messages-service';
import { MockMessageService } from '../../../../test/message-service.mock';
import { Observable } from 'rxjs/Observable';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { async } from '@angular/core/testing';
import { ContentTypesInfoService } from '../../../../api/services/content-types-info';

describe('ContentTypesCreateEditPortletComponent', () => {
    let comp: ContentTypesCreateEditPortletComponent;
    let fixture: ComponentFixture<ContentTypesCreateEditPortletComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    let route: ActivatedRoute;
    let params: Params;
    let url: UrlSegment[];

    beforeEach(async(() => {
        let messageServiceMock = new MockMessageService({
            'Permissions': 'Permissions',
            'fields': 'Fields',
            'publisher_push_history': 'Push History'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypesFormComponent,
                ContentTypesCreateEditPortletComponent,
            ],
            imports: [
                FieldValidationMessageModule,
                ReactiveFormsModule,
                RouterTestingModule.withRoutes([{
                    component: ContentTypesCreateEditPortletComponent,
                    path: 'test'
                }]),
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: MessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {}
                    }
                },
                CrudService,
                ContentTypesInfoService
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesCreateEditPortletComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('#content-type'));
        el = de.nativeElement;
        route = fixture.debugElement.injector.get(ActivatedRoute);

    }));

    it('should have Content Types Form', () => {
        de = de.query(By.css('content-types-form'));
        expect(de).toBeDefined();
    });

    it('should have not call content types endpoint on create mode', () => {
        params = {type: 'content'};
        url = [new UrlSegment('create', { name: 'create' })];

        route.snapshot.url = url;
        route.snapshot.params = params;

        let crudService = fixture.debugElement.injector.get(CrudService);
        spyOn(crudService, 'getDataById');

        comp.ngOnInit();

        expect(crudService.getDataById).not.toHaveBeenCalled();
    });

    it('should have call content types endpoint on edit mode', () => {
        params = {id: '123'};
        url = [new UrlSegment('edit', { name: 'edit' })];

        route.snapshot.url = url;
        route.snapshot.params = params;

        let crudService = fixture.debugElement.injector.get(CrudService);
        spyOn(crudService, 'getDataById');

        comp.ngOnInit();

        expect(crudService.getDataById).toHaveBeenCalledWith('/v1/contenttype', '123');
    });

});
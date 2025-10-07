import { of } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { DotRouterService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CONTAINER_SOURCE } from '@dotcms/dotcms-models';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotContainerCreateComponent } from './dot-container-create.component';

@Pipe({
    name: 'dm',
    standalone: false
})
class DotMessageMockPipe implements PipeTransform {
    transform(): string {
        return 'Required';
    }
}
describe('ContainerCreateComponent', () => {
    let component: DotContainerCreateComponent;
    let fixture: ComponentFixture<DotContainerCreateComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotContainerCreateComponent, DotMessageMockPipe],
            schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of({
                            container: {
                                container: {
                                    archived: false,
                                    live: true,
                                    working: true,
                                    locked: false,
                                    identifier: '',
                                    name: '',
                                    type: '',
                                    source: CONTAINER_SOURCE.DB,
                                    parentPermissionable: {
                                        hostname: 'dotcms.com'
                                    }
                                },
                                containerStructures: []
                            }
                        }),
                        snapshot: {
                            params: {
                                id: '123'
                            }
                        }
                    }
                },
                DotRouterService
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContainerCreateComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

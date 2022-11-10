import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContainerCreateComponent } from './dot-container-create.component';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { ActivatedRoute } from '@angular/router';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { Pipe, PipeTransform } from '@angular/core';
import { of } from 'rxjs';
import { CONTAINER_SOURCE } from '@dotcms/app/shared/models/container/dot-container.model';

@Pipe({
    name: 'dm'
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

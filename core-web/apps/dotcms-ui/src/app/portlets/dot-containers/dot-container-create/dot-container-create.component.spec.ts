import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { DotEventsService, DotRouterService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CONTAINER_SOURCE } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotContainerCreateComponent } from './dot-container-create.component';

@Pipe({
    name: 'dm'
})
class DotMessageMockPipe implements PipeTransform {
    transform(): string {
        return 'Required';
    }
}

class MockDotEventsService {
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    notify() {}
    listen() {
        return of();
    }
}
describe('ContainerCreateComponent', () => {
    let component: DotContainerCreateComponent;
    let fixture: ComponentFixture<DotContainerCreateComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotContainerCreateComponent],
            schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotEventsService, useClass: MockDotEventsService },
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
                                    hostName: 'dotcms.com'
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
        })
            .overrideComponent(DotContainerCreateComponent, {
                remove: { imports: [DotMessagePipe] },
                add: { imports: [DotMessageMockPipe] }
            })
            .compileComponents();

        fixture = TestBed.createComponent(DotContainerCreateComponent);
        component = fixture.componentInstance;
        // Note: detectChanges() is not called here because child components require additional dependencies
        // that are not relevant for this simple creation test
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});

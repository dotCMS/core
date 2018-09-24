import { of as observableOf, Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { DotRenderedPageState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { mockUser } from './login-service.mock';
import { DotRenderedPage } from '@portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { mockDotLayout, mockDotPage } from './dot-rendered-page.mock';

const mockDotRenderedPage: DotRenderedPage = {
    page: {
        ...mockDotPage,
        rendered: ''
    },
    layout: mockDotLayout,
    canCreateTemplate: true,
    viewAs: null
};

@Injectable()
export class DotPageStateServiceMock {
    get(_url: string): Observable<DotRenderedPageState> {
        return observableOf(new DotRenderedPageState(mockUser, mockDotRenderedPage));
    }
}

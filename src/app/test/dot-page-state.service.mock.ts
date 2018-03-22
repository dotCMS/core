import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { DotRenderedPageState } from '../portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { mockUser } from './login-service.mock';
import { DotRenderedPage } from '../portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { mockDotLayout, mockDotPage } from './dot-rendered-page.mock';

const mockDotRenderedPage: DotRenderedPage = {
    page: mockDotPage,
    html: '',
    layout: mockDotLayout,
    canCreateTemplate: true,
    viewAs: null
};

@Injectable()
export class DotPageStateServiceMock {
    get(url: string): Observable<DotRenderedPageState> {
        return Observable.of(new DotRenderedPageState(mockUser, mockDotRenderedPage));
    }
}

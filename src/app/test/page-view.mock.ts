import { of as observableOf } from 'rxjs';
import { DotPageView } from '@portlets/dot-edit-page/shared/models/dot-page-view.model';
import {
    mockDotPage,
    mockDotLayout,
    mockDotContainers,
    mockDotTemplate
} from './dot-page-render.mock';

export const fakePageView: DotPageView = {
    containers: mockDotContainers(),
    page: mockDotPage(),
    layout: mockDotLayout(),
    template: mockDotTemplate(),
    canEditTemplate: true
};

export class PageViewServiceMock {
    get() {
        return observableOf(fakePageView);
    }
}

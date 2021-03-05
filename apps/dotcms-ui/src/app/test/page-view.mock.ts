import { of } from 'rxjs';
import { DotPageView } from '@models/dot-page/dot-page-view.model';
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
        return of(fakePageView);
    }
}

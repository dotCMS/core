import { DotPageRenderState, DotPageRender } from '@portlets/dot-edit-page/shared/models';
import { mockUser } from './login-service.mock';
import { mockDotRenderedPage } from './dot-page-render.mock';

export const mockDotRenderedPageState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage())
);

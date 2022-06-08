import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models';
import { mockUser } from './login-service.mock';
import { mockDotRenderedPage } from './dot-page-render.mock';
import { DotPageRender } from '@models/dot-page/dot-rendered-page.model';

export const mockDotRenderedPageState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage())
);

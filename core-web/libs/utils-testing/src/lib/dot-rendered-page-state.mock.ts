import { mockUser } from './login-service.mock';
import { mockDotRenderedPage } from './dot-page-render.mock';
import { DotPageRender, DotPageRenderState } from '@dotcms/dotcms-models';

export const mockDotRenderedPageState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage()),
    null
);

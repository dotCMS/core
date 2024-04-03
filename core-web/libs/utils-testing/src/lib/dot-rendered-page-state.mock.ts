import { DotPageRender, DotPageRenderState } from '@dotcms/dotcms-models';

import { mockDotRenderedPage } from './dot-page-render.mock';
import { mockUser } from './login-service.mock';

export const mockDotRenderedPageState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage()),
    null
);

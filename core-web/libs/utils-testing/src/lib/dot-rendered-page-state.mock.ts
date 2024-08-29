import { DotPageRender, DotPageRenderState } from '@dotcms/dotcms-models';

import { mockDotRenderedPage } from './dot-page-render.mock';
import { dotcmsContentletMock } from './dotcms-contentlet.mock';
import { mockUser } from './login-service.mock';

export const mockDotRenderedPageState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage())
);

export const mockDotRenderedPageStateWithPersona = new DotPageRenderState(
    mockUser(),
    new DotPageRender({
        ...mockDotRenderedPage(),
        viewAs: {
            ...mockDotRenderedPage().viewAs,
            persona: {
                ...dotcmsContentletMock,
                name: 'Super Persona',
                keyTag: 'SuperPersona',
                personalized: true
            }
        }
    })
);

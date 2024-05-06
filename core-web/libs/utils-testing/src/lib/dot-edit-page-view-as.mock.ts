import { DotEditPageViewAs, DotPageMode } from '@dotcms/dotcms-models';

import { mockDotDevices } from './dot-device.mock';
import { mockDotLanguage } from './dot-language.mock';
import { mockDotPersona } from './dot-persona.mock';

export const mockDotEditPageViewAs: DotEditPageViewAs = {
    language: mockDotLanguage,
    device: mockDotDevices[0],
    persona: mockDotPersona,
    mode: DotPageMode.EDIT
};

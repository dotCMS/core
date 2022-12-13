import { mockDotLanguage } from './dot-language.mock';
import { mockDotDevices } from './dot-device.mock';
import { mockDotPersona } from './dot-persona.mock';
import { DotEditPageViewAs, DotPageMode } from '@dotcms/dotcms-models';

export const mockDotEditPageViewAs: DotEditPageViewAs = {
    language: mockDotLanguage,
    device: mockDotDevices[0],
    persona: mockDotPersona,
    mode: DotPageMode.EDIT
};

import { DotEditPageViewAs } from '@models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { mockDotLanguage } from './dot-language.mock';
import { mockDotDevices } from './dot-device.mock';
import { mockDotPersona } from './dot-persona.mock';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';

export const mockDotEditPageViewAs: DotEditPageViewAs = {
    language: mockDotLanguage.id,
    device: mockDotDevices[0],
    persona: mockDotPersona,
    mode: DotPageMode.EDIT
};

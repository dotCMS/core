import { DotEditPageViewAs } from '../shared/models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { mockDotLanguage } from './dot-language.mock';
import { mockDotDevices } from './dot-device.mock';
import { mockDotPersona } from './dot-persona.mock';

export const mockDotEditPageViewAs: DotEditPageViewAs = {
    language: mockDotLanguage,
    device: mockDotDevices[0],
    persona: mockDotPersona
};

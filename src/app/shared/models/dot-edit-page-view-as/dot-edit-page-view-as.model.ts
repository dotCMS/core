import { DotDevice } from '../dot-device/dot-device.model';
import { DotLanguage } from '../dot-language/dot-language.model';
import { DotPersona } from '../dot-persona/dot-persona.model';

export interface DotEditPageViewAs {
    persona?: DotPersona;
    language?: DotLanguage;
    device?: DotDevice;
}

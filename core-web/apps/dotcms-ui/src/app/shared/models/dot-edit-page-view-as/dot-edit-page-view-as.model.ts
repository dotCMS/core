import { DotDevice } from '../dot-device/dot-device.model';
import { DotPersona } from '../dot-persona/dot-persona.model';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';
import { DotLanguage } from '../dot-language/dot-language.model';

export interface DotEditPageViewAs {
    persona?: DotPersona;
    language?: DotLanguage;
    device?: DotDevice;
    mode: DotPageMode;
}

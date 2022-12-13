import { DotDevice } from './dot-device.model';
import { DotLanguage } from './dot-language.model';
import { DotPageMode } from './dot-page-mode.enum';
import { DotPersona } from './dot-persona.model';

export interface DotEditPageViewAs {
    persona?: DotPersona;
    language?: DotLanguage;
    device?: DotDevice;
    mode: DotPageMode;
}

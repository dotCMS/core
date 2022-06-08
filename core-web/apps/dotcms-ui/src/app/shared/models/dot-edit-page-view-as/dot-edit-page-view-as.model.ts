import { DotDevice } from '../dot-device/dot-device.model';
import { DotPersona } from '../dot-persona/dot-persona.model';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';

export interface DotEditPageViewAs {
    persona?: DotPersona;
    language?: number;
    device?: DotDevice;
    mode: DotPageMode;
}

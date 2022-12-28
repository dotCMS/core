import { DotDevice } from './dot-device.model';
import { DotPageMode } from './dot-page-mode.enum';
import { DotPersona } from './dot-persona.model';

export interface DotPageRenderOptionsViewAs {
    persona?: DotPersona;
    language?: number;
    device?: DotDevice;
}

export interface DotPageRenderOptions {
    url?: string;
    mode?: DotPageMode;
    viewAs?: DotPageRenderOptionsViewAs;
}

export interface DotPageRenderRequestParams {
    persona_id?: string;
    language_id?: string;
    device_inode?: string;
    mode?: DotPageMode;
}

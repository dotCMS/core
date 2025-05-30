import { DotCMSContentlet } from './dot-contentlet.model';

export interface DotPersona extends DotCMSContentlet {
    description?: string;
    hostFolder?: string;
    keyTag: string;
    name: string;
    personalized?: boolean;
    photo?: string;
    photoContentAsset?: string;
    photoVersion?: string;
    tags?: string;
}

import { CurrentUser } from '@dotcms/dotcms-js';
import { DotExperiment, DotLanguage } from '@dotcms/dotcms-models';

import { DotPageApiResponse } from '../services/dot-page-api.service';

export interface UVEState {
    isEnterprise: boolean;
    page?: DotPageApiResponse;
    languages: DotLanguage[];
    currentUser?: CurrentUser;
    experiment?: DotExperiment;
}

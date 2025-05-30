import { DotContainerMap } from '@dotcms/dotcms-models';
import { DotCMSLayout } from '@dotcms/types';

export interface LayoutProps {
    containersMap: DotContainerMap;
    layout: DotCMSLayout;
    template: {
        identifier: string;
        themeId: string;
        anonymous?: boolean;
    };
    pageId: string;
}

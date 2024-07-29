import { DotContainerMap, DotLayout } from '@dotcms/dotcms-models';

export interface LayoutProps {
    containersMap: DotContainerMap;
    layout: DotLayout;
    template: {
        identifier: string;
        themeId: string;
    };
    pageId: string;
}

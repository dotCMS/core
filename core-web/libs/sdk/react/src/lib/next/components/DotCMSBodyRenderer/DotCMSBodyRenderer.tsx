import { isInsideEditor } from '@dotcms/client';

import { DotCMSRenderContext } from '../../contexts/DotCMSRenderContext';
import { DotCMSContentlet, DotCMSPageAsset } from '../../types';
import { Row } from '../Row/Row';

interface DotCMSBodyRendererProps {
    dotCMSPageAsset: DotCMSPageAsset;
    customComponents?: Record<string, React.ComponentType<DotCMSContentlet>>;
    devMode?: boolean;
}

export const DotCMSBodyRenderer = ({
    dotCMSPageAsset,
    customComponents,
    devMode
}: DotCMSBodyRendererProps) => {
    const dotCMSPageBody = dotCMSPageAsset?.layout?.body;
    const isDevMode = !!devMode || isInsideEditor();

    if (!dotCMSPageBody) {
        console.warn('The page body is not defined');

        if (isDevMode) {
            return <div>The page body is not defined, please provide a valid dotCMSPageAsset</div>;
        }

        return null;
    }

    return (
        <DotCMSRenderContext.Provider value={{ dotCMSPageAsset, customComponents, isDevMode }}>
            {dotCMSPageBody.rows.map((row, index) => (
                <Row key={index} row={row} />
            ))}
        </DotCMSRenderContext.Provider>
    );
};

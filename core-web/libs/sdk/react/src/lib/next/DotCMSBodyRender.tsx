import { isInsideEditor } from '@dotcms/client';
import { Row } from './components/Row/Row';
import { DotCMSRenderContext } from './contexts/DotCMSRenderContext';
import { DotCMSPageAsset } from './types';

interface DotCMSBodyRenderProps {
    dotCMSPageAsset: DotCMSPageAsset;
    customComponents?: Record<string, React.ComponentType<any>>;
    devMode?: boolean;
}

export const DotCMSBodyRender = ({
    dotCMSPageAsset,
    customComponents,
    devMode
}: DotCMSBodyRenderProps) => {
    const dotCMSPageBody = dotCMSPageAsset?.layout?.body;

    if (!dotCMSPageBody) {
        console.warn('The page body is not defined');

        return null;
    }

    const isDevMode = !!devMode || isInsideEditor();

    return (
        <DotCMSRenderContext.Provider value={{ dotCMSPageAsset, customComponents, isDevMode }}>
            {dotCMSPageBody.rows.map((row, index) => (
                <Row key={index} row={row} />
            ))}
        </DotCMSRenderContext.Provider>
    );
};

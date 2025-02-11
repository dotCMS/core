import { ErrorMessage } from './components/ErrorMessage';

import { DotCMSPageContext, RendererMode } from '../../contexts/DotCMSPageContext';
import { DotCMSContentlet, DotCMSPageAsset } from '../../types';
import { Row } from '../Row/Row';

interface DotCMSLayoutBodyRendererProps {
    page: DotCMSPageAsset;
    components?: Record<string, React.ComponentType<DotCMSContentlet>>;
    mode?: RendererMode;
}

export const DotCMSLayoutBodyRenderer = ({
    page,
    components,
    mode = 'production'
}: DotCMSLayoutBodyRendererProps) => {
    const dotCMSPageBody = page?.layout?.body;

    if (!dotCMSPageBody) {
        return <ErrorMessage mode={mode} />;
    }

    const contextValue = {
        pageAsset: page,
        userComponents: components,
        mode
    };

    return (
        <DotCMSPageContext.Provider value={contextValue}>
            {dotCMSPageBody.rows.map((row, index) => (
                <Row key={index} row={row} />
            ))}
        </DotCMSPageContext.Provider>
    );
};

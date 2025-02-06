import { useContext, useRef, useMemo } from 'react';
import { isInsideEditor } from '@dotcms/client';

import { DotCMSRenderContext, DotCMSRenderContextI } from '../../contexts/DotCMSRenderContext';
import { DotCMSContentlet } from '../../types';
import { getDotAttributes } from '../../utils/utils';
import { useCheckVisibleContent } from '../../hooks/useCheckHaveContent';

// Define props interfaces for better type safety
interface ContentletProps {
    contentlet: DotCMSContentlet;
    container: string;
}

interface CustomComponentProps {
    contentlet: DotCMSContentlet;
    debugMode: boolean;
}

export function Contentlet({ contentlet, container }: ContentletProps) {
    const { devMode } = useContext(DotCMSRenderContext) as DotCMSRenderContextI;

    const ref = useRef<HTMLDivElement | null>(null);
    const haveContent = useCheckVisibleContent(ref);

    const debugMode = devMode || isInsideEditor();

    const style = useMemo(
        () => (debugMode ? { minHeight: haveContent ? undefined : '4rem' } : {}),
        [debugMode, haveContent]
    );
    const dotAttributes = useMemo(
        () => (debugMode ? getDotAttributes(contentlet, container) : {}),
        [debugMode, contentlet, container]
    );

    return (
        <div {...dotAttributes} data-dot-object="contentlet" ref={ref} style={style}>
            <CustomComponent
                contentlet={contentlet}
                debugMode={debugMode}
            />
        </div>
    );
}

function CustomComponent({ contentlet, debugMode }: CustomComponentProps) {
    const { customComponents } = useContext(DotCMSRenderContext) as DotCMSRenderContextI;
    const UserComponent = customComponents?.[contentlet?.contentType];
    
    if (UserComponent) {
        return <UserComponent {...contentlet} />;
    }

    if(debugMode) {
        return <EmptyContent />;
    }

    const NoComponentFound = customComponents?.['CustomNoComponent'] || NoComponent;

    return <NoComponentFound {...contentlet} />;
}

/**
 * Component to render when there is no component for the content type.
 *
 * @param {{ readonly contentType: string }} { contentType }
 * @return {*}
 */
function NoComponent({ contentType }: { readonly contentType: string }) {
    return <div data-testid="no-component">No Component for {contentType}</div>;
}

/**
 * Component to render when there is no content in the container.
 *
 * @return {*}
 */
function EmptyContent() {
    return null;
}

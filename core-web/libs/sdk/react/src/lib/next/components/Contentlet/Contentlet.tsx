import { useContext, useRef, useMemo } from 'react';
import { isInsideEditor } from '@dotcms/client';

import { DotCMSRenderContext, DotCMSRenderContextI } from '../../contexts/DotCMSRenderContext';
import { DotCMSContentlet } from '../../types';
import { getDotContentletAttributes } from '../../utils';
import { useCheckVisibleContent } from '../../hooks/useCheckHaveContent';

// Define props interfaces for better type safety
interface ContentletProps {
    contentlet: DotCMSContentlet;
    container: string;
}

interface CustomComponentProps {
    contentlet: DotCMSContentlet;
    isDevMode: boolean;
}

export function Contentlet({ contentlet, container }: ContentletProps) {
    const { devMode } = useContext(DotCMSRenderContext) as DotCMSRenderContextI;

    const ref = useRef<HTMLDivElement | null>(null);
    const haveContent = useCheckVisibleContent(ref);

    const isDevMode = devMode || isInsideEditor();

    const style = useMemo(
        () => (isDevMode ? { minHeight: haveContent ? undefined : '4rem' } : {}),
        [isDevMode, haveContent]
    );
    const dotAttributes = useMemo(
        () => (isDevMode ? getDotContentletAttributes(contentlet, container) : {}),
        [isDevMode, contentlet, container]
    );

    return (
        <div {...dotAttributes} data-dot-object="contentlet" ref={ref} style={style}>
            <CustomComponent contentlet={contentlet} isDevMode={isDevMode} />
        </div>
    );
}

function CustomComponent({ contentlet, isDevMode }: CustomComponentProps) {
    const { customComponents } = useContext(DotCMSRenderContext) as DotCMSRenderContextI;
    const UserComponent = customComponents?.[contentlet?.contentType];

    if (UserComponent) {
        return <UserComponent {...contentlet} />;
    }

    if (!isDevMode) {
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
    return (
        <div data-testid="no-component">
            No Component for <strong>{contentType}</strong>.
        </div>
    );
}

/**
 * Component to render when there is no content in the container.
 *
 * @return {*}
 */
function EmptyContent() {
    return null;
}

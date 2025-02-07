import { useContext, useRef, useMemo } from 'react';
import { isInsideEditor } from '@dotcms/client';

import { DotCMSRenderContext, DotCMSRenderContextI } from '../../contexts/DotCMSRenderContext';
import { DotCMSContentlet } from '../../types';
import { getDotContentletAttributes } from './utils';
import { useCheckVisibleContent } from '../../hooks/useCheckHaveContent';
import { FallbackComponent, NoComponentType } from './FallbackComponent';

/**
 * Props for the Contentlet component
 * @interface ContentletProps
 * @property {DotCMSContentlet} contentlet - The contentlet data to be rendered
 * @property {string} container - The container identifier where the contentlet is placed
 */
interface ContentletProps {
    contentlet: DotCMSContentlet;
    container: string;
}

/**
 * Props for the CustomComponent
 * @interface CustomComponentProps
 * @property {DotCMSContentlet} contentlet - The contentlet data to be rendered
 * @property {boolean} isDevMode - Flag indicating if the component is in development mode
 */
interface CustomComponentProps {
    contentlet: DotCMSContentlet;
    isDevMode: boolean;
}

/**
 * Contentlet component that renders DotCMS content with development mode support
 *
 * @component
 * @param {ContentletProps} props - Component properties
 * @param {DotCMSContentlet} props.contentlet - The contentlet to be rendered
 * @param {string} props.container - The container identifier
 * @returns {JSX.Element} Rendered contentlet with appropriate wrapper and attributes
 *
 * @example
 * ```tsx
 * <Contentlet
 *   contentlet={myContentlet}
 *   container="container-1"
 * />
 * ```
 */
export function Contentlet({ contentlet, container }: ContentletProps) {
    const { isDevMode } = useContext(DotCMSRenderContext) as DotCMSRenderContextI;

    const ref = useRef<HTMLDivElement | null>(null);
    const haveContent = useCheckVisibleContent(ref);

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

/**
 * Renders a custom component based on the contentlet type or falls back to a default component
 *
 * @component
 * @param {CustomComponentProps} props - Component properties
 * @param {DotCMSContentlet} props.contentlet - The contentlet data to render
 * @param {boolean} props.isDevMode - Whether the component is in development mode
 * @returns {JSX.Element} The rendered custom component or fallback component
 *
 * @internal
 */
function CustomComponent({ contentlet, isDevMode }: CustomComponentProps) {
    const { customComponents } = useContext(DotCMSRenderContext) as DotCMSRenderContextI;
    const UserComponent = customComponents?.[contentlet?.contentType];

    if (UserComponent) {
        return <UserComponent {...contentlet} />;
    }

    const UserNoComponent = customComponents?.['CustomNoComponent'] as NoComponentType;

    return (
        <FallbackComponent
            UserNoComponent={UserNoComponent}
            contentlet={contentlet}
            isDevMode={isDevMode}
        />
    );
}

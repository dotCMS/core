import { useContext, useRef, useMemo } from 'react';

import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';
import { useCheckVisibleContent } from '../../hooks/useCheckVisibleContent';
import { useIsDevMode } from '../../hooks/useIsDevMode';
import { DotCMSContentlet } from '../../types';
import { getDotContentletAttributes } from '../../utils';
import { FallbackComponent, NoComponentType } from '../FallbackComponent/FallbackComponent';

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
 */
interface CustomComponentProps {
    contentlet: DotCMSContentlet;
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
    const ref = useRef<HTMLDivElement | null>(null);
    const isDevMode = useIsDevMode();
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
            <CustomComponent contentlet={contentlet} />
        </div>
    );
}

/**
 * Renders a custom component based on the contentlet type or falls back to a default component
 *
 * @component
 * @param {CustomComponentProps} props - Component properties
 * @param {DotCMSContentlet} props.contentlet - The contentlet data to render
 * @returns {JSX.Element} The rendered custom component or fallback component
 *
 * @internal
 */
function CustomComponent({ contentlet }: CustomComponentProps) {
    const { userComponents } = useContext(DotCMSPageContext);
    const UserComponent = userComponents?.[contentlet?.contentType];

    if (UserComponent) {
        return <UserComponent {...contentlet} />;
    }

    const UserNoComponent = userComponents?.['CustomNoComponent'] as NoComponentType;

    return <FallbackComponent UserNoComponent={UserNoComponent} contentlet={contentlet} />;
}

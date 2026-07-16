'use client';

import { useContext, useMemo, useRef } from 'react';

import { DotCMSBasicContentlet } from '@dotcms/types';
import {
    CUSTOM_NO_COMPONENT,
    getAnalyticsContentletAttributes,
    getDotContentletAttributes
} from '@dotcms/uve/internal';

import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';
import { useCheckVisibleContent } from '../../hooks/useCheckVisibleContent';
import { useIsAnalyticsActive } from '../../hooks/useIsAnalyticsActive';
import { useIsDevMode } from '../../hooks/useIsDevMode';
import { FallbackComponent } from '../FallbackComponent/FallbackComponent';

/**
 * CSS class name for contentlet elements
 */
export const CONTENTLET_CLASS = 'dotcms-contentlet';

/**
 * @internal
 *
 * Props for the Contentlet component
 * @interface DotCMSContentletRendererProps
 * @property {DotCMSContentlet} contentlet - The contentlet data to be rendered
 * @property {string} container - The container identifier where the contentlet is placed
 */
interface DotCMSContentletRendererProps {
    contentlet: DotCMSBasicContentlet;
    container: string;
}

/**
 * Props for the CustomComponent
 * @interface CustomComponentProps
 * @property {DotCMSContentlet} contentlet - The contentlet data to be rendered
 */
interface CustomComponentProps {
    contentlet: DotCMSBasicContentlet;
}

/**
 * Contentlet component that renders DotCMS content with development mode support
 *
 * @component
 * @param {DotCMSContentletRendererProps} props - Component properties
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
export function Contentlet({ contentlet, container }: DotCMSContentletRendererProps) {
    const ref = useRef<HTMLDivElement | null>(null);
    const isDevMode = useIsDevMode();
    const isAnalyticsActive = useIsAnalyticsActive();
    const haveContent = useCheckVisibleContent(ref);

    const style = useMemo(
        () => (isDevMode ? { minHeight: haveContent ? undefined : '4rem' } : {}),
        [isDevMode, haveContent]
    );

    // In edit mode we emit the full set of editor metadata. In live mode we
    // strip it to avoid leaking internal identifiers, keeping only the minimal
    // set Analytics needs (and only while Analytics is active).
    const dotAttributes = useMemo(() => {
        if (isDevMode) {
            return {
                ...getDotContentletAttributes(contentlet, container),
                'data-dot-object': 'contentlet'
            };
        }

        if (isAnalyticsActive) {
            return getAnalyticsContentletAttributes(contentlet);
        }

        return {};
    }, [isDevMode, isAnalyticsActive, contentlet, container]);

    return (
        <div {...dotAttributes} className={CONTENTLET_CLASS} ref={ref} style={style}>
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
    const { userComponents, slots } = useContext(DotCMSPageContext);

    const slotNode = contentlet?.identifier ? slots?.[contentlet.identifier] : undefined;

    if (slotNode !== undefined) {
        return <>{slotNode}</>;
    }

    const UserComponent = userComponents[contentlet?.contentType];

    if (UserComponent) {
        return <UserComponent {...contentlet} />;
    }

    const UserNoComponent = userComponents[CUSTOM_NO_COMPONENT];

    return <FallbackComponent UserNoComponent={UserNoComponent} contentlet={contentlet} />;
}

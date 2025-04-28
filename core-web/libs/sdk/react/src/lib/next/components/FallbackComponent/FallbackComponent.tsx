import { DotCMSBasicContentlet } from '@dotcms/types';

import { useIsDevMode } from '../../hooks/useIsDevMode';

/**
 * @internal
 *
 * Type definition for components that can be used as fallback when no matching component is found
 */
export type NoComponentType = React.ComponentType<DotCMSBasicContentlet>;

/**
 * @internal
 *
 * Props for the FallbackComponent
 * @interface DotCMSFallbackComponentProps
 * @property {React.ComponentType<DotCMSBasicContentlet>} [UserNoComponent] - Optional custom component to render when no matching component is found
 * @property {DotCMSBasicContentlet} [contentlet] - The contentlet that couldn't be rendered
 */
interface DotCMSFallbackComponentProps {
    contentlet: DotCMSBasicContentlet;
    UserNoComponent?: React.ComponentType<DotCMSBasicContentlet>;
}

/**
 * @internal
 *
 * Renders a fallback component when no matching component is found for a content type
 *
 * @component
 * @param {DotCMSFallbackComponentProps} props - Component properties
 * @param {NoComponentType} [props.UserNoComponent] - Optional custom component to render
 * @param {DotCMSContentlet} [props.contentlet] - The contentlet that couldn't be rendered
 * @returns {JSX.Element} The rendered fallback component
 *
 * @example
 * ```tsx
 * <FallbackComponent
 *   UserNoComponent={CustomNoComponent}
 *   contentlet={contentlet}
 * />
 * ```
 */
export function FallbackComponent({ UserNoComponent, contentlet }: DotCMSFallbackComponentProps) {
    const isDevMode = useIsDevMode();

    if (!isDevMode) {
        return null;
    }

    const NoComponentFound = UserNoComponent || NoComponent;

    return <NoComponentFound {...contentlet} />;
}

/**
 * @internal
 *
 * Component to render when there is no component for the content type.
 *
 * @param {DotCMSBasicContentlet} contentType - The content type that couldn't be rendered
 * @return {*}
 */
function NoComponent({ contentType }: DotCMSBasicContentlet) {
    return (
        <div data-testid="no-component">
            No Component for <strong>{contentType}</strong>.
        </div>
    );
}

import { useIsDevMode } from '../../hooks/useIsDevMode';
import { DotCMSContentlet } from '../../types';

/**
 * @internal
 *
 * Type definition for components that can be used as fallback when no matching component is found
 */
export type NoComponentType = React.ComponentType<DotCMSContentlet>;

/**
 * @internal
 *
 * Props for the FallbackComponent
 * @interface DotCMSFallbackComponentProps
 * @property {React.ComponentType<DotCMSContentlet>} [UserNoComponent] - Optional custom component to render when no matching component is found
 * @property {DotCMSContentlet} [contentlet] - The contentlet that couldn't be rendered
 */
interface DotCMSFallbackComponentProps {
    contentlet: DotCMSContentlet;
    UserNoComponent?: React.ComponentType<DotCMSContentlet>;
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
 * @param {DotCMSContentlet} contentType - The content type that couldn't be rendered
 * @return {*}
 */
function NoComponent({ contentType }: DotCMSContentlet) {
    return (
        <div data-testid="no-component">
            No Component for <strong>{contentType}</strong>.
        </div>
    );
}

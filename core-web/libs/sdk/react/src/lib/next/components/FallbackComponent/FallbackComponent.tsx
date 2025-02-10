import { DotCMSContentlet } from '../../types';

/**
 * Type definition for components that can be used as fallback when no matching component is found
 */
export type NoComponentType = React.ComponentType<DotCMSContentlet>;

/**
 * Props for the FallbackComponent
 * @interface FallbackComponentProps
 * @property {NoComponentType} [UserNoComponent] - Optional custom component to render when no matching component is found
 * @property {DotCMSContentlet} [contentlet] - The contentlet that couldn't be rendered
 * @property {boolean} isDevMode - Whether the component is in development mode
 */
interface FallbackComponentProps {
    UserNoComponent: NoComponentType;
    contentlet: DotCMSContentlet;
    isDevMode: boolean;
}

/**
 * Renders a fallback component when no matching component is found for a content type
 *
 * @component
 * @param {FallbackComponentProps} props - Component properties
 * @param {NoComponentType} [props.UserNoComponent] - Optional custom component to render
 * @param {DotCMSContentlet} [props.contentlet] - The contentlet that couldn't be rendered
 * @param {boolean} props.isDevMode - Whether the component is in development mode
 * @returns {JSX.Element} The rendered fallback component
 *
 * @example
 * ```tsx
 * <FallbackComponent
 *   UserNoComponent={CustomNoComponent}
 *   contentlet={contentlet}
 *   isDevMode={true}
 * />
 * ```
 */
export function FallbackComponent({
    UserNoComponent,
    contentlet,
    isDevMode
}: FallbackComponentProps) {
    if (!isDevMode) {
        return <EmptyContent />;
    }

    const NoComponentFound = UserNoComponent || NoComponent;

    return <NoComponentFound {...contentlet} />;
}

/**
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

/**
 * Component to render when there is no content in the container and the component is not in dev mode.
 *
 * @return {*}
 */
function EmptyContent() {
    return null;
}

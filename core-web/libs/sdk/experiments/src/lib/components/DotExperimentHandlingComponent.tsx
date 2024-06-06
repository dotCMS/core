import { DotcmsPageProps } from '@dotcms/react';

import { useExperimentVariant } from '../hooks/useExperimentVariant';

interface ExperimentHandlingProps extends DotcmsPageProps {
    WrappedComponent: React.ComponentType<DotcmsPageProps>;
}

/**
 * A React functional component that conditionally renders a WrappedComponent based on the
 * experiment variant state. It uses the `useExperimentVariant` hook to determine if there's a
 * variant mismatch. If the current variant does not match the assigned variant, it temporarily
 * hides the WrappedComponent by rendering it with `visibility: hidden`. Once the correct variant
 * is confirmed, it renders the WrappedComponent normally.
 *
 * @param {React.ComponentType<DotcmsPageProps>} WrappedComponent - The React component that will be
 *        conditionally rendered based on the experiment variant.
 * @param {DotcmsPageProps} props - Props expected by the WrappedComponent, along with any additional
 *        props that extend from DotcmsPageProps.
 * @returns {React.ReactElement} A React element that either renders the WrappedComponent hidden or visible
 *          based on the experiment variant.
 */
export const DotExperimentHandlingComponent: React.FC<ExperimentHandlingProps> = ({
    WrappedComponent,
    ...props
}) => {
    const { shouldWaitForVariant } = useExperimentVariant(props.pageContext.pageAsset);

    if (shouldWaitForVariant) {
        return (
            <div style={{ visibility: 'hidden' }}>
                <WrappedComponent {...props} />
            </div>
        );
    }

    return <WrappedComponent {...props} />;
};

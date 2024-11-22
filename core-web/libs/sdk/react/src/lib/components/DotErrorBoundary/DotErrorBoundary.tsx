import { Component, ReactNode } from 'react';

import { DotError, DotErrorCodes, ERROR_MAP } from './DotError';

interface Props {
    children?: ReactNode;
}

interface State {
    error?: DotError;
}

class DotErrorBoundary extends Component<Props, State> {
    state: State = {};

    static getDerivedStateFromError(error: DotError) {
        // Update state so the next render will show the fallback UI.
        return { error };
    }

    componentDidCatch(error: DotError) {
        // COLXXX: Column error
        // ROWXXX: Row error
        // CONXXX: Container error

        console.error(ERROR_MAP[error.message as DotErrorCodes](error.context));
    }

    render() {
        if (this.state.error) {
            return (
                <div
                    style={{
                        padding: '10px',
                        border: '1px solid red',
                        backgroundColor: 'rgba(255, 0, 0, 0.1)',
                        color: 'red'
                    }}>
                    {
                        ERROR_MAP[this.state.error.message as DotErrorCodes](
                            this.state.error.context
                        ) // Probably will not show anything Im just testing
                    }
                </div>
            );
        }

        return this.props.children;
    }
}

export default DotErrorBoundary;

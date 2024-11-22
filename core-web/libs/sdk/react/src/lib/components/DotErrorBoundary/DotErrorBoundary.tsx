import { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
    children?: ReactNode;
}

interface State {
    hasError: boolean;
}

class DotErrorBoundary extends Component<Props, State> {
    state: State = {
        hasError: false
    };

    constructor(props: object) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError(_: Error) {
        // Update state so the next render will show the fallback UI.
        return { hasError: true };
    }

    componentDidCatch(error: Error, info: ErrorInfo) {
        // Here i should get a code as:
        // COLXXX: Column error
        // ROWXXX: Row error
        // CONXXX: Container error

        // Then consult a map with common errors and show a message that is more user friendly
        // The map could return a function that takes the error and builds the message

        console.error('ErrorBoundary', error, info);
    }

    render() {
        if (this.state.hasError) {
            return null; // Don't show anything
        }

        return this.props.children;
    }
}

export default DotErrorBoundary;

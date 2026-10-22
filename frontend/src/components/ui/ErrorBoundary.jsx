import React from "react";
import ErrorState from "./ErrorState";

export default class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, errorInfo) {
        console.error("ErrorBoundary caught an unhandled error:", error, errorInfo);
    }

    handleRetry = () => {
        this.setState({ hasError: false, error: null });
        if (this.props.onReset) {
            this.props.onReset();
        }
    };

    render() {
        if (this.state.hasError) {
            if (this.props.fallback) {
                return this.props.fallback({
                    error: this.state.error,
                    retry: this.handleRetry
                });
            }

            return (
                <div style={{ padding: "24px" }}>
                    <ErrorState
                        title={this.props.title || "Something went wrong loading this section"}
                        message={this.state.error?.message || "An unexpected error occurred while rendering this page."}
                        onRetry={this.handleRetry}
                        retryLabel="Try Again"
                        secondaryLabel="Reload Page"
                        onSecondaryAction={() => window.location.reload()}
                    />
                </div>
            );
        }

        return this.props.children;
    }
}

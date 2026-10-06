declare module '@analytics/router-utils' {
    /**
     * Callback function called when route changes
     * @param newPath - The new route path
     */
    type RouteChangeCallback = (newPath: string) => void;

    /**
     * Listen to route changes in single page applications
     * Handles Next.js, React Router, Vue Router, and other SPA frameworks
     * @param callback - Function to call when route changes
     */
    function onRouteChange(callback: RouteChangeCallback): void;

    export default onRouteChange;
}


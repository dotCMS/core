const noop = () => {
    //
};

export function mockMatchMedia() {
    // needed in component specs that open a prime-ng modal
    window.matchMedia =
        window.matchMedia ||
        function () {
            return {
                matches: false,
                media: '',
                onchange: null,
                addListener: noop, // deprecated
                removeListener: noop, // deprecated
                addEventListener: noop,
                removeEventListener: noop,
                dispatchEvent: () => true
            };
        };
}

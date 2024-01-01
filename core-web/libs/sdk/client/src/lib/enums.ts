export enum CUSTOMER_ACTIONS {
    SET_URL = 'set-url', // User navigate internally within the ema
    SET_BOUNDS = 'set-bounds', // Receive the position of the rows, columns, containers and contentlets
    SET_CONTENTLET = 'set-contentlet', // Receive the position of the rows, columns, containers and contentlets
    IFRAME_SCROLL = 'scroll', // Emit the scroll inside the iframe
    NOOP = 'noop'
}

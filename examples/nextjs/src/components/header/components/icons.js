/**
 * Icon Components
 *
 * A collection of SVG icon components used throughout the header and search UI.
 * All icons follow a consistent API with `size` and `className` props.
 *
 * @module icons
 *
 * @example
 * import { SearchIcon, CloseIcon } from './icons'
 *
 * <SearchIcon size={24} className="text-blue-500" />
 * <CloseIcon className="hover:text-red-500" />
 */

/**
 * Bot icon - Represents AI/chatbot functionality.
 * Used in the AI search welcome screen.
 *
 * @param {Object} props
 * @param {number} [props.size=64] - Width and height in pixels
 * @param {string} [props.className=''] - Additional CSS classes
 */
export function BotIcon({ size = 64, className = "" }) {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={className}
        >
            <path d="M12 8V4H8" />
            <rect width="16" height="12" x="4" y="8" rx="2" />
            <path d="M2 14h2" />
            <path d="M20 14h2" />
            <path d="M15 13v2" />
            <path d="M9 13v2" />
        </svg>
    );
}

/**
 * Search icon - Magnifying glass for search actions.
 * Used in the search button and search input.
 *
 * @param {Object} props
 * @param {number} [props.size=20] - Width and height in pixels
 * @param {string} [props.className=''] - Additional CSS classes
 */
export function SearchIcon({ size = 20, className = "" }) {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={className}
        >
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
        </svg>
    );
}

/**
 * Close icon - X mark for dismissing/closing elements.
 * Used in the dialog close button and clear search input.
 *
 * @param {Object} props
 * @param {number} [props.size=24] - Width and height in pixels
 * @param {string} [props.className=''] - Additional CSS classes
 */
export function CloseIcon({ size = 24, className = "" }) {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={className}
        >
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
    );
}

/**
 * Logo icon - Concentric circles representing a target/brand mark.
 * Used in the AI search dialog header.
 *
 * @param {Object} props
 * @param {number} [props.size=24] - Width and height in pixels
 * @param {string} [props.className=''] - Additional CSS classes
 */
export function LogoIcon({ size = 24, className = "" }) {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={className}
        >
            <circle cx="12" cy="12" r="10" />
            <circle cx="12" cy="12" r="3" />
        </svg>
    );
}

/**
 * Document icon - Generic file/page representation.
 * Used as fallback icon in search results for unknown content types.
 *
 * @param {Object} props
 * @param {number} [props.size=24] - Width and height in pixels
 * @param {string} [props.className=''] - Additional CSS classes
 */
export function DocumentIcon({ size = 24, className = "" }) {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={className}
        >
            <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
            <polyline points="14 2 14 8 20 8" />
        </svg>
    );
}

/**
 * Blog icon - Document with text lines representing articles/posts.
 * Used in search results for Blog content type.
 *
 * @param {Object} props
 * @param {number} [props.size=24] - Width and height in pixels
 * @param {string} [props.className=''] - Additional CSS classes
 */
export function BlogIcon({ size = 24, className = "" }) {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={className}
        >
            <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
            <line x1="10" y1="9" x2="8" y2="9" />
        </svg>
    );
}

/**
 * Destination icon - Map pin/marker for locations.
 * Used in search results for Destination content type.
 *
 * @param {Object} props
 * @param {number} [props.size=24] - Width and height in pixels
 * @param {string} [props.className=''] - Additional CSS classes
 */
export function DestinationIcon({ size = 24, className = "" }) {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={className}
        >
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
            <circle cx="12" cy="10" r="3" />
        </svg>
    );
}

/**
 * Product icon - Shopping bag representing merchandise/products.
 * Used in search results for Product content type.
 *
 * @param {Object} props
 * @param {number} [props.size=24] - Width and height in pixels
 * @param {string} [props.className=''] - Additional CSS classes
 */
export function ProductIcon({ size = 24, className = "" }) {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={className}
        >
            <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
            <line x1="3" y1="6" x2="21" y2="6" />
            <path d="M16 10a4 4 0 0 1-8 0" />
        </svg>
    );
}

/**
 * Link icon - Chain links representing URLs/hyperlinks.
 * Used in search results to indicate clickable URLs.
 *
 * @param {Object} props
 * @param {number} [props.size=16] - Width and height in pixels
 * @param {string} [props.className=''] - Additional CSS classes
 */
export function LinkIcon({ size = 16, className = "" }) {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={className}
        >
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
        </svg>
    );
}

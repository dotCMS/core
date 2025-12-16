import React from "react";
import Link from "next/link";
import {
    BlogIcon,
    DestinationIcon,
    ProductIcon,
    DocumentIcon,
    LinkIcon,
} from "./icons";

/**
 * Resolves the appropriate URL for a search result based on its content type.
 *
 * Different content types in dotCMS store their URLs in different fields:
 * - Blog: Uses `URL_MAP_FOR_CONTENT` (SEO-friendly URL)
 * - Activity: Uses `url` field directly
 * - Product: Constructs URL from category (e.g., `/store/backpacks`)
 * - Default: Falls back to the `url` field
 *
 * @param {Object} result - The search result object from dotCMS AI search
 * @returns {string|undefined} The resolved URL or undefined if not available
 */
const getURL = (result) => {
    const { url, contentType } = result;
    if (contentType === "Blog") {
        return result.URL_MAP_FOR_CONTENT;
    }
    if (contentType === "Activity") {
        return result.url;
    }
    if (contentType === "Product") {
        const category = Object.keys(result.category?.[0])[0];
        // return `/store/${category}`;
        return `/store/${category}`;
    }
    return url;
};

/**
 * SearchResult - Displays a single AI search result card.
 *
 * Renders a search result with content-type-specific icon, title, metadata,
 * extracted text snippet, and a link to the content.
 *
 * @component
 * @param {Object} props
 * @param {Object} props.result - The search result object from dotCMS AI search
 * @param {string} props.result.title - The content title
 * @param {string} props.result.contentType - The dotCMS content type (Blog, Product, Destination, etc.)
 * @param {string} [props.result.identifier] - Unique content identifier
 * @param {string} [props.result.url] - Direct URL to the content
 * @param {string} [props.result.URL_MAP_FOR_CONTENT] - SEO URL for Blog content
 * @param {Array} [props.result.matches] - Array of match objects from vector search
 * @param {number} [props.result.matches[].distance] - Vector distance (0 = perfect match, 1 = no match)
 * @param {string} [props.result.matches[].extractedText] - Text snippet containing the match
 *
 * @example
 * <SearchResult
 *     result={{
 *         title: 'Costa Rica Adventure',
 *         contentType: 'Destination',
 *         url: '/destinations/costa-rica',
 *         matches: [{ distance: 0.15, extractedText: '...' }]
 *     }}
 * />
 *
 * @remarks
 * - Score percentage is calculated as `(1 - distance) * 100`
 * - Icons are selected based on content type (Blog, Destination, Product, or fallback Document)
 */
function SearchResult({ result }) {
    const { title, contentType, matches } = result;

    const actualURL = getURL(result);

    // Calculate similarity score (inverse of distance)
    // Distance 0 = perfect match (100%), distance 1 = completely different (0%)
    // This gives a more intuitive percentage where higher = better match
    const scorePercentage =
        matches?.[0]?.distance !== undefined
            ? Math.max(
                  0,
                  Math.min(100, Math.round((1 - matches[0].distance) * 100)),
              )
            : null;

    // Format matches text
    const matchCount = matches?.length || 0;
    const matchesText =
        matchCount > 0
            ? `${matchCount} ${matchCount === 1 ? "match" : "matches"}`
            : null;

    // Select icon based on content type
    const getIcon = () => {
        const iconProps = { size: 28, className: "text-orange-500" };

        const normalizedType = contentType?.toLowerCase();

        if (normalizedType?.includes("blog")) {
            return <BlogIcon {...iconProps} />;
        }
        if (normalizedType?.includes("destination")) {
            return <DestinationIcon {...iconProps} />;
        }
        if (normalizedType?.includes("product")) {
            return <ProductIcon {...iconProps} />;
        }

        return <DocumentIcon {...iconProps} />;
    };

    return (
        <div className="bg-blue-50 rounded-lg p-6 hover:bg-blue-100 transition-colors">
            <div className="flex gap-4">
                {/* Icon */}
                <div className="shrink-0">{getIcon()}</div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                    {/* Header with title and metadata */}
                    <div className="flex items-start justify-between gap-4 mb-2">
                        <h3 className="text-xl font-semibold text-gray-900 flex-1">
                            {title || "Untitled"}
                        </h3>
                        <div className="flex items-center gap-4 shrink-0 text-sm text-gray-600">
                            {contentType && (
                                <span className="font-medium">
                                    {contentType}
                                </span>
                            )}
                            {scorePercentage !== null && (
                                <span className="font-medium">
                                    Score: {scorePercentage}%
                                </span>
                            )}
                            {matchesText && (
                                <span className="font-medium">
                                    {matchesText}
                                </span>
                            )}
                        </div>
                    </div>

                    {/* Extracted Text */}
                    {matches?.[0]?.extractedText && (
                        <p className="text-gray-700 text-base mb-3 line-clamp-2">
                            {matches[0].extractedText}
                        </p>
                    )}

                    {/* URL Link */}
                    {actualURL && (
                        <Link
                            href={actualURL}
                            className="text-purple-600 hover:text-purple-700 text-sm inline-flex items-center gap-1 hover:underline"
                        >
                            <LinkIcon className="shrink-0" />
                            {actualURL}
                        </Link>
                    )}
                </div>
            </div>
        </div>
    );
}

export default SearchResult;

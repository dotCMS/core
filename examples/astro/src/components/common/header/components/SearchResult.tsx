import type {
  DotCMSBasicContentlet,
  DotCMSAISearchContentletData,
} from "@dotcms/types";
import {
  BlogIcon,
  DestinationIcon,
  ProductIcon,
  DocumentIcon,
  LinkIcon,
} from "./icons";

interface SearchResultContentlet extends DotCMSBasicContentlet {
  URL_MAP_FOR_CONTENT?: string;
  category?: Record<string, unknown>[];
}

interface SearchResultProps {
  result: DotCMSAISearchContentletData<SearchResultContentlet>;
}

/**
 * Resolves the appropriate URL for a search result based on its content type.
 *
 * Different content types in dotCMS store their URLs in different fields:
 * - Blog: Uses `URL_MAP_FOR_CONTENT` (SEO-friendly URL)
 * - Activity: Uses `url` field directly
 * - Product: Constructs URL from category (e.g., `/store/backpacks`)
 * - Default: Falls back to the `url` field
 *
 * @param {DotCMSAISearchContentletData<SearchResultContentlet>} result - The search result object from dotCMS AI search
 * @returns {string|undefined} The resolved URL or undefined if not available
 */
const getURL = (
  result: DotCMSAISearchContentletData<SearchResultContentlet>,
): string | undefined => {
  const { url, contentType } = result;
  if (contentType === "Blog") {
    return result.URL_MAP_FOR_CONTENT;
  }
  if (contentType === "Activity") {
    return result.url;
  }
  if (contentType === "Product") {
    const category = result.category?.[0]
      ? Object.keys(result.category[0])[0]
      : undefined;
    return category ? `/store/${category}` : undefined;
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
 * @param {DotCMSAISearchContentletData<SearchResultContentlet>} props.result - The search result object from dotCMS AI search
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
function SearchResult({ result }: SearchResultProps) {
  const { title, contentType, matches } = result;

  const actualURL = getURL(result);

  // Calculate similarity score (inverse of distance)
  const scorePercentage =
    matches?.[0]?.distance !== undefined
      ? Math.max(0, Math.min(100, Math.round((1 - matches[0].distance) * 100)))
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
    <div className="bg-violet-50 rounded-lg p-6 hover:bg-violet-100 transition-colors">
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
                <span className="font-medium">{contentType}</span>
              )}
              {scorePercentage !== null && (
                <span className="font-medium">Score: {scorePercentage}%</span>
              )}
              {matchesText && (
                <span className="font-medium">{matchesText}</span>
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
            <a
              href={actualURL}
              className="text-purple-600 hover:text-purple-700 text-sm inline-flex items-center gap-1 hover:underline"
            >
              <LinkIcon className="shrink-0" />
              {actualURL}
            </a>
          )}
        </div>
      </div>
    </div>
  );
}

export default SearchResult;

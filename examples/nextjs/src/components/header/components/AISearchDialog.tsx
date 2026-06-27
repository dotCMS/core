import React, {
    useEffect,
    useRef,
    useState,
    useSyncExternalStore,
} from "react";
import { createPortal } from "react-dom";
import { useAISearch } from "@dotcms/react";

import { DotCMSEntityState } from "@dotcms/types";
import { dotCMSClient } from "@/lib/dotCMSClient";
import { aiSearchIndexName } from "@/config/dotcms.config";
import { BotIcon, SearchIcon, CloseIcon, LogoIcon } from "./icons";
import SearchResult from "./SearchResult";

/**
 * AISearchDialog - A modal dialog component for AI-powered semantic search.
 *
 * Uses the `useAISearch` hook from `@dotcms/react` to perform vector-based
 * similarity searches against a dotCMS AI index.
 *
 * @component
 * @param {Object} props
 * @param {boolean} props.isOpen - Controls the visibility of the dialog
 * @param {function} props.onClose - Callback function invoked when the dialog should close
 *
 * @example
 * const [isOpen, setIsOpen] = useState(false)
 *
 * <AISearchDialog
 *     isOpen={isOpen}
 *     onClose={() => setIsOpen(false)}
 * />
 *
 * @accessibility
 * - Uses `role="dialog"` and `aria-modal="true"` for screen readers
 * - Closes on Escape key press
 * - Closes on click outside the dialog panel
 * - Prevents body scroll when open
 *
 * @remarks
 * The AI index must be created in dotCMS with the following configuration:
 * - Query: `+contentType:(Blog OR Product OR Destination) +live:true +working:true +deleted:false`
 * - Index name: `example-travel-lux`
 */
interface AISearchDialogProps {
    isOpen: boolean;
    onClose: () => void;
}

const emptySubscribe = () => () => {};

function AISearchDialog({ isOpen, onClose }: AISearchDialogProps) {
    const dialogRef = useRef<HTMLDivElement>(null);
    const [searchQuery, setSearchQuery] = useState("");

    // Portals need the DOM. `useSyncExternalStore` returns the client snapshot
    // (true) after hydration and the server snapshot (false) during SSR, so the
    // portal renders only on the client without an effect-driven setState.
    const mounted = useSyncExternalStore(
        emptySubscribe,
        () => true,
        () => false,
    );

    // When creating the index use the following query for better results:
    //+contentType:(Blog OR Product OR Destination) +live:true +working:true +deleted:false
    // And the indexName: example-travel-lux
    const { search, reset, results, status } = useAISearch({
        client: dotCMSClient,
        indexName: aiSearchIndexName,
        params: {
            query: {
                limit: 5,
            },
            config: {
                threshold: 0.4,
            },
        },
    });

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (
                dialogRef.current &&
                !dialogRef.current.contains(event.target as Node)
            ) {
                onClose();
            }
        };

        const handleEscape = (event: KeyboardEvent) => {
            if (event.key !== "Escape") return;

            onClose();
        };

        if (isOpen) {
            document.addEventListener("mousedown", handleClickOutside);
            document.addEventListener("keydown", handleEscape);
        }

        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
            document.removeEventListener("keydown", handleEscape);
        };
    }, [isOpen, onClose]);

    useEffect(() => {
        document.documentElement.style.overflow = isOpen ? "hidden" : "unset";

        return () => {
            document.documentElement.style.overflow = "unset";
        };
    }, [isOpen]);

    const handleSearch = () => {
        if (searchQuery.trim()) {
            search(searchQuery);
        }
    };

    const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") {
            handleSearch();
        }
    };

    const handleClearSearch = () => {
        setSearchQuery("");
        reset();
    };

    if (!isOpen || !mounted) return null;

    // Rendered through a portal to document.body so the fixed overlay fills the
    // viewport — the header's backdrop-blur would otherwise become the
    // containing block for `fixed` and clip the overlay to the header.
    return createPortal(
        <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="search-dialog-title"
            className="fixed inset-0 z-(--z-modal) flex items-start justify-center bg-primary-deep/40 p-4 pt-[12vh] backdrop-blur-sm"
        >
            <div
                ref={dialogRef}
                className="max-h-[80vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-bg shadow-2xl ring-1 ring-line"
            >
                <div className="p-6 sm:p-8">
                    {/* Header */}
                    <div className="mb-6 flex items-center justify-between">
                        <div className="flex items-center gap-2 text-primary">
                            <LogoIcon />
                            <h2
                                id="search-dialog-title"
                                className="font-display text-2xl font-semibold text-ink"
                            >
                                TravelLuxAI
                            </h2>
                        </div>
                        <button
                            onClick={onClose}
                            className="rounded-full p-1 text-muted transition-colors hover:bg-surface hover:text-ink"
                            aria-label="Close dialog"
                        >
                            <CloseIcon />
                        </button>
                    </div>

                    {/* Search Input */}
                    <div className="mb-8 flex gap-3">
                        <div className="relative flex-1">
                            <input
                                type="text"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                onKeyDown={handleKeyPress}
                                placeholder="Search destinations, stories, guides..."
                                className="w-full rounded-full border border-line bg-bg px-5 py-3 pr-10 text-ink placeholder-muted focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                            />
                            {searchQuery && (
                                <button
                                    onClick={handleClearSearch}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted transition-colors hover:text-ink"
                                    aria-label="Clear search"
                                >
                                    <CloseIcon size={18} />
                                </button>
                            )}
                        </div>
                        <button
                            onClick={handleSearch}
                            className="flex items-center gap-2 rounded-full bg-primary px-6 py-3 font-semibold text-bg transition-colors hover:bg-primary-deep"
                        >
                            <SearchIcon />
                            <span className="hidden sm:inline">Search</span>
                        </button>
                    </div>

                    {/* Welcome Section */}
                    {!results?.length &&
                        status.state !== DotCMSEntityState.LOADING && (
                            <div className="py-8 text-center">
                                <div className="mb-6 flex justify-center text-primary">
                                    <BotIcon />
                                </div>
                                <h3 className="mb-3 font-display text-2xl font-semibold text-ink">
                                    Ask TravelLuxAI
                                </h3>
                                <p className="mx-auto max-w-md text-muted">
                                    Search across destinations, stories, and guides
                                    to find your next trip.
                                </p>
                            </div>
                        )}

                    {/* Loading State */}
                    {status.state === DotCMSEntityState.LOADING && (
                        <div className="py-8 text-center">
                            <div className="inline-block size-8 animate-spin rounded-full border-4 border-line border-t-primary" />
                            <p className="mt-4 text-muted">Searching...</p>
                        </div>
                    )}

                    {/* Results */}
                    {results?.length > 0 &&
                        status.state === DotCMSEntityState.SUCCESS && (
                            <div className="space-y-4">
                                {results.map((result, index) => (
                                    <SearchResult
                                        key={result.identifier || index}
                                        result={result}
                                    />
                                ))}
                            </div>
                        )}

                    {/* Error State */}
                    {status.state === DotCMSEntityState.ERROR && (
                        <div className="text-center py-8">
                            <p className="text-red-600">
                                {status.error?.message ||
                                    "An error occurred while searching."}
                            </p>
                        </div>
                    )}
                </div>
            </div>
        </div>,
        document.body,
    );
}

export default AISearchDialog;

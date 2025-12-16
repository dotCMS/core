import React, { useEffect, useRef, useState } from "react";
import { useAISearch } from "@dotcms/react";

import { DotCMSEntityState } from "@dotcms/types";
import { dotCMSClient } from "@/utils/dotCMSClient";
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
function AISearchDialog({ isOpen, onClose }) {
    const dialogRef = useRef(null);
    const [searchQuery, setSearchQuery] = useState("");

    // When creating the index use the following query for better results:
    //+contentType:(Blog OR Product OR Destination) +live:true +working:true +deleted:false
    // And the indexName: example-travel-lux
    const { search, reset, results, status } = useAISearch({
        client: dotCMSClient,
        indexName: "example-travel-lux",
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
        const handleClickOutside = (event) => {
            if (
                dialogRef.current &&
                !dialogRef.current.contains(event.target)
            ) {
                onClose();
            }
        };

        const handleEscape = (event) => {
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
            document.body.style.overflow = "unset";
        };
    }, [isOpen]);

    const handleSearch = () => {
        if (searchQuery.trim()) {
            search(searchQuery);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === "Enter") {
            handleSearch();
        }
    };

    const handleClearSearch = () => {
        setSearchQuery("");
        reset();
    };

    if (!isOpen) return null;

    return (
        <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="search-dialog-title"
            className="absolute inset-0 z-50 flex items-center justify-center bg-black/40"
        >
            <div
                ref={dialogRef}
                className="bg-white rounded-xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-y-auto mx-4"
            >
                <div className="p-8">
                    {/* Header */}
                    <div className="flex items-center justify-between mb-6">
                        <div className="flex items-center gap-2">
                            <LogoIcon className="text-gray-700" />
                            <h2
                                id="search-dialog-title"
                                className="text-2xl font-semibold text-gray-800"
                            >
                                TravelLuxAI
                            </h2>
                        </div>
                        <button
                            onClick={onClose}
                            className="text-gray-400 hover:text-gray-600 transition-colors"
                            aria-label="Close dialog"
                        >
                            <CloseIcon />
                        </button>
                    </div>

                    {/* Search Input */}
                    <div className="flex gap-3 mb-8">
                        <div className="relative flex-1">
                            <input
                                type="text"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                onKeyDown={handleKeyPress}
                                placeholder="Search the site..."
                                className="w-full px-4 py-3 pr-10 border-2 border-blue-500 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-700 placeholder-gray-400"
                            />
                            {searchQuery && (
                                <button
                                    onClick={handleClearSearch}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                                    aria-label="Clear search"
                                >
                                    <CloseIcon size={18} />
                                </button>
                            )}
                        </div>
                        <button
                            onClick={handleSearch}
                            className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors flex items-center gap-2"
                        >
                            <SearchIcon />
                            Search
                        </button>
                    </div>

                    {/* Welcome Section */}
                    {!results?.length &&
                        status.state !== DotCMSEntityState.LOADING && (
                            <div className="text-center py-8">
                                <div className="flex justify-center mb-6">
                                    <BotIcon className="text-blue-600" />
                                </div>
                                <h3 className="text-3xl font-semibold text-gray-800 mb-4">
                                    Welcome to TravelLuxAI
                                </h3>
                                <p className="text-gray-600 text-lg max-w-xl mx-auto">
                                    I can help you find information about the
                                    TravelLux service
                                </p>
                            </div>
                        )}

                    {/* Loading State */}
                    {status.state === DotCMSEntityState.LOADING && (
                        <div className="text-center py-8">
                            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-gray-200 border-t-blue-600" />
                            <p className="mt-4 text-gray-600">Searching...</p>
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
        </div>
    );
}

export default AISearchDialog;

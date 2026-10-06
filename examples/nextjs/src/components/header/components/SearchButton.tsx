"use client";

import { SearchIcon } from "./icons";

/**
 * SearchButton - A trigger button to open the AI search dialog.
 *
 * A simple presentational component that renders a search icon button.
 * State management is handled by the parent component (Header).
 *
 * @component
 * @param {Object} props
 * @param {function} props.onClick - Callback function invoked when the button is clicked
 *
 * @example
 * <SearchButton onClick={() => setIsSearchOpen(true)} />
 *
 * @accessibility
 * - Has `aria-label` for screen readers
 * - Includes hover state for visual feedback
 */
interface SearchButtonProps {
    onClick: () => void;
}

function SearchButton({ onClick }: SearchButtonProps) {
    return (
        <button
            type="button"
            aria-label="Search destinations and stories"
            onClick={onClick}
            className="group flex items-center gap-2.5 rounded-full border border-line bg-surface px-3 text-muted transition-colors hover:border-primary/40 hover:bg-primary-tint hover:text-primary md:h-11 md:w-72 md:px-4 lg:w-80"
        >
            <SearchIcon size={20} className="shrink-0 transition-colors" />
            <span className="hidden text-[0.95rem] md:inline">
                Search destinations &amp; stories
            </span>
            <kbd className="ml-auto hidden rounded-md border border-line bg-bg px-1.5 py-0.5 font-sans text-xs font-medium text-muted md:inline-block">
                /
            </kbd>
        </button>
    );
}

export default SearchButton;

import { SearchIcon } from "./icons";

interface SearchButtonProps {
  /** Callback function invoked when the button is clicked */
  onClick: () => void;
}

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
function SearchButton({ onClick }: SearchButtonProps) {
  return (
    <button
      aria-label="toggle-search"
      className="flex items-center justify-center pl-2 hover:cursor-pointer"
      onClick={onClick}
    >
      <SearchIcon className="text-white hover:text-gray-300" />
    </button>
  );
}

export default SearchButton;

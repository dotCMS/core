import { useState } from "react";
import { useIsEditMode } from "@/hooks";
import { ReorderMenuButton } from "@/components/ui";
import type { DotCMSNavigationItem } from "@dotcms/types";
import { SearchButton, AISearchDialog } from "./components";

interface HeaderProps {
  /** Navigation data from dotCMS containing menu structure */
  navigation: DotCMSNavigationItem;
}

interface NavigationProps {
  /** Array of navigation items to render */
  navItems: DotCMSNavigationItem[];
}

/**
 * Header - Main application header with navigation and AI search.
 *
 * Renders the site header with logo, navigation menu, and AI-powered search functionality.
 * Integrates with dotCMS Universal Visual Editor (UVE) for content management.
 *
 * @component
 * @param {Object} props
 * @param {DotCMSNavigationItem} props.navigation - Navigation data from dotCMS containing menu structure
 *
 * @example
 * <Header navigation={pageResponse.nav} />
 *
 * @remarks
 * - Shows ReorderMenuButton when in dotCMS edit mode
 * - Includes AI-powered search dialog for semantic content search
 * - Navigation highlights current page based on URL pathname
 */
function Header({ navigation }: HeaderProps) {
  const isEditMode = useIsEditMode();
  const navItems = navigation?.children;
  const [isSearchOpen, setIsSearchOpen] = useState(false);

  return (
    <div className="flex items-center justify-between p-4 bg-violet-800">
      <div className="flex items-center">
        <h2 className="text-3xl font-bold text-white">
          <a href="/">TravelLux in Astro</a>
        </h2>

        {isEditMode && <ReorderMenuButton />}
      </div>

      <div className="flex items-center space-x-4">
        {navItems && <Navigation navItems={navItems} />}
        <SearchButton onClick={() => setIsSearchOpen(true)} />
      </div>
      <AISearchDialog
        isOpen={isSearchOpen}
        onClose={() => setIsSearchOpen(false)}
      />
    </div>
  );
}

/**
 * Navigation - Renders the main navigation menu.
 *
 * @component
 * @param {Object} props
 * @param {DotCMSNavigationItem[]} props.navItems - Array of navigation items from dotCMS
 *
 * @remarks
 * - Automatically highlights current page based on URL pathname
 * - Supports external links via target attribute
 */
function Navigation({ navItems }: NavigationProps) {
  const pathname = window.location.pathname;

  return (
    <nav>
      <ul className="flex space-x-4 text-white">
        <li>
          <a
            href="/"
            className={`underline-offset-4 hover:underline ${pathname === "/" && "underline"}`}
          >
            Home
          </a>
        </li>
        {navItems.map(({ folder, href, target, title }) => (
          <li key={folder}>
            <a
              href={href}
              className={`underline-offset-4 hover:underline ${pathname === href && "underline"}`}
              target={target}
            >
              {title}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export default Header;

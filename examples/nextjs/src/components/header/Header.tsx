"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";

import { cn } from "@/lib/utils";
import { useIsEditMode } from "@/hooks/useIsEditMode";
import type { NavItem } from "@/types/content";
import { ReorderMenuButton } from "../editor/ReorderMenuButton";
import AISearchDialog from "./components/AISearchDialog";
import SearchButton from "./components/SearchButton";

interface HeaderProps {
    navItems?: NavItem[];
}

function Header({ navItems }: HeaderProps) {
    const isEditMode = useIsEditMode();
    const [isSearchOpen, setIsSearchOpen] = useState(false);
    const [isMenuOpen, setIsMenuOpen] = useState(false);

    // Press "/" to open search (ignored while typing in a field).
    useEffect(() => {
        const onKeyDown = (e: KeyboardEvent) => {
            if (e.key !== "/" || e.metaKey || e.ctrlKey || e.altKey) return;
            const el = e.target as HTMLElement | null;
            if (el && /^(INPUT|TEXTAREA|SELECT)$/.test(el.tagName)) return;
            if (el?.isContentEditable) return;
            e.preventDefault();
            setIsSearchOpen(true);
        };
        document.addEventListener("keydown", onKeyDown);
        return () => document.removeEventListener("keydown", onKeyDown);
    }, []);

    return (
        <header className="sticky top-0 z-(--z-sticky) border-b border-line bg-bg/85 backdrop-blur-md">
            <div className="container mx-auto flex h-16 items-center justify-between gap-4 px-4 sm:px-6">
                <div className="flex shrink-0 items-center gap-3">
                    <Link
                        href="/"
                        className="font-display text-2xl font-semibold tracking-tight text-primary-deep"
                    >
                        TravelLux
                    </Link>
                    {isEditMode && <ReorderMenuButton />}
                </div>

                <div className="flex shrink-0 items-center gap-3 sm:gap-5">
                    {navItems && (
                        <Navigation navItems={navItems} className="hidden md:flex" />
                    )}
                    <SearchButton onClick={() => setIsSearchOpen(true)} />
                    <button
                        type="button"
                        aria-label={isMenuOpen ? "Close menu" : "Open menu"}
                        aria-expanded={isMenuOpen}
                        onClick={() => setIsMenuOpen((open) => !open)}
                        className="grid size-10 place-items-center rounded-full text-ink transition-colors hover:bg-surface md:hidden"
                    >
                        <MenuIcon open={isMenuOpen} />
                    </button>
                </div>
            </div>

            {/* Mobile navigation */}
            {navItems && isMenuOpen && (
                <Navigation
                    navItems={navItems}
                    onNavigate={() => setIsMenuOpen(false)}
                    className="flex flex-col gap-1 border-t border-line px-4 py-3 md:hidden"
                />
            )}

            <AISearchDialog
                isOpen={isSearchOpen}
                onClose={() => setIsSearchOpen(false)}
            />
        </header>
    );
}

interface NavigationProps {
    navItems: NavItem[];
    className?: string;
    onNavigate?: () => void;
}

function Navigation({ navItems, className, onNavigate }: NavigationProps) {
    const pathname = usePathname();

    const items = [{ folder: "__home", href: "/", target: "", title: "Home" }, ...navItems];

    return (
        <nav aria-label="Primary" className={className}>
            {items.map(({ folder, href, target, title }) => {
                const isActive = pathname === href;
                return (
                    <Link
                        key={folder}
                        href={{ pathname: href }}
                        target={target || undefined}
                        aria-current={isActive ? "page" : undefined}
                        onClick={onNavigate}
                        className={cn(
                            "relative rounded-full px-3 py-2 text-sm font-medium transition-colors",
                            "after:absolute after:inset-x-3 after:-bottom-px after:h-0.5 after:origin-left after:scale-x-0 after:rounded-full after:bg-accent after:transition-transform after:duration-300 after:content-['']",
                            "hover:text-primary hover:after:scale-x-100",
                            isActive ? "text-primary after:scale-x-100" : "text-ink",
                        )}
                    >
                        {title}
                    </Link>
                );
            })}
        </nav>
    );
}

function MenuIcon({ open }: { open: boolean }) {
    return (
        <svg
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            aria-hidden="true"
        >
            {open ? (
                <>
                    <line x1="6" y1="6" x2="18" y2="18" />
                    <line x1="6" y1="18" x2="18" y2="6" />
                </>
            ) : (
                <>
                    <line x1="3" y1="7" x2="21" y2="7" />
                    <line x1="3" y1="12" x2="21" y2="12" />
                    <line x1="3" y1="17" x2="21" y2="17" />
                </>
            )}
        </svg>
    );
}

export default Header;

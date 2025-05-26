"use client";

import Link from "next/link";

import { useIsEditMode } from "@/hooks/isEditMode";
import { ReorderMenuButton } from "./editor/ReorderMenuButton";
import { usePathname } from "next/navigation";

function Header({ navItems }) {
    const isEditMode = useIsEditMode();

    return (
        <div className="flex items-center justify-between p-4 bg-blue-500">
            <div className="flex items-center">
                <h2 className="text-3xl font-bold text-white">
                    <Link href="/">TravelLux in NextJS</Link>
                </h2>

                {isEditMode && <ReorderMenuButton />}
            </div>

            {navItems && <Navigation navItems={navItems} />}
        </div>
    );
}

function Navigation({ navItems }) {
    const pathname = usePathname();

    return (
        <nav>
            <ul className="flex space-x-4 text-white">
                <li>
                    <Link
                        href={{ pathname: "/" }}
                        className={`underline-offset-4 hover:underline ${pathname === "/" && "underline"}`}
                    >
                        Home
                    </Link>
                </li>
                {navItems.map(({ folder, href, target, title }) => (
                    <li key={folder}>
                        <Link
                            href={{ pathname: href }}
                            className={`underline-offset-4 hover:underline ${pathname === href && "underline"}`}
                            target={target}
                        >
                            {title}
                        </Link>
                    </li>
                ))}
            </ul>
        </nav>
    );
}

export default Header;

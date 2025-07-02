import { useIsEditMode } from "@/hooks";
import { ReorderMenuButton } from "@/components/ui";
import type { DotCMSNavigationItem } from "@dotcms/types";

function Header({ navigation }: { navigation: DotCMSNavigationItem }) {
    const isEditMode = useIsEditMode();
    const navItems = navigation?.children;

    return (
        <div className="flex items-center justify-between p-4 bg-violet-800">
            <div className="flex items-center">
                <h2 className="text-3xl font-bold text-white">
                    <a href="/">TravelLux in Astro</a>
                </h2>

                {isEditMode && <ReorderMenuButton />}
            </div>

            {navItems && <Navigation navItems={navItems} />}
        </div>
    );
}

function Navigation({ navItems }: { navItems: DotCMSNavigationItem[] }) {
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
                {navItems.map(({ folder, href, target, title }: any) => (
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

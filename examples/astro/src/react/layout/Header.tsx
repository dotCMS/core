// import { useIsEditMode } from "@react/hooks/isEditMode";

function Header({ navItems }: { navItems: any }) {
    console.log(navItems);
    // const isEditMode = useIsEditMode();

    return (
        <div className="flex items-center justify-between p-4 bg-blue-500">
            <div className="flex items-center">
                <h2 className="text-3xl font-bold text-white">
                    <a href="/">TravelLux in NextJS</a>
                </h2>

                {/* {isEditMode && <ReorderMenuButton />} */}
            </div>

            {navItems && <Navigation navItems={navItems} />}
        </div>
    );
}

function Navigation({ navItems }: { navItems: any }) {
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

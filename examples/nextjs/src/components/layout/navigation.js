import Link from "next/link";
import { useSearchParams, usePathname } from "next/navigation";

function Navigation({ items, className }) {
  const searchParams = useSearchParams();
  // Add usePathname hook to get the current path
  const pathname = usePathname();

  return (
    <nav className={className}>
      <ul className="flex space-x-4 text-white">
        <li>
          <Link
            href={{
              pathname: '/',
              query: Object.fromEntries(searchParams.entries())
            }}
            // Add active class if the current path is home
            className={pathname === '/' ? 'font-bold' : ''}>
            Home
          </Link>
        </li>
        {items.map((item) => (
          <li key={item.folder}>
            <Link
              href={{
                pathname: item.href,
                query: Object.fromEntries(searchParams.entries())
              }}
              target={item.target}
              // Add active class if the current path matches the item's href
              className={pathname === item.href ? 'font-bold' : ''}>
              {item.title}
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export default Navigation;

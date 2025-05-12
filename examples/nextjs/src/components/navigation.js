import Link from "next/link";
import { useSearchParams } from "next/navigation";

function Navigation({ items, className }) {
  const searchParams = useSearchParams();

  return (
    <nav className={className}>
      <ul className="flex space-x-4 text-white">
        <li>
          <Link
            href={{
              pathname: '/',
              query: Object.fromEntries(searchParams.entries())
            }}>
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
              target={item.target}>
              {item.title}
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export default Navigation;

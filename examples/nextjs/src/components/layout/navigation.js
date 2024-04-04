import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useExperiments } from "@dotcms/experiments";


function Navigation({ items, className }) {
    const searchParams = useSearchParams();
    const {getVariantAsQueryParamObject} = useExperiments();

    const currentQueryParams = Object.fromEntries(searchParams.entries())

    return (
        <nav className={className}>
            <ul className="flex space-x-4 text-white">
                <li>
                    <Link
                        href={{
                            pathname: '/',
                          query: {...getVariantAsQueryParamObject('/', currentQueryParams)} // We need to maintain the query params on the navigation, this way next loads the page with the same query params
                        }}>
                        Home
                    </Link>
                </li>
                {items.map((item) => (
                    <li key={item.folder}>
                        <Link
                            href={{
                                pathname: item.href,
                                query: {...getVariantAsQueryParamObject(item.href, currentQueryParams)} // We need to maintain the query params on the navigation, this way next loads the page with the same query params
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

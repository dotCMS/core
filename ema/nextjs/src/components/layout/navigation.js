import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { LOCAL_STORAGE_KEY, QUERY_PARAM_VARIANT_KEY } from '../../../public/experiment_logic';

function Navigation({ items, className }) {
    const searchParams = useSearchParams();
    // I dont want mantain the queryParam of Experiment
    const queryParams = Object.fromEntries(
        [...searchParams.entries()].filter(([key]) => key !== QUERY_PARAM_VARIANT_KEY)
    );

    return (
        <nav className={className}>
            <ul className="flex space-x-4 text-white">
                <li>
                    <Link
                        href={{
                            pathname: '/',
                            query: queryParams // We need to maintain the query params on the navigation, this way next loads the page with the same query params
                        }}>
                        Home
                    </Link>
                </li>
                {items.map((item) => (
                    <li key={item.folder}>
                        <Link
                            href={{
                                pathname: item.href,
                                query: queryParams // We need to maintain the query params on the navigation, this way next loads the page with the same query params
                            }}
                            target={item.target}>
                            {item.title}
                        </Link>
                    </li>
                ))}
                <li>
                    <Link
                        href={{
                            pathname: '/sale',
                            query: queryParams // We need to maintain the query params on the navigation, this way next loads the page with the same query params
                        }}>
                        Sale
                    </Link>
                </li>
            </ul>
        </nav>
    );
}

export default Navigation;

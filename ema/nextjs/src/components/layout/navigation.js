import Link from 'next/link';

function Navigation({ nav, className }) {
    return (
        <nav className={className}>
            <ul className="flex space-x-4">
                <li>
                    <Link href="/">Home</Link>
                </li>
                {nav.map((item) => (
                    <li key={item.folder}>
                        <Link href={item.href} target={item.target}>
                            {item.title}
                        </Link>
                    </li>
                ))}
            </ul>
        </nav>
    );
}

export default Navigation;

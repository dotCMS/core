import Link from 'next/link';
import ReorderButton from './components/reorderMenu';

function Header({ children }) {
    return (
        <header className="flex items-center justify-between p-4 bg-purple-500">
            <div className="flex items-center">
                <h2 className="text-3xl font-bold text-white">
                    <Link href="/">TravelLux in NextJS</Link>
                </h2>
            </div>
            <ReorderButton />
            {children}
        </header>
    );
}




export default Header;

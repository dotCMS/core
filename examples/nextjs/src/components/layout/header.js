import Link from "next/link";
import Image from "next/image";

function Header({ children }) {
    return (
        <header className="bg-purple-800">
            <div className="container">
                <div className="flex justify-between items-center p-4">
                    <Link
                        href="/"
                        className="flex gap-2 items-center text-3xl font-medium text-white"
                    >
                        <Image
                            className="invert"
                            src="/local/nextjs.svg"
                            alt="TravelLux"
                            width={50}
                            height={50}
                        />
                        TravelLux
                    </Link>
                    {children}
                </div>
            </div>
        </header>
    );
}

export default Header;

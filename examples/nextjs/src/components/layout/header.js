import Link from 'next/link';
import { reorderMenu } from '@dotcms/client';

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

function ReorderButton() {
    const ArrowDown = () => (
        <svg 
            xmlns="http://www.w3.org/2000/svg" 
            xmlnsXlink="http://www.w3.org/1999/xlink" 
            version="1.1" 
            width="24" 
            height="24" 
            viewBox="0 0 256 256"
            xmlSpace="preserve"
        >
            <g 
                style={{
                    stroke: "none",
                    strokeWidth: 0,
                    strokeDasharray: "none", 
                    strokeLinecap: "butt",
                    strokeLinejoin: "miter",
                    strokeMiterlimit: 10,
                    fill: "none",
                    fillRule: "nonzero",
                    opacity: 1
                }}
                transform="translate(1.4065934065934016 1.4065934065934016) scale(2.81 2.81)"
            >
                <path 
                    d="M 90 24.25 c 0 -0.896 -0.342 -1.792 -1.025 -2.475 c -1.366 -1.367 -3.583 -1.367 -4.949 0 L 45 60.8 L 5.975 21.775 c -1.367 -1.367 -3.583 -1.367 -4.95 0 c -1.366 1.367 -1.366 3.583 0 4.95 l 41.5 41.5 c 1.366 1.367 3.583 1.367 4.949 0 l 41.5 -41.5 C 89.658 26.042 90 25.146 90 24.25 z"
                    style={{
                        stroke: "none",
                        strokeWidth: 1,
                        strokeDasharray: "none",
                        strokeLinecap: "butt",
                        strokeLinejoin: "miter", 
                        strokeMiterlimit: 10,
                        fill: "#FFFFFF",
                        fillRule: "nonzero",
                        opacity: 1
                    }}
                    transform="matrix(1 0 0 1 0 0)"
                />
            </g>
        </svg>
    )

    const ArrowUp = () => (
        <svg 
            xmlns="http://www.w3.org/2000/svg" 
            xmlnsXlink="http://www.w3.org/1999/xlink" 
            version="1.1" 
            width="24" 
            height="24" 
            viewBox="0 0 256 256"
            xmlSpace="preserve"
        >
            <g
                style={{
                    stroke: "none", 
                    strokeWidth: 0,
                    strokeDasharray: "none",
                    strokeLinecap: "butt",
                    strokeLinejoin: "miter",
                    strokeMiterlimit: 10,
                    fill: "none",
                    fillRule: "nonzero",
                    opacity: 1
                }}
                transform="translate(1.4065934065934016 1.4065934065934016) scale(2.81 2.81)"
            >
                <path
                    d="M 90 65.75 c 0 0.896 -0.342 1.792 -1.025 2.475 c -1.366 1.367 -3.583 1.367 -4.949 0 L 45 29.2 L 5.975 68.225 c -1.367 1.367 -3.583 1.367 -4.95 0 c -1.366 -1.367 -1.366 -3.583 0 -4.95 l 41.5 -41.5 c 1.366 -1.367 3.583 -1.367 4.949 0 l 41.5 41.5 C 89.658 63.958 90 64.854 90 65.75 z"
                    style={{
                        stroke: "none",
                        strokeWidth: 1,
                        strokeDasharray: "none", 
                        strokeLinecap: "butt",
                        strokeLinejoin: "miter",
                        strokeMiterlimit: 10,
                        fill: "#FFFFFF",
                        fillRule: "nonzero",
                        opacity: 1
                    }}
                    transform="matrix(1 0 0 1 0 0)"
                />
            </g>
        </svg>
    )
    return (
        <button className="bg-[#426BF0] rounded-sm flex cursor-pointer border-none px-2 py-1 gap-2"  onClick={() => reorderMenu()} >
            <ArrowUp />
            <ArrowDown />
        </button>
    );
}


export default Header;

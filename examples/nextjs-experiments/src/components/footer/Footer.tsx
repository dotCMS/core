import Link from "next/link";

import type { Blog, Destination } from "@/types/content";
import Blogs from "./components/blogs";
import Destinations from "./components/destinations";

interface FooterProps {
    blogs?: Blog[];
    destinations?: Destination[];
}

function Footer({ blogs, destinations }: FooterProps) {
    return (
        <footer className="bg-primary-deep text-bg">
            <div className="container mx-auto grid gap-12 px-4 py-16 sm:px-6 md:grid-cols-[1.4fr_1fr_1fr] md:py-20 lg:gap-16">
                <div className="flex flex-col gap-5">
                    <Link
                        href="/"
                        className="font-display text-3xl font-semibold tracking-tight"
                    >
                        TravelLux
                    </Link>
                    <p className="max-w-sm text-[0.95rem] leading-relaxed text-bg/75">
                        A travel journal of places worth the journey. Our writers
                        and photographers share first-hand guides to where to go
                        next, and how to make the most of it once you arrive.
                    </p>
                </div>

                <Blogs blogs={blogs} />
                <Destinations destinations={destinations} />
            </div>

            <div className="border-t border-bg/15">
                <div className="container mx-auto flex flex-col items-center justify-between gap-2 px-4 py-6 text-sm text-bg/60 sm:flex-row sm:px-6">
                    <p>&copy; {new Date().getFullYear()} TravelLux. All rights reserved.</p>
                    <p>Built with dotCMS &amp; Next.js</p>
                </div>
            </div>
        </footer>
    );
}

export default Footer;

import type { Metadata } from "next";
import { Hanken_Grotesk, Spectral } from "next/font/google";
import "./globals.css";

const spectral = Spectral({
    subsets: ["latin"],
    weight: ["400", "500", "600", "700"],
    style: ["normal", "italic"],
    variable: "--font-spectral",
    display: "swap",
});

const hanken = Hanken_Grotesk({
    subsets: ["latin"],
    variable: "--font-hanken",
    display: "swap",
});

export const metadata: Metadata = {
    title: "TravelLux — Travel stories, destinations & guides",
    description:
        "An editorial travel front end powered by dotCMS and Next.js: destinations, stories, and guides, fully editable in the Universal Visual Editor.",
};

export default function RootLayout({
    children,
}: Readonly<{ children: React.ReactNode }>) {
    return (
        <html lang="en" className={`${spectral.variable} ${hanken.variable}`}>
            <body>{children}</body>
        </html>
    );
}

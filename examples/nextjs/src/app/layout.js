import { Inter } from "next/font/google";
import "./globals.css";
import SchemaRegistry from "@/components/editor/SchemaRegistry";

const inter = Inter({ subsets: ["latin"] });

export default function RootLayout({ children }) {
    return (
        <html lang="en">
            <body className={inter.className}>
                <SchemaRegistry />
                {children}
            </body>
        </html>
    );
}

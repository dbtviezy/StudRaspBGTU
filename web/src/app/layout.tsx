import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "СтудРаспБГТУ",
  description: "Расписание занятий БГТУ: пары, числитель и знаменатель, дела, заметки и оценки преподавателей",
  applicationName: "СтудРаспБГТУ",
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    title: "СтудРасп",
    statusBarStyle: "default",
  },
  formatDetection: { telephone: false },
  icons: {
    icon: [
      { url: "/icons/favicon-32.png", sizes: "32x32", type: "image/png" },
      { url: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
    ],
    apple: [{ url: "/icons/apple-touch-icon.png", sizes: "180x180" }],
  },
  other: {
    "apple-mobile-web-app-capable": "yes",
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: "#F4F6FB",
};

// Applies the saved theme before first paint so dark mode does not flash.
const themeScript = `try{var d=localStorage.getItem("studrasp.dark")==="true";document.documentElement.dataset.theme=d?"dark":"light";var l=JSON.parse(localStorage.getItem("studrasp.lang")||"null");if(l)document.documentElement.lang=l}catch(e){}`;

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ru" data-theme="light" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
      <body>{children}</body>
    </html>
  );
}

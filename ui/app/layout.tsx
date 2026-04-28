import "./globals.css";

export const metadata = { title: "Revenue Command Center" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <html lang="en">
        <body className="bg-slate-900 text-white min-h-screen">
          <main className="container mx-auto p-8">{children}</main>
        </body>
      </html>
    </>
  );
}

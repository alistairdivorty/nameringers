import Layout from '@/components/Layout';
import '../styles/globals.css';
import type { AppProps } from 'next/app';
import { Londrina_Solid, Noto_Sans_Display } from '@next/font/google';
import { DomainsContextProvider } from '@/context/domainsContext';

const londrina = Londrina_Solid({
    weight: '400',
    subsets: ['latin'],
    variable: '--font-londrina',
});

const noto = Noto_Sans_Display({
    subsets: ['latin'],
    variable: '--font-noto',
});

export default function App({ Component, pageProps }: AppProps) {
    return (
        <main className={`${londrina.variable} ${noto.variable} font-noto`}>
            <DomainsContextProvider>
                <Layout>
                    <Component {...pageProps} />
                </Layout>
            </DomainsContextProvider>
        </main>
    );
}

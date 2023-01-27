import Head from 'next/head';
import Hero from '@/components/Hero';
import Box from '@/components/Box';
import Domains from '@/components/Domains';
import SearchForm from '@/components/SearchForm';
import useDomainsContext from '@/hooks/useDomainsContext';

export default function Home() {
    const { domains, isLoading } = useDomainsContext() ?? {};

    return (
        <>
            <Head>
                <title>NameRingers</title>
                <meta
                    name="description"
                    content="Protect your brand name by carry out approximate search of newly registered domains."
                />
                <meta
                    name="viewport"
                    content="width=device-width, initial-scale=1"
                />
                <link rel="icon" href="/favicon.ico" />
            </Head>
            <main className="bg-slate-100 overflow-hidden">
                <div className="min-h-screen h-full flex flex-col gap-12 pb-14">
                    <Hero className="lg:h-[75vh]" />
                    <div className="place-self-center flex flex-col items-center w-full max-w-7xl space-y-8 text-indigo-900 px-6">
                        <Box className="w-full">
                            <div className="w-full flex flex-col lg:flex-row lg:items-center gap-5">
                                <div className="lg:w-1/3 space-y-2">
                                    <h2 className="font-londrina text-3xl">
                                        Search domains
                                    </h2>
                                    <p className="text-md">
                                        Find domain names registered yesterday,
                                        ranked by similarity to your query.
                                    </p>
                                </div>
                                <SearchForm className="flex-1" />
                            </div>
                        </Box>
                        <Domains
                            domains={domains ?? []}
                            isLoading={isLoading ?? false}
                        />
                    </div>
                </div>
            </main>
        </>
    );
}

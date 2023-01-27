import { useEffect, useState } from 'react';
import BrandName from '@/components/BrandName';

const Header = () => {
    const [yOffset, setYOffset] = useState(0);

    useEffect(() => {
        const onScroll = () => setYOffset(window.pageYOffset);
        window.removeEventListener('scroll', onScroll);
        window.addEventListener('scroll', onScroll, { passive: true });
        return () => window.removeEventListener('scroll', onScroll);
    }, []);

    return (
        <header className="fixed top-0 w-screen flex justify-start items-center text-white z-20 h-20">
            <div
                className="absolute w-full h-full bg-slate-300"
                style={{
                    opacity: yOffset / 300,
                }}
            />
            <BrandName className="text-4xl mx-8 z-20" />
        </header>
    );
};

export default Header;

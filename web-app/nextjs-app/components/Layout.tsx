import React from 'react';
import Header from '@/components/Header';
import Footer from '@/components/Footer';

interface Props {
    children: React.ReactNode;
}

const Layout = ({ children }: Props) => {
    return (
        <>
            <Header />
            <main>{children}</main>
            <Footer />
        </>
    );
};

export default Layout;

import React from 'react';
import clsx from 'clsx';

interface Props {
    children: React.ReactNode;
    className?: String;
}

const Box = ({ children, className }: Props) => (
    <div
        className={clsx(
            className,
            'bg-gradient-to-bl from-blue-300 to-emerald-300 p-10 rounded-3xl'
        )}
    >
        {children}
    </div>
);

export default Box;

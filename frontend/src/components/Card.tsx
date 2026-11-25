
import React from 'react';
import './Card.css';

interface CardProps {
    title: string;
    children: React.ReactNode;
    className?: string;
}

export const Card: React.FC<CardProps> = ({
    title,
    children,
    className = '',
}) => {
    return (
        <section className={`responsive-card ${className}`}>
            <h3 className="responsive-card__title">
                {title}
            </h3>

            <div className="responsive-card__content">
                {children}
            </div>
        </section>
    );
};

export default Card;

import React from 'react';
import './Skeleton.css';

export type SkeletonVariant = 'line' | 'circle' | 'rect';

export interface SkeletonProps {
    variant?: SkeletonVariant;
    width?: string | number;
    height?: string | number;
    className?: string;
}

function toCssSize(value: string | number | undefined): string | undefined {
    if (value === undefined) return undefined;
    return typeof value === 'number' ? `${value}px` : value;
}

export const Skeleton: React.FC<SkeletonProps> = ({
    variant = 'line',
    width,
    height,
    className = '',
}) => {
    const defaultHeight =
        height ?? (variant === 'circle' ? 40 : variant === 'rect' ? 120 : 16);
    const defaultWidth =
        width ?? (variant === 'circle' ? defaultHeight : '100%');

    const style: React.CSSProperties = {
        width: toCssSize(defaultWidth),
        height: toCssSize(defaultHeight),
    };

    return (
        <span
            className={`skeleton skeleton--${variant} ${className}`.trim()}
            style={style}
            aria-hidden="true"
        />
    );
};
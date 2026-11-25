export function formatCurrency(
    amountString: string,
    currency: string
): string {
    const amount = Number(amountString);

    if (!Number.isFinite(amount)) return amountString;

    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(amount);
}

export function formatNumber(n: number): string {
    return new Intl.NumberFormat('en-IN').format(n);
}

export function formatRelativeTime(isoString: string): string {
    const date = new Date(isoString);

    if (Number.isNaN(date.getTime())) return 'Unknown';

    const seconds = Math.floor((date.getTime() - Date.now()) / 1000);
    const formatter = new Intl.RelativeTimeFormat('en', {
        numeric: 'auto',
    });

    const intervals: [number, Intl.RelativeTimeFormatUnit][] = [
        [60, 'second'],
        [60, 'minute'],
        [24, 'hour'],
        [7, 'day'],
        [4.345, 'week'],
        [12, 'month'],
        [Number.POSITIVE_INFINITY, 'year'],
    ];

    let value = seconds;

    for (const [limit, unit] of intervals) {
        if (Math.abs(value) < limit) {
            return formatter.format(Math.round(value), unit);
        }

        value /= limit;
    }

    return formatter.format(Math.round(value), 'year');
}
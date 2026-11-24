import './TransactionSummary.css';

interface TransactionSummaryProps {
    type: string;
    wallet: string;
    amount: string;
    currency: string;
    description: string;
}

function formatAmount(amount: string, currency: string): string {
    const cleanAmount = amount.replace(/,/g, '').trim();
    const isNegative = cleanAmount.startsWith('-');
    const unsignedAmount = isNegative
        ? cleanAmount.slice(1)
        : cleanAmount;

    const [integerPart = '0', decimalPart] = unsignedAmount.split('.');

    try {
        const formatter = new Intl.NumberFormat(undefined, {
            style: 'currency',
            currency,
            minimumFractionDigits: 0,
            maximumFractionDigits: 0,
        });

        const parts = formatter.formatToParts(BigInt(integerPart || '0'));

        const formattedInteger = parts
            .map((part) => part.value)
            .join('');

        const decimalSeparator =
            new Intl.NumberFormat(undefined)
                .formatToParts(1.1)
                .find((part) => part.type === 'decimal')?.value ?? '.';

        const result = decimalPart !== undefined
            ? `${formattedInteger}${decimalSeparator}${decimalPart}`
            : formattedInteger;

        return isNegative ? `-${result}` : result;
    } catch {
        return `${currency} ${amount}`;
    }
}

export function TransactionSummary({
    type,
    wallet,
    amount,
    currency,
    description,
}: TransactionSummaryProps) {
    return (
        <dl className="transaction-summary">
            <div className="transaction-summary__item">
                <dt>Transaction Type</dt>
                <dd>{type}</dd>
            </div>

            <div className="transaction-summary__item">
                <dt>Wallet</dt>
                <dd>{wallet}</dd>
            </div>

            <div className="transaction-summary__item">
                <dt>Amount</dt>
                <dd className="transaction-summary__amount">
                    {formatAmount(amount, currency)}
                </dd>
            </div>

            <div className="transaction-summary__item">
                <dt>Currency</dt>
                <dd>{currency}</dd>
            </div>

            <div className="transaction-summary__item">
                <dt>Description</dt>
                <dd
                    className="transaction-summary__description"
                    title={description}
                >
                    {description || 'No description provided'}
                </dd>
            </div>
        </dl>
    );
}
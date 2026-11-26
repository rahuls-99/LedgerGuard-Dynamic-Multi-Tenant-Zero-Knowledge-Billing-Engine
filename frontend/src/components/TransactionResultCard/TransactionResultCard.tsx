import './TransactionResultCard.css';
import { useState } from 'react';

export type TransactionResultVariant =
  | 'success'
  | 'failed'
  | 'rolledBack';

export interface TransactionDetail {
  label: string;
  value: string;
}

export interface TransactionResultAction {
  label: string;
  onClick: () => void;
}

export interface TransactionResultCardProps {
  variant: TransactionResultVariant;
  title: string;
  message: string;
  details?: TransactionDetail[];
  traceId?: string;
  actions?: TransactionResultAction[];
  note?: string;
}

const RESULT_ICONS: Record<TransactionResultVariant, string> = {
  success: '✓',
  failed: '✕',
  rolledBack: '↶',
};

export function TransactionResultCard({
  variant,
  title,
  message,
  details = [],
  traceId,
  actions = [],
  note,
}: TransactionResultCardProps): React.ReactElement {
  const [copied, setCopied] = useState<string | null>(null);

  const copyValue = async (value: string, label: string) => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(label);

      window.setTimeout(() => {
        setCopied(null);
      }, 1500);
    } catch {
      setCopied(null);
    }
  };

  const isSuccess = variant === 'success';

  return (
    <section
      className={`transaction-result transaction-result-${variant}`}
      role={isSuccess ? 'status' : 'alert'}
      aria-live={isSuccess ? 'polite' : 'assertive'}
    >
      <header className="transaction-result-header">
        <span
          className="transaction-result-icon"
          aria-hidden="true"
        >
          {RESULT_ICONS[variant]}
        </span>

        <div>
          <h2>{title}</h2>
          <p>{message}</p>
        </div>
      </header>

      {variant === 'rolledBack' && (
        <p className="transaction-result-rollback-note">
          Nothing was charged.
        </p>
      )}

      {details.length > 0 && (
        <dl className="transaction-result-details">
          {details.map((detail) => (
            <div key={detail.label}>
              <dt>{detail.label}</dt>
              <dd>
                <span>{detail.value}</span>

                {(detail.label === 'Transaction ID' ||
                  detail.label === 'Trace ID') && (
                  <button
                    type="button"
                    onClick={() =>
                      copyValue(detail.value, detail.label)
                    }
                    aria-label={`Copy ${detail.label}`}
                  >
                    {copied === detail.label ? 'Copied' : 'Copy'}
                  </button>
                )}
              </dd>
            </div>
          ))}
        </dl>
      )}

      {traceId && (
        <div className="transaction-result-trace">
          <span>Trace ID: {traceId}</span>
          <button
            type="button"
            onClick={() => copyValue(traceId, 'Trace ID')}
          >
            {copied === 'Trace ID' ? 'Copied' : 'Copy trace ID'}
          </button>
        </div>
      )}

      {note && <p className="transaction-result-note">{note}</p>}

      {actions.length > 0 && (
        <footer className="transaction-result-actions">
          {actions.map((action) => (
            <button
              type="button"
              key={action.label}
              onClick={action.onClick}
            >
              {action.label}
            </button>
          ))}
        </footer>
      )}
    </section>
  );
}

export default TransactionResultCard;
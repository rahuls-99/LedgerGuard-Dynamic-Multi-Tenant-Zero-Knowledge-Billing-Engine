
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ErrorState } from './ErrorState';
import './FullPageErrors.css';

export const ForbiddenPage: React.FC = () => {
    const navigate = useNavigate();

    return (
        <main className="full-page-error">
            <div style={{
                width: '100%',
                maxWidth: '600px',
                textAlign: 'center'
            }}>
                <ErrorState
                    variant="forbidden"
                    title="403 - Access Forbidden"
                    message="You do not have permission to access this page."
                />

                <button
                    type="button"
                    onClick={() => navigate('/dashboard')}
                    style={{
                        minHeight: '44px',
                        marginTop: '16px',
                        padding: '10px 20px',
                        border: 'none',
                        borderRadius: '8px',
                        background: '#2563eb',
                        color: '#ffffff',
                        cursor: 'pointer'
                    }}
                >
                    Go to Dashboard
                </button>
            </div>
        </main>
    );
};

export default ForbiddenPage;

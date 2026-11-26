
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ErrorState } from './ErrorState';
import './FullPageErrors.css';

export const ServerErrorPage: React.FC = () => {
    const navigate = useNavigate();

    return (
        <main className="full-page-error">
            <div>
                <ErrorState
                    variant="server"
                    title="500 - Internal Server Error"
                    message="Something went wrong on our server. Please try again later."
                    onRetry={() => window.location.reload()}
                />

                <button
                    type="button"
                    onClick={() => navigate('/dashboard')}
                >
                    Go to Dashboard
                </button>
            </div>
        </main>
    );
};

export default ServerErrorPage;

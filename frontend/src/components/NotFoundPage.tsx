
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { EmptyState } from './EmptyState';
import './FullPageErrors.css';

export const NotFoundPage: React.FC = () => {
    const navigate = useNavigate();

    return (
        <main className="full-page-error">
            <EmptyState
                icon="🔎"
                title="404 - Page Not Found"
                description="Sorry, the page you are looking for does not exist or may have been moved."
                actionLabel="Go to Dashboard"
                onAction={() => navigate('/dashboard')}
            />
        </main>
    );
};

export default NotFoundPage;

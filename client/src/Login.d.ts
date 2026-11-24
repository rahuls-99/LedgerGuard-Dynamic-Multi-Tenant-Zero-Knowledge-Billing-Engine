import type { ComponentType } from 'react';

interface LoginProps {
    onLoginSuccess: (user: unknown) => void;
}

declare const Login: ComponentType<LoginProps>;

export default Login;
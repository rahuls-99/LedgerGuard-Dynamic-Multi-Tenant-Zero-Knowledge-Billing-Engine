
import React, { useState, useEffect } from 'react';
import Login from './Login';
import { ComponentsDemoPage } from './components/ComponentsDemoPage';
import { fetchUserProfile } from './api/authService';

export function App(): React.ReactElement {
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false);
  const [isChecking, setIsChecking] = useState<boolean>(true);

  // Check whether the user is already logged in
  useEffect(() => {
    const checkLogin = async () => {
      const token = localStorage.getItem('token');

      if (!token) {
        setIsChecking(false);
        return;
      }

      try {
        await fetchUserProfile();
        setIsLoggedIn(true);
      } catch (error) {
        console.error('Authentication failed:', error);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setIsLoggedIn(false);
      } finally {
        setIsChecking(false);
      }
    };

    checkLogin();
  }, []);

  // Handle successful login
  const handleLoginSuccess = (user: unknown): void => {
    console.log('User logged in:', user);
    setIsLoggedIn(true);
  };

  // Handle logout
  const handleLogout = (): void => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setIsLoggedIn(false);
  };

  if (isChecking) {
    return <main>Checking login...</main>;
  }

  return (
  <main>
    {isLoggedIn ? (
      <ComponentsDemoPage onLogout={handleLogout} />
    ) : (
      <Login onLoginSuccess={handleLoginSuccess} />
    )}
  </main>
);
}

export default App;

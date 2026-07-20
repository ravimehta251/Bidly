import { Link, useNavigate } from 'react-router-dom';
import { LogOut, User, Gavel, Plus } from 'lucide-react';
import { useState, useEffect } from 'react';

const Navbar = () => {
  const navigate = useNavigate();
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Simple check for token - in a real app you'd want React Context
  // We'll rely on local storage events to keep this somewhat in sync for now,
  // or just force remounts.
  useEffect(() => {
    const token = localStorage.getItem('token');
    setIsAuthenticated(!!token);

    // Listen to storage changes from other tabs, or custom events from login
    const handleStorage = () => setIsAuthenticated(!!localStorage.getItem('token'));
    window.addEventListener('auth-change', handleStorage);
    return () => window.removeEventListener('auth-change', handleStorage);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    window.dispatchEvent(new Event('auth-change'));
    navigate('/login');
  };

  return (
    <header className="header">
      <div className="header-content">
        <Link to="/" className="logo">
          <Gavel size={24} color="var(--color-accent)" />
          Bidly
        </Link>
        <nav style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          <Link to="/" style={{ color: 'var(--color-text-primary)', fontWeight: 500 }}>Auctions</Link>
          {isAuthenticated ? (
            <>
              <Link to="/auctions/create" className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', padding: '0.4rem 1rem' }}>
                <Plus size={16} /> Create Auction
              </Link>
              <button className="btn btn-outline" onClick={handleLogout} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.4rem 1rem' }}>
                <LogOut size={16} /> Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" style={{ color: 'var(--color-text-secondary)', fontWeight: 500 }}>Login</Link>
              <Link to="/register" className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <User size={16} /> Register
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
};

export default Navbar;

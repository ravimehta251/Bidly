import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';

const Register = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ displayName: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await api.post('/auth/register', formData);
      // Auto login or redirect to login
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.title || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '400px', margin: '4rem auto' }} className="card">
      <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
        <h2>Create an account</h2>
        <p style={{ color: 'var(--color-text-secondary)' }}>Join BidFlare to start bidding</p>
      </div>

      {error && (
        <div style={{ backgroundColor: 'rgba(227,0,0,0.1)', color: 'var(--color-status-live)', padding: '0.8rem', borderRadius: 'var(--radius-sm)', marginBottom: '1rem', fontSize: '0.9rem' }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div>
          <label style={{ display: 'block', marginBottom: '0.4rem', fontSize: '0.9rem', fontWeight: 500 }}>Display Name</label>
          <input 
            type="text" 
            className="input-field" 
            placeholder="John Doe"
            value={formData.displayName}
            onChange={(e) => setFormData({...formData, displayName: e.target.value})}
            required 
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '0.4rem', fontSize: '0.9rem', fontWeight: 500 }}>Email</label>
          <input 
            type="email" 
            className="input-field" 
            placeholder="name@example.com"
            value={formData.email}
            onChange={(e) => setFormData({...formData, email: e.target.value})}
            required 
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '0.4rem', fontSize: '0.9rem', fontWeight: 500 }}>Password</label>
          <input 
            type="password" 
            className="input-field" 
            placeholder="••••••••"
            value={formData.password}
            onChange={(e) => setFormData({...formData, password: e.target.value})}
            required 
            minLength={6}
          />
        </div>
        
        <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '1rem' }} disabled={loading}>
          {loading ? 'Creating account...' : 'Create account'}
        </button>
      </form>
      
      <p style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>
        Already have an account? <Link to="/login" style={{ fontWeight: 600 }}>Sign in</Link>
      </p>
    </div>
  );
};

export default Register;

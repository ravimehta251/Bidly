import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';

const CreateAuction = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  // Set default start time to 5 minutes from now, end time to 1 hour from now
  const now = new Date();
  const defaultStart = new Date(now.getTime() + 5 * 60000);
  const defaultEnd = new Date(now.getTime() + 65 * 60000);

  // Helper to format for datetime-local input
  const formatForInput = (d) => {
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  };

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    startingPrice: 10.00,
    minIncrement: 1.00,
    startTime: formatForInput(defaultStart),
    endTime: formatForInput(defaultEnd)
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // Backend expects standard ISO-8601 Strings for OffsetDateTime
      const payload = {
        ...formData,
        startTime: new Date(formData.startTime).toISOString(),
        endTime: new Date(formData.endTime).toISOString(),
        startingPrice: parseFloat(formData.startingPrice),
        minIncrement: parseFloat(formData.minIncrement)
      };

      const res = await api.post('/auctions', payload);
      navigate(`/auctions/${res.data.id}`);
    } catch (err) {
      setError(err.response?.data?.title || err.response?.data?.detail || 'Failed to create auction.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: '600px', margin: '2rem auto' }}>
      <h2 style={{ marginBottom: '1.5rem' }}>Create New Auction</h2>

      {error && (
        <div style={{ backgroundColor: 'rgba(227,0,0,0.1)', color: 'var(--color-status-live)', padding: '0.8rem', borderRadius: 'var(--radius-sm)', marginBottom: '1rem', fontSize: '0.9rem' }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.2rem' }}>
        <div>
          <label style={{ display: 'block', marginBottom: '0.4rem', fontSize: '0.9rem', fontWeight: 500 }}>Title</label>
          <input 
            type="text" 
            className="input-field" 
            placeholder="Vintage Watch..."
            value={formData.title}
            onChange={(e) => setFormData({...formData, title: e.target.value})}
            required 
            maxLength={255}
          />
        </div>

        <div>
          <label style={{ display: 'block', marginBottom: '0.4rem', fontSize: '0.9rem', fontWeight: 500 }}>Description</label>
          <textarea 
            className="input-field" 
            rows="4"
            placeholder="Detailed description..."
            value={formData.description}
            onChange={(e) => setFormData({...formData, description: e.target.value})}
          />
        </div>

        <div style={{ display: 'flex', gap: '1rem' }}>
          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', marginBottom: '0.4rem', fontSize: '0.9rem', fontWeight: 500 }}>Starting Price ($)</label>
            <input 
              type="number" 
              step="0.01"
              min="0.01"
              className="input-field" 
              value={formData.startingPrice}
              onChange={(e) => setFormData({...formData, startingPrice: e.target.value})}
              required 
            />
          </div>
          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', marginBottom: '0.4rem', fontSize: '0.9rem', fontWeight: 500 }}>Min Increment ($)</label>
            <input 
              type="number" 
              step="0.01"
              min="0.01"
              className="input-field" 
              value={formData.minIncrement}
              onChange={(e) => setFormData({...formData, minIncrement: e.target.value})}
            />
          </div>
        </div>

        <div style={{ display: 'flex', gap: '1rem' }}>
          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', marginBottom: '0.4rem', fontSize: '0.9rem', fontWeight: 500 }}>Start Time</label>
            <input 
              type="datetime-local" 
              className="input-field" 
              value={formData.startTime}
              onChange={(e) => setFormData({...formData, startTime: e.target.value})}
              required 
            />
          </div>
          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', marginBottom: '0.4rem', fontSize: '0.9rem', fontWeight: 500 }}>End Time</label>
            <input 
              type="datetime-local" 
              className="input-field" 
              value={formData.endTime}
              onChange={(e) => setFormData({...formData, endTime: e.target.value})}
              required 
            />
          </div>
        </div>

        <button type="submit" className="btn btn-primary" style={{ marginTop: '1rem' }} disabled={loading}>
          {loading ? 'Creating...' : 'Create Auction'}
        </button>
      </form>
    </div>
  );
};

export default CreateAuction;

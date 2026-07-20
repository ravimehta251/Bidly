import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import { Clock } from 'lucide-react';

const AuctionList = () => {
  const [auctions, setAuctions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState(''); // '' means all

  useEffect(() => {
    fetchAuctions();
  }, [statusFilter]);

  const fetchAuctions = async () => {
    setLoading(true);
    try {
      const endpoint = statusFilter ? `/auctions?status=${statusFilter}` : '/auctions';
      const res = await api.get(endpoint);
      // Depending on pagination structure from backend, adjust this:
      setAuctions(res.data.content || res.data || []);
    } catch (err) {
      console.error('Failed to load auctions', err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'LIVE': return <span className="badge badge-live">Live</span>;
      case 'SCHEDULED': return <span className="badge badge-scheduled">Scheduled</span>;
      case 'ENDED': return <span className="badge badge-ended">Ended</span>;
      default: return <span className="badge">{status}</span>;
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1>Discover Auctions</h1>
        
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {['', 'LIVE', 'SCHEDULED', 'ENDED'].map(status => (
            <button 
              key={status}
              className={`btn ${statusFilter === status ? 'btn-primary' : 'btn-outline'}`}
              onClick={() => setStatusFilter(status)}
              style={{ padding: '0.4rem 1rem', fontSize: '0.85rem' }}
            >
              {status === '' ? 'All' : status}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '4rem', color: 'var(--color-text-secondary)' }}>Loading...</div>
      ) : auctions.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '4rem', backgroundColor: 'var(--color-bg-secondary)', borderRadius: 'var(--radius-lg)' }}>
          <p style={{ color: 'var(--color-text-secondary)' }}>No auctions found matching your criteria.</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1.5rem' }}>
          {auctions.map(auction => (
            <Link to={`/auctions/${auction.id}`} key={auction.id} style={{ color: 'inherit' }}>
              <div className="card" style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                  <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', lineHeight: 1.3 }}>{auction.title}</h3>
                  {getStatusBadge(auction.status)}
                </div>
                
                <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem', flexGrow: 1 }}>
                  {auction.description?.length > 100 ? auction.description.substring(0, 100) + '...' : auction.description}
                </p>
                
                <div style={{ borderTop: '1px solid var(--color-border-light)', paddingTop: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.2rem' }}>Current Price</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 600 }}>${auction.currentPrice.toLocaleString()}</div>
                  </div>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--color-text-secondary)', fontSize: '0.85rem' }}>
                    <Clock size={16} />
                    <span>
                      {auction.status === 'SCHEDULED' ? 'Starts soon' : 
                       auction.status === 'LIVE' ? 'Ending soon' : 'Finished'}
                    </span>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
};

export default AuctionList;

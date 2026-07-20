import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { stompService } from '../api/stompClient';
import AuctionClosedOverlay from '../components/AuctionClosedOverlay';
import { ArrowUp, Clock, History, Trash2 } from 'lucide-react';

const AuctionDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [auction, setAuction] = useState(null);
  const [bids, setBids] = useState([]);
  const [bidAmount, setBidAmount] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [closedData, setClosedData] = useState(null);
  const [userId, setUserId] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setUserId(payload.userId);
      } catch (e) {
        console.error("Invalid token format");
      }
    }
    fetchAuctionData();
    
    // Connect and subscribe to real-time updates
    stompService.connect(() => {
      stompService.subscribe(`/topic/auctions/${id}`, (message) => {
        setAuction(prev => ({
          ...prev,
          currentPrice: message.amount,
          currentWinnerDisplayName: message.leaderDisplayName,
          endTime: message.endTime
        }));

        setBids(prev => [{
          id: message.bidId || message.ts,
          amount: message.amount,
          bidderDisplayName: message.leaderDisplayName,
          createdAt: message.ts
        }, ...prev]);
      });

      // Auction-close events use a separate Redis-backed topic.
      stompService.subscribe(`/topic/auctions/${id}/closed`, (message) => {
        setAuction(prev => ({ ...prev, status: 'ENDED' }));
        setClosedData(message);
      });
    });

    return () => stompService.disconnect();
  }, [id]);

  const fetchAuctionData = async () => {
    try {
      const [auctionRes, bidsRes] = await Promise.all([
        api.get(`/auctions/${id}`),
        api.get(`/auctions/${id}/bids`)
      ]);
      setAuction(auctionRes.data);
      setBids(bidsRes.data.content || bidsRes.data || []);
      setBidAmount(auctionRes.data.currentPrice + auctionRes.data.minIncrement);
    } catch (err) {
      setError('Failed to load auction details.');
    } finally {
      setLoading(false);
    }
  };

  const handleBid = async (e) => {
    e.preventDefault();
    setError('');
    const token = localStorage.getItem('token');
    if (!token) {
      setError('You must be logged in to bid.');
      return;
    }
    
    try {
      await api.post(`/auctions/${id}/bids`, { amount: Number(bidAmount) });
      setBidAmount('');
    } catch (err) {
      setError(err.response?.data?.title || 'Bid failed.');
    }
  };

  const handleDelete = async () => {
    if (!window.confirm("Are you sure you want to delete this auction?")) return;
    try {
      await api.delete(`/auctions/${id}`);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.title || 'Failed to delete auction.');
    }
  };

  // Simple countdown hook logic
  const [timeLeft, setTimeLeft] = useState('');
  useEffect(() => {
    if (!auction?.endTime || auction.status === 'ENDED') return;
    
    const interval = setInterval(() => {
      const now = new Date();
      const end = new Date(auction.endTime);
      const diff = end - now;
      
      if (diff <= 0) {
        setTimeLeft('Ended');
        clearInterval(interval);
      } else {
        const h = Math.floor(diff / (1000 * 60 * 60));
        const m = Math.floor((diff / 1000 / 60) % 60);
        const s = Math.floor((diff / 1000) % 60);
        setTimeLeft(`${h}h ${m}m ${s}s`);
      }
    }, 1000);
    
    return () => clearInterval(interval);
  }, [auction?.endTime, auction?.status]);

  if (loading) return <div style={{ padding: '4rem', textAlign: 'center' }}>Loading...</div>;
  if (!auction) return <div style={{ padding: '4rem', textAlign: 'center' }}>Auction not found</div>;

  const isLive = auction.status === 'LIVE';

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '2rem', alignItems: 'start' }}>
      
      {/* Main Content */}
      <div>
        <div style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1rem' }}>
            <h1 style={{ fontSize: '2.5rem' }}>{auction.title}</h1>
            {auction.status === 'LIVE' && <span className="badge badge-live">Live</span>}
            {auction.status === 'SCHEDULED' && <span className="badge badge-scheduled">Scheduled</span>}
            {auction.status === 'ENDED' && <span className="badge badge-ended">Ended</span>}
            
            {userId === auction.sellerId && (
              <button 
                onClick={handleDelete}
                className="btn btn-outline" 
                style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '0.4rem', borderColor: 'var(--color-status-ended)', color: 'var(--color-status-ended)' }}
              >
                <Trash2 size={16} /> Delete
              </button>
            )}
          </div>
          <p style={{ fontSize: '1.1rem', color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
            {auction.description}
          </p>
        </div>

        <div className="card" style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <History size={20} /> Bid History
          </h2>
          {bids.length === 0 ? (
            <p style={{ color: 'var(--color-text-secondary)' }}>No bids yet. Be the first!</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {bids.map((bid, index) => (
                <div key={bid.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', backgroundColor: index === 0 ? 'rgba(0, 102, 204, 0.05)' : 'var(--color-bg-tertiary)', borderRadius: 'var(--radius-sm)', borderLeft: index === 0 ? '3px solid var(--color-accent)' : '3px solid transparent' }}>
                  <div>
                    <div style={{ fontWeight: 600 }}>{bid.bidderDisplayName || 'Anonymous'}</div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                      {new Date(bid.createdAt).toLocaleTimeString()}
                    </div>
                  </div>
                  <div style={{ fontSize: '1.1rem', fontWeight: 600 }}>
                    ${bid.amount.toLocaleString()}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Sidebar / Bidding Panel */}
      <div style={{ position: 'sticky', top: '100px', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        
        <div className="card" style={{ backgroundColor: 'var(--color-bg-primary)', border: '2px solid var(--color-border-light)' }}>
          <div style={{ textAlign: 'center', paddingBottom: '1.5rem', borderBottom: '1px solid var(--color-border-light)', marginBottom: '1.5rem' }}>
            <div style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>Current Price</div>
            <div style={{ fontSize: '3rem', fontWeight: 700, letterSpacing: '-0.03em', color: 'var(--color-text-primary)' }}>
              ${auction.currentPrice.toLocaleString()}
            </div>
            {auction.currentWinnerDisplayName && (
              <div style={{ marginTop: '0.5rem', fontSize: '0.9rem' }}>
                Leading: <span style={{ fontWeight: 600 }}>{auction.currentWinnerDisplayName}</span>
              </div>
            )}
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', color: isLive ? 'inherit' : 'var(--color-text-secondary)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 500 }}>
              <Clock size={18} color={isLive ? 'var(--color-status-live)' : 'currentColor'} /> 
              {isLive ? 'Ends in' : (auction.status === 'SCHEDULED' ? 'Starts in' : 'Status')}
            </div>
            <div style={{ fontSize: '1.2rem', fontWeight: 600, color: isLive ? 'var(--color-status-live)' : 'inherit' }}>
              {timeLeft || (auction.status === 'ENDED' ? 'Ended' : 'TBD')}
            </div>
          </div>

          {error && (
            <div style={{ backgroundColor: 'rgba(227,0,0,0.1)', color: 'var(--color-status-live)', padding: '0.8rem', borderRadius: 'var(--radius-sm)', marginBottom: '1rem', fontSize: '0.9rem' }}>
              {error}
            </div>
          )}

          <form onSubmit={handleBid}>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <div style={{ position: 'relative', flexGrow: 1 }}>
                <span style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-secondary)', fontWeight: 600 }}>$</span>
                <input 
                  type="number"
                  className="input-field"
                  style={{ paddingLeft: '2rem', fontSize: '1.1rem', fontWeight: 600 }}
                  value={bidAmount}
                  onChange={(e) => setBidAmount(e.target.value)}
                  min={auction.currentPrice + auction.minIncrement}
                  step="1"
                  disabled={!isLive}
                  placeholder={`Min ${auction.currentPrice + auction.minIncrement}`}
                />
              </div>
              <button 
                type="submit" 
                className="btn btn-primary" 
                disabled={!isLive || !bidAmount || bidAmount < (auction.currentPrice + auction.minIncrement)}
                style={{ padding: '0 1.5rem' }}
              >
                <ArrowUp size={20} />
              </button>
            </div>
            <p style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', textAlign: 'center', marginTop: '0.8rem' }}>
              Minimum increment: ${auction.minIncrement}
            </p>
          </form>
        </div>

      </div>

      {closedData && (
        <AuctionClosedOverlay 
          data={closedData} 
          onClose={() => setClosedData(null)} 
        />
      )}

    </div>
  );
};

export default AuctionDetail;

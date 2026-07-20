const AuctionClosedOverlay = ({ data, onClose }) => {
  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      width: '100vw',
      height: '100vh',
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      backdropFilter: 'blur(4px)',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      zIndex: 1000
    }}>
      <div className="card" style={{ maxWidth: '400px', width: '90%', textAlign: 'center', padding: '3rem 2rem' }}>
        <h2 style={{ fontSize: '2rem', marginBottom: '1rem' }}>Auction Ended!</h2>
        
        {data.winnerDisplayName ? (
          <div>
            <p style={{ fontSize: '1.1rem', color: 'var(--color-text-secondary)', marginBottom: '1.5rem' }}>
              Winning bid by <strong style={{ color: 'var(--color-text-primary)' }}>{data.winnerDisplayName}</strong>
            </p>
            <div style={{ fontSize: '3.5rem', fontWeight: 700, color: 'var(--color-accent)', marginBottom: '2rem' }}>
              ${data.finalPrice?.toLocaleString()}
            </div>
          </div>
        ) : (
          <p style={{ fontSize: '1.1rem', color: 'var(--color-text-secondary)', marginBottom: '2rem' }}>
            This auction ended without any bids.
          </p>
        )}
        
        <button className="btn btn-primary" onClick={onClose} style={{ width: '100%' }}>
          Close
        </button>
      </div>
    </div>
  );
};

export default AuctionClosedOverlay;

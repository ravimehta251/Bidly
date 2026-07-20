import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Login from './pages/Login';
import Register from './pages/Register';
import AuctionList from './pages/AuctionList';
import AuctionDetail from './pages/AuctionDetail';
import CreateAuction from './pages/CreateAuction';

function App() {
  return (
    <div className="layout-container">
      <Navbar />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<AuctionList />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/auctions/create" element={<CreateAuction />} />
          <Route path="/auctions/:id" element={<AuctionDetail />} />
        </Routes>
      </main>
    </div>
  );
}

export default App;

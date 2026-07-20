import ws from 'k6/ws';
import { check } from 'k6';

// Run with: k6 run -e BASE_URL=http://localhost -e AUCTION_ID=1 load-tests/websocket.js
export const options = {
  scenarios: {
    websocket_clients: {
      executor: 'constant-vus',
      vus: 500,
      duration: '30s',
    },
  },
};

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://localhost';
  const auctionId = __ENV.AUCTION_ID || '1';
  const wsUrl = `${baseUrl.replace(/^http/, 'ws')}/ws/websocket`;
  const response = ws.connect(wsUrl, {}, (socket) => {
    socket.on('open', () => {
      socket.send('CONNECT\naccept-version:1.2\nheart-beat:4000,4000\n\n\u0000');
      socket.send(`SUBSCRIBE\nid:auction-${auctionId}\ndestination:/topic/auctions/${auctionId}\n\n\u0000`);
      socket.send(`SUBSCRIBE\nid:auction-${auctionId}-closed\ndestination:/topic/auctions/${auctionId}/closed\n\n\u0000`);
    });
    socket.setTimeout(() => socket.close(), 25000);
  });

  check(response, { 'websocket handshake succeeded': (r) => r && r.status === 101 });
}

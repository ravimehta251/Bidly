import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class StompService {
  constructor() {
    this.client = null;
  }

  connect(onConnect) {
    if (this.client && this.client.active) {
      if (onConnect) onConnect();
      return;
    }

    // SockJS endpoint is configured in backend WebSocketConfig
    // Nginx proxies /ws to the backend apps. Vite dev server proxies it to nginx.
    const socket = new SockJS('/ws');
    
    this.client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = () => {
      console.log('STOMP connected');
      if (onConnect) onConnect();
    };

    this.client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    this.client.activate();
  }

  subscribe(topic, callback) {
    if (!this.client || !this.client.active) {
      console.warn('Cannot subscribe, STOMP client not connected');
      return null;
    }
    return this.client.subscribe(topic, (message) => {
      callback(JSON.parse(message.body));
    });
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }
}

export const stompService = new StompService();

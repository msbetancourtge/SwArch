import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import type { KitchenOrder } from '@/lib/types';
import { getSession } from '@/lib/auth';

/**
 * Realtime kitchen events from OrderService over STOMP/WebSocket.
 *
 * Architectural note: this connects DIRECTLY to the OrderService WebSocket
 * endpoint (ws://localhost:8085/ws/kitchen), not through the API Gateway.
 * The MVC flavor of Spring Cloud Gateway does not transparently proxy the
 * HTTP Upgrade handshake; REST calls still go through the gateway at 8080.
 * See RouteConfig.java in the APIGateway for the full rationale.
 */

const DEFAULT_WS_URL = 'ws://localhost:8085/ws/kitchen';
const WS_URL = (import.meta.env.VITE_ORDER_WS_URL as string | undefined) ?? DEFAULT_WS_URL;

export type KitchenEventType = 'ORDER_CREATED' | 'ORDER_STATUS_CHANGED';

export interface KitchenEvent {
  type: KitchenEventType;
  order: KitchenOrder;
  timestamp: string;
}

export interface KitchenRealtimeHandle {
  disconnect: () => void;
}

export function subscribeKitchen(
  restaurantId: number,
  onEvent: (event: KitchenEvent) => void,
  onStatusChange?: (connected: boolean) => void,
): KitchenRealtimeHandle {
  const session = getSession();
  let subscription: StompSubscription | null = null;

  const client = new Client({
    brokerURL: WS_URL,
    // Carry JWT in STOMP CONNECT headers. The server does not yet validate it
    // (MVP) but keeps the contract stable for when role-based auth is added.
    connectHeaders: session?.token ? { Authorization: `Bearer ${session.token}` } : {},
    reconnectDelay: 3_000,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000,
  });

  client.onConnect = () => {
    onStatusChange?.(true);
    subscription = client.subscribe(
      `/topic/kitchen/${restaurantId}`,
      (message: IMessage) => {
        try {
          const event = JSON.parse(message.body) as KitchenEvent;
          onEvent(event);
        } catch (err) {
          console.error('Failed to parse kitchen event:', err, message.body);
        }
      },
    );
  };

  client.onWebSocketClose = () => {
    onStatusChange?.(false);
  };

  client.onStompError = (frame) => {
    console.error('STOMP error:', frame.headers['message'], frame.body);
    onStatusChange?.(false);
  };

  client.activate();

  return {
    disconnect: () => {
      try {
        subscription?.unsubscribe();
      } catch {
        /* ignore */
      }
      void client.deactivate();
    },
  };
}

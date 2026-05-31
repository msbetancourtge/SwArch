import React from 'react';
import {
  View,
  ScrollView,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { useLocalSearchParams, Stack } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { ThemedText } from '@/presentation/theme/components/themed-text';
import { ThemedView } from '@/presentation/theme/components/themed-view';
import { useOrder } from '@/presentation/orders/hooks/useOrder';
import type { OrderStatus } from '@/core/orders/interface/order';

const statusColors: Record<OrderStatus, string> = {
  PENDING: '#f39c12',
  IN_PREPARATION: '#f39c12',
  READY: '#3498db',
  DELIVERED: '#27ae60',
  CANCELLED: '#e74c3c',
};

const statusLabels: Record<OrderStatus, string> = {
  PENDING: 'Pendiente',
  IN_PREPARATION: 'Preparando',
  READY: 'Listo',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado',
};

export default function OrderDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data: order, isLoading } = useOrder(Number(id));

  if (isLoading) {
    return (
      <ThemedView style={styles.center}>
        <Stack.Screen options={{ title: 'Pedido' }} />
        <ActivityIndicator size="large" />
      </ThemedView>
    );
  }

  if (!order) {
    return (
      <ThemedView style={styles.center}>
        <Stack.Screen options={{ title: 'Pedido' }} />
        <Ionicons name="alert-circle-outline" size={48} color="#ccc" />
        <ThemedText style={{ marginTop: 12, color: '#999' }}>
          Pedido no encontrado
        </ThemedText>
      </ThemedView>
    );
  }

  const statusColor = statusColors[order.status] || '#999';

  return (
    <ThemedView style={{ flex: 1 }}>
      <Stack.Screen options={{ title: `Pedido #${order.id}` }} />
      <ScrollView contentContainerStyle={{ padding: 16 }}>
        {/* Status */}
        <View style={styles.statusContainer}>
          <View
            style={[styles.statusBadgeLarge, { backgroundColor: statusColor + '20' }]}
          >
            <Ionicons
              name={
                order.status === 'CANCELLED'
                  ? 'close-circle'
                  : order.status === 'DELIVERED'
                  ? 'checkmark-circle'
                  : 'time'
              }
              size={32}
              color={statusColor}
            />
            <ThemedText
              style={{ color: statusColor, fontSize: 20, fontWeight: '700', marginLeft: 8 }}
            >
              {statusLabels[order.status] || order.status}
            </ThemedText>
          </View>
        </View>

        {/* Info Section */}
        <View style={styles.section}>
          <ThemedText type="subtitle" style={{ marginBottom: 12 }}>
            Detalles
          </ThemedText>
          <InfoRow icon="calendar-outline" label="Fecha" value={
            order.createdAt
              ? new Date(order.createdAt).toLocaleDateString('es', {
                  day: 'numeric', month: 'long', year: 'numeric',
                  hour: '2-digit', minute: '2-digit',
                })
              : 'N/A'
          } />
          <InfoRow icon="restaurant-outline" label="Mesa" value={String(order.tableNumber)} />
          {order.notes && (
            <InfoRow icon="document-text-outline" label="Notas" value={order.notes} />
          )}
        </View>

        {/* Items */}
        <View style={styles.section}>
          <ThemedText type="subtitle" style={{ marginBottom: 12 }}>
            Artículos
          </ThemedText>
          {order.items?.map((item, index) => (
            <View key={item.id ?? index} style={styles.itemRow}>
              <View style={{ flex: 1 }}>
                <ThemedText type="defaultSemiBold">{item.itemName}</ThemedText>
                {item.notes && (
                  <ThemedText style={{ color: '#999', fontSize: 13 }}>
                    {item.notes}
                  </ThemedText>
                )}
              </View>
            </View>
          ))}
        </View>
      </ScrollView>
    </ThemedView>
  );
}

function InfoRow({ icon, label, value }: { icon: any; label: string; value: string }) {
  return (
    <View style={styles.infoRow}>
      <Ionicons name={icon} size={18} color="#999" />
      <ThemedText style={{ marginLeft: 8, color: '#999', width: 60 }}>{label}</ThemedText>
      <ThemedText style={{ flex: 1 }}>{value}</ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  statusContainer: { alignItems: 'center', marginBottom: 20 },
  statusBadgeLarge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 16,
  },
  section: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  itemRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#f5f5f5',
  },
  totalContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
});

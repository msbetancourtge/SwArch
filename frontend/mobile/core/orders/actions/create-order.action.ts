import { productsApi } from '@/core/api/productsApi';
import type { CreateOrderRequest, Order } from '../interface/order';

export const createOrder = async (order: CreateOrderRequest): Promise<Order | null> => {
  try {
    const { data } = await productsApi.post<Order>('/order', order);
    return data;
  } catch (error) {
    console.log('Error creating order:', error);
    return null;
  }
};

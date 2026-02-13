export interface Payment {
  id: string;
  orderId: string;
  amount: number;
  status: 'SUCCESS' | 'FAILED';
  gatewayReference: string | null;
  createdAt: string;
}

export interface PaymentRequest {
  amount: number;
}

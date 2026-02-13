import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { PaymentService } from '../../../core/services/payment.service';
import { Order } from '../../../core/models/order.model';
import { Payment } from '../../../core/models/payment.model';

/**
 * Order detail component for viewing and managing individual orders
 */
@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss'],
})
export class OrderDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly paymentService = inject(PaymentService);

  order = signal<Order | null>(null);
  payments = signal<Payment[]>([]);
  loading = signal(true);
  paymentLoading = signal(false);
  cancelLoading = signal(false);
  showCancelDialog = signal(false);
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (orderId) {
      this.loadOrderDetails(orderId);
    } else {
      this.router.navigate(['/orders']);
    }
  }

  /**
   * Loads order and payment details
   */
  private loadOrderDetails(orderId: string): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    // Load order and payments in parallel
    this.orderService.getOrderById(orderId).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
        this.loadPayments(orderId);
      },
      error: (err) => {
        console.error('Failed to load order', err);
        this.errorMessage.set('Failed to load order details');
        this.loading.set(false);
      },
    });
  }

  /**
   * Loads payment history for the order
   */
  private loadPayments(orderId: string): void {
    this.paymentService.getPayments(orderId).subscribe({
      next: (payments) => {
        this.payments.set(payments);
      },
      error: (err) => {
        console.error('Failed to load payments', err);
      },
    });
  }

  /**
   * Processes payment for the order
   */
  payOrder(): void {
    const order = this.order();
    if (!order || this.paymentLoading()) {
      return;
    }

    this.paymentLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.paymentService.processPayment(order.id, { amount: order.totalAmount }).subscribe({
      next: (payment) => {
        this.paymentLoading.set(false);
        
        if (payment.status === 'SUCCESS') {
          this.successMessage.set('Payment successful!');
          // Reload order to get updated status
          this.loadOrderDetails(order.id);
        } else {
          this.errorMessage.set('Payment failed. Please try again.');
          // Reload payments to show the failed attempt
          this.loadPayments(order.id);
        }
      },
      error: (err) => {
        this.paymentLoading.set(false);
        const errorDetail = err.error?.detail || err.error?.message || 'Payment processing failed';
        this.errorMessage.set(errorDetail);
      },
    });
  }

  /**
   * Shows cancel confirmation dialog
   */
  showCancelConfirmation(): void {
    this.showCancelDialog.set(true);
  }

  /**
   * Hides cancel confirmation dialog
   */
  hideCancelConfirmation(): void {
    this.showCancelDialog.set(false);
  }

  /**
   * Cancels the order
   */
  confirmCancel(): void {
    const order = this.order();
    if (!order || this.cancelLoading()) {
      return;
    }

    this.cancelLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.showCancelDialog.set(false);

    this.orderService.cancelOrder(order.id).subscribe({
      next: (cancelledOrder) => {
        this.cancelLoading.set(false);
        this.successMessage.set('Order cancelled successfully');
        this.order.set(cancelledOrder);
      },
      error: (err) => {
        this.cancelLoading.set(false);
        const errorDetail = err.error?.detail || err.error?.message || 'Failed to cancel order';
        this.errorMessage.set(errorDetail);
      },
    });
  }

  /**
   * Gets CSS class for order status badge
   */
  getStatusClass(status: string): string {
    switch (status) {
      case 'CONFIRMED':
        return 'badge-blue';
      case 'PAID':
        return 'badge-green';
      case 'CANCELLED':
        return 'badge-red';
      default:
        return 'badge-gray';
    }
  }

  /**
   * Gets CSS class for payment status badge
   */
  getPaymentStatusClass(status: string): string {
    return status === 'SUCCESS' ? 'badge-green' : 'badge-red';
  }

  /**
   * Truncates payment ID for display
   */
  truncateId(id: string): string {
    return id.substring(0, 8);
  }
}

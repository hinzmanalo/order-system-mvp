import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.model';

/**
 * Order list component for viewing user's order history
 */
@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.scss'],
})
export class OrderListComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  orders = signal<Order[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  currentPage = signal(0);
  totalPages = signal(0);
  statusFilter = signal<string>('');

  ngOnInit(): void {
    this.loadOrders();
  }

  /**
   * Loads orders with current filters and pagination
   */
  loadOrders(): void {
    this.loading.set(true);
    this.error.set(null);

    const params: any = {
      page: this.currentPage(),
      size: 10,
    };

    if (this.statusFilter()) {
      params.status = this.statusFilter();
    }

    this.orderService.getMyOrders(params).subscribe({
      next: (page) => {
        console.log('Orders loaded successfully:', page);
        this.orders.set(page.content);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load orders - Full error:', err);
        console.error('Error status:', err.status);
        console.error('Error message:', err.message);
        console.error('Error details:', err.error);
        const errorMsg = err.error?.detail || err.error?.message || 'Failed to load orders. Please try refreshing the page.';
        this.error.set(errorMsg);
        this.loading.set(false);
      },
    });
  }

  /**
   * Applies status filter and reloads orders
   */
  onStatusFilterChange(status: string): void {
    this.statusFilter.set(status);
    this.currentPage.set(0);
    this.loadOrders();
  }

  /**
   * Navigates to a specific page
   */
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadOrders();
    }
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
   * Truncates order ID for display
   */
  truncateOrderId(id: string): string {
    return id.substring(0, 8);
  }
}

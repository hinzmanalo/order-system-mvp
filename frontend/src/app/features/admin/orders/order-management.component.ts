import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { Order } from '../../../core/models/order.model';

/**
 * Order management component for admin users
 * Allows viewing all orders and cancelling them
 */
@Component({
  selector: 'app-order-management',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, PaginationComponent],
  templateUrl: './order-management.component.html',
  styleUrls: ['./order-management.component.scss'],
})
export class OrderManagementComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);

  // State signals
  orders = signal<Order[]>([]);
  loading = signal(true);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  expandedOrderId = signal<string | null>(null);

  // Filter controls
  statusFilter = new FormControl('');
  createdAfterControl = new FormControl('');
  createdBeforeControl = new FormControl('');

  ngOnInit(): void {
    this.setupFilterListeners();
    this.loadOrders();
  }

  /**
   * Sets up filter change listeners
   */
  private setupFilterListeners(): void {
    this.statusFilter.valueChanges.subscribe(() => {
      this.currentPage.set(0);
      this.loadOrders();
    });

    this.createdAfterControl.valueChanges.subscribe(() => {
      this.currentPage.set(0);
      this.loadOrders();
    });

    this.createdBeforeControl.valueChanges.subscribe(() => {
      this.currentPage.set(0);
      this.loadOrders();
    });
  }

  /**
   * Loads all orders with filters
   */
  loadOrders(): void {
    this.loading.set(true);

    const params: any = {
      page: this.currentPage(),
      size: 20,
    };

    if (this.statusFilter.value) {
      params.status = this.statusFilter.value;
    }
    if (this.createdAfterControl.value) {
      params.createdAfter = this.createdAfterControl.value;
    }
    if (this.createdBeforeControl.value) {
      params.createdBefore = this.createdBeforeControl.value;
    }

    this.adminService.getAllOrders(params).subscribe({
      next: (response) => {
        this.orders.set(response.content);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.currentPage.set(response.number);
        this.loading.set(false);
      },
      error: (error) => {
        this.toastService.error('Failed to load orders');
        this.loading.set(false);
      },
    });
  }

  /**
   * Cancels an order
   */
  cancelOrder(order: Order): void {
    if (!confirm(`Are you sure you want to cancel order ${this.getShortId(order.id)}?`)) {
      return;
    }

    this.adminService.cancelOrder(order.id).subscribe({
      next: () => {
        this.toastService.success('Order cancelled successfully');
        this.loadOrders();
      },
      error: (error) => {
        const message = error.error?.message || 'Failed to cancel order';
        this.toastService.error(message);
      },
    });
  }

  /**
   * Toggles order detail expansion
   */
  toggleOrderDetail(orderId: string): void {
    if (this.expandedOrderId() === orderId) {
      this.expandedOrderId.set(null);
    } else {
      this.expandedOrderId.set(orderId);
    }
  }

  /**
   * Checks if order is expanded
   */
  isExpanded(orderId: string): boolean {
    return this.expandedOrderId() === orderId;
  }

  /**
   * Handles page change
   */
  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadOrders();
  }

  /**
   * Gets short version of order ID
   */
  getShortId(id: string): string {
    return id.substring(0, 8);
  }

  /**
   * Formats the date for display
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  /**
   * Clears all filters
   */
  clearFilters(): void {
    this.statusFilter.setValue('');
    this.createdAfterControl.setValue('');
    this.createdBeforeControl.setValue('');
  }
}


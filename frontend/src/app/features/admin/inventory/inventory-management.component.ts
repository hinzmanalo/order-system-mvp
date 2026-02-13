import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { InventoryResponse } from '../../../core/models/inventory.model';

/**
 * Inventory management component for admin users
 * Allows setting and adjusting stock levels
 */
@Component({
  selector: 'app-inventory-management',
  standalone: true,
  imports: [CommonModule, RouterLink, PaginationComponent],
  templateUrl: './inventory-management.component.html',
  styleUrls: ['./inventory-management.component.scss'],
})
export class InventoryManagementComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);

  // State signals
  inventory = signal<InventoryResponse[]>([]);
  loading = signal(true);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  editingStock: { [productId: string]: boolean } = {};
  editingAdjust: { [productId: string]: boolean } = {};

  ngOnInit(): void {
    this.loadInventory();
  }

  /**
   * Loads inventory data
   */
  loadInventory(): void {
    this.loading.set(true);

    this.adminService.getInventory({ 
      page: this.currentPage(), 
      size: 20 
    }).subscribe({
      next: (response) => {
        this.inventory.set(response.content);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.currentPage.set(response.number);
        this.loading.set(false);
      },
      error: (error) => {
        this.toastService.error('Failed to load inventory');
        this.loading.set(false);
      },
    });
  }

  /**
   * Opens set stock input
   */
  openSetStock(productId: string): void {
    this.editingStock[productId] = true;
  }

  /**
   * Cancels set stock
   */
  cancelSetStock(productId: string): void {
    this.editingStock[productId] = false;
  }

  /**
   * Sets absolute stock quantity
   */
  setStock(productId: string, inputElement: HTMLInputElement): void {
    const quantity = parseInt(inputElement.value, 10);
    
    if (isNaN(quantity) || quantity < 0) {
      this.toastService.error('Please enter a valid quantity');
      return;
    }

    this.adminService.setStock(productId, quantity).subscribe({
      next: () => {
        this.toastService.success('Stock updated successfully');
        this.editingStock[productId] = false;
        this.loadInventory();
      },
      error: (error) => {
        const message = error.error?.message || 'Failed to update stock';
        this.toastService.error(message);
      },
    });
  }

  /**
   * Opens adjust stock input
   */
  openAdjustStock(productId: string): void {
    this.editingAdjust[productId] = true;
  }

  /**
   * Cancels adjust stock
   */
  cancelAdjustStock(productId: string): void {
    this.editingAdjust[productId] = false;
  }

  /**
   * Adjusts stock by delta
   */
  adjustStock(productId: string, inputElement: HTMLInputElement): void {
    const adjustment = parseInt(inputElement.value, 10);
    
    if (isNaN(adjustment)) {
      this.toastService.error('Please enter a valid adjustment');
      return;
    }

    this.adminService.adjustStock(productId, adjustment).subscribe({
      next: () => {
        this.toastService.success('Stock adjusted successfully');
        this.editingAdjust[productId] = false;
        this.loadInventory();
      },
      error: (error) => {
        const message = error.error?.message || 'Failed to adjust stock';
        this.toastService.error(message);
      },
    });
  }

  /**
   * Handles page change
   */
  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadInventory();
  }

  /**
   * Checks if set stock is being edited
   */
  isEditingStock(productId: string): boolean {
    return this.editingStock[productId] === true;
  }

  /**
   * Checks if adjust stock is being edited
   */
  isEditingAdjust(productId: string): boolean {
    return this.editingAdjust[productId] === true;
  }
}


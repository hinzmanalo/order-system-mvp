import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { ProductService } from '../../../core/services/product.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ProductFormComponent } from './product-form.component';
import { Product } from '../../../core/models/product.model';

/**
 * Product management component for admin users
 * Allows creating, updating, and toggling product status
 */
@Component({
  selector: 'app-product-management',
  standalone: true,
  imports: [CommonModule, RouterLink, PaginationComponent, ProductFormComponent],
  templateUrl: './product-management.component.html',
  styleUrls: ['./product-management.component.scss'],
})
export class ProductManagementComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly productService = inject(ProductService);
  private readonly toastService = inject(ToastService);

  // State signals
  products = signal<Product[]>([]);
  loading = signal(true);
  showForm = signal(false);
  editingProduct = signal<Product | null>(null);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);

  ngOnInit(): void {
    this.loadProducts();
  }

  /**
   * Loads all products including inactive ones
   */
  loadProducts(): void {
    this.loading.set(true);

    // Load all products via regular endpoint with large page size
    // In a real app, we'd use an admin endpoint that returns all products
    this.productService.getProducts({ 
      page: this.currentPage(), 
      size: 20 
    }).subscribe({
      next: (response) => {
        this.products.set(response.content);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.currentPage.set(response.number);
        this.loading.set(false);
      },
      error: (error) => {
        this.toastService.error('Failed to load products');
        this.loading.set(false);
      },
    });
  }

  /**
   * Opens the form in create mode
   */
  openCreateForm(): void {
    this.editingProduct.set(null);
    this.showForm.set(true);
  }

  /**
   * Opens the form in edit mode
   */
  openEditForm(product: Product): void {
    this.editingProduct.set(product);
    this.showForm.set(true);
  }

  /**
   * Handles form save event
   */
  onFormSaved(): void {
    this.showForm.set(false);
    this.editingProduct.set(null);
    this.loadProducts();
  }

  /**
   * Handles form cancel event
   */
  onFormCancelled(): void {
    this.showForm.set(false);
    this.editingProduct.set(null);
  }

  /**
   * Toggles product active status
   */
  toggleProductStatus(product: Product): void {
    const newStatus = !product.active;
    const action = newStatus ? 'activate' : 'deactivate';
    
    if (!confirm(`Are you sure you want to ${action} "${product.name}"?`)) {
      return;
    }

    this.adminService.updateProductStatus(product.id, newStatus).subscribe({
      next: () => {
        this.toastService.success(`Product ${action}d successfully`);
        this.loadProducts();
      },
      error: (error) => {
        this.toastService.error(`Failed to ${action} product`);
      },
    });
  }

  /**
   * Handles page change
   */
  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadProducts();
  }
}


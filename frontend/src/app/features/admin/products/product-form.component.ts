import { Component, OnInit, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Product, ProductRequest } from '../../../core/models/product.model';

/**
 * Product form component for creating and editing products
 */
@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './product-form.component.html',
  styleUrls: ['./product-form.component.scss'],
})
export class ProductFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);

  @Input() product: Product | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  submitting = signal(false);
  isEditMode = signal(false);

  ngOnInit(): void {
    this.isEditMode.set(this.product !== null);
    this.initializeForm();
  }

  /**
   * Initializes the form with optional pre-filled values
   */
  private initializeForm(): void {
    this.form = this.fb.group({
      name: [this.product?.name || '', [Validators.required, Validators.maxLength(255)]],
      description: [this.product?.description || ''],
      price: [this.product?.price || '', [Validators.required, Validators.min(0.01)]],
      sku: [this.product?.sku || '', [Validators.required, Validators.maxLength(50)]],
    });
  }

  /**
   * Handles form submission
   */
  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const request: ProductRequest = this.form.value;

    const operation = this.isEditMode()
      ? this.adminService.updateProduct(this.product!.id, request)
      : this.adminService.createProduct(request);

    operation.subscribe({
      next: () => {
        const message = this.isEditMode() 
          ? 'Product updated successfully' 
          : 'Product created successfully';
        this.toastService.success(message);
        this.saved.emit();
        this.submitting.set(false);
      },
      error: (error) => {
        const message = error.error?.message || 
          (this.isEditMode() ? 'Failed to update product' : 'Failed to create product');
        this.toastService.error(message);
        this.submitting.set(false);
      },
    });
  }

  /**
   * Handles form cancellation
   */
  onCancel(): void {
    this.cancelled.emit();
  }

  /**
   * Checks if a field has an error and has been touched
   */
  hasError(fieldName: string, errorType: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field?.hasError(errorType) && field?.touched);
  }

  /**
   * Gets the error message for a field
   */
  getErrorMessage(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (!field || !field.touched) return '';

    if (field.hasError('required')) {
      return `${this.getFieldLabel(fieldName)} is required`;
    }
    if (field.hasError('min')) {
      return `${this.getFieldLabel(fieldName)} must be greater than 0`;
    }
    if (field.hasError('maxLength')) {
      const maxLength = field.getError('maxLength').requiredLength;
      return `${this.getFieldLabel(fieldName)} cannot exceed ${maxLength} characters`;
    }
    return '';
  }

  /**
   * Gets user-friendly label for a field
   */
  private getFieldLabel(fieldName: string): string {
    const labels: { [key: string]: string } = {
      name: 'Product name',
      description: 'Description',
      price: 'Price',
      sku: 'SKU',
    };
    return labels[fieldName] || fieldName;
  }
}

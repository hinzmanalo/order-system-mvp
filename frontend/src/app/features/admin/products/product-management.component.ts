import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-product-management',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>Product Management</h2>
        <p>Product management placeholder - will be implemented in phase 15</p>
      </div>
    </div>
  `,
})
export class ProductManagementComponent {}

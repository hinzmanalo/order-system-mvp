import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>Product Catalog</h2>
        <p>Product list component placeholder - will be implemented in phase 13</p>
      </div>
    </div>
  `,
})
export class ProductListComponent {}

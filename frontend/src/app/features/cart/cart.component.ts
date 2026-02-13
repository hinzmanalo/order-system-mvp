import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>Shopping Cart</h2>
        <p>Cart component placeholder - will be implemented in phase 14</p>
      </div>
    </div>
  `,
})
export class CartComponent {}

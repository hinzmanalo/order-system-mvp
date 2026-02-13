import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>Checkout</h2>
        <p>Checkout component placeholder - will be implemented in phase 14</p>
      </div>
    </div>
  `,
})
export class CheckoutComponent {}

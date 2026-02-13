import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>My Orders</h2>
        <p>Order list component placeholder - will be implemented in phase 14</p>
      </div>
    </div>
  `,
})
export class OrderListComponent {}

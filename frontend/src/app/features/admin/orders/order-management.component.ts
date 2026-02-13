import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-order-management',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>Order Management</h2>
        <p>Order management placeholder - will be implemented in phase 15</p>
      </div>
    </div>
  `,
})
export class OrderManagementComponent {}

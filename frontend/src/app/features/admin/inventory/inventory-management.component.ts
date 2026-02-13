import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-inventory-management',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>Inventory Management</h2>
        <p>Inventory management placeholder - will be implemented in phase 15</p>
      </div>
    </div>
  `,
})
export class InventoryManagementComponent {}

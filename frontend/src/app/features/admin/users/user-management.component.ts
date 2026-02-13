import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>User Management</h2>
        <p>User management placeholder - will be implemented in phase 15</p>
      </div>
    </div>
  `,
})
export class UserManagementComponent {}

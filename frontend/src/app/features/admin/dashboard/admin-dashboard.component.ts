import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>Admin Dashboard</h2>
        <p>Admin dashboard placeholder - will be implemented in phase 15</p>
      </div>
    </div>
  `,
})
export class AdminDashboardComponent {}

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

/**
 * Admin dashboard with navigation to all admin management pages
 */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container">
      <div class="dashboard-header">
        <h1>Admin Dashboard</h1>
        <p class="subtitle">Manage products, inventory, users, and orders</p>
      </div>

      <div class="dashboard-cards">
        <a routerLink="/admin/products" class="dashboard-card">
          <div class="card-icon">📦</div>
          <h3>Products</h3>
          <p>Manage product catalog</p>
          <span class="card-arrow">→</span>
        </a>

        <a routerLink="/admin/inventory" class="dashboard-card">
          <div class="card-icon">📊</div>
          <h3>Inventory</h3>
          <p>Manage stock levels</p>
          <span class="card-arrow">→</span>
        </a>

        <a routerLink="/admin/users" class="dashboard-card">
          <div class="card-icon">👥</div>
          <h3>Users</h3>
          <p>Manage user accounts</p>
          <span class="card-arrow">→</span>
        </a>

        <a routerLink="/admin/orders" class="dashboard-card">
          <div class="card-icon">🛒</div>
          <h3>Orders</h3>
          <p>View and manage all orders</p>
          <span class="card-arrow">→</span>
        </a>
      </div>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem;
    }

    .dashboard-header {
      margin-bottom: 3rem;
      text-align: center;
    }

    .dashboard-header h1 {
      font-size: 2.5rem;
      margin-bottom: 0.5rem;
      color: #1a1a1a;
    }

    .subtitle {
      color: #666;
      font-size: 1.1rem;
    }

    .dashboard-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 2rem;
      max-width: 900px;
      margin: 0 auto;
    }

    .dashboard-card {
      display: block;
      padding: 2rem;
      background: white;
      border: 2px solid #e0e0e0;
      border-radius: 12px;
      text-decoration: none;
      color: inherit;
      transition: all 0.3s ease;
      position: relative;
      cursor: pointer;
    }

    .dashboard-card:hover {
      border-color: #007bff;
      transform: translateY(-4px);
      box-shadow: 0 8px 16px rgba(0, 123, 255, 0.1);
    }

    .card-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    .dashboard-card h3 {
      font-size: 1.5rem;
      margin-bottom: 0.5rem;
      color: #1a1a1a;
    }

    .dashboard-card p {
      color: #666;
      margin: 0;
    }

    .card-arrow {
      position: absolute;
      bottom: 1.5rem;
      right: 1.5rem;
      font-size: 1.5rem;
      color: #007bff;
      transition: transform 0.3s ease;
    }

    .dashboard-card:hover .card-arrow {
      transform: translateX(4px);
    }

    @media (max-width: 768px) {
      .dashboard-cards {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class AdminDashboardComponent {}


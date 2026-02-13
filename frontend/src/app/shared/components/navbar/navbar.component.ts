import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <div class="container navbar-container">
        <div class="navbar-brand">
          <a routerLink="/products" class="brand-link">OrderHub</a>
          <button class="hamburger" (click)="toggleMenu()" [class.active]="menuOpen">
            <span></span>
            <span></span>
            <span></span>
          </button>
        </div>
        
        <div class="navbar-menu" [class.active]="menuOpen">
          <div class="navbar-links">
            <a routerLink="/products" routerLinkActive="active" class="nav-link">Products</a>
            
            @if (authService.isAuthenticated()) {
              <a routerLink="/cart" routerLinkActive="active" class="nav-link cart-link">
                Cart
                @if (cartService.cartCount() > 0) {
                  <span class="badge">{{ cartService.cartCount() }}</span>
                }
              </a>
              <a routerLink="/orders" routerLinkActive="active" class="nav-link">Orders</a>
              
              @if (authService.isAdmin()) {
                <a routerLink="/admin" routerLinkActive="active" class="nav-link">Admin</a>
              }
            }
          </div>
          
          <div class="navbar-actions">
            @if (authService.isAuthenticated()) {
              <span class="user-name">{{ authService.currentUser()?.firstName }}</span>
              <button class="btn btn-outline" (click)="logout()">Logout</button>
            } @else {
              <a routerLink="/login" class="btn btn-outline">Login</a>
              <a routerLink="/register" class="btn btn-primary">Register</a>
            }
          </div>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      background-color: var(--color-primary);
      color: white;
      padding: 1rem 0;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .navbar-container {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .navbar-brand {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .brand-link {
      font-size: 1.5rem;
      font-weight: bold;
      color: white;
      text-decoration: none;
    }

    .hamburger {
      display: none;
      flex-direction: column;
      gap: 4px;
      background: none;
      border: none;
      cursor: pointer;
      padding: 0.5rem;
    }

    .hamburger span {
      width: 24px;
      height: 3px;
      background-color: white;
      transition: all 0.3s;
    }

    .navbar-menu {
      display: flex;
      align-items: center;
      gap: 2rem;
    }

    .navbar-links {
      display: flex;
      gap: 1.5rem;
    }

    .nav-link {
      color: white;
      text-decoration: none;
      padding: 0.5rem 1rem;
      border-radius: 4px;
      transition: background-color 0.3s;
      position: relative;
    }

    .nav-link:hover,
    .nav-link.active {
      background-color: rgba(255, 255, 255, 0.1);
    }

    .cart-link {
      position: relative;
    }

    .badge {
      position: absolute;
      top: 0;
      right: 0;
      background-color: var(--color-danger);
      color: white;
      border-radius: 50%;
      padding: 0.2rem 0.5rem;
      font-size: 0.75rem;
      font-weight: bold;
      min-width: 20px;
      text-align: center;
    }

    .navbar-actions {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .user-name {
      font-weight: 500;
    }

    .btn {
      padding: 0.5rem 1rem;
      border-radius: 4px;
      text-decoration: none;
      transition: all 0.3s;
      border: 1px solid white;
      cursor: pointer;
      font-size: 0.9rem;
    }

    .btn-outline {
      background-color: transparent;
      color: white;
    }

    .btn-outline:hover {
      background-color: white;
      color: var(--color-primary);
    }

    .btn-primary {
      background-color: white;
      color: var(--color-primary);
    }

    .btn-primary:hover {
      opacity: 0.9;
    }

    @media (max-width: 768px) {
      .hamburger {
        display: flex;
      }

      .navbar-menu {
        position: absolute;
        top: 100%;
        left: 0;
        right: 0;
        background-color: var(--color-primary);
        flex-direction: column;
        padding: 1rem;
        display: none;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      }

      .navbar-menu.active {
        display: flex;
      }

      .navbar-links,
      .navbar-actions {
        flex-direction: column;
        width: 100%;
      }

      .nav-link {
        width: 100%;
        text-align: center;
      }
    }
  `],
})
export class NavbarComponent {
  protected readonly authService = inject(AuthService);
  protected readonly cartService = inject(CartService);
  
  menuOpen = false;

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  logout(): void {
    this.authService.logout();
    this.menuOpen = false;
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { User } from '../../../core/models/user.model';

/**
 * User management component for admin users
 * Allows viewing users and promoting them to admin
 */
@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, RouterLink, PaginationComponent],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
})
export class UserManagementComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);

  // State signals
  users = signal<User[]>([]);
  loading = signal(true);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);

  ngOnInit(): void {
    this.loadUsers();
  }

  /**
   * Loads all users
   */
  loadUsers(): void {
    this.loading.set(true);

    this.adminService.getUsers({ 
      page: this.currentPage(), 
      size: 20 
    }).subscribe({
      next: (response) => {
        this.users.set(response.content);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.currentPage.set(response.number);
        this.loading.set(false);
      },
      error: (error) => {
        this.toastService.error('Failed to load users');
        this.loading.set(false);
      },
    });
  }

  /**
   * Promotes a user to admin role
   */
  promoteToAdmin(user: User): void {
    if (!confirm(`Are you sure you want to promote ${user.email} to Admin?`)) {
      return;
    }

    this.adminService.updateUserRole(user.id, 'ADMIN').subscribe({
      next: () => {
        this.toastService.success(`${user.email} promoted to Admin successfully`);
        this.loadUsers();
      },
      error: (error) => {
        const message = error.error?.message || 'Failed to promote user';
        this.toastService.error(message);
      },
    });
  }

  /**
   * Handles page change
   */
  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadUsers();
  }

  /**
   * Formats the date for display
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}


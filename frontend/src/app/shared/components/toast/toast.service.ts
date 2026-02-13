import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info';
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private nextId = 0;
  toasts = signal<Toast[]>([]);

  /**
   * Shows a toast notification
   */
  show(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    const toast: Toast = {
      id: this.nextId++,
      message,
      type,
    };

    this.toasts.update((toasts) => [...toasts, toast]);

    // Auto-dismiss after 4 seconds
    setTimeout(() => {
      this.dismiss(toast.id);
    }, 4000);
  }

  /**
   * Dismisses a toast by ID
   */
  dismiss(id: number): void {
    this.toasts.update((toasts) => toasts.filter((t) => t.id !== id));
  }

  /**
   * Convenience methods
   */
  success(message: string): void {
    this.show(message, 'success');
  }

  error(message: string): void {
    this.show(message, 'error');
  }

  info(message: string): void {
    this.show(message, 'info');
  }
}

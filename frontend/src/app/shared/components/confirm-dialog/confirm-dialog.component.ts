import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmDialogService } from './confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (dialogService.dialogData()) {
      <div class="dialog-overlay" (click)="dialogService.onCancel()">
        <div class="dialog-container" (click)="$event.stopPropagation()">
          <div class="dialog-header">
            <h3>Confirm Action</h3>
          </div>
          <div class="dialog-body">
            <p>{{ dialogService.dialogData()?.message }}</p>
          </div>
          <div class="dialog-footer">
            <button class="btn btn-outline" (click)="dialogService.onCancel()">
              Cancel
            </button>
            <button class="btn btn-primary" (click)="dialogService.onConfirm()">
              Confirm
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .dialog-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10000;
      animation: fadeIn 0.2s ease-out;
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
      }
      to {
        opacity: 1;
      }
    }

    .dialog-container {
      background-color: white;
      border-radius: 8px;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
      max-width: 500px;
      width: 90%;
      animation: slideUp 0.3s ease-out;
    }

    @keyframes slideUp {
      from {
        transform: translateY(50px);
        opacity: 0;
      }
      to {
        transform: translateY(0);
        opacity: 1;
      }
    }

    .dialog-header {
      padding: 1.5rem;
      border-bottom: 1px solid var(--color-border);
    }

    .dialog-header h3 {
      margin: 0;
      color: var(--color-text);
    }

    .dialog-body {
      padding: 1.5rem;
    }

    .dialog-body p {
      margin: 0;
      color: var(--color-text);
      line-height: 1.6;
    }

    .dialog-footer {
      padding: 1.5rem;
      border-top: 1px solid var(--color-border);
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
    }

    .btn {
      padding: 0.5rem 1.5rem;
      border-radius: 4px;
      border: none;
      cursor: pointer;
      font-size: 0.9rem;
      transition: all 0.3s;
    }

    .btn-outline {
      background-color: transparent;
      border: 1px solid var(--color-border);
      color: var(--color-text);
    }

    .btn-outline:hover {
      background-color: var(--color-border);
    }

    .btn-primary {
      background-color: var(--color-primary);
      color: white;
    }

    .btn-primary:hover {
      opacity: 0.9;
    }
  `],
})
export class ConfirmDialogComponent {
  protected readonly dialogService = inject(ConfirmDialogService);
}

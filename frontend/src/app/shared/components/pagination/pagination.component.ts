import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="pagination">
      <button 
        class="pagination-btn"
        [disabled]="currentPage === 0"
        (click)="onPageChange(currentPage - 1)"
      >
        Previous
      </button>

      <div class="pagination-info">
        Page {{ currentPage + 1 }} of {{ totalPages || 1 }}
        <span class="total-items">({{ totalElements }} items)</span>
      </div>

      <button 
        class="pagination-btn"
        [disabled]="currentPage >= totalPages - 1"
        (click)="onPageChange(currentPage + 1)"
      >
        Next
      </button>
    </div>
  `,
  styles: [`
    .pagination {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      padding: 1rem 0;
    }

    .pagination-btn {
      padding: 0.5rem 1rem;
      border: 1px solid var(--color-border);
      background-color: white;
      color: var(--color-text);
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.3s;
      font-size: 0.9rem;
    }

    .pagination-btn:hover:not(:disabled) {
      background-color: var(--color-primary);
      color: white;
      border-color: var(--color-primary);
    }

    .pagination-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .pagination-info {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.25rem;
      min-width: 150px;
      text-align: center;
    }

    .total-items {
      font-size: 0.85rem;
      color: #666;
    }

    @media (max-width: 768px) {
      .pagination {
        flex-direction: column;
        gap: 0.5rem;
      }

      .pagination-info {
        order: -1;
      }
    }
  `],
})
export class PaginationComponent {
  @Input() currentPage = 0;
  @Input() totalPages = 0;
  @Input() totalElements = 0;
  @Output() pageChange = new EventEmitter<number>();

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.pageChange.emit(page);
    }
  }
}

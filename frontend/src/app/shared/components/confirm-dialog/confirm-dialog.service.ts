import { Injectable, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';

export interface ConfirmDialogData {
  message: string;
  subject: Subject<boolean>;
}

@Injectable({
  providedIn: 'root',
})
export class ConfirmDialogService {
  dialogData = signal<ConfirmDialogData | null>(null);

  /**
   * Shows a confirmation dialog
   * @returns Observable that emits true if confirmed, false if cancelled
   */
  confirm(message: string): Observable<boolean> {
    const subject = new Subject<boolean>();
    this.dialogData.set({ message, subject });
    return subject.asObservable();
  }

  /**
   * Handles user confirmation
   */
  onConfirm(): void {
    const data = this.dialogData();
    if (data) {
      data.subject.next(true);
      data.subject.complete();
      this.dialogData.set(null);
    }
  }

  /**
   * Handles user cancellation
   */
  onCancel(): void {
    const data = this.dialogData();
    if (data) {
      data.subject.next(false);
      data.subject.complete();
      this.dialogData.set(null);
    }
  }
}

import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class UiFeedbackService {
  private readonly snackBar = inject(MatSnackBar);

  showSuccess(message: string): void {
    this.open(message, 'OK');
  }

  showError(message: string): void {
    this.open(message, 'OK');
  }

  showInfo(message: string): void {
    this.open(message, 'OK');
  }

  private open(message: string, action: string): void {
    this.snackBar.open(message, action, { duration: 3500 });
  }
}

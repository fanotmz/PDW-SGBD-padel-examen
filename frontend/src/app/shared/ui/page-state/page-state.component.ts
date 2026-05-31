import { Component, Input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

export type PageStateType = 'loading' | 'error' | 'empty' | 'success' | 'info';

@Component({
  selector: 'app-page-state',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  templateUrl: './page-state.component.html',
  styleUrl: './page-state.component.css'
})
export class PageStateComponent {
  @Input({ required: true }) message = '';
  @Input() type: PageStateType = 'info';
  @Input() spinnerDiameter = 28;

  protected get isLoading(): boolean {
    return this.type === 'loading';
  }

  protected get role(): string | null {
    return this.type === 'error' ? 'alert' : null;
  }
}

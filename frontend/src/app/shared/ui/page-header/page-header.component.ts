import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.css'
})
export class PageHeaderComponent {
  @Input({ required: true }) eyebrow = '';
  @Input({ required: true }) title = '';
  @Input() description = '';
  @Input() backLink = '';
  @Input() backLabel = 'Retour';
  @Input() backButton = false;
  @Output() backClick = new EventEmitter<void>();

  protected emitBackClick(): void {
    this.backClick.emit();
  }
}

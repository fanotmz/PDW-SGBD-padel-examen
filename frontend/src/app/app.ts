import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UiMessageService } from './core/ui-message.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly uiMessageService = inject(UiMessageService);

  protected clearMessage(): void {
    this.uiMessageService.clear();
  }
}

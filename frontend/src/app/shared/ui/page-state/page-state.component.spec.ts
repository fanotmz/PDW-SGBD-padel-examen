import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageStateComponent } from './page-state.component';

describe('PageStateComponent', () => {
  let fixture: ComponentFixture<PageStateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageStateComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(PageStateComponent);
  });

  it('renders the provided message', () => {
    fixture.componentRef.setInput('message', 'Aucun match à afficher.');
    fixture.componentRef.setInput('type', 'empty');
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucun match à afficher.');
  });

  it('sets alert role for error states', () => {
    fixture.componentRef.setInput('message', 'Impossible de charger les données.');
    fixture.componentRef.setInput('type', 'error');
    fixture.detectChanges();

    const state = fixture.nativeElement.querySelector('.page-state') as HTMLElement | null;

    expect(state?.getAttribute('role')).toBe('alert');
    expect(state?.classList.contains('is-error')).toBe(true);
  });

  it('renders a spinner and message for loading states', () => {
    fixture.componentRef.setInput('message', 'Chargement...');
    fixture.componentRef.setInput('type', 'loading');
    fixture.detectChanges();

    const spinner = fixture.nativeElement.querySelector('mat-spinner');

    expect(spinner).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Chargement...');
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PageHeaderComponent } from './page-header.component';

describe('PageHeaderComponent', () => {
  let fixture: ComponentFixture<PageHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageHeaderComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(PageHeaderComponent);
    fixture.componentRef.setInput('eyebrow', 'Matchs');
    fixture.componentRef.setInput('title', 'Détail du match');
  });

  it('renders the eyebrow, title and optional description', () => {
    fixture.componentRef.setInput('description', 'Consultez les informations du match.');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.eyebrow')?.textContent?.trim()).toBe('Matchs');
    expect(element.querySelector('h1')?.textContent?.trim()).toBe('Détail du match');
    expect(element.querySelector('.page-intro')?.textContent?.trim()).toBe('Consultez les informations du match.');
  });

  it('renders a router back link when backLink is provided', () => {
    fixture.componentRef.setInput('backLink', '/matchs');
    fixture.componentRef.setInput('backLabel', 'Retour aux matchs');
    fixture.detectChanges();

    const backLink = fixture.nativeElement.querySelector('.back-link') as HTMLAnchorElement | null;

    expect(backLink?.tagName).toBe('A');
    expect(backLink?.getAttribute('href')).toBe('/matchs');
    expect(backLink?.textContent?.trim()).toContain('Retour aux matchs');
  });

  it('emits backClick when configured as a button', () => {
    const emitSpy = vi.fn();
    fixture.componentRef.setInput('backButton', true);
    fixture.componentRef.setInput('backLabel', 'Retour');
    fixture.componentInstance.backClick.subscribe(emitSpy);
    fixture.detectChanges();

    const backButton = fixture.nativeElement.querySelector('.back-link') as HTMLButtonElement | null;
    backButton?.click();

    expect(backButton?.tagName).toBe('BUTTON');
    expect(emitSpy).toHaveBeenCalledOnce();
  });
});

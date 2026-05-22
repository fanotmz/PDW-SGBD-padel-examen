import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UiFeedbackService } from './ui-feedback.service';

describe('UiFeedbackService', () => {
  const snackBarMock = {
    open: vi.fn()
  };

  let service: UiFeedbackService;

  beforeEach(() => {
    snackBarMock.open.mockClear();

    TestBed.configureTestingModule({
      providers: [
        UiFeedbackService,
        { provide: MatSnackBar, useValue: snackBarMock }
      ]
    });

    service = TestBed.inject(UiFeedbackService);
  });

  it('shows a success message through MatSnackBar', () => {
    service.showSuccess('Match créé.');

    expect(snackBarMock.open).toHaveBeenCalledWith('Match créé.', 'OK', { duration: 3500 });
  });

  it('shows an error message through MatSnackBar', () => {
    service.showError('Impossible de charger les données.');

    expect(snackBarMock.open).toHaveBeenCalledWith('Impossible de charger les données.', 'OK', {
      duration: 3500
    });
  });

  it('shows an info message through MatSnackBar', () => {
    service.showInfo('Chargement en cours.');

    expect(snackBarMock.open).toHaveBeenCalledWith('Chargement en cours.', 'OK', {
      duration: 3500
    });
  });
});

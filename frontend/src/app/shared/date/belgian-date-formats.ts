import { Provider } from '@angular/core';
import { MAT_DATE_LOCALE, MatDateFormats, provideNativeDateAdapter } from '@angular/material/core';

export const BELGIAN_DATE_FORMATS: MatDateFormats = {
  parse: {
    dateInput: { day: '2-digit', month: '2-digit', year: 'numeric' }
  },
  display: {
    dateInput: { day: '2-digit', month: '2-digit', year: 'numeric' },
    monthYearLabel: { month: 'long', year: 'numeric' },
    dateA11yLabel: { day: 'numeric', month: 'long', year: 'numeric' },
    monthYearA11yLabel: { month: 'long', year: 'numeric' }
  }
};

export const BELGIAN_DATE_PROVIDERS: Provider[] = [
  { provide: MAT_DATE_LOCALE, useValue: 'fr-BE' },
  ...provideNativeDateAdapter(BELGIAN_DATE_FORMATS)
];

export function formatDateForApi(value: Date | string | null | undefined): string {
  if (!value) {
    return '';
  }

  if (typeof value === 'string') {
    return value;
  }

  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

export function parseApiDateForPicker(value: string): Date | null {
  if (!value) {
    return null;
  }

  const [year, month, day] = value.split('-').map((part) => Number(part));

  if (!year || !month || !day) {
    return null;
  }

  return new Date(year, month - 1, day);
}

import {
  BELGIAN_DATE_FORMATS,
  formatDateForApi,
  parseApiDateForPicker
} from './belgian-date-formats';

describe('belgian-date-formats', () => {
  it('exposes Belgian parse and display date formats', () => {
    expect(BELGIAN_DATE_FORMATS.parse.dateInput).toEqual({
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
    expect(BELGIAN_DATE_FORMATS.display.dateInput).toEqual({
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
    expect(BELGIAN_DATE_FORMATS.display.monthYearLabel).toEqual({
      month: 'long',
      year: 'numeric'
    });
    expect(BELGIAN_DATE_FORMATS.display.dateA11yLabel).toEqual({
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
    expect(BELGIAN_DATE_FORMATS.display.monthYearA11yLabel).toEqual({
      month: 'long',
      year: 'numeric'
    });
  });

  describe('formatDateForApi', () => {
    it('returns an empty string for empty values', () => {
      expect(formatDateForApi(null)).toBe('');
      expect(formatDateForApi(undefined)).toBe('');
    });

    it('keeps an already formatted string unchanged', () => {
      expect(formatDateForApi('2026-05-09')).toBe('2026-05-09');
    });

    it('formats a Date as yyyy-MM-dd', () => {
      expect(formatDateForApi(new Date(2026, 4, 9))).toBe('2026-05-09');
    });
  });

  describe('parseApiDateForPicker', () => {
    it('returns null for empty or invalid values', () => {
      expect(parseApiDateForPicker('')).toBeNull();
      expect(parseApiDateForPicker('date-invalide')).toBeNull();
    });

    it('parses an API date as a local Date', () => {
      const result = parseApiDateForPicker('2026-05-09');

      expect(result).toBeInstanceOf(Date);
      expect(result?.getFullYear()).toBe(2026);
      expect(result?.getMonth()).toBe(4);
      expect(result?.getDate()).toBe(9);
    });
  });
});

import { computeConfiguration } from './insurance-configurator.component';

/**
 * Pure premium-calculation tests (M5.2 §35) — deliberately avoid a full
 * TestBed/Material harness, since {@link computeConfiguration} is a pure
 * function exercising exactly the same logic used by the reactive
 * `configuration$` stream.
 */
describe('computeConfiguration', () => {
  it('creates a row for each of the 3 demo coverages and totals 0 when all disabled', () => {
    const result = computeConfiguration([
      { code: 'GAP', enabled: false, option: null },
      { code: 'ASSISTANCE', enabled: false, option: null },
      { code: 'REPLACEMENT_CAR', enabled: false, option: null },
    ]);

    expect(result.coverages.length).toBe(3);
    expect(result.totalMonthlyPremium).toBe(0);
  });

  it('GAP STANDARD contributes 79', () => {
    const result = computeConfiguration([{ code: 'GAP', enabled: true, option: 'STANDARD' }]);
    expect(result.totalMonthlyPremium).toBe(79);
  });

  it('GAP PREMIUM contributes 109', () => {
    const result = computeConfiguration([{ code: 'GAP', enabled: true, option: 'PREMIUM' }]);
    expect(result.totalMonthlyPremium).toBe(109);
  });

  it('Assistance EUROPE contributes 49', () => {
    const result = computeConfiguration([{ code: 'ASSISTANCE', enabled: true, option: 'EUROPE' }]);
    expect(result.totalMonthlyPremium).toBe(49);
  });

  it('Replacement Car contributes a flat 39 when enabled', () => {
    const result = computeConfiguration([{ code: 'REPLACEMENT_CAR', enabled: true, option: null }]);
    expect(result.totalMonthlyPremium).toBe(39);
  });

  it('combined selections sum correctly', () => {
    const result = computeConfiguration([
      { code: 'GAP', enabled: true, option: 'PREMIUM' },
      { code: 'ASSISTANCE', enabled: true, option: 'EUROPE' },
      { code: 'REPLACEMENT_CAR', enabled: true, option: null },
    ]);
    expect(result.totalMonthlyPremium).toBe(109 + 49 + 39);
  });

  it('disabling a coverage removes its premium even if an option value remains set', () => {
    const result = computeConfiguration([{ code: 'GAP', enabled: false, option: 'PREMIUM' }]);
    expect(result.totalMonthlyPremium).toBe(0);
    expect(result.coverages[0].monthlyPremium).toBe(0);
  });

  it('an unselected option on an enabled coverage contributes 0 (option requirement enforced by form validators)', () => {
    const result = computeConfiguration([{ code: 'GAP', enabled: true, option: null }]);
    expect(result.totalMonthlyPremium).toBe(0);
  });
});

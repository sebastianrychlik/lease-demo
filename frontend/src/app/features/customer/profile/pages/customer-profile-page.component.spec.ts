import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CurrentCustomerService } from '../services/current-customer.service';
import { CustomerProfilePageComponent } from './customer-profile-page.component';

describe('CustomerProfilePageComponent', () => {
  let fixture: ComponentFixture<CustomerProfilePageComponent>;

  const sampleProfile = {
    id: '1',
    firstName: 'Jan',
    lastName: 'Kowalski',
    email: 'jan.kowalski@example.com',
    phoneNumber: '+48123456789',
    dateOfBirth: '1944-05-14',
    gender: 'MALE' as const,
    createdAt: '2025-01-01T00:00:00Z',
  };

  async function setup(profile: typeof sampleProfile | null) {
    await TestBed.configureTestingModule({
      imports: [CustomerProfilePageComponent],
      providers: [
        { provide: CurrentCustomerService, useValue: { profile: () => profile } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerProfilePageComponent);
    fixture.detectChanges();
  }

  it('renders the persisted Customer profile fields', async () => {
    await setup(sampleProfile);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Jan Kowalski');
    expect(text).toContain('jan.kowalski@example.com');
    expect(text).toContain('+48123456789');
    expect(text).toContain('1944-05-14');
    expect(text).toContain('Male');
  });

  it('never renders a plaintext PESEL value — only a restrained status', async () => {
    await setup(sampleProfile);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('PESEL');
    expect(text).toContain('Registered');
    // The known sample PESEL for this profile's DOB/gender must never appear.
    expect(text).not.toContain('44051401359');
  });

  it('shows a fallback message when no profile is loaded', async () => {
    await setup(null);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('could not be loaded');
  });
});

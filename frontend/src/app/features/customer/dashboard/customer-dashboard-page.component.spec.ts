import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CurrentCustomerService } from '../profile/services/current-customer.service';
import { CustomerDashboardPageComponent } from './customer-dashboard-page.component';

describe('CustomerDashboardPageComponent', () => {
  let fixture: ComponentFixture<CustomerDashboardPageComponent>;

  async function setup(profile: { firstName: string } | null) {
    await TestBed.configureTestingModule({
      imports: [CustomerDashboardPageComponent],
      providers: [
        { provide: CurrentCustomerService, useValue: { profile: () => profile } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerDashboardPageComponent);
    fixture.detectChanges();
  }

  it('greets the customer by first name when a profile is loaded', async () => {
    await setup({ firstName: 'Jan' });
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Welcome, Jan');
  });

  it('falls back to a generic subtitle when no profile is loaded', async () => {
    await setup(null);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Welcome, ');
    expect(text).toContain('View your leases');
  });
});

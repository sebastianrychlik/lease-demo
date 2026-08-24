import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CustomerLayoutComponent } from './customer-layout.component';
import { AuthService } from '../../core/services/auth.service';

describe('CustomerLayoutComponent', () => {
  let fixture: ComponentFixture<CustomerLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerLayoutComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            username: () => 'customer-user',
            logout: () => {},
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerLayoutComponent);
    fixture.detectChanges();
  });

  it('renders Customer navigation items', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Dashboard');
    expect(text).toContain('My leases');
    expect(text).toContain('Documents');
    expect(text).toContain('My profile');
  });

  it('does not render UX Demo', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('UX Demo');
  });

  it('does not render Admin-only "Customers" navigation', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Customers');
  });
});

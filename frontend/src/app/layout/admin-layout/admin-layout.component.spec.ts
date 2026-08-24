import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AdminLayoutComponent } from './admin-layout.component';
import { AuthService } from '../../core/services/auth.service';

describe('AdminLayoutComponent', () => {
  let fixture: ComponentFixture<AdminLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminLayoutComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            username: () => 'admin-user',
            logout: () => {},
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLayoutComponent);
    fixture.detectChanges();
  });

  it('renders Admin navigation items', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Dashboard');
    expect(text).toContain('Customers');
    expect(text).toContain('Leases');
  });

  it('renders the UX Demo developer link', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('UX Demo');
  });

  it('does not render Customer-only navigation labels', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('My leases');
    expect(text).not.toContain('My profile');
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppHeaderComponent } from './app-header.component';
import { AuthService } from '../../../core/services/auth.service';

describe('AppHeaderComponent', () => {
  let fixture: ComponentFixture<AppHeaderComponent>;
  let logoutSpy: jasmine.Spy;

  beforeEach(async () => {
    logoutSpy = jasmine.createSpy('logout');

    await TestBed.configureTestingModule({
      imports: [AppHeaderComponent],
      providers: [
        {
          provide: AuthService,
          useValue: {
            username: () => 'jane.doe',
            logout: logoutSpy,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppHeaderComponent);
    fixture.detectChanges();
  });

  it('renders the authenticated username', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('jane.doe');
  });

  it('renders the LeaseDemo brand', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('LeaseDemo');
  });

  it('delegates Logout to the existing AuthService', () => {
    const button: HTMLButtonElement = fixture.nativeElement.querySelector(
      '.app-header__logout'
    );
    button.click();
    expect(logoutSpy).toHaveBeenCalled();
  });
});

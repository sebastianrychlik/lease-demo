import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { UxDemoPageComponent } from './ux-demo-page.component';

describe('UxDemoPageComponent', () => {
  let fixture: ComponentFixture<UxDemoPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UxDemoPageComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(UxDemoPageComponent);
    fixture.detectChanges();
  });

  it('renders the page', () => {
    expect(fixture.nativeElement.querySelector('.ux-demo')).toBeTruthy();
  });

  it('composes the real app-page-header control', () => {
    const header: HTMLElement = fixture.nativeElement.querySelector('app-page-header h1');
    expect(header.textContent).toContain('LeaseDemo UX Demo');
  });

  it('composes multiple real app-button variants', () => {
    const buttons = fixture.nativeElement.querySelectorAll('app-button button.app-button');
    expect(buttons.length).toBeGreaterThan(0);
  });

  it('composes real app-card surfaces for the dummy lease customers', () => {
    const cards = fixture.nativeElement.querySelectorAll('.app-card');
    expect(cards.length).toBeGreaterThan(0);
  });

  it('composes real app-input controls', () => {
    const inputs = fixture.nativeElement.querySelectorAll('app-input input');
    expect(inputs.length).toBeGreaterThan(0);
  });
});

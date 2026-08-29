import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ButtonComponent } from './button.component';

@Component({
  standalone: true,
  imports: [ButtonComponent],
  template: `<app-button variant="primary">Save customer</app-button>`,
})
class HostComponent {}

describe('ButtonComponent (projected content)', () => {
  it('renders projected content', async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
    const hostFixture = TestBed.createComponent(HostComponent);
    hostFixture.detectChanges();
    const text = (hostFixture.nativeElement as HTMLElement).querySelector('button')!.textContent;
    expect(text).toContain('Save customer');
  });
});

describe('ButtonComponent', () => {
  let fixture: ComponentFixture<ButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ButtonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ButtonComponent);
  });

  function buttonEl(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button.app-button');
  }

  it('reflects the requested variant', () => {
    fixture.componentRef.setInput('variant', 'danger');
    fixture.detectChanges();
    expect(buttonEl().classList.contains('app-button--danger')).toBeTrue();
    expect(buttonEl().classList.contains('app-button--primary')).toBeFalse();
  });

  it('defaults to the primary variant', () => {
    fixture.detectChanges();
    expect(buttonEl().classList.contains('app-button--primary')).toBeTrue();
  });

  it('applies the native disabled attribute semantically', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    expect(buttonEl().disabled).toBeTrue();
  });

  it('is enabled by default', () => {
    fixture.detectChanges();
    expect(buttonEl().disabled).toBeFalse();
  });
});


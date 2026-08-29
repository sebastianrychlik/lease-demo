import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { InputComponent } from './input.component';

@Component({
  standalone: true,
  imports: [FormsModule, InputComponent],
  template: `<app-input label="Email" [(ngModel)]="email"></app-input>`,
})
class HostComponent {
  email = '';
}

describe('InputComponent', () => {
  let fixture: ComponentFixture<InputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InputComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(InputComponent);
  });

  function nativeInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input');
  }

  it('renders the label', () => {
    fixture.componentRef.setInput('label', 'Email');
    fixture.detectChanges();
    const label: HTMLElement = fixture.nativeElement.querySelector('mat-label');
    expect(label.textContent).toContain('Email');
  });

  it('applies the placeholder', () => {
    fixture.componentRef.setInput('placeholder', 'jane.doe@example.com');
    fixture.detectChanges();
    expect(nativeInput().placeholder).toBe('jane.doe@example.com');
  });

  it('supports the disabled state', async () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(nativeInput().disabled).toBeTrue();
  });

  it('renders the error message when invalid', () => {
    fixture.componentRef.setInput('label', 'PESEL');
    fixture.componentRef.setInput('invalid', true);
    fixture.componentRef.setInput('errorMessage', 'Invalid PESEL');
    fixture.detectChanges();
    fixture.detectChanges();
    const error: HTMLElement | null = fixture.nativeElement.querySelector('mat-error');
    expect(error).toBeTruthy();
    expect(error!.textContent).toContain('Invalid PESEL');
  });
});

describe('InputComponent (number semantics)', () => {
  let fixture: ComponentFixture<InputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InputComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(InputComponent);
    fixture.componentRef.setInput('type', 'number');
    fixture.detectChanges();
  });

  function nativeInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input');
  }

  it('emits a real number (not a string) for "51"', () => {
    let emitted: unknown;
    fixture.componentInstance.registerOnChange((value) => (emitted = value));
    nativeInput().value = '51';
    nativeInput().dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(emitted).toBe(51);
    expect(typeof emitted).toBe('number');
  });

  it('preserves decimals for "7.2"', () => {
    let emitted: unknown;
    fixture.componentInstance.registerOnChange((value) => (emitted = value));
    nativeInput().value = '7.2';
    nativeInput().dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(emitted).toBe(7.2);
  });

  it('emits null when cleared', () => {
    let emitted: unknown = undefined;
    fixture.componentInstance.registerOnChange((value) => (emitted = value));
    nativeInput().value = '';
    nativeInput().dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(emitted).toBeNull();
  });
});

describe('InputComponent (text semantics unaffected)', () => {
  let fixture: ComponentFixture<InputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InputComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(InputComponent);
    fixture.detectChanges();
  });

  it('keeps "51" as a string for type="text"', () => {
    let emitted: unknown;
    fixture.componentInstance.registerOnChange((value) => (emitted = value));
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
    input.value = '51';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(emitted).toBe('51');
  });
});

describe('InputComponent (ngModel integration)', () => {
  let hostFixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HostComponent, NoopAnimationsModule],
    }).compileComponents();

    hostFixture = TestBed.createComponent(HostComponent);
    hostFixture.detectChanges();
  });

  it('writes typed values back to the bound ngModel', async () => {
    const input: HTMLInputElement = hostFixture.nativeElement.querySelector('input');
    input.value = 'jane.doe@example.com';
    input.dispatchEvent(new Event('input'));
    hostFixture.detectChanges();
    await hostFixture.whenStable();
    expect(hostFixture.componentInstance.email).toBe('jane.doe@example.com');
  });
});

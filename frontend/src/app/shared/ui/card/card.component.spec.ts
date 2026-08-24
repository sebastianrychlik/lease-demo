import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CardComponent } from './card.component';

@Component({
  standalone: true,
  imports: [CardComponent],
  template: `
    <app-card>
      <p>Lease #LD-10234 — 2023 Volvo XC60</p>
    </app-card>
  `,
})
class HostComponent {}

describe('CardComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  it('renders projected content inside the card surface', () => {
    const compiled: HTMLElement = fixture.nativeElement;
    const card = compiled.querySelector('.app-card');
    expect(card).toBeTruthy();
    expect(card!.textContent).toContain('Lease #LD-10234');
  });
});

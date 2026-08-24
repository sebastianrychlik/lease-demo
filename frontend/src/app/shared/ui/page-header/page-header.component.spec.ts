import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageHeaderComponent } from './page-header.component';

@Component({
  standalone: true,
  imports: [PageHeaderComponent],
  template: `
    <app-page-header title="Customers" subtitle="Manage leasing customers">
      <button id="add-customer">Add customer</button>
    </app-page-header>
  `,
})
class HostComponent {}

describe('PageHeaderComponent', () => {
  let fixture: ComponentFixture<PageHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageHeaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PageHeaderComponent);
    fixture.componentRef.setInput('title', 'Customers');
  });

  it('renders the title as a semantic heading', () => {
    fixture.detectChanges();
    const heading: HTMLElement = fixture.nativeElement.querySelector('h1.app-page-header__title');
    expect(heading).toBeTruthy();
    expect(heading.textContent).toContain('Customers');
  });

  it('does not render a subtitle when none is provided', () => {
    fixture.detectChanges();
    const subtitle = fixture.nativeElement.querySelector('.app-page-header__subtitle');
    expect(subtitle).toBeNull();
  });

  it('renders the optional subtitle when provided', () => {
    fixture.componentRef.setInput('subtitle', 'Manage leasing customers');
    fixture.detectChanges();
    const subtitle: HTMLElement = fixture.nativeElement.querySelector(
      '.app-page-header__subtitle'
    );
    expect(subtitle.textContent).toContain('Manage leasing customers');
  });

});

describe('PageHeaderComponent (projected actions)', () => {
  let hostFixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HostComponent],
    }).compileComponents();

    hostFixture = TestBed.createComponent(HostComponent);
    hostFixture.detectChanges();
  });

  it('projects action content into the actions area', () => {
    const compiled: HTMLElement = hostFixture.nativeElement;
    const button = compiled.querySelector('.app-page-header__actions #add-customer');
    expect(button).toBeTruthy();
    expect(button!.textContent).toContain('Add customer');
  });
});

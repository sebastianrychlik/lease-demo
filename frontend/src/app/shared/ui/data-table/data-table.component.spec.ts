import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DataTableComponent } from './data-table.component';
import { DataTableColumn, DataTableSortEvent } from './data-table.model';

interface DemoRow {
  name: string;
  amount: string;
}

describe('DataTableComponent', () => {
  let fixture: ComponentFixture<DataTableComponent<DemoRow>>;
  const columns: DataTableColumn<DemoRow>[] = [
    { key: 'name', label: 'Name', sortable: true },
    { key: 'amount', label: 'Amount' },
  ];
  const rows: DemoRow[] = [
    { name: 'Anna', amount: '100' },
    { name: 'Marek', amount: '200' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DataTableComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent<DataTableComponent<DemoRow>>(DataTableComponent);
    fixture.componentRef.setInput('columns', columns);
    fixture.componentRef.setInput('rows', rows);
    fixture.detectChanges();
  });

  it('renders the supplied rows and columns', () => {
    const headerCells = fixture.nativeElement.querySelectorAll('th');
    expect(headerCells.length).toBe(2);
    expect(headerCells[0].textContent).toContain('Name');

    const dataRows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(dataRows.length).toBe(2);
    expect(dataRows[0].textContent).toContain('Anna');
  });

  it('shows a loading indicator when loading() is true', () => {
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeTruthy();
  });

  it('emits sortChange when a sortable header is activated', () => {
    let emitted: DataTableSortEvent | undefined;
    fixture.componentInstance.sortChange.subscribe((event: DataTableSortEvent) => {
      emitted = event;
    });

    const sortHeader: HTMLElement = fixture.nativeElement.querySelector(
      '.mat-sort-header-container',
    );
    sortHeader.click();
    fixture.detectChanges();

    expect(emitted).toBeTruthy();
    expect(emitted?.field).toBe('name');
  });
});

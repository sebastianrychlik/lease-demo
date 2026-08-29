/**
 * UX Demo dummy data — STATIC, frontend-only, fictional.
 *
 * NOT sourced from the backend/Customer API/PostgreSQL. Never contains real
 * personal information. Used exclusively to give /ux-demo's shared/ui
 * examples realistic, leasing-domain-flavored visual content.
 */

export interface DemoLeaseCustomer {
  readonly customerName: string;
  readonly leaseReference: string;
  readonly vehicleDescription: string;
  readonly monthlyAmount: string;
  readonly status: 'Active' | 'Pending' | 'Closed';
}

export const DEMO_LEASE_CUSTOMERS: readonly DemoLeaseCustomer[] = [
  {
    customerName: 'Anna Kowalska',
    leaseReference: 'LD-10234',
    vehicleDescription: '2023 Volvo XC60 B4',
    monthlyAmount: 'PLN 2,450.00',
    status: 'Active',
  },
  {
    customerName: 'Marek Wiśniewski',
    leaseReference: 'LD-10391',
    vehicleDescription: '2022 Škoda Superb Combi',
    monthlyAmount: 'PLN 1,890.00',
    status: 'Pending',
  },
  {
    customerName: 'Katarzyna Nowak',
    leaseReference: 'LD-09980',
    vehicleDescription: '2021 Audi A4 Avant',
    monthlyAmount: 'PLN 2,120.00',
    status: 'Closed',
  },
];

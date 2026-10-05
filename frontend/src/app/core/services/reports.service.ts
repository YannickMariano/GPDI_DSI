import { Injectable, signal } from '@angular/core';
import { ReportTemplate } from '@core/models/report.model';
import { SEED_REPORT_TEMPLATES } from './seed-data';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly _templates = signal<ReportTemplate[]>(SEED_REPORT_TEMPLATES);
  readonly templates = this._templates.asReadonly();
}

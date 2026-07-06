
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';

@Component({
  selector: 'app-planning-kpi',
  standalone: true,
  imports: [CommonModule, TRANSLOCO_IMPORTS],
  templateUrl: './planning-kpi.component.html',
  styleUrls: ['./planning-kpi.component.scss']
})
export class PlanningKpiComponent {
  @Input() halfLifeTime: string | null = null;
  @Input() completionTime: string | null = null;
}

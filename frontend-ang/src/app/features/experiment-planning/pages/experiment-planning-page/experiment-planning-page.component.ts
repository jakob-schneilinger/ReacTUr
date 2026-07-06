import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ConcUnit, PlanningSidebarComponent } from '../../components/planning-sidebar/planning-sidebar.component';
import { PlanningKpiComponent } from '../../components/planning-kpi/planning-kpi.component';
import { PlanningChartComponent } from '../../components/planning-chart/planning-chart.component';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { KineticsCalculatorService, KineticsDataPoint, KineticsParams } from '../../kinetics-calculator.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-experiment-planning-page',
  standalone: true,
  imports: [
    CommonModule,
    TRANSLOCO_IMPORTS,
    PlanningSidebarComponent,
    PlanningKpiComponent,
    PlanningChartComponent
  ],
  templateUrl: './experiment-planning-page.component.html',
  styleUrls: ['./experiment-planning-page.component.scss']
})
export class ExperimentPlanningPageComponent implements OnInit {

  private kineticsService = inject(KineticsCalculatorService);
  private route = inject(ActivatedRoute);

  chartData: KineticsDataPoint[] = [];
  halfLifeTime: string | null = null;
  completionTime: string | null = null;
  initialKValue: number = 75;
  nameA: string = 'A';
  nameB: string = 'B';

  selectedUnit: ConcUnit = 'mM';

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['k']) {
        this.initialKValue = Number(params['k']);
        if (params['nameA']) this.nameA = params['nameA'];
        if (params['nameB']) this.nameB = params['nameB'];
      }
    });
  }

  onSimulate(params: {k: number, a0: number, b0: number, duration: number, unit: ConcUnit}) {

    this.selectedUnit = params.unit;

    const requestParams: KineticsParams = {
      k: params.k,
      a0: params.a0,
      b0: params.b0,
      timeSeconds: params.duration,
      steps: 200
    };

    this.chartData = this.kineticsService.calculateKinetics(requestParams);

    this.calculateKpis(params.k, params.a0, params.b0);
  }

  private calculateKpis(k: number, a0: number, b0: number) {
    const t50 = this.kineticsService.calculateExactTimeForConversion(k, a0, b0, 0.50);
    const t99 = this.kineticsService.calculateExactTimeForConversion(k, a0, b0, 0.99);

    this.halfLifeTime = this.kineticsService.formatTime(t50);
    this.completionTime = this.kineticsService.formatTime(t99);
  }
}

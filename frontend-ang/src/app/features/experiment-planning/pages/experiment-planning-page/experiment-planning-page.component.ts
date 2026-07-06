import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PlanningSidebarComponent } from '../../components/planning-sidebar/planning-sidebar.component';
import { PlanningKpiComponent } from '../../components/planning-kpi/planning-kpi.component';
import { PlanningChartComponent } from '../../components/planning-chart/planning-chart.component';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { KineticsCalculatorService, KineticsDataPoint, KineticsParams } from '../../kinetics-calculator.service';

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
export class ExperimentPlanningPageComponent {

  private kineticsService = inject(KineticsCalculatorService);

  // State für die Child-Components
  chartData: KineticsDataPoint[] = [];
  halfLifeTime: string | null = null;
  completionTime: string | null = null;

  // Wird aufgerufen, wenn in der Sidebar auf "Simulate" geklickt wird
  onSimulate(params: {k: number, a0: number, b0: number, duration: number}) {

    // 1. Daten berechnen (wir nehmen z.B. 200 Schritte für einen smoothen Graphen)
    const requestParams: KineticsParams = {
      k: params.k,
      a0: params.a0,
      b0: params.b0,
      timeSeconds: params.duration,
      steps: 200
    };

    this.chartData = this.kineticsService.calculateKinetics(requestParams);

    // 2. KPIs berechnen (50% und 99% Zeiten aus dem Array fischen)
    this.calculateKpis();
  }

  private calculateKpis() {
    this.halfLifeTime = null;
    this.completionTime = null;

    for (const point of this.chartData) {
      if (!this.halfLifeTime && point.conversion >= 50) {
        this.halfLifeTime = point.timeLabel;
      }
      if (!this.completionTime && point.conversion >= 99) {
        this.completionTime = point.timeLabel;
      }
    }

    // Wenn 99% nicht erreicht wurden im Zeitraum
    if (!this.completionTime) {
      this.completionTime = "> " + this.chartData[this.chartData.length - 1].timeLabel;
    }
  }
}

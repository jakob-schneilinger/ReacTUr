import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxEchartsModule } from 'ngx-echarts';
import { EChartsOption } from 'echarts';
import { KineticsDataPoint } from '../../kinetics-calculator.service';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { ConcUnit } from '../planning-sidebar/planning-sidebar.component';

@Component({
  selector: 'app-planning-chart',
  standalone: true,
  imports: [CommonModule, NgxEchartsModule, TRANSLOCO_IMPORTS],
  templateUrl: './planning-chart.component.html',
  styleUrls: ['./planning-chart.component.scss']
})
export class PlanningChartComponent implements OnChanges {
  @Input() data: KineticsDataPoint[] = [];
  @Input() unit: ConcUnit = 'mM';
  @Input() nameA: string = 'A';
  @Input() nameB: string = 'B';

  chartOptions: EChartsOption = {};

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['data'] || changes['unit']) && this.data.length > 0) {
      this.updateChart();
    }
  }

  private updateChart() {
    const scale = this.unit === 'M' ? 1 : this.unit === 'mM' ? 1e3 : 1e6;
    const u = this.unit;

    const labelA = `Conc [${this.nameA}]`;
    const labelB = `Conc [${this.nameB}]`;

    this.chartOptions = {
      tooltip: {
        trigger: 'axis'
      },
      legend: { data: [labelA, labelB, 'Product', 'Conversion %'] },
      xAxis: {
        type: 'value',
        name: 'Time (min)',
        nameLocation: 'middle',
        nameGap: 30
      },
      yAxis: [
        {
          type: 'value',
          name: `Concentration (${u})`,
          position: 'left'
        },
        {
          type: 'value',
          name: 'Conversion (%)',
          position: 'right',
          max: 100,
          axisLine: { show: true, lineStyle: { color: '#91cc75' } }
        }
      ],
      series: [
        {
          name: labelA, type: 'line',
          data: this.data.map(d => [d.time / 60, d.a * scale]),
          showSymbol: false,
          tooltip: { valueFormatter: (val: any) => Number(val).toFixed(2) + ' ' + u }
        },
        {
          name: labelB, type: 'line',
          data: this.data.map(d => [d.time / 60, d.b * scale]),
          showSymbol: false,
          tooltip: { valueFormatter: (val: any) => Number(val).toFixed(2) + ' ' + u }
        },
        {
          name: 'Product', type: 'line',
          data: this.data.map(d => [d.time / 60, d.product * scale]),
          showSymbol: false,
          tooltip: { valueFormatter: (val: any) => Number(val).toFixed(2) + ' ' + u }
        },
        {
          name: 'Conversion %', type: 'line', yAxisIndex: 1, lineStyle: { type: 'dashed' },
          data: this.data.map(d => [d.time / 60, d.conversion]),
          showSymbol: false,
          tooltip: { valueFormatter: (val: any) => Number(val).toFixed(1) + ' %' }
        }
      ]
    };
  }
}

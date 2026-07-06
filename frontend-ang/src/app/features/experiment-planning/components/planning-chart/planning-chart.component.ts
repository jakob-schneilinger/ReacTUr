
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxEchartsModule } from 'ngx-echarts';
import { EChartsOption } from 'echarts';
import { KineticsDataPoint } from '../../kinetics-calculator.service';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';

@Component({
  selector: 'app-planning-chart',
  standalone: true,
  imports: [CommonModule, NgxEchartsModule, TRANSLOCO_IMPORTS],
  templateUrl: './planning-chart.component.html',
  styleUrls: ['./planning-chart.component.scss']
})
export class PlanningChartComponent implements OnChanges {
  @Input() data: KineticsDataPoint[] = [];

  chartOptions: EChartsOption = {};

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data'] && this.data.length > 0) {
      this.updateChart();
    }
  }

  private updateChart() {
    this.chartOptions = {
      tooltip: {
        trigger: 'axis'
      },
      legend: { data: ['Conc [A]', 'Conc [B]', 'Product', 'Conversion %'] },
      xAxis: {
        type: 'category',
        data: this.data.map(d => d.timeLabel),
        name: 'Time'
      },
      yAxis: [
        {
          type: 'value',
          name: 'Concentration (mM)',
          position: 'left',
          axisLabel: { formatter: (val: number) => (val * 1000).toFixed(1) }
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
          name: 'Conc [A]', type: 'line', data: this.data.map(d => d.a),
          tooltip: { valueFormatter: (val: any) => (val * 1000).toFixed(2) + ' mM' }
        },
        {
          name: 'Conc [B]', type: 'line', data: this.data.map(d => d.b),
          tooltip: { valueFormatter: (val: any) => (val * 1000).toFixed(2) + ' mM' }
        },
        {
          name: 'Product', type: 'line', data: this.data.map(d => d.product),
          tooltip: { valueFormatter: (val: any) => (val * 1000).toFixed(2) + ' mM' }
        },
        {
          name: 'Conversion %', type: 'line', yAxisIndex: 1, lineStyle: { type: 'dashed' },
          data: this.data.map(d => d.conversion),
          tooltip: { valueFormatter: (val: any) => val.toFixed(1) + ' %' }
        }
      ]
    };
  }
}

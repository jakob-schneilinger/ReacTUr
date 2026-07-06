
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxEchartsModule } from 'ngx-echarts';
import { EChartsOption } from 'echarts';
import { KineticsDataPoint } from '../../kinetics-calculator.service';

@Component({
  selector: 'app-planning-chart',
  standalone: true,
  // WICHTIG: NgxEchartsModule importieren
  imports: [CommonModule, NgxEchartsModule],
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
      tooltip: { trigger: 'axis' },
      legend: { data: ['Conc [A]', 'Conc [B]', 'Product', 'Conversion %'] },
      xAxis: {
        type: 'category',
        data: this.data.map(d => d.timeLabel),
        name: 'Time'
      },
      yAxis: [
        {
          type: 'value',
          name: 'Concentration (M)',
          position: 'left',
          axisLabel: { formatter: (val: number) => val.toExponential(2) } // Wissenschaftliche Notation (1e-5)
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
        { name: 'Conc [A]', type: 'line', data: this.data.map(d => d.a) },
        { name: 'Conc [B]', type: 'line', data: this.data.map(d => d.b) },
        { name: 'Product', type: 'line', data: this.data.map(d => d.product) },
        {
          name: 'Conversion %',
          type: 'line',
          yAxisIndex: 1, // Nutzt die rechte Achse
          lineStyle: { type: 'dashed' },
          data: this.data.map(d => d.conversion)
        }
      ]
    };
  }
}

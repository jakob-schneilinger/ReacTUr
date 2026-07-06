import { Injectable } from '@angular/core';

export interface KineticsParams {
  k: number;          // Geschwindigkeitskonstante (M^-1 s^-1)
  a0: number;         // Startkonzentration A (M)
  b0: number;         // Startkonzentration B (M)
  timeSeconds: number;// Wie lange soll simuliert werden? (s)
  steps: number;      // Anzahl der Datenpunkte für den Graphen
}

export interface KineticsDataPoint {
  time: number;
  timeLabel: string;
  a: number;
  b: number;
  product: number;
  conversion: number;
}

@Injectable({
  providedIn: 'root'
})
export class KineticsCalculatorService {

  constructor() { }

  public calculateKinetics(params: KineticsParams): KineticsDataPoint[] {
    const data: KineticsDataPoint[] = [];
    const dt = params.timeSeconds / params.steps;

    const limitingConcentration = Math.min(params.a0, params.b0);

    for (let i = 0; i <= params.steps; i++) {
      const t = i * dt;
      let a_t = 0;
      let b_t = 0;

      if (params.a0 === params.b0) {
        a_t = params.a0 / (1 + params.k * t * params.a0);
        b_t = a_t;
      } else {
        const delta = params.b0 - params.a0;
        const expTerm = Math.exp(-params.k * delta * t);

        a_t = (params.a0 * delta * expTerm) / (params.b0 - params.a0 * expTerm);
        b_t = a_t + delta;
      }

      const product = params.a0 - a_t;
      const conversion = (product / limitingConcentration) * 100;

      data.push({
        time: t,
        timeLabel: this.formatTime(t),
        a: a_t,
        b: b_t,
        product: product,
        conversion: conversion
      });
    }

    return data;
  }

  public calculateExactTimeForConversion(k: number, a0: number, b0: number, conversionTarget: number): number {
    if (a0 === b0) {
      // Formel für [A]0 = [B]0
      return (1 / (k * a0)) * (conversionTarget / (1 - conversionTarget));
    } else {
      // Formel für [A]0 ≠ [B]0 (wie in Excel)
      const lim = Math.min(a0, b0);
      const exc = Math.max(a0, b0);
      const delta = exc - lim;

      return (1 / (k * delta)) * Math.log((exc - conversionTarget * lim) / (exc * (1 - conversionTarget)));
    }
  }

  public formatTime(seconds: number): string {
    if (seconds < 360) return `${seconds.toFixed(0)} s`;
    if (seconds < 7200) return `${(seconds / 60).toFixed(1)} min`;
    return `${(seconds / 3600).toFixed(2)} h`;
  }
}

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
  timeLabel: string; // Formatiert für den Graphen (z.B. "10 min")
  a: number;
  b: number;
  product: number;
  conversion: number; // in Prozent (0 - 100)
}

@Injectable({
  providedIn: 'root'
})
export class KineticsCalculatorService {

  constructor() { }

  public calculateKinetics(params: KineticsParams): KineticsDataPoint[] {
    const data: KineticsDataPoint[] = [];
    const dt = params.timeSeconds / params.steps;

    // Bestimme das limitierende Reagenz für die %-Umsatz Berechnung
    const limitingConcentration = Math.min(params.a0, params.b0);

    for (let i = 0; i <= params.steps; i++) {
      const t = i * dt;
      let a_t = 0;
      let b_t = 0;

      if (params.a0 === params.b0) {
        // Fall 1: Konzentrationen sind gleich (1/[A] = 1/[A]0 + kt)
        a_t = params.a0 / (1 + params.k * t * params.a0);
        b_t = a_t;
      } else {
        // Fall 2: Konzentrationen sind unterschiedlich (wie in deiner Excel)
        const delta = params.b0 - params.a0;
        const expTerm = Math.exp(-params.k * delta * t);

        a_t = (params.a0 * delta * expTerm) / (params.b0 - params.a0 * expTerm);
        b_t = a_t + delta;
      }

      // Produkte und Umsatz berechnen
      const product = params.a0 - a_t; // Wie viel A wurde verbraucht?
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

  // Hilfsfunktion: Sekunden hübsch formatieren für die X-Achse
  private formatTime(seconds: number): string {
    if (seconds < 360) return `${seconds.toFixed(0)} s`;
    if (seconds < 7200) return `${(seconds / 60).toFixed(1)} min`;
    return `${(seconds / 3600).toFixed(2)} h`;
  }
}

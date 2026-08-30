import { Injectable, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { fromEvent, Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class ShortcutService {
  private keySubscription?: Subscription;

  constructor(
    private router: Router,
    private zone: NgZone,
  ) {}

  initPublicShortcuts(): void {
    if (this.keySubscription) return;

    this.keySubscription = fromEvent<KeyboardEvent>(window, 'keydown')
      .pipe(
        // Filter out events from typing in forms or text fields
        filter((event) => !this.isTargetInputField(event)),
      )
      .subscribe((event) => {
        this.handleShortcut(event);
      });
  }

  destroyShortcuts(): void {
    this.keySubscription?.unsubscribe();
  }

  private isTargetInputField(event: KeyboardEvent): boolean {
    const target = event.target as HTMLElement;
    return target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable;
  }

  private handleShortcut(event: KeyboardEvent): void {
    //Alt + E -> Go to Experiment Create Site
    if (event.altKey && event.key.toLowerCase() === 'e') {
      event.preventDefault();
      this.zone.run(() => this.router.navigate(['/experiments/create']));
    }

    // Alt + R -> Go to Reactant Site
    if (event.altKey && event.key.toLowerCase() === 'r') {
      event.preventDefault();
      this.zone.run(() => this.router.navigate(['/reactants']));
    }

    // Alt + S -> Go to Search Site
    if (event.altKey && event.key.toLowerCase() === 's') {
      event.preventDefault();
      this.zone.run(() => this.router.navigate(['/search']));
    }

    // Alt + A -> Go to Admin Site
    if (event.altKey && event.key.toLowerCase() === 'a') {
      event.preventDefault();
      this.zone.run(() => this.router.navigate(['/admin']));
    }

    // Alt + P -> Go to Planning Site
    if (event.altKey && event.key.toLowerCase() === 'p') {
      event.preventDefault();
      this.zone.run(() => this.router.navigate(['/experiment-planning']));
    }

    // Alt + M -> Go to My Experiment Site
    if (event.altKey && event.key.toLowerCase() === 'm') {
      event.preventDefault();
      this.zone.run(() => this.router.navigate(['/my-experiments']));
    }

    // Alt + C -> If on Reactant Site go to Reactant Create Site
    if (event.altKey && event.key.toLowerCase() === 'c' && this.router.url.includes('/reactants')) {
      event.preventDefault();
      this.zone.run(() => this.router.navigate(['/reactants/create']));
    }
  }
}

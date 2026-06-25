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
    //Alt + E -> Create Experiment
    if (event.altKey && event.key.toLowerCase() === 'e') {
      event.preventDefault();
      this.zone.run(() => this.router.navigate(['/experiments/create']));
    }

    // Alt + R -> Create Reactant
    if (event.altKey && event.key.toLowerCase() === 'r') {
      event.preventDefault();
      this.zone.run(() => this.router.navigate(['/reactants/create']));
    }

    // Alt + S -> Go to Search Site
    if (event.altKey && event.key.toLowerCase() === 's') {
      event.preventDefault();
      this.zone.run(() => this.router.navigate(['/search']));
    }
  }
}

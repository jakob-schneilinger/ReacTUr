import { Directive, Input, OnDestroy, OnInit, TemplateRef, ViewContainerRef } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { from, Subscription, switchMap } from 'rxjs';

@Directive({
  selector: '[appHasRole]',
  standalone: true,
})
export class HasRoleDirective implements OnDestroy {
  private subscription: Subscription = new Subscription();
  private hasView = false;

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService,
  ) {}

  @Input() set appHasRole(allowedRoles: string[]) {
    this.subscription.unsubscribe();
    this.subscription = new Subscription();

    this.subscription.add(
      from(this.authService.initialLoginSequence)
        .pipe(
          switchMap(() => this.authService.getLoggedInUser()),
          switchMap(() => this.authService.hasRole(allowedRoles)),
        )
        .subscribe((isAuthorized) => {
          if (isAuthorized && !this.hasView) {
            this.viewContainer.createEmbeddedView(this.templateRef);
            this.hasView = true;
          } else if (!isAuthorized && this.hasView) {
            this.viewContainer.clear();
            this.hasView = false;
          }
        }),
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }
}

import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Globals } from "../../../global/globals";
import { Notification, NotificationService, NotificationType } from "../../../services/notification.service";

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.scss'],
  standalone: false,
})
export class NotificationsComponent implements OnInit {
  closed: boolean = true;
  notifications: Notification[] = [];

  getTypeIcon(type: NotificationType) {
    const types: { [key in NotificationType]: string } = {
      error: 'error bi bi-exclamation-diamond-fill',
      info: 'info bi bi-info-circle-fill',
      log: 'log bi bi-question-circle-fill',
      success: 'success bi bi-check-circle-fill',
      warn: 'warn bi bi-exclamation-triangle-fill',
    };
    return types[type];
  }

  constructor(
    private not: NotificationService,
    private globals: Globals,
    private cdr: ChangeDetectorRef
  ) {
    // inject console
    const injectTo: NotificationType[] = globals.debugMode ? ['error'] : [];
    for (const type of injectTo) {
      const old = console[type as keyof Console];
      console[type as keyof Console] = (...params: any[]) => {
        let e = params[0];

        let message: string;
        let status: string | number = 'console';
        let location: string;

        if (e instanceof ErrorEvent) {
          location = 'Browser';
          message = e.message;
        } else if (e?.error instanceof ErrorEvent) {
          location = 'Backend';
          message = e.error.message;
          status = e.status ?? status;
        } else if (e?.error) {
          location = 'Backend';
          message = typeof e.error === 'string' ? e.error : JSON.stringify(e.error);
          status = e.status ?? status;
        } else {
          if (params[1] && params[1].error) {
            e = params[1];
            status = e.status ?? status;
            location = 'Backend';
            message = typeof e.error === 'string' ? e.error : JSON.stringify(e.error);
          } else {
            location = 'Unknown';
            message = params.join(', ');
          }
        }

        (old as Function).apply(console, params);

        this.spawnNotification({
          type,
          title: `${location} ${type} (${status})`,
          message,
          closeAfter: 5000,
        });
      };
    }
  }

  ngOnInit(): void {
    this.not.notification$.subscribe({
      next: (notification) => this.spawnNotification(notification),
      error: console.error,
    });
  }

  spawnNotification(notification: Notification) {
    if (!notification.type) notification.type = 'log';
    this.notifications.push(notification);

    this.cdr.detectChanges();

    if (!notification.closeAfter) return;
    setTimeout(() => this.close(notification), notification.closeAfter);
  }

  close(notification: Notification) {
    const i = this.notifications.indexOf(notification);
    if (i < 0) return;

    this.notifications.splice(i, 1);

    this.cdr.detectChanges();
  }
}

import { Injectable } from "@angular/core";
import { Subject } from "rxjs";

export type NotificationType = "info" | "log" | "success" | "warn" | "error";

export interface Notification {
  type: NotificationType;
  title: string;
  message?: string;
  closeAfter?: number;
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  public notification$: Subject<Notification> = new Subject<Notification>();

  public info(title: string, message?: string, duration: number = 3000) {
    this.notify(title, 'info', duration, message);
  }

  public log(title: string, message?: string, duration: number = 3000) {
    this.notify(title, 'log', duration, message);
  }

  public success(title: string, message?: string, duration: number = 3500) {
    this.notify(title, 'success', duration, message);
  }

  public warn(title: string, message?: string, duration: number = 5000) {
    this.notify(title, 'warn', duration, message);
  }

  public error(title: string, message?: string, duration: number = 6000) {
    this.notify(title, 'error', duration, message);
  }

  private notify(title: string,type: NotificationType, duration: number, message?: string) {
    this.notification$.next({
      title,
      message,
      type,
      closeAfter: duration,
    });
  }
}

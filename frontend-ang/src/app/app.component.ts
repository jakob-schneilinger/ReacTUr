import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HeaderComponent } from './components/shared/layout/header/header.component';
import { NotificationsComponent } from './components/shared/notification/notifications.component';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationStart, Router } from "@angular/router";
import { AuthService } from './services/auth.service';
import { ShortcutService } from './services/shortcut.service';

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.scss"],
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent, NotificationsComponent],
})
export class AppComponent implements OnInit, OnDestroy{
  title = "ReacTUr";

  showHeader: boolean = true;

  constructor(
    private router: Router,
    private authService: AuthService,
    private shortcutService: ShortcutService
  ) {
    const disableHeaderOn = ["/login",];
    router.events.forEach((event) => {
      if (event instanceof NavigationStart) {
        this.showHeader = !disableHeaderOn.includes(event["url"].split("?")[0]);
      }
    });
  }

  ngOnInit(): void {
    this.shortcutService.initPublicShortcuts();
  }

  ngOnDestroy(): void {
    this.shortcutService.destroyShortcuts();
  }
}

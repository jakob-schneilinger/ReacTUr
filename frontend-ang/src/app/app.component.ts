import { Component } from "@angular/core";
import { NavigationStart, Router } from "@angular/router";
import { AuthService } from './services/auth.service';

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.scss"],
  standalone: false,
})
export class AppComponent {
  title = "ReacTUr";

  showHeader: boolean = true;

  constructor(private router: Router, private authService: AuthService) {
    const disableHeaderOn = ["/login",];
    router.events.forEach((event) => {
      if (event instanceof NavigationStart) {
        this.showHeader = !disableHeaderOn.includes(event["url"].split("?")[0]);
      }
    });
  }
}

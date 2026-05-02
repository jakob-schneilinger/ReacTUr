import { BrowserModule } from "@angular/platform-browser";
import { NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { provideHttpClient, withInterceptorsFromDi } from "@angular/common/http";

import { LoginComponent as UserLoginComponent } from "./components/user/login/login.component";
import { RouterLink } from "@angular/router";
import { UserComponent } from "./components/user/user.component";

import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { HeaderComponent } from "./components/header/header.component";
import { HomeComponent } from "./components/home/home.component";
import { NgbModule } from "@ng-bootstrap/ng-bootstrap";
import { httpInterceptorProviders } from "./interceptors";
import { CommonModule, NgOptimizedImage } from "@angular/common";
import { NotificationsComponent } from "./components/notification/notifications.component";
import { LayoutComponent } from './components/layout/layout.component';

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    HomeComponent,
    UserLoginComponent,
    UserComponent,
    NotificationsComponent,
    LayoutComponent,
  ],
  bootstrap: [AppComponent],
  imports: [
    CommonModule,
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    NgbModule,
    FormsModule,
    RouterLink,
    NgOptimizedImage,
  ],
  providers: [httpInterceptorProviders, provideHttpClient(withInterceptorsFromDi())],
})
export class AppModule {}

import { BrowserModule } from "@angular/platform-browser";
import { NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { HttpClientModule } from "@angular/common/http";

import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { HeaderComponent } from "./components/header/header.component";
import { HomeComponent } from "./components/home/home.component";
import { LoginComponent as UserLoginComponent } from "./components/user/login/login.component";
import { UserComponent } from "./components/user/user.component";
import { NotificationsComponent } from "./components/notification/notifications.component";
import { LayoutComponent } from './components/layout/layout.component';
import { NgOptimizedImage } from "@angular/common";

import { NgbModule } from "@ng-bootstrap/ng-bootstrap";
import { OAuthModule } from "angular-oauth2-oidc";

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
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    HttpClientModule,
    NgOptimizedImage,
    NgbModule,
    OAuthModule.forRoot({
      resourceServer: {
        allowedUrls: ['http://localhost:8080/api'],
        sendAccessToken: true
      }
    }),
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {}

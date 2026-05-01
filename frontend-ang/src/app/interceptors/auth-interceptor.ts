import { Injectable } from "@angular/core";
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from "@angular/common/http";
import { AuthService } from "../services/auth.service";
import { catchError, Observable, throwError } from "rxjs";
import { Globals } from "../global/globals";
import { Router } from "@angular/router";

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  unauthorized: string[];

  constructor(
    private authService: AuthService,
    private globals: Globals,
    private router: Router
  ) {
    this.unauthorized = [
      this.globals.backendUri + "/authentication",
      this.globals.backendUri + "/user/register",
    ];
  }

  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    // Do not intercept unauthorized requests
    if (this.unauthorized.includes(req.url)) {
      return next.handle(req);
    }

    const authReq = req.clone({
      headers: req.headers.set("Authorization", "" + this.authService.getToken()),
    });

    //console.log("Intercepted", this.authService.getToken());

    return next.handle(authReq).pipe(
      catchError((e, caught) => {
        if (e.error instanceof ErrorEvent) {
          // client error
          // console.error("An error occurred:", e.error.message);
        } else {
          //server error
          const { status, error } = e;
          if (status == 401) {
            this.router.navigate(["/login"], {
              queryParams: { goto: this.router.url },
            });
          }
          // console.error(`Backend returned code ${status}, body was: ${error}`);
        }
        return throwError(() => e);
      })
    );
  }
}

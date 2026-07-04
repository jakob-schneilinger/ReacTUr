import { Injectable } from "@angular/core";
import { environment } from "../../../environment/environment";

@Injectable({
  providedIn: "root",
})
export class Globals {
  readonly backendUri: string = this.findBackendUrl();

  readonly debugMode: boolean = true;

  private findBackendUrl(): string {
    if (window.location.port === "4200") {
      // local `ng serve`, backend at localhost:8080
      return `${environment.apiUrl}:8080/api`;
    } else {
      // assume deployed somewhere and backend is available at same host/port as frontend
      return window.location.protocol + "//" + window.location.host + window.location.pathname + "api";
    }
  }
}

// https://stackoverflow.com/questions/35969656/how-can-i-generate-the-opposite-color-according-to-current-color
export function invertColor(hex: string, bw: boolean = false) {
  if (hex.indexOf("#") === 0) hex = hex.slice(1);
  if (hex.length === 3) hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
  if (hex.length !== 6) throw new Error("Invalid HEX color.");

  var r = parseInt(hex.slice(0, 2), 16),
    g = parseInt(hex.slice(2, 4), 16),
    b = parseInt(hex.slice(4, 6), 16);

  if (bw) return r * 0.299 + g * 0.587 + b * 0.114 > 186 ? "#000000" : "#FFFFFF";
  r = 255 - r;
  g = 255 - g;
  b = 255 - b;
  return "#" + r.toString(16).padStart(2, "0") + g.toString(16).padStart(2, "0") + b.toString(16).padStart(2, "0");
}

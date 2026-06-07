import {TestBed} from '@angular/core/testing';

import {AdminService} from './admin.service';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {ReactiveFormsModule} from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('AdminService', () => {
  beforeEach(() => TestBed.configureTestingModule({
    imports: [RouterTestingModule, ReactiveFormsModule],
    providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
}));

  it('should be created', () => {
    const service: AdminService = TestBed.inject(AdminService);
    expect(service).toBeTruthy();
  });
});

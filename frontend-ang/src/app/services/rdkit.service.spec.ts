import {TestBed} from '@angular/core/testing';

import { RDKitService } from './rdkit.service';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {ReactiveFormsModule} from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('RDKitService', () => {
  beforeEach(() => TestBed.configureTestingModule({
    imports: [RouterTestingModule, ReactiveFormsModule],
    providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
}));

  it('should be created', () => {
    const service: RDKitService = TestBed.inject(RDKitService);
    expect(service).toBeTruthy();
  });
});

import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ReactantDetailsFormComponent } from './reactant-details-form.component';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('ReactantDetailsFormComponent', () => {
  let component: ReactantDetailsFormComponent;
  let fixture: ComponentFixture<ReactantDetailsFormComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ReactantDetailsFormComponent],
      imports: [RouterTestingModule, ReactiveFormsModule],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ReactantDetailsFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

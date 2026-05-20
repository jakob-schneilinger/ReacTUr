import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ReactantSiteComponent } from './reactant-site.component';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('ReactantSiteComponent', () => {
  let component: ReactantSiteComponent;
  let fixture: ComponentFixture<ReactantSiteComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ReactantSiteComponent],
      imports: [RouterTestingModule, ReactiveFormsModule],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ReactantSiteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

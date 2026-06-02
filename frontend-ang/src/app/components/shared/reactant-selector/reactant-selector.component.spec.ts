import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ReactantSelectorComponent } from './reactant-selector.component';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('ReactantSelectorComponent', () => {
  let component: ReactantSelectorComponent;
  let fixture: ComponentFixture<ReactantSelectorComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ReactantSelectorComponent],
      imports: [RouterTestingModule, ReactiveFormsModule],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ReactantSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from "@angular/core/testing";

import { AppConfirmModalComponent } from './confirm-modal.component';


describe('AppConfirmModalComponent', () => {
  let component: AppConfirmModalComponent;
  let fixture: ComponentFixture<AppConfirmModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppConfirmModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AppConfirmModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

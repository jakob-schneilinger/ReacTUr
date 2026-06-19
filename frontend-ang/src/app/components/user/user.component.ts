import { Component, OnInit } from "@angular/core";
import { UntypedFormBuilder, UntypedFormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { Globals } from "../../global/globals";

@Component({
  selector: "app-user",
  templateUrl: "./user.component.html",
  styleUrls: ["./user.component.scss"],
  standalone: false,
})
export class UserComponent implements OnInit {
  canManage: boolean = false;
  isOwn: boolean = false;
  sentFriendRequest: boolean = false;

  updateForm: UntypedFormGroup;

  constructor(
    private formBuilder: UntypedFormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private globals: Globals
  ) {
    this.updateForm = this.formBuilder.group({
      displayName: [],
      email: [],
      password: [],
      passwordRepeat: [],
    });
  }

  ngOnInit(): void {
    this.updateForm.reset();
  }
}

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-reactant-details-site',
  templateUrl: './reactant-details-site.component.html',
  styleUrls: ['./reactant-details-site.component.scss'],
  standalone: false,
})
export class ReactantDetailsSiteComponent implements OnInit {
  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
  ) {}

  ngOnInit(): void {
    const state = this.route.snapshot.data['state'];
  }
}

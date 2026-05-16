import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '../../../services/notification.service';
import { ReactantService } from '../../../services/reactant.service';
import { Reactant } from '../../../dtos/reactant';

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
    private reactantService: ReactantService,
    private cd: ChangeDetectorRef,
  ) {}

  reactant: Reactant | undefined;
  loadingStructures: boolean = true;

  ngOnInit() {

    this.route.params.subscribe({
      next: (params) => {
        const shortName = params['name'];
        if (!shortName) {
          this.notification.error("Missing Reactant", "No Reactant found in route!");
          return;
        }
        this.reactantService.getReactant(params["name"]).subscribe({
          next: reactant => {
            this.reactant = reactant;
            this.loadingStructures = true;
            this.reactantService.getReactantStructures(this.reactant.shortName).subscribe({
              next: (files: File[]) => {
                setTimeout(() => {
                  if (this.reactant) {
                    this.reactant.structures = files;
                  }
                  this.loadingStructures = false;
                  this.cd.markForCheck();
                });
              },
              error: (err) => {
                setTimeout(() => {
                  this.loadingStructures = false;
                  this.notification.error(
                    'Missing 3D File',
                    `3D File(s) of Reactant not found! Error: ${err}`,
                  );
                  this.cd.markForCheck();
                });
              },
            });
          },
          error: err => this.notification.error("Invalid Reactant", `No Reactant found with this name: ${shortName}! Error: ${err}`),
        })
      },
      error: err => this.notification.error("Missing Reactant", `No Reactant found in route! Error: ${err}`),
    })
  }
}

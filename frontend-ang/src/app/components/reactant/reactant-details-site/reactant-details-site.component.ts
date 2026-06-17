import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '../../../services/notification.service';
import { ReactantService } from '../../../services/reactant.service';
import { ReactantDto } from '../../../dtos/reactant.dto';

export type FormState = 'create' | 'edit';

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

  reactant: ReactantDto | undefined;
  smiles: string = "";
  loadingStructures: boolean = true;

  state: FormState = 'create';

  structures: File[] = [];

  ngOnInit() {
    const state = this.route.snapshot.data['state'];
    if (state != 'create') {
      this.state = state;

      this.route.params.subscribe({
        next: (params) => {
          const reactantUuid = params['uuid'];
          if (!reactantUuid) {
            this.notification.error('Missing Reactant', 'No Reactant found in route!');
            return;
          }
          this.reactantService.getReactantDetail(reactantUuid).subscribe({
            next: (reactant) => {
              this.reactant = reactant;
              console.log(reactant);
              this.loadingStructures = true;
              this.reactantService.getReactantStructures(reactantUuid).subscribe({
                next: (files: File[]) => {
                  setTimeout(() => {
                    this.structures = files;
                    //TODO: check if needed
                    /*
                    if (this.reactant) {
                      this.reactant.structures = files;
                    }

                     */
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
            error: (err) =>
              this.notification.error(
                'Invalid Reactant',
                `No Reactant found with this id! Error: ${err}`,
              ),
          });
        },
        error: (err) =>
          this.notification.error('Missing Reactant', `No Reactant found in route! Error: ${err}`),
      });
    } else {
      this.state = 'create';
    }
  }

  get isEditMode(): boolean {
    return this.state === 'edit';
  }

  get isCreateMode(): boolean {
    return this.state === 'create';
  }

  updateSmiles(smiles: string) {
    this.loadingStructures = !smiles;
    this.smiles = smiles;
    if (this.reactant) {
      this.reactant.smiles = smiles;
    }
  }

  handleUploadedFiles(files: File[]) {
    this.structures = files;
  }
}

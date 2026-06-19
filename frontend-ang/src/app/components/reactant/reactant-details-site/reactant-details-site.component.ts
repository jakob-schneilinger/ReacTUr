import { CommonModule } from '@angular/common';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { ReactantDetailsFormComponent } from './reactant-details-form/reactant-details-form.component';
import { ReactantStructureComponent } from '../reactant-structure/reactant-structure.component';
import { FileUploadComponent } from '../../shared/file-upload/file-upload.component';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '../../../services/notification.service';
import { ReactantService } from '../../../services/reactant.service';
import { ReactantDto } from '../../../dtos/reactant.dto';
import { filter, map, Subject, switchMap, takeUntil } from 'rxjs';

export type FormState = 'create' | 'edit';

@Component({
  selector: 'app-reactant-details-site',
  templateUrl: './reactant-details-site.component.html',
  styleUrls: ['./reactant-details-site.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    LayoutComponent,
    ReactantDetailsFormComponent,
    ReactantStructureComponent,
    FileUploadComponent
  ],
})
export class ReactantDetailsSiteComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  reactant: ReactantDto | undefined;
  smiles: string = "";
  loadingStructures: boolean = true;
  state: FormState = 'create';
  structures: File[] = [];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
    private reactantService: ReactantService,
    private cd: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.state = this.route.snapshot.data['state'] || 'create';

    if (this.state === 'edit') {
      this.loadReactantData();
    } else {
      this.loadingStructures = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadReactantData() {
    this.route.params.pipe(
      map(params => params['uuid']),
      filter(uuid => !!uuid),
      switchMap(uuid => this.reactantService.getReactantDetail(uuid)),
      switchMap(reactant => {
        this.reactant = reactant;
        this.loadingStructures = true;
        this.cd.markForCheck();
        return this.reactantService.loadReactantStructures(reactant);
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (files) => {
        this.structures = files;
        this.loadingStructures = false;
        this.cd.markForCheck();
      },
      error: (err) => {
        this.loadingStructures = false;
        this.notification.error('Error', err.message || 'Failed to load reactant data');
        this.cd.markForCheck();
      }
    });
  }

  get isEditMode(): boolean {
    return this.state === 'edit';
  }

  get isCreateMode(): boolean {
    return this.state === 'create';
  }

  updateSmiles(smiles: string) {
    this.smiles = smiles;
    if (this.reactant) {
      this.reactant.smiles = smiles;
    }
  }

  handleUploadedFiles(files: File[]) {
    this.structures = files;
  }
}

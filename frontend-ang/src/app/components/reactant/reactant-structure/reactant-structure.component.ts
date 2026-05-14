import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';

declare var $3Dmol: any;

@Component({
  selector: 'app-reactant-structure',
  templateUrl: './reactant-structure.component.html',
  styleUrls: ['./reactant-structure.component.scss'],
  standalone: false,
})
export class ReactantStructureComponent implements AfterViewInit, OnChanges {
  @Input() structure: File | undefined;

  @ViewChild('viewerContainer', { static: true }) viewerContainer!: ElementRef;

  private viewer: any;

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initViewer();
      this.renderModel();
    }, 50);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['structure'] && this.structure) {
      this.renderModel();
    }
  }

  private initViewer(): void {
    if (typeof $3Dmol !== 'undefined') {
      this.viewer = $3Dmol.createViewer(this.viewerContainer.nativeElement, {
        backgroundColor: 'white',
      });
    } else {
      console.error('3Dmol is missing!');
    }
  }

  private async renderModel(): Promise<void> {
    if (!this.viewer || !this.structure) {
      return;
    }

    this.viewer.clear();

    try {
      const xyzDataString = await this.structure.text();

      if (xyzDataString.trim().startsWith('<')) {
        console.error('expected XYZ data but received HTML, check file path');
        return;
      }

      this.viewer.addModel(xyzDataString, 'xyz');
      this.viewer.setStyle({}, { stick: { radius: 0.15 }, sphere: { radius: 0.4 } });
      this.viewer.zoomTo();
      this.viewer.render();

      setTimeout(() => {
        this.viewer.resize();
      }, 100);
    } catch (error) {
      console.error('error rendering the XYZ file:', error);
    }
  }
}

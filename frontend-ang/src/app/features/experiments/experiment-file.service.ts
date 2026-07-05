import { Injectable } from '@angular/core';
import { Globals } from '../../core/global/globals';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
class ExperimentFileService {
  private experimentFileBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
  ) {
    this.experimentFileBaseUri = this.globals.backendUri + '/file'; //TODO: Set correct path
  }


  /**
   * Extracts metadata from a file and returns it as a object.
   * @param file the file to extract metadata from.
   */
  extractMetadataFromFile(file: File): Promise<any | null> {
    return new Promise((resolve, reject) => {
      const blob = file.slice(0, 4096);
      const reader = new FileReader();

      reader.onload = (event) => {
        const content = event.target?.result as string;

        if (!content.startsWith('ProDataCSV')) {
          return resolve(null);
        }

        try {
          const lines = content.split(/\r?\n/).slice(0, 50);
          const metadata: any = {};
          const remarks: string[] = [];
          let inRemarks = false;

          for (let i = 0; i < lines.length; i++) {
            const line = lines[i].trim();

            if (line.startsWith('#Date:')) {
              metadata.date = line.replace('#Date:', '').trim();
            } else if (line.startsWith('#Temperature:')) {
              const match = line.match(/([\d.]+)/);
              if (match) metadata.temperature = parseFloat(match[1]);
            } else if (line.startsWith('#Cell type:')) {
              metadata.instrument = line.replace('#Cell type:', '').trim();
            } else if (line.startsWith('#HV') || line === 'Absorbance') {
              if (line.includes('Absorbance')) metadata.modality = 'Absorbance';
            } else if (line.startsWith('Wavelength,Wavelength:')) {
              const match = line.match(/Wavelength:\s*([\d.]+)/);
              if (match) {
                const wavelength = parseFloat(match[1]);
                if (wavelength == 280) {
                  metadata.wavelength = 535; //TODO: check with chemists for LED wavelength
                } else {
                  metadata.wavelength = parseFloat(match[1]);
                }
              }
            } else if (line === 'Remarks:') {
              inRemarks = true;
            } else if (inRemarks && line === '') {
              inRemarks = false;
            } else if (inRemarks) {
              remarks.push(line);
              const concMatch = line.match(/([\d.]+)\s*mM/i);
              if (concMatch) {
                const val = parseFloat(concMatch[1]);
                if (metadata.concentrationA === undefined) {
                  metadata.concentrationA = val;
                } else if (metadata.concentrationB === undefined) {
                  metadata.concentrationB = val;
                }
              }
            }
          }

          if (remarks.length > 0) {
            metadata.comment = remarks.join('\n');
          }

          resolve(metadata);
        } catch (error) {
          reject(error);
        }
      };

      reader.onerror = (error) => {
        reject(error);
      };

      reader.readAsText(blob);
    });
  }
}

export default ExperimentFileService;

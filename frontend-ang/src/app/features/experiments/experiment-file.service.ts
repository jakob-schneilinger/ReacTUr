import { Injectable } from '@angular/core';
import { Globals } from '../../core/global/globals';
import { HttpClient } from '@angular/common/http';
import { SearchParametersDto } from '../search/search.dto';
import { Observable } from 'rxjs';
import { GetExperimentDto } from './experiment.dto';
import { MOCK_EXPERIMENTS } from '../../mock/mock-experiments';

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

  /*
  uploadFile(experimentUuid: string, file: File): Observable<any> {
    //TODO: remove when backend connected
    return of({ status: 'Mock Upload Success' }).pipe(delay(1000));
    const formData = new FormData();
    formData.append('file', file);
    return this.httpClient.post(`${this.fileBaseUri}/${experimentUuid}/`, formData);
  }

  download()

  getFileDetails()

  removeFile(experimentUuid: string, fileUuid: string): Observable<void> {
    //TODO: remove when backend connected
    return of(undefined).pipe(delay(500));
    return this.httpClient.delete<void>(`${this.fileBaseUri}/${experimentUuid}/${fileUuid}`);
  }
  */

  /**
   * Extracts metadata from a file and returns it as an object.
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

            if (line.startsWith('#')) {
              inRemarks = false;
            }

            if (line.startsWith('#Date:')) {
              metadata.date = line.replace('#Date:', '').trim();
            } else if (line.startsWith('#Temperature:')) {
              // expect "C"
              const match = line.match(/([\d.]+)\s*C/i);
              if (match) metadata.temperature = parseFloat(match[1]);
            } else if (line.startsWith('#User: ')) {
              const user = line.replace('#User: ', '').trim();
              if (user.includes('SX User')) metadata.instrument = 'SX20';
            } else if (line.startsWith('#Cell type:')) {
              metadata.modality = line.replace('#Cell type:', '').trim();
            } else if (line.startsWith('Wavelength,Wavelength:')) {
              // expect 'nm'
              const match = line.match(/Wavelength:\s*([\d.]+)\s*nm/i);
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

              // Extract reactant name and concentration (mM)
              const reactantMatch = line.match(/^([^:]+):.*?([\d.]+)\s*(mM|nM)/i);

              if (reactantMatch) {
                const reactantName = reactantMatch[1].trim();
                const val = parseFloat(reactantMatch[2]);

                if (metadata.concentrationA === undefined) {
                  metadata.reactantA = reactantName;
                  metadata.concentrationA = val;
                } else if (metadata.concentrationB === undefined) {
                  metadata.reactantB = reactantName;
                  metadata.concentrationB = val;
                }
              } else {
                // expect "mM"
                const concMatch = line.match(/([\d.]+)\s*(mM)/i);
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

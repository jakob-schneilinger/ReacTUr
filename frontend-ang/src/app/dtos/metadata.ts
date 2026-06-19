//TODO: overthink interface
export interface Metadata {
  // TODO: We need to update (according to gemini) this code to
  //  send the UUIDs for scientist, solvent, instrument, and modality.
  //  apparently right now the Front-End Formulary sends the names instead
  //  of the UUIDs:
  /* TODO
      The UUID vs. String Mismatch: Your Angular form sends string names for the scientist, solvent,
       instrument, and modality (e.g., "Water" or "Curie"). But the Java SaveMetadataDto expects
       UUIDs. The frontend needs to be updated to capture the selected UUID from the dropdown
       options and send that in the JSON, rather than the raw string.
      The Optional/Required Contradiction: In the Angular UI, the instrument and modality fields
       are visually listed under an "Optional" divider. However, in
       ExperimentDetailsFormComponent.ts, they are initialized with Validators.required.
       The team needs to decide if these are truly optional or mandatory.
      The Scientist Field: The frontend auto-fills the scientist string using
       this.fullName. As we discussed earlier, passing the user identity via the JSON payload is
       a security risk. You should ideally drop this field from the DTO entirely and extract the
       logged-in user's UUID from the Spring Security context in the Service layer.
   */
  date: Date;
  scientist: string;
  temperature: number;
  solvent: string;
  concentrations: number[];
  instrument?: string;
  modality?: string;
  wavelength?: number;
  rawDataFile?: File;
  comment?: string;
}

export interface AdvancedSearch {
  dateDay?: string;
  dateMonth?: string;
  dateYear?: string;
  scientist?: string;
  tempFrom?: string;
  tempTo?: string;
  solvent?: string;
  concentrationA?: string;
  concentrationB?: string;
  instrument?: string;
  modality?: string;
  wavelength?: string;
  comment?: string;
  minRange?: string;
  maxRange?: string;
}

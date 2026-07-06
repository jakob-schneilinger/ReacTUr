export interface ExperimentHistoryDto {
  authorId: string;
  commitDate: string;
  commitType: 'CREATE' | 'UPDATE' | 'ARCHIVE' | 'RESTORE';
  changes: Record<string, string>;
}
